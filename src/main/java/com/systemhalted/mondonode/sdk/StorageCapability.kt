package com.systemhalted.mondonode.sdk

import kotlinx.serialization.Serializable

/**
 * Describes one operation a storage plugin can perform.
 *
 * [type] determines the execution semantics:
 *  - [StorageCapabilityType.STREAM_WRITE] — fire-and-forget sink; declare no [outputPorts].
 *  - [StorageCapabilityType.QUERY] — ACID read/write; declare an [outputPorts] entry with
 *    id="output" carrying the query result so downstream nodes can consume it.
 *
 * The defaults cover the STREAM_WRITE (sink) case.  QUERY capabilities must override
 * [outputPorts] to expose their result port.
 */
@Serializable
data class StorageCapability(
    val id: String,
    val name: String,
    val description: String,
    val type: StorageCapabilityType,
    val parameters: List<PluginParameterSpec> = emptyList(),
    val inputPorts: List<PortSpec> = listOf(
        PortSpec("input", "Input", description = "Data or query parameters"),
    ),
    val outputPorts: List<PortSpec> = emptyList(),
)
