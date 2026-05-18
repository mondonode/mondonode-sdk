package com.systemhalted.mondonode.sdk

import kotlinx.serialization.Serializable

/**
 * Self-description returned by a storage plugin via `IStoragePlugin.getManifest()`.
 *
 * The host reads this after a successful challenge-response authentication to learn which
 * storage capabilities the plugin exposes. Storage plugins are persistent data stores that
 * pipeline steps can both write to ([StorageCapabilityType.STREAM_WRITE]) and query from
 * ([StorageCapabilityType.QUERY]), unlike output plugins which are write-only sinks.
 *
 * Extend [StorageServiceBase] in your `Service` to handle authentication automatically.
 *
 * @property storageId Stable reverse-DNS package ID (e.g.
 *   `"com.systemhalted.mondonode.storage.kv"`).
 * @property name Human-readable plugin name shown in the host UI.
 * @property version Semantic version string (e.g. `"1.0.0"`).
 * @property author Plugin author or organization name.
 * @property description Short description shown in the host UI.
 * @property capabilities List of capabilities this storage plugin exposes. Each capability
 *   is either a [StorageCapabilityType.STREAM_WRITE] sink or a [StorageCapabilityType.QUERY]
 *   source-and-sink.
 * @property publicKeyBase64 Base64-encoded DER EC public key (`secp256r1`). Managed
 *   automatically by [StorageServiceBase] via the Android Keystore.
 */
@Serializable
data class StorageManifest(
    val storageId: String,
    val name: String,
    val version: String,
    val author: String,
    val description: String,
    val capabilities: List<StorageCapability>,
    val publicKeyBase64: String = ""
)
