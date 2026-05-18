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
 * Base class for MondoNode transformer plugin services.
 *
 * Handles all authentication boilerplate (Keystore key generation, nonce signing)
 * so transformer authors only implement the transformation logic.
 *
 * To create a transformer plugin:
 *  1. Extend this class and implement [onGetManifest] and [onTransform].
 *  2. Declare the service in AndroidManifest.xml with:
 *       <intent-filter>
 *           <action android:name="com.systemhalted.mondonode.TRANSFORMER" />
 *       </intent-filter>
 *     and android:exported="true".
 *  3. Include the mondonode-sdk as a dependency.
 *
 * Output convention for [onTransform]:
 *   - Always include a "value" key for the primary result.
 *   - Include a "reason" key with a human-readable explanation when relevant
 *     (e.g. why validation failed).
 *   - For multi-valued outputs (e.g. array splits), use string keys "0", "1", … plus
 *     a "count" key.  The host will store these as "<outputKey>.0", "<outputKey>.1", …
 *   - Call ITransformCallback.onResult() exactly once with a JSON-encoded
 *     Map<String, String>, or call onError() on failure.
 */
abstract class TransformerServiceBase : Service() {

    protected val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val binder = object : ITransformerPlugin.Stub() {

        override fun authenticate(nonce: ByteArray): String {
            requireCallerIsHost(this@TransformerServiceBase)
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            val privateKey = keyStore.getKey(AuthConstants.TRANSFORMER_PLUGIN_KEYSTORE_ALIAS, null) as PrivateKey
            val sig = Signature.getInstance(AuthConstants.SIGNATURE_ALGORITHM).apply {
                initSign(privateKey)
                update(nonce)
            }
            return Base64.encodeToString(sig.sign(), Base64.NO_WRAP)
        }

        override fun getPublicKey(): String {
            requireCallerIsHost(this@TransformerServiceBase)
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            val cert = keyStore.getCertificate(AuthConstants.TRANSFORMER_PLUGIN_KEYSTORE_ALIAS)
            return Base64.encodeToString(cert.publicKey.encoded, Base64.NO_WRAP)
        }

        override fun getManifest(): String {
            requireCallerIsHost(this@TransformerServiceBase)
            return Json.encodeToString(onGetManifest())
        }

        override fun transform(inputJson: String?, configJson: String?, callback: ITransformCallback?) {
            requireCallerIsHost(this@TransformerServiceBase)
            if (inputJson == null || configJson == null || callback == null) return
            serviceScope.launch(Dispatchers.IO) {
                runCatching {
                    val inputMap = Json.parseToJsonElement(inputJson).jsonObject
                    val configMap = Json.parseToJsonElement(configJson).jsonObject
                    val inputValue = inputMap["value"]?.jsonPrimitive?.content ?: ""
                    val capabilityId = configMap["capability_id"]?.jsonPrimitive?.content ?: ""
                    val parameters = configMap
                        .filterKeys { it != "capability_id" }
                        .mapValues { it.value.jsonPrimitive.content }
                    onTransform(capabilityId, inputValue, parameters, callback)
                }.onFailure { e ->
                    runCatching { callback.onError("TRANSFORM_ERROR", e.message ?: "Unknown error") }
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
    // Transformer implementation hooks
    // -------------------------------------------------------------------------

    /** Return a TransformerManifest describing this transformer's id, version, and capabilities. */
    protected abstract fun onGetManifest(): TransformerManifest

    /**
     * Perform the transformation identified by [capabilityId].
     *
     * [inputValue] is the primary string input (extracted from inputJson["value"]).
     * [parameters] contains user-specified configuration (all keys from configJson
     * except "capability_id").
     * [callback] must receive exactly one onResult() or onError() call.
     *
     * This method is called on [Dispatchers.IO]; it may do blocking I/O.
     * For async work, use [serviceScope].
     */
    protected abstract fun onTransform(
        capabilityId: String,
        inputValue: String,
        parameters: Map<String, String>,
        callback: ITransformCallback
    )

    // -------------------------------------------------------------------------

    private fun ensureKeystoreKey() {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (keyStore.containsAlias(AuthConstants.TRANSFORMER_PLUGIN_KEYSTORE_ALIAS)) return

        KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore").apply {
            initialize(
                KeyGenParameterSpec.Builder(
                    AuthConstants.TRANSFORMER_PLUGIN_KEYSTORE_ALIAS,
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
