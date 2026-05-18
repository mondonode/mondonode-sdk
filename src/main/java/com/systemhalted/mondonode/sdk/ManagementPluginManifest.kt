package com.systemhalted.mondonode.sdk

import kotlinx.serialization.Serializable

/**
 * Self-description returned by a management app via `IManagementPlugin.getManifest()`.
 *
 * The host reads this after a successful challenge-response authentication to identify the
 * management app and enforce its declared access level on subsequent [IMondoNodeHost] calls.
 *
 * Unlike [PluginManifest], management manifests are intentionally minimal — management apps
 * declare their capabilities through the [IMondoNodeHost] interface they receive via
 * `IManagementPlugin.onHostConnected()`, not through capability lists.
 *
 * Extend [ManagementServiceBase] in your management app's `Service` to handle authentication
 * and host connection automatically; implement `onGetManifest()` to return this type.
 *
 * @property pluginId Stable reverse-DNS package ID (e.g. `"com.example.mymanager"`). Must
 *   match the `applicationId` in the management app's `build.gradle.kts`.
 * @property label Human-readable app name shown in the host UI.
 * @property version Semantic version string (e.g. `"1.0.0"`).
 * @property accessLevel The IPC access level this app requires. Defaults to [ManagementAccessLevel.READ_WRITE]
 *   for backward compatibility. Declare [ManagementAccessLevel.READ_ONLY] for metric-subscriber
 *   apps that never need to modify the pipeline or plugin configuration.
 */
@Serializable
data class ManagementPluginManifest(
    val pluginId: String,
    val label: String,
    val version: String,
    val accessLevel: ManagementAccessLevel = ManagementAccessLevel.READ_WRITE
)
