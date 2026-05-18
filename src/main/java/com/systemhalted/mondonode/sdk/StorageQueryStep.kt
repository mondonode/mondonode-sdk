package com.systemhalted.mondonode.sdk

import kotlinx.serialization.Serializable

/**
 * A legacy pipeline step that queries a storage plugin and injects the result into the metric.
 *
 * **Deprecated — use the `PipelineGraph` model instead.** In the graph model, storage query
 * operations are represented by a `PipelineGraphNode` with `type = PipelineNodeType.STORAGE`
 * whose capability has `type = StorageCapabilityType.QUERY` and an outgoing edge from its
 * `"output"` port.
 *
 * @property storageId Package ID of the storage plugin to query.
 * @property capabilityId Capability ID within the storage plugin to invoke.
 * @property resultKey Key under which the query result is stored in the metric's `values` map.
 * @property parameters Static parameters passed to the capability.
 */
@Deprecated("Use PipelineGraphNode (type=STORAGE) with a QUERY capability and an outgoing edge instead.")
@Serializable
data class StorageQueryStep(
    val storageId: String,
    val capabilityId: String,
    val resultKey: String,
    val parameters: Map<String, String> = emptyMap()
)
