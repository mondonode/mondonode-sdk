package com.systemhalted.mondonode.sdk

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Base class for MondoNode management plugin services.
 *
 * Mirrors PluginServiceBase for monitoring plugins. Handles Keystore key generation and
 * nonce signing so management UI authors only implement onGetManifest(), onHostConnected(),
 * and onHostDisconnected().
 *
 * To create a management plugin:
 *  1. Extend this class and implement the abstract methods.
 *  2. Declare the service in AndroidManifest.xml with:
 *       <intent-filter>
 *           <action android:name="com.systemhalted.mondonode.MANAGEMENT_PLUGIN" />
 *       </intent-filter>
 *  3. Include the mondonode-sdk as a dependency.
 */
abstract class ManagementServiceBase : Service() {

    private val _linkedHostBinder = AtomicReference<IBinder?>(null)

    // Guards against onHostDisconnected() firing twice: once from linkToDeath and once from
    // the host calling the AIDL method on a clean disconnect.
    private val _disconnected = AtomicBoolean(false)

    private val hostDeathRecipient = IBinder.DeathRecipient {
        _linkedHostBinder.set(null)
        if (_disconnected.compareAndSet(false, true)) onHostDisconnected()
    }

    private val binder = object : IManagementPlugin.Stub() {

        override fun authenticate(nonce: ByteArray): String {
            requireCallerIsHost(this@ManagementServiceBase)
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            val privateKey = keyStore.getKey(AuthConstants.MANAGEMENT_PLUGIN_KEYSTORE_ALIAS, null) as PrivateKey
            val sig = Signature.getInstance(AuthConstants.SIGNATURE_ALGORITHM).apply {
                initSign(privateKey)
                update(nonce)
            }
            return Base64.encodeToString(sig.sign(), Base64.NO_WRAP)
        }

        override fun getPublicKey(): String {
            requireCallerIsHost(this@ManagementServiceBase)
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            val cert = keyStore.getCertificate(AuthConstants.MANAGEMENT_PLUGIN_KEYSTORE_ALIAS)
            return Base64.encodeToString(cert.publicKey.encoded, Base64.NO_WRAP)
        }

        override fun getManifest(): String {
            requireCallerIsHost(this@ManagementServiceBase)
            return Json.encodeToString(onGetManifest())
        }

        override fun onHostConnected(host: IMondoNodeHost?) {
            requireCallerIsHost(this@ManagementServiceBase)
            if (host == null) return
            _disconnected.set(false)
            val binderRef = host.asBinder()
            _linkedHostBinder.set(binderRef)
            binderRef.linkToDeath(hostDeathRecipient, 0)
            this@ManagementServiceBase.onHostConnected(host)
        }

        override fun onHostDisconnected() {
            requireCallerIsHost(this@ManagementServiceBase)
            _linkedHostBinder.getAndSet(null)?.unlinkToDeath(hostDeathRecipient, 0)
            if (_disconnected.compareAndSet(false, true)) this@ManagementServiceBase.onHostDisconnected()
        }
    }

    override fun onCreate() {
        super.onCreate()
        ensureKeystoreKey()
    }

    override fun onDestroy() {
        super.onDestroy()
        _linkedHostBinder.getAndSet(null)?.unlinkToDeath(hostDeathRecipient, 0)
    }

    override fun onBind(intent: Intent): IBinder = binder

    // -------------------------------------------------------------------------
    // Management plugin implementation hooks
    // -------------------------------------------------------------------------

    /** Return a ManagementPluginManifest describing this management plugin. */
    protected abstract fun onGetManifest(): ManagementPluginManifest

    /** Host has connected and delivered its IMondoNodeHost reference. Register a
     *  management callback and issue commands via [host]. */
    protected abstract fun onHostConnected(host: IMondoNodeHost)

    /** Host has disconnected; release any references to the host binder. */
    protected abstract fun onHostDisconnected()

    // -------------------------------------------------------------------------

    private fun ensureKeystoreKey() {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (keyStore.containsAlias(AuthConstants.MANAGEMENT_PLUGIN_KEYSTORE_ALIAS)) return

        KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore").apply {
            initialize(
                KeyGenParameterSpec.Builder(
                    AuthConstants.MANAGEMENT_PLUGIN_KEYSTORE_ALIAS,
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
