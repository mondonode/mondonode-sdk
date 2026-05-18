package com.systemhalted.mondonode.sdk

import kotlinx.serialization.Serializable

/**
 * Self-description returned by a monitoring plugin via `IMondoPlugin.getManifest()`.
 *
 * The host reads this after a successful challenge-response authentication to learn which
 * capabilities the plugin exposes, what parameters each accepts, and which other plugins it
 * depends on. The host stores this manifest for the duration of the binding and re-reads it
 * on reconnect.
 *
 * @property pluginId Stable reverse-DNS package ID (e.g. `"com.example.myplugin"`). Must
 *   match the `applicationId` in the plugin's `build.gradle.kts`.
 * @property name Human-readable plugin name shown in the host UI.
 * @property version Semantic version string (e.g. `"1.0.0"`).
 * @property author Plugin author or organization name.
 * @property description Short description shown in the host UI.
 * @property capabilities List of capabilities this plugin exposes. Each capability maps to
 *   one measureable thing the plugin can do.
 * @property publicKeyBase64 Base64-encoded DER EC public key (`secp256r1`). Must match the
 *   key used to sign the authentication challenge in `IMondoPlugin.authenticate()`. The
 *   SDK base class [PluginServiceBase] generates and manages this key automatically in the
 *   Android Keystore.
 * @property pluginDependencies Other monitoring plugins this plugin depends on. The host
 *   evaluates these after authentication and restricts cross-plugin calls via
 *   `IPluginHostBridge` to declared entries only.
 */
@Serializable
data class PluginManifest(
    val pluginId: String,
    val name: String,
    val version: String,
    val author: String,
    val description: String,
    val capabilities: List<PluginCapability>,
    /** Base64-encoded DER EC public key; matches the key used in authenticate(). */
    val publicKeyBase64: String,
    /**
     * Other monitoring plugins this plugin depends on.
     * The host evaluates these after authentication and enforces that cross-plugin
     * calls via IPluginHostBridge are restricted to declared entries only.
     */
    val pluginDependencies: List<PluginDependency> = emptyList()
)
