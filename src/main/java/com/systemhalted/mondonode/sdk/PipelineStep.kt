package com.systemhalted.mondonode.sdk

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One step in a MonitorDefinition's post-execution pipeline.
 *
 * @deprecated The linear per-monitor pipeline model is superseded by [PipelineGraph], which
 * represents the entire data-flow as a directed graph with typed ports.  [PipelineStep] and
 * all its subtypes will be removed once the host execution engine is migrated to the graph
 * model.  New code should use [PipelineGraphNode] + [PipelineGraphEdge] instead.
 */
@Deprecated("Use PipelineGraph / PipelineGraphNode instead")
@Serializable
sealed class PipelineStep

@Serializable
@SerialName("transformer")
data class TransformerPipelineStep(val step: TransformerStep) : PipelineStep()

@Serializable
@SerialName("router")
data class RouterPipelineStep(val step: RouterStep) : PipelineStep()

@Serializable
@SerialName("loop")
data class LoopPipelineStep(val step: LoopStep) : PipelineStep()

@Serializable
@SerialName("queue_write")
data class QueueWritePipelineStep(val step: QueueStep) : PipelineStep()

@Serializable
@SerialName("queue_drain")
data class QueueDrainPipelineStep(val step: QueueDrainStep) : PipelineStep()

@Serializable
@SerialName("output")
data class OutputPipelineStep(val step: OutputStep) : PipelineStep()

@Serializable
@SerialName("storage_write")
data class StorageStreamWritePipelineStep(val step: StorageStreamWriteStep) : PipelineStep()

@Serializable
@SerialName("storage_query")
data class StorageQueryPipelineStep(val step: StorageQueryStep) : PipelineStep()
