package com.systemhalted.mondonode.sdk

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.util.concurrent.atomic.AtomicReference

/**
 * Base class for MondoNode plugin services.
 *
 * Handles all authentication boilerplate (Keystore key generation, nonce signing)
 * so plugin authors only implement the monitoring logic.
 *
 * To create a plugin:
 *  1. Extend this class and implement the abstract methods.
 *  2. Declare the service in AndroidManifest.xml with:
 *       <intent-filter>
 *           <action android:name="com.systemhalted.mondonode.PLUGIN" />
 *       </intent-filter>
 *  3. Include the mondonode-sdk as a dependency.
 */
abstract class PluginServiceBase : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _hostBridge = AtomicReference<IPluginHostBridge?>(null)

    /**
     * The host bridge, available after the host completes authentication and calls
     * setHostBridge(). Use this to invoke declared plugin dependencies via the host.
     * Null until authentication completes, and null again if the host process dies.
     */
    protected val hostBridge: IPluginHostBridge? get() = _hostBridge.get()

    // Retained so we can unlinkToDeath before replacing or on destroy.
    private val _linkedBridgeBinder = AtomicReference<IBinder?>(null)

    private val bridgeDeathRecipient = IBinder.DeathRecipient {
        _hostBridge.set(null)
        _linkedBridgeBinder.set(null)
        onHostBridgeDied()
    }

    private val binder = object : IMondoPlugin.Stub() {

        override fun authenticate(nonce: ByteArray): String {
            requireCallerIsHost(this@PluginServiceBase)
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            val privateKey = keyStore.getKey(AuthConstants.PLUGIN_KEYSTORE_ALIAS, null) as PrivateKey
            val sig = Signature.getInstance(AuthConstants.SIGNATURE_ALGORITHM).apply {
                initSign(privateKey)
                update(nonce)
            }
            return Base64.encodeToString(sig.sign(), Base64.NO_WRAP)
        }

        override fun getPublicKey(): String {
            requireCallerIsHost(this@PluginServiceBase)
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            val cert = keyStore.getCertificate(AuthConstants.PLUGIN_KEYSTORE_ALIAS)
            return Base64.encodeToString(cert.publicKey.encoded, Base64.NO_WRAP)
        }

        override fun getManifest(): String {
            requireCallerIsHost(this@PluginServiceBase)
            return Json.encodeToString(onGetManifest())
        }

        override fun subscribe(callback: IDataCallback?) {
            requireCallerIsHost(this@PluginServiceBase)
            if (callback != null) onSubscribe(callback)
        }

        override fun unsubscribe() {
            requireCallerIsHost(this@PluginServiceBase)
            onUnsubscribe()
        }

        override fun configure(configJson: String?): Boolean {
            requireCallerIsHost(this@PluginServiceBase)
            return if (configJson != null) onConfigure(configJson) else false
        }

        override fun execute(configJson: String?, callback: IDataCallback?) {
            requireCallerIsHost(this@PluginServiceBase)
            if (configJson == null || callback == null) return
            onExecute(configJson, callback)
        }

        override fun setHostBridge(bridge: IPluginHostBridge?) {
            requireCallerIsHost(this@PluginServiceBase)
            if (bridge == null) return
            // Unlink any previous bridge before replacing.
            _linkedBridgeBinder.getAndSet(null)?.unlinkToDeath(bridgeDeathRecipient, 0)
            _hostBridge.set(bridge)
            val binderRef = bridge.asBinder()
            _linkedBridgeBinder.set(binderRef)
            binderRef.linkToDeath(bridgeDeathRecipient, 0)
        }
    }

    override fun onCreate() {
        super.onCreate()
        ensureKeystoreKey()
    }

    override fun onDestroy() {
        super.onDestroy()
        _linkedBridgeBinder.getAndSet(null)?.unlinkToDeath(bridgeDeathRecipient, 0)
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent): IBinder = binder

    // -------------------------------------------------------------------------
    // Plugin implementation hooks
    // -------------------------------------------------------------------------

    /** Return a PluginManifest describing this plugin's id, version, and capabilities. */
    protected abstract fun onGetManifest(): PluginManifest

    /** Host has subscribed; begin pushing metrics via [callback]. */
    protected abstract fun onSubscribe(callback: IDataCallback)

    /** Host has unsubscribed; stop all metric delivery. */
    protected abstract fun onUnsubscribe()

    /**
     * Apply the provided JSON configuration. Return true if applied successfully.
     * The schema is defined by the plugin and communicated via [PluginManifest].
     */
    protected abstract fun onConfigure(configJson: String): Boolean

    /**
     * Run one measurement cycle using [configJson] and deliver exactly one result
     * (success or error) via [callback], then stop.  Must return immediately —
     * any async work should be launched on [serviceScope].
     *
     * The default implementation applies the config, subscribes, waits for the
     * first emission, then unsubscribes.  Override for a more efficient single-shot
     * path (e.g., a one-off network request without starting a polling loop).
     *
     * The host enforces the monitor's timeout independently; the 65-second guard
     * here is a safety net for the plugin-side subscription.
     */
    protected open fun onExecute(configJson: String, callback: IDataCallback) {
        serviceScope.launch {
            onConfigure(configJson)
            val done = CompletableDeferred<Unit>()
            val oneShot = object : IDataCallback.Stub() {
                override fun onMetricReceived(metricJson: String) {
                    if (done.isActive) {
                        runCatching { callback.onMetricReceived(metricJson) }
                        done.complete(Unit)
                    }
                }
                override fun onPluginError(errorCode: String, message: String) {
                    if (done.isActive) {
                        runCatching { callback.onPluginError(errorCode, message) }
                        done.complete(Unit)
                    }
                }
                override fun onPluginStatusChanged(status: String) {}
            }
            onSubscribe(oneShot)
            withTimeoutOrNull(65_000L) { done.await() }
            onUnsubscribe()
        }
    }

    /**
     * Called when the host process has died unexpectedly (crash, OOM kill, or force-stop).
     *
     * At the time this is called, [hostBridge] is already null. Subclasses should stop
     * any in-flight work that depends on the bridge and clean up state. The host will
     * call setHostBridge() again if/when it restarts and re-authenticates.
     *
     * Not called on a clean host unbind — only on process death.
     */
    protected open fun onHostBridgeDied() {}

    // -------------------------------------------------------------------------

    private fun ensureKeystoreKey() {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (keyStore.containsAlias(AuthConstants.PLUGIN_KEYSTORE_ALIAS)) return

        KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore").apply {
            initialize(
                KeyGenParameterSpec.Builder(
                    AuthConstants.PLUGIN_KEYSTORE_ALIAS,
                    KeyProperties.PURPOSE_SIGN
                )
                    .setAlgorithmParameterSpec(ECGenParameterSpec(AuthConstants.EC_CURVE))
                    .setDigests(KeyProperties.DIGEST_SHA256)
                    .build()
            )
            generateKeyPair()
        }
    }
}
