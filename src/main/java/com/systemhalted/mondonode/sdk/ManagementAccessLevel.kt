package com.systemhalted.mondonode.sdk

import kotlinx.serialization.Serializable

/**
 * Declares the IPC access level a management plugin requires from the host.
 *
 * The host reads this from [ManagementPluginManifest.accessLevel] during authentication and
 * enforces it at every [IMondoNodeHost] call site. This prevents a metric-subscriber app (which
 * only needs read access) from accidentally or maliciously calling write methods such as
 * [IMondoNodeHost.setPipelineGraph] or [IMondoNodeHost.configurePlugin].
 *
 * **Declaring access in a management plugin:**
 * ```kotlin
 * override fun onGetManifest() = ManagementPluginManifest(
 *     pluginId = "com.example.myviewer",
 *     label    = "My Viewer",
 *     version  = "1.0.0",
 *     accessLevel = ManagementAccessLevel.READ_ONLY
 * )
 * ```
 */
@Serializable
enum class ManagementAccessLevel {
    /**
     * The plugin only reads data from the host (metrics, plugin lists, pipeline graph, results).
     * Write methods ([IMondoNodeHost.setPipelineGraph], [IMondoNodeHost.setMonitors],
     * [IMondoNodeHost.configurePlugin]) are blocked by the host and return a failure value.
     */
    READ_ONLY,

    /**
     * The plugin may both read and write. Full access to all [IMondoNodeHost] methods.
     * This is the default and matches the behaviour before access-level enforcement was added.
     */
    READ_WRITE
}
