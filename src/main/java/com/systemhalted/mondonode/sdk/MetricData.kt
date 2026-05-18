package com.systemhalted.mondonode.sdk

import kotlinx.serialization.Serializable

/**
 * A single metric observation produced by a monitoring plugin capability.
 *
 * Passed to `IDataCallback.onDataReceived` by the plugin and stored by the host. All
 * [values] keys are capability-defined strings; there is no fixed schema — consult the
 * producing plugin's [PluginManifest] for field names and their meanings.
 *
 * Timestamps are always **UTC epoch milliseconds**. Never store or compare timestamps using
 * any timezone-aware type — use [System.currentTimeMillis] or `Instant.now().toEpochMilli()`.
 */
@Serializable
data class MetricData(
    /** Package ID of the plugin that produced this metric. */
    val pluginId: String,
    /** Capability ID within the plugin that produced this metric. */
    val capabilityId: String,
    /** UTC epoch milliseconds at the time the metric was produced. */
    val timestampMs: Long,
    /** Arbitrary key-value pairs; plugin defines the schema per capability. */
    val values: Map<String, String>,
    /** Set by the host when the metric is produced by a scheduled monitor execution. */
    val monitorId: String? = null
)
