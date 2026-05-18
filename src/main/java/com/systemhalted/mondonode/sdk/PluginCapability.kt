package com.systemhalted.mondonode.sdk

import kotlinx.serialization.Serializable

@Serializable
enum class PluginCapabilityType {
    NETWORK_MONITOR,
    SYSTEM_MONITOR,
    ANALYZER,
    REPORTER,
    CUSTOM
}

/**
 * [inputQueueMode] declares how this capability's reactive input queue should behave
 * when upstream trigger values arrive faster than the monitor can process them.
 *
 * [InputQueueMode.QUEUED]      — host buffers all triggers; each is a separate execution.
 * [InputQueueMode.UNQUEUED]    — host keeps only the latest trigger value (CONFLATED);
 *                                use when only the current state matters.
 * [InputQueueMode.USER_CHOICE] — plugin has no requirement; user configures per-monitor
 *                                (default).
 *
 * If [QUEUED] or [UNQUEUED] is declared, the host enforces that mode regardless of the
 * per-monitor [MonitorDefinition.inputQueueMode] setting.
 *
 * **Graph port declarations:**
 *
 * [outputPorts] declares what metric data this capability emits when it executes.  The
 * default is a single JSON output port ("output") carrying the full metric values map.
 * Capabilities that produce multiple distinct named values should declare one port per
 * key so the editor can show them as separate, connectable outputs.
 *
 * [inputPorts] declares which parameters this capability is designed to accept as live
 * wire inputs from upstream nodes.  An empty list (the default) means all parameters
 * declared in [parameters] can receive dynamic values via incoming edges — the editor
 * renders a parameter-input port for each [PluginParameterSpec].  Declaring explicit
 * input ports here restricts wiring to only the listed ports, which is useful for
 * capabilities that should not accept all parameters dynamically.
 */
@Serializable
data class PluginCapability(
    val type: PluginCapabilityType,
    val id: String,
    val name: String,
    val description: String,
    val parameters: List<PluginParameterSpec> = emptyList(),
    val inputQueueMode: InputQueueMode = InputQueueMode.USER_CHOICE,
    val outputPorts: List<PortSpec> = listOf(
        PortSpec("output", "Output", dataType = PortDataType.JSON,
            description = "Metric values produced by this monitor execution"),
    ),
    val inputPorts: List<PortSpec> = emptyList(),
)
