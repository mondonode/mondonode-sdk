package com.systemhalted.mondonode.sdk

import kotlinx.serialization.Serializable

/**
 * Binds a MonitorDefinition parameter to a metric value produced by another monitor.
 *
 * @deprecated Cross-node data routing is now expressed as [PipelineGraphEdge]s in the
 * [PipelineGraph].  New code should connect an upstream node's output port to a
 * downstream monitor node's parameter input port via a [PipelineGraphEdge] where
 * [PipelineGraphEdge.toPortId] equals the target parameter key.
 */
@Deprecated("Use PipelineGraphEdge instead")
@Serializable
data class ParameterBinding(
    val sourceMonitorId: String,
    val metricKey: String,
    val fallback: String? = null
)
