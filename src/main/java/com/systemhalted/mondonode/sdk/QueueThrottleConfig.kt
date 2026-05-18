package com.systemhalted.mondonode.sdk

import kotlinx.serialization.Serializable

/**
 * Rate-limiting configuration for a [LoopStep]'s output queue.
 *
 * All fields are optional — defaults produce a serial, unthrottled queue.
 *
 * [interIterationDelayMs] — minimum pause between consecutive dispatches.
 *   Use this to avoid bursting many executions into the host simultaneously.
 *   0 = no pause (default).
 *
 * [maxRatePerMinute] — hard cap on dispatches per minute across the queue.
 *   null = unlimited (default).
 *
 * [maxConcurrent] — how many downstream executions may be in flight at once.
 *   1 = strictly serial (default); > 1 allows pipelining when the downstream
 *   monitor is idempotent and can run concurrently.  Must be >= 1.
 */
@Serializable
data class QueueThrottleConfig(
    val interIterationDelayMs: Long = 0L,
    val maxRatePerMinute: Int? = null,
    val maxConcurrent: Int = 1
)
