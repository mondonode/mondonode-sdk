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
import kotlinx.coroutines.launch
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
 * Base class for MondoNode output plugin services.
 *
 * Handles all authentication boilerplate (Keystore key generation, nonce signing)
 * so output plugin authors only implement the write logic.
 *
 * To create an output plugin:
 *  1. Extend this class and implement [onGetManifest] and [onWrite].
 *  2. Declare the service in AndroidManifest.xml with:
 *       <intent-filter>
 *           <action android:name="com.systemhalted.mondonode.OUTPUT" />
 *       </intent-filter>
 *     and android:exported="true".
 *  3. Include the mondonode-sdk as a dependency.
 *
 * Contract for [onWrite]:
 *  - Call [IOutputCallback.onSuccess] exactly once on success.
 *  - Call [IOutputCallback.onError] exactly once on failure.
 *  - The host imposes a 10-second timeout; output errors do not mark the monitor
 *    as failed — they are logged only.
 */
abstract class OutputServiceBase : Service() {

    protected val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val binder = object : IOutputPlugin.Stub() {

        override fun authenticate(nonce: ByteArray): String {
            requireCallerIsHost(this@OutputServiceBase)
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            val privateKey = keyStore.getKey(AuthConstants.OUTPUT_PLUGIN_KEYSTORE_ALIAS, null) as PrivateKey
            val sig = Signature.getInstance(AuthConstants.SIGNATURE_ALGORITHM).apply {
                initSign(privateKey)
                update(nonce)
            }
            return Base64.encodeToString(sig.sign(), Base64.NO_WRAP)
        }

        override fun getPublicKey(): String {
            requireCallerIsHost(this@OutputServiceBase)
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            val cert = keyStore.getCertificate(AuthConstants.OUTPUT_PLUGIN_KEYSTORE_ALIAS)
            return Base64.encodeToString(cert.publicKey.encoded, Base64.NO_WRAP)
        }

        override fun getManifest(): String {
            requireCallerIsHost(this@OutputServiceBase)
            return Json.encodeToString(onGetManifest())
        }

        override fun write(inputJson: String?, configJson: String?, callback: IOutputCallback?) {
            requireCallerIsHost(this@OutputServiceBase)
            if (inputJson == null || configJson == null || callback == null) return
            serviceScope.launch(Dispatchers.IO) {
                runCatching {
                    val inputValues = Json.decodeFromString<Map<String, String>>(inputJson)
                    val configMap = Json.parseToJsonElement(configJson).jsonObject
                    val capabilityId = configMap["capability_id"]?.jsonPrimitive?.content ?: ""
                    val parameters = configMap
                        .filterKeys { it != "capability_id" }
                        .mapValues { it.value.jsonPrimitive.content }
                    onWrite(capabilityId, inputValues, parameters, callback)
                }.onFailure { e ->
                    runCatching { callback.onError("WRITE_ERROR", e.message ?: "Unknown error") }
                }
            }
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
    // Output plugin implementation hooks
    // -------------------------------------------------------------------------

    /** Return an OutputManifest describing this plugin's id, version, and capabilities. */
    protected abstract fun onGetManifest(): OutputManifest

    /**
     * Perform the output operation identified by [capabilityId].
     *
     * [inputValues] is the full metric values map from the monitor execution.
     * [parameters] contains user-specified configuration (all keys from configJson
     * except "capability_id").
     * [callback] must receive exactly one onSuccess() or onError() call.
     *
     * This method is called on [Dispatchers.IO]; it may do blocking I/O.
     * For async work, use [serviceScope].
     */
    protected abstract fun onWrite(
        capabilityId: String,
        inputValues: Map<String, String>,
        parameters: Map<String, String>,
        callback: IOutputCallback
    )

    // -------------------------------------------------------------------------

    private fun ensureKeystoreKey() {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (keyStore.containsAlias(AuthConstants.OUTPUT_PLUGIN_KEYSTORE_ALIAS)) return

        KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore").apply {
            initialize(
                KeyGenParameterSpec.Builder(
                    AuthConstants.OUTPUT_PLUGIN_KEYSTORE_ALIAS,
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
