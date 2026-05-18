package com.systemhalted.mondonode.sdk

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.ECGenParameterSpec

/**
 * Base class for MondoNode Event plugin services.
 *
 * Handles all authentication boilerplate (Keystore key generation, nonce signing)
 * so event plugin authors only implement the event-generation logic.
 *
 * To create an event plugin:
 *  1. Extend this class and implement [onGetManifest], [onStartEvent], and [onStopEvent].
 *  2. Declare the service in AndroidManifest.xml with:
 *       <service android:name=".YourEventService"
 *                android:exported="true"
 *                android:permission="com.systemhalted.mondonode.BIND_EVENT">
 *           <intent-filter>
 *               <action android:name="com.systemhalted.mondonode.EVENT" />
 *           </intent-filter>
 *       </service>
 *  3. Include the mondonode-sdk as a dependency.
 *
 * Contract for [onStartEvent]:
 *  - Called synchronously on a Binder thread. Must return a non-negative handle or -1 on failure.
 *  - After returning, the plugin may call [IEventCallback.onEvent] from any thread at any time
 *    until [onStopEvent] is called with the same handle.
 *  - Call [IEventCallback.onError] to signal a non-recoverable error; the handle is invalidated.
 *  - Use [serviceScope] for coroutines — it is cancelled in [onDestroy].
 *
 * Contract for [onStopEvent]:
 *  - Called synchronously on a Binder thread. Must not throw. No-op for unknown handles.
 *  - After returning, must not call any method on the previously-associated callback, and must
 *    call [IEventCallback.onStopped] to confirm the stop.
 */
abstract class EventServiceBase : Service() {

    protected val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val binder = object : IEventPlugin.Stub() {

        override fun authenticate(nonce: ByteArray): String {
            requireCallerIsHost(this@EventServiceBase)
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            val privateKey = keyStore.getKey(AuthConstants.EVENT_PLUGIN_KEYSTORE_ALIAS, null) as PrivateKey
            val sig = Signature.getInstance(AuthConstants.SIGNATURE_ALGORITHM).apply {
                initSign(privateKey)
                update(nonce)
            }
            return Base64.encodeToString(sig.sign(), Base64.NO_WRAP)
        }

        override fun getPublicKey(): String {
            requireCallerIsHost(this@EventServiceBase)
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            val cert = keyStore.getCertificate(AuthConstants.EVENT_PLUGIN_KEYSTORE_ALIAS)
            return Base64.encodeToString(cert.publicKey.encoded, Base64.NO_WRAP)
        }

        override fun getManifest(): String {
            requireCallerIsHost(this@EventServiceBase)
            return Json.encodeToString(onGetManifest())
        }

        override fun startEvent(configJson: String?, callback: IEventCallback?): Int {
            requireCallerIsHost(this@EventServiceBase)
            if (configJson == null || callback == null) return -1
            return runCatching {
                val configMap = Json.parseToJsonElement(configJson).jsonObject
                val capabilityId = configMap["capability_id"]?.jsonPrimitive?.content ?: ""
                val parameters = configMap
                    .filterKeys { it != "capability_id" }
                    .mapValues { it.value.jsonPrimitive.content }
                onStartEvent(capabilityId, parameters, callback)
            }.getOrElse { e ->
                runCatching { callback?.onError(-1, 1, e.message ?: "Unknown error") }
                -1
            }
        }

        override fun stopEvent(handle: Int) {
            requireCallerIsHost(this@EventServiceBase)
            runCatching { onStopEvent(handle) }
        }
    }

    override fun onCreate() {
        super.onCreate()
        ensureKeystoreKey()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent): IBinder = binder

    // -------------------------------------------------------------------------
    // Event plugin implementation hooks
    // -------------------------------------------------------------------------

    /** Return an EventManifest describing this plugin's id, version, and capabilities. */
    protected abstract fun onGetManifest(): EventManifest

    /**
     * Start one event instance for the given capability. Called synchronously on a Binder thread.
     *
     * [capabilityId] selects which event type to produce.
     * [parameters] contains the user's configuration for this instance.
     * [callback] is the host's receiver — call [IEventCallback.onEvent] when the condition fires.
     *
     * Returns a non-negative handle on success, or -1 on failure (also call callback.onError).
     * Async work should be launched on [serviceScope].
     */
    protected abstract fun onStartEvent(
        capabilityId: String,
        parameters: Map<String, String>,
        callback: IEventCallback,
    ): Int

    /**
     * Stop the event instance identified by [handle]. Called synchronously on a Binder thread.
     *
     * Must not throw. Must call [IEventCallback.onStopped] after cleanup.
     * No-op for unknown handles (no onStopped required in that case).
     */
    protected abstract fun onStopEvent(handle: Int)

    // -------------------------------------------------------------------------

    private fun ensureKeystoreKey() {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (keyStore.containsAlias(AuthConstants.EVENT_PLUGIN_KEYSTORE_ALIAS)) return

        KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore").apply {
            initialize(
                KeyGenParameterSpec.Builder(
                    AuthConstants.EVENT_PLUGIN_KEYSTORE_ALIAS,
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
