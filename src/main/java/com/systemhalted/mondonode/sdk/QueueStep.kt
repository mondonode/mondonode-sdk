package com.systemhalted.mondonode.sdk

import kotlinx.serialization.Serializable

/**
 * Write side of a named queue pipeline step.
 *
 * Reads [metric.values[inputKey]], enqueues it into the shared buffer identified by
 * [queueId], and writes queue diagnostics into the metric under [diagnosticsKey] (if
 * set).  Multiple monitors may write to the same [queueId] to achieve fan-in
 * aggregation — the host creates the buffer on first write and the config is fixed at
 * that point; subsequent writers with a different [hwm] or [overflowPolicy] silently
 * inherit the existing config.
 *
 * The write step is paired with a [QueueDrainStep] on a separate (or the same) monitor
 * to snapshot and consume the buffered values.
 *
 * [queueId]         — user-defined name shared across all producers and the consumer.
 * [inputKey]        — metric key whose value is enqueued.
 * [hwm]             — buffer capacity; items beyond this are handled per [overflowPolicy].
 * [overflowPolicy]  — what happens when the buffer is full (see [OverflowPolicy]).
 * [diagnosticsKey]  — optional metric key prefix for _q.* diagnostic outputs.  If null,
 *                     no diagnostics are written into the metric (silent write).
 */
@Serializable
data class QueueStep(
    val queueId: String,
    val inputKey: String,
    val hwm: Int = DEFAULT_HWM,
    val overflowPolicy: OverflowPolicy = OverflowPolicy.DROP_NEWEST,
    val diagnosticsKey: String? = null
) {
    companion object {
        const val DEFAULT_HWM = 64
    }
}

/**
 * Read/drain side of a named queue pipeline step.
 *
 * Snapshots the buffer identified by [queueId] and writes the contents as an indexed
 * array into the metric under [outputKey]:
 *
 *   "${outputKey}.count"      — number of items in the snapshot
 *   "${outputKey}.0" … ".N-1" — items in FIFO order
 *   "${outputKey}._q.*"       — queue diagnostic counters
 *
 * If [drainPolicy] is [DrainPolicy.ACCUMULATE] the buffer is cleared after the
 * snapshot so the next batch starts fresh.  If [DrainPolicy.SLIDING] the buffer is
 * left intact (ring-buffer semantics — the snapshot is a point-in-time window).
 *
 * If the queue does not exist yet (no writer has run) the step outputs count=0 and
 * no item keys.
 *
 * [queueId]     — identifies the shared buffer (must match the producer's QueueStep).
 * [outputKey]   — metric key prefix for count, indexed items, and diagnostics.
 * [drainPolicy] — whether to clear the buffer after snapshotting (see [DrainPolicy]).
 */
@Serializable
data class QueueDrainStep(
    val queueId: String,
    val outputKey: String,
    val drainPolicy: DrainPolicy = DrainPolicy.ACCUMULATE
)
