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
 * Base class for MondoNode storage plugin services.
 *
 * Handles all authentication boilerplate (Keystore key generation, nonce signing)
 * so storage plugin authors only implement the write, query, and stream logic.
 *
 * To create a storage plugin:
 *  1. Extend this class and implement [onGetManifest], [onStreamWrite], [onQueryRead], and
 *     [onQueryWrite]. Optionally override [onStartStream] / [onStopStream] for STREAM_OUTPUT
 *     capabilities.
 *  2. Declare the service in AndroidManifest.xml with:
 *       <intent-filter>
 *           <action android:name="com.systemhalted.mondonode.STORAGE" />
 *       </intent-filter>
 *     and android:exported="true".
 *  3. Include the mondonode-sdk as a dependency.
 *
 * Contract for [onStreamWrite]:
 *  - Fire-and-forget; errors do not fail the monitor.
 *  - Call [IStorageWriteCallback.onSuccess] or [IStorageWriteCallback.onError] exactly once.
 *  - The host imposes a 5-second timeout and swallows errors.
 *
 * Contract for [onQueryRead] and [onQueryWrite]:
 *  - ACID; the host awaits the result with a 10-second timeout.
 *  - Call [IStorageQueryCallback.onResult] with a JSON object (Map<String,String>), or
 *    [IStorageQueryCallback.onError] exactly once.
 *  - Result keys follow transformer output conventions: "value" → resultKey in metric;
 *    other keys k → "${resultKey}.${k}" in metric.
 *  - [onQueryRead] handles QUERY_READ capabilities (reads only; must not mutate stored data).
 *  - [onQueryWrite] handles QUERY_WRITE capabilities (mutations; may also return result data).
 *
 * Contract for [onStartStream]:
 *  - Called synchronously on a Binder thread when the host activates a STREAM_OUTPUT node.
 *  - Must return a non-negative handle that identifies this stream instance, or -1 on failure.
 *  - After returning, the plugin may call [callback.onData] from any thread at any time until
 *    [onStopStream] is called with the same handle.
 *  - Data JSON format: `{"ts": <epoch_ms_long>, "v": <numeric_value_as_string>}`.
 *  - Call [callback.onError] or [callback.onEnd] to signal stream termination; the handle is
 *    invalidated after either call.
 *
 * Contract for [onStopStream]:
 *  - Called synchronously on a Binder thread. Must not throw. No-op for unknown handles.
 *  - After returning, the plugin must not call any method on the previously-associated callback.
 */
abstract class StorageServiceBase : Service() {

    protected val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val binder = object : IStoragePlugin.Stub() {

        override fun authenticate(nonce: ByteArray): String {
            requireCallerIsHost(this@StorageServiceBase)
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            val privateKey = keyStore.getKey(AuthConstants.STORAGE_PLUGIN_KEYSTORE_ALIAS, null) as PrivateKey
            val sig = Signature.getInstance(AuthConstants.SIGNATURE_ALGORITHM).apply {
                initSign(privateKey)
                update(nonce)
            }
            return Base64.encodeToString(sig.sign(), Base64.NO_WRAP)
        }

        override fun getPublicKey(): String {
            requireCallerIsHost(this@StorageServiceBase)
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            val cert = keyStore.getCertificate(AuthConstants.STORAGE_PLUGIN_KEYSTORE_ALIAS)
            return Base64.encodeToString(cert.publicKey.encoded, Base64.NO_WRAP)
        }

        override fun getManifest(): String {
            requireCallerIsHost(this@StorageServiceBase)
            return Json.encodeToString(onGetManifest())
        }

        override fun streamWrite(inputJson: String?, configJson: String?, callback: IStorageWriteCallback?) {
            requireCallerIsHost(this@StorageServiceBase)
            if (inputJson == null || configJson == null || callback == null) return
            serviceScope.launch(Dispatchers.IO) {
                runCatching {
                    val inputValues = Json.decodeFromString<Map<String, String>>(inputJson)
                    val configMap = Json.parseToJsonElement(configJson).jsonObject
                    val capabilityId = configMap["capability_id"]?.jsonPrimitive?.content ?: ""
                    val parameters = configMap
                        .filterKeys { it != "capability_id" }
                        .mapValues { it.value.jsonPrimitive.content }
                    onStreamWrite(capabilityId, inputValues, parameters, callback)
                }.onFailure { e ->
                    runCatching { callback.onError("WRITE_ERROR", e.message ?: "Unknown error") }
                }
            }
        }

        override fun queryRead(inputJson: String?, configJson: String?, callback: IStorageQueryCallback?) {
            requireCallerIsHost(this@StorageServiceBase)
            if (inputJson == null || configJson == null || callback == null) return
            serviceScope.launch(Dispatchers.IO) {
                runCatching {
                    val inputValues = Json.decodeFromString<Map<String, String>>(inputJson)
                    val configMap = Json.parseToJsonElement(configJson).jsonObject
                    val capabilityId = configMap["capability_id"]?.jsonPrimitive?.content ?: ""
                    val parameters = configMap
                        .filterKeys { it != "capability_id" }
                        .mapValues { it.value.jsonPrimitive.content }
                    onQueryRead(capabilityId, inputValues, parameters, callback)
                }.onFailure { e ->
                    runCatching { callback.onError("QUERY_ERROR", e.message ?: "Unknown error") }
                }
            }
        }

        override fun queryWrite(inputJson: String?, configJson: String?, callback: IStorageQueryCallback?) {
            requireCallerIsHost(this@StorageServiceBase)
            if (inputJson == null || configJson == null || callback == null) return
            serviceScope.launch(Dispatchers.IO) {
                runCatching {
                    val inputValues = Json.decodeFromString<Map<String, String>>(inputJson)
                    val configMap = Json.parseToJsonElement(configJson).jsonObject
                    val capabilityId = configMap["capability_id"]?.jsonPrimitive?.content ?: ""
                    val parameters = configMap
                        .filterKeys { it != "capability_id" }
                        .mapValues { it.value.jsonPrimitive.content }
                    onQueryWrite(capabilityId, inputValues, parameters, callback)
                }.onFailure { e ->
                    runCatching { callback.onError("QUERY_ERROR", e.message ?: "Unknown error") }
                }
            }
        }

        override fun startStream(configJson: String?, callback: IStorageStreamCallback?): Int {
            requireCallerIsHost(this@StorageServiceBase)
            if (configJson == null || callback == null) return -1
            return runCatching {
                val configMap = Json.parseToJsonElement(configJson).jsonObject
                val capabilityId = configMap["capability_id"]?.jsonPrimitive?.content ?: ""
                val parameters = configMap
                    .filterKeys { it != "capability_id" }
                    .mapValues { it.value.jsonPrimitive.content }
                onStartStream(capabilityId, parameters, callback)
            }.getOrElse { e ->
                runCatching { callback.onError("START_ERROR", e.message ?: "Unknown error") }
                -1
            }
        }

        override fun stopStream(handle: Int) {
            requireCallerIsHost(this@StorageServiceBase)
            runCatching { onStopStream(handle) }
        }

        override fun onTrigger(handle: Int, value: Boolean) {
            requireCallerIsHost(this@StorageServiceBase)
            runCatching { this@StorageServiceBase.onTrigger(handle, value) }
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
    // Storage plugin implementation hooks
    // -------------------------------------------------------------------------

    /** Return a StorageManifest describing this plugin's id, version, and capabilities. */
    protected abstract fun onGetManifest(): StorageManifest

    /**
     * Handle a fire-and-forget stream write.
     *
     * [capabilityId] identifies which stream write capability to use (e.g. "stream_write_single").
     * [inputValues] is the map from inputJson — for single-key writes this contains {"value": "..."};
     * for map writes it contains the full set of key/value pairs to store.
     * [parameters] contains user-specified config (all keys from configJson except "capability_id").
     * [callback] must receive exactly one onSuccess() or onError() call.
     *
     * This method is called on [Dispatchers.IO]; it may do blocking I/O. For async work use [serviceScope].
     */
    protected abstract fun onStreamWrite(
        capabilityId: String,
        inputValues: Map<String, String>,
        parameters: Map<String, String>,
        callback: IStorageWriteCallback
    )

    /**
     * Handle a QUERY_READ capability — a read-only query that must not mutate stored data.
     *
     * [capabilityId] identifies which read capability to use (e.g. "kv_get", "kv_get_all").
     * [inputValues] is the map from inputJson (typically empty for pure reads).
     * [parameters] contains user-specified config (e.g. "key", "namespace").
     * [callback] must receive exactly one onResult(resultsJson) or onError() call.
     *
     * Result JSON keys follow transformer output conventions:
     *   "value" → merged into metric at resultKey
     *   other key k → merged into metric at "${resultKey}.${k}"
     *
     * This method is called on [Dispatchers.IO]; it may do blocking I/O. For async work use [serviceScope].
     */
    protected abstract fun onQueryRead(
        capabilityId: String,
        inputValues: Map<String, String>,
        parameters: Map<String, String>,
        callback: IStorageQueryCallback
    )

    /**
     * Handle a QUERY_WRITE capability — a mutating operation that may also return result data.
     *
     * [capabilityId] identifies which write capability to use (e.g. "kv_set", "kv_delete").
     * [inputValues] carries data values for the mutation (e.g. {"value": "..."} for kv_set).
     * [parameters] contains user-specified config (e.g. "key", "namespace").
     * [callback] must receive exactly one onResult(resultsJson) or onError() call.
     *
     * Result JSON keys follow transformer output conventions:
     *   "value" → merged into metric at resultKey
     *   other key k → merged into metric at "${resultKey}.${k}"
     *
     * This method is called on [Dispatchers.IO]; it may do blocking I/O. For async work use [serviceScope].
     */
    protected abstract fun onQueryWrite(
        capabilityId: String,
        inputValues: Map<String, String>,
        parameters: Map<String, String>,
        callback: IStorageQueryCallback
    )

    /**
     * Start a STREAM_OUTPUT stream. Called synchronously on a Binder thread.
     *
     * [capabilityId] identifies the STREAM_OUTPUT capability (e.g. "ts_stream").
     * [parameters] contains static config from the pipeline node (e.g. "namespace", "start_mode").
     * [callback] may be called from any thread at any time until [onStopStream] is called.
     *
     * Returns a non-negative handle on success, or -1 on failure (callback.onError is also
     * expected in the failure case). The handle is opaque to the host — use it to identify
     * the stream in [onStopStream].
     *
     * Plugins that do not support STREAM_OUTPUT do not need to override this method; the
     * default implementation reports NOT_SUPPORTED and returns -1.
     */
    protected open fun onStartStream(
        capabilityId: String,
        parameters: Map<String, String>,
        callback: IStorageStreamCallback,
    ): Int {
        callback.onError("NOT_SUPPORTED", "This storage plugin does not support STREAM_OUTPUT")
        return -1
    }

    /**
     * Stop the stream identified by [handle]. Called synchronously on a Binder thread.
     *
     * Must not throw. Must not call any method on the callback after returning.
     * No-op for unknown or already-stopped handles.
     */
    protected open fun onStopStream(handle: Int) = Unit

    /**
     * Called when a boolean value arrives on the "trigger" input port of an active STREAM_OUTPUT
     * stream.  [handle] identifies the stream; [value] is the boolean received.
     *
     * Override this in queue-like plugins to gate flushing: [value]=true → start flushing,
     * [value]=false → pause.  The default implementation is a no-op — storage plugins that
     * do not declare a "trigger" port never receive this call.
     *
     * Called on a Binder thread; must not throw.
     */
    protected open fun onTrigger(handle: Int, value: Boolean) = Unit

    // -------------------------------------------------------------------------

    private fun ensureKeystoreKey() {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (keyStore.containsAlias(AuthConstants.STORAGE_PLUGIN_KEYSTORE_ALIAS)) return

        KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore").apply {
            initialize(
                KeyGenParameterSpec.Builder(
                    AuthConstants.STORAGE_PLUGIN_KEYSTORE_ALIAS,
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
