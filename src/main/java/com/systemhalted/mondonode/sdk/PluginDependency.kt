package com.systemhalted.mondonode.sdk

import kotlinx.serialization.Serializable

/**
 * Declares a dependency on another monitoring plugin.
 *
 * Included in [PluginManifest.pluginDependencies] to tell the host that this plugin needs
 * to call another plugin's capabilities at runtime via `IPluginHostBridge.requestExecute()`.
 * The host only permits cross-plugin calls to plugins listed here; undeclared calls are
 * rejected regardless of what is installed.
 *
 * Use [OfficialPluginRegistry] constants for [pluginId] when depending on official plugins
 * to avoid hardcoding package name strings.
 *
 * @property pluginId Package ID of the plugin this plugin depends on (e.g.
 *   `"com.systemhalted.mondonode.plugin.icmp"`).
 * @property name Human-readable name shown in the core app UI alongside this dependency.
 * @property required `true` if this plugin cannot function at all without the dependency;
 *   `false` if it degrades gracefully when the dependency is absent.
 * @property reason Short explanation shown to the user explaining why this dependency is
 *   needed (e.g. `"Used to verify host reachability before querying DNS"`).
 */
@Serializable
data class PluginDependency(
    /** Package ID of the plugin this plugin depends on. */
    val pluginId: String,
    /** Human-readable name shown in the core app UI. */
    val name: String,
    /**
     * True = this plugin cannot function without the dependency.
     * False = reduced functionality if absent.
     */
    val required: Boolean,
    /** Short explanation shown to the user explaining why this dependency is needed. */
    val reason: String
)
