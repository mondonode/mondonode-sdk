package com.systemhalted.mondonode.sdk

import kotlinx.serialization.Serializable

/**
 * One case in a router step: a condition to evaluate and the sub-pipeline to execute
 * if the condition matches.
 *
 * [condition] is sent to the router plugin for evaluation; [branch] is executed by the
 * host if the plugin reports this case's index as a match.  Branch steps may themselves
 * include nested [RouterPipelineStep]s.
 */
@Serializable
data class RouterCase(
    val condition: RouterCondition,
    val branch: List<PipelineStep> = emptyList()
)
