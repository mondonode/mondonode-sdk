package com.systemhalted.mondonode.sdk

import kotlinx.serialization.Serializable

/**
 * A legacy pipeline step that writes the current metric values to a storage plugin.
 *
 * **Deprecated — use the `PipelineGraph` model instead.** In the graph model, stream-write
 * operations are represented by a `PipelineGraphNode` with `type = PipelineNodeType.STORAGE`
 * whose capability has `type = StorageCapabilityType.STREAM_WRITE` and no outgoing edges.
 *
 * @property storageId Package ID of the storage plugin to write to.
 * @property capabilityId Capability ID within the storage plugin to invoke.
 * @property parameters Static parameters passed to the capability alongside the metric values.
 */
@Deprecated("Use PipelineGraphNode (type=STORAGE) with a STREAM_WRITE capability and no outgoing edges instead.")
@Serializable
data class StorageStreamWriteStep(
    val storageId: String,
    val capabilityId: String,
    val parameters: Map<String, String> = emptyMap()
)
