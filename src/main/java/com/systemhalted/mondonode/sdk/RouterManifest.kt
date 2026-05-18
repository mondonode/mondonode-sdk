package com.systemhalted.mondonode.sdk

import kotlinx.serialization.Serializable

/**
 * Describes a router plugin — its identity, capabilities, and the public key
 * used for challenge-response authentication.
 *
 * Delivered as JSON via IRouterPlugin.getManifest().
 *
 * [routerId] must be globally unique; use the APK package name by convention.
 * [publicKeyBase64] is the Base64-encoded DER EC public key matching the Keystore
 * key the router uses in authenticate().  The host verifies it matches the live
 * signing key before accepting any evaluate() request.
 */
@Serializable
data class RouterManifest(
    val routerId: String,
    val name: String,
    val version: String,
    val author: String,
    val description: String,
    val capabilities: List<RouterCapability>,
    val publicKeyBase64: String
)
