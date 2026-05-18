package com.systemhalted.mondonode.sdk

import kotlinx.serialization.Serializable

/**
 * One router step in a monitor's post-execution pipeline.
 *
 * The host reads [inputKey] from the metric's values map and sends it to the
 * router plugin identified by [routerId] for condition evaluation.  The plugin
 * returns the indices of the matching cases; the host then executes each matching
 * case's [RouterCase.branch] sub-pipeline in order.
 *
 * If no case matches and [defaultBranch] is non-null, the default branch is executed.
 * If no case matches and [defaultBranch] is null, no output is written.
 *
 * [matchMode] controls whether evaluation stops after the first match ([MatchMode.FIRST])
 * or continues to collect all matches ([MatchMode.MULTIPLE]).
 */
@Serializable
data class RouterStep(
    val routerId: String,
    val capabilityId: String,
    val inputKey: String,
    val matchMode: MatchMode = MatchMode.FIRST,
    val cases: List<RouterCase>,
    val defaultBranch: List<PipelineStep>? = null
)
