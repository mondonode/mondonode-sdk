package com.systemhalted.mondonode.sdk

import kotlinx.serialization.Serializable

/**
 * Describes one output operation an output plugin can perform.
 *
 * [id] is used in OutputStep.capabilityId and in the "capability_id" key of
 * the configJson passed to IOutputPlugin.write().
 *
 * [inputDescription] is a human-readable label for the management UI describing
 * what data the operation consumes.
 *
 * [parameters] describes user-configurable options (e.g. file path, write mode).
 * Reuses PluginParameterSpec — same type system, validation, and UI rendering.
 *
 * Output plugins are data sinks — they have input ports but no output ports.
 */
@Serializable
data class OutputCapability(
    val id: String,
    val name: String,
    val description: String,
    val inputDescription: String,
    val parameters: List<PluginParameterSpec> = emptyList(),
    val inputPorts: List<PortSpec> = listOf(
        PortSpec("input", "Input", dataType = PortDataType.ANY, description = "Data to write"),
    ),
)
