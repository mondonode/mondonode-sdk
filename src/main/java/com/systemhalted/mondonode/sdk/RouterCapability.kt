package com.systemhalted.mondonode.sdk

import kotlinx.serialization.Serializable

/**
 * Describes one routing capability offered by a router plugin.
 *
 * [id] is used in RouterStep.capabilityId and in "capability_id" in the configJson
 * passed to IRouterPlugin.evaluate().
 *
 * [supportedConditionTypes] lists which RouterCondition subtype names this capability
 * handles ("string", "numeric", "regex").  Management UIs use this to filter which
 * condition builders to show when a user picks this capability.
 *
 * Output ports for a router node are dynamic — there is one output port per configured
 * case plus one optional default port.  The manifest declares a single dynamic template
 * port; the actual port list for each node instance is stored in
 * [PipelineGraphNode.instanceOutputPorts].
 */
@Serializable
data class RouterCapability(
    val id: String,
    val name: String,
    val description: String,
    val supportedConditionTypes: List<String>,
    val inputPorts: List<PortSpec> = listOf(
        PortSpec("input", "Input", description = "Value to route"),
    ),
    val outputPorts: List<PortSpec> = listOf(
        PortSpec(
            "case_output", "Case Output",
            isDynamic = true,
            description = "One port per configured case; actual ports set on node instance",
        ),
        PortSpec("default", "Default", description = "Taken when no case matches"),
    ),
)
