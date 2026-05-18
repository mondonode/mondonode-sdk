package com.systemhalted.mondonode.sdk

import kotlinx.serialization.Serializable

/**
 * Describes one transformation a transformer plugin can perform.
 *
 * [id] is used in TransformerStep.capabilityId and in the "capability_id" key of
 * the configJson passed to ITransformerPlugin.transform().
 *
 * Input and output are always single strings (the "value" key in their respective
 * JSON maps).  [inputDescription] and [outputDescription] are human-readable labels
 * for the management UI.
 *
 * [parameters] describes user-configurable options (e.g. delimiter, format string).
 * Reuses PluginParameterSpec — same type system, validation, and UI rendering.
 *
 * [inputPorts] and [outputPorts] declare the named connection points for this capability
 * in the pipeline graph.  Defaults cover the common single-input / single-output case.
 * Capabilities with fail or debug paths (e.g. validators, parsers) declare additional
 * output ports with [PortSpec.isFailPath] or [PortSpec.isDebugPath] set.  Capabilities
 * whose port count varies by configuration (e.g. multi-output extractors) declare
 * [PortSpec.isDynamic] = true; the actual ports for a node instance are stored in
 * [PipelineGraphNode.instanceOutputPorts].
 */
@Serializable
data class TransformerCapability(
    val id: String,
    val name: String,
    val description: String,
    val inputDescription: String,
    val outputDescription: String,
    val parameters: List<PluginParameterSpec> = emptyList(),
    val inputPorts: List<PortSpec> = listOf(
        PortSpec("input", "Input", description = "Value to transform"),
    ),
    val outputPorts: List<PortSpec> = listOf(
        PortSpec("output", "Output", description = "Transformed result"),
    ),
)
