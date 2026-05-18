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
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.ECGenParameterSpec

/**
 * Base class for MondoNode router plugin services.
 *
 * Handles all authentication boilerplate (Keystore key generation, nonce signing)
 * so router authors only implement condition evaluation logic.
 *
 * To create a router plugin:
 *  1. Extend this class and implement [onGetManifest] and [onEvaluate].
 *  2. Declare the service in AndroidManifest.xml with:
 *       <intent-filter>
 *           <action android:name="com.systemhalted.mondonode.ROUTER" />
 *       </intent-filter>
 *     and android:exported="true".
 *  3. Include the mondonode-sdk as a dependency.
 *
 * Responsibility split:
 *   - Plugin ([onEvaluate]): evaluate each condition against the input, return matched indices.
 *   - Host: execute the branch sub-pipelines for the matched case indices.
 *
 * [onEvaluate] is called on [Dispatchers.IO] and may do blocking work.
 * Call [IRouteCallback.onMatched], [IRouteCallback.onNoMatch], or [IRouteCallback.onError]
 * exactly once.
 */
abstract class RouterServiceBase : Service() {

    protected val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val binder = object : IRouterPlugin.Stub() {

        override fun authenticate(nonce: ByteArray): String {
            requireCallerIsHost(this@RouterServiceBase)
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            val privateKey = keyStore.getKey(AuthConstants.ROUTER_PLUGIN_KEYSTORE_ALIAS, null) as PrivateKey
            val sig = Signature.getInstance(AuthConstants.SIGNATURE_ALGORITHM).apply {
                initSign(privateKey)
                update(nonce)
            }
            return Base64.encodeToString(sig.sign(), Base64.NO_WRAP)
        }

        override fun getPublicKey(): String {
            requireCallerIsHost(this@RouterServiceBase)
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            val cert = keyStore.getCertificate(AuthConstants.ROUTER_PLUGIN_KEYSTORE_ALIAS)
            return Base64.encodeToString(cert.publicKey.encoded, Base64.NO_WRAP)
        }

        override fun getManifest(): String {
            requireCallerIsHost(this@RouterServiceBase)
            return Json.encodeToString(onGetManifest())
        }

        override fun evaluate(inputJson: String?, configJson: String?, callback: IRouteCallback?) {
            requireCallerIsHost(this@RouterServiceBase)
            if (inputJson == null || configJson == null || callback == null) return
            serviceScope.launch(Dispatchers.IO) {
                runCatching {
                    val inputMap = Json.parseToJsonElement(inputJson).jsonObject
                    val configMap = Json.parseToJsonElement(configJson).jsonObject

                    val inputValue = inputMap["value"]?.jsonPrimitive?.content ?: ""
                    val capabilityId = configMap["capability_id"]?.jsonPrimitive?.content ?: ""
                    val matchMode = configMap["match_mode"]?.jsonPrimitive?.content
                        ?.let { runCatching { MatchMode.valueOf(it) }.getOrDefault(MatchMode.FIRST) }
                        ?: MatchMode.FIRST

                    val conditions: List<RouterCondition> = configMap["conditions"]
                        ?.jsonArray
                        ?.map { Json.decodeFromJsonElement(RouterCondition.serializer(), it) }
                        ?: emptyList()

                    onEvaluate(capabilityId, inputValue, conditions, matchMode, callback)
                }.onFailure { e ->
                    runCatching { callback.onError("EVALUATE_ERROR", e.message ?: "Unknown error") }
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
    // Router implementation hooks
    // -------------------------------------------------------------------------

    /** Return a RouterManifest describing this router's id, version, and capabilities. */
    protected abstract fun onGetManifest(): RouterManifest

    /**
     * Evaluate [conditions] against [inputValue] and report which indices matched.
     *
     * [capabilityId] identifies which routing capability to use.
     * [conditions] is the ordered list of conditions from RouterStep.cases — one per case.
     *   Index 0 corresponds to cases[0], index 1 to cases[1], etc.
     * [matchMode] controls whether to stop after the first match or collect all matches.
     * [callback] must receive exactly one call: onMatched(), onNoMatch(), or onError().
     *
     * Matched indices must be reported in ascending order.
     * If no condition matches, call onNoMatch() (not onMatched with an empty list).
     *
     * This method is called on [Dispatchers.IO]; it may do blocking work.
     */
    protected abstract fun onEvaluate(
        capabilityId: String,
        inputValue: String,
        conditions: List<RouterCondition>,
        matchMode: MatchMode,
        callback: IRouteCallback
    )

    // -------------------------------------------------------------------------

    private fun ensureKeystoreKey() {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (keyStore.containsAlias(AuthConstants.ROUTER_PLUGIN_KEYSTORE_ALIAS)) return

        KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore").apply {
            initialize(
                KeyGenParameterSpec.Builder(
                    AuthConstants.ROUTER_PLUGIN_KEYSTORE_ALIAS,
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
