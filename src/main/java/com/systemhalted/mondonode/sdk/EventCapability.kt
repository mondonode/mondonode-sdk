package com.systemhalted.mondonode.sdk

import kotlinx.serialization.Serializable

/**
 * One event-generating capability within an Event plugin.
 *
 * [id] is stable and unique within the plugin (e.g. "simple_timer", "motion_detected").
 * [outputPorts] describes what this capability emits.  Event capabilities always
 * have at least one boolean "output" port; they may declare additional ports for
 * richer data (e.g. a sensor reading alongside the trigger boolean).
 * [parameters] are the configuration knobs exposed to the user in the graph editor.
 */
@Serializable
data class EventCapability(
    val id: String,
    val name: String,
    val description: String = "",
    val outputPorts: List<PortSpec> = listOf(
        PortSpec("output", "Output", PortDataType.BOOLEAN, description = "Emits true when the event condition is met.")
    ),
    val parameters: List<PluginParameterSpec> = emptyList(),
)
