package com.systemhalted.mondonode.sdk

import kotlinx.serialization.Serializable

/**
 * One step in a monitor's post-execution transformer pipeline.
 *
 * After a monitor plugin runs and produces a MetricData result, the host applies
 * each TransformerStep in order, enriching the metric's values map.
 *
 * Execution per step:
 *  1. Read metric.values[inputKey] as the input string (empty string if absent).
 *  2. Call ITransformerPlugin.transform() with:
 *       inputJson  = {"value": <inputKey value>}
 *       configJson = {"capability_id": capabilityId, ...parameters}
 *  3. Parse the outputJson result map.
 *  4. Write output["value"] → metric.values[outputKey].
 *     Write all other output keys k → metric.values["${outputKey}.${k}"].
 *
 * This convention lets subsequent steps reference derived values by dotted key
 * (e.g. inputKey = "parts.0" to read the first element of a prior split step).
 */
@Serializable
data class TransformerStep(
    val transformerId: String,
    val capabilityId: String,
    val inputKey: String,
    val outputKey: String,
    val parameters: Map<String, String> = emptyMap()
)
