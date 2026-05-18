package com.systemhalted.mondonode.sdk

import kotlinx.serialization.Serializable

/**
 * Describes an output plugin — its identity, capabilities, and the public key
 * used for challenge-response authentication.
 *
 * Delivered as JSON via IOutputPlugin.getManifest().
 *
 * [outputId] must be globally unique; use the APK package name by convention.
 * [publicKeyBase64] is the Base64-encoded DER EC public key matching the Keystore
 * key the plugin uses in authenticate().  The host verifies it matches the live
 * signing key before accepting any write request.
 */
@Serializable
data class OutputManifest(
    val outputId: String,
    val name: String,
    val version: String,
    val author: String,
    val description: String,
    val capabilities: List<OutputCapability>,
    val publicKeyBase64: String
)
