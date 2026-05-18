package com.systemhalted.mondonode.sdk

import kotlinx.serialization.Serializable

/**
 * One output step in a monitor's post-execution pipeline.
 *
 * Output steps are sinks: they consume metric values and write them to an external
 * destination.  They do not modify the metric — the pipeline continues unchanged
 * after an output step.
 *
 * Execution per step:
 *  1. Serialize metric.values (Map<String, String>) as the inputJson.
 *  2. Call IOutputPlugin.write() with:
 *       inputJson  = JSON-encoded metric.values
 *       configJson = {"capability_id": capabilityId, ...parameters}
 *  3. Await onSuccess() or onError() (10 s timeout); errors are logged but do not
 *     abort the pipeline or mark the monitor as failed.
 *  4. Return the metric unchanged.
 */
@Serializable
data class OutputStep(
    val outputId: String,
    val capabilityId: String,
    val parameters: Map<String, String> = emptyMap()
)
