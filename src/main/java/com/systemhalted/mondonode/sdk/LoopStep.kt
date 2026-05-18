package com.systemhalted.mondonode.sdk

import kotlinx.serialization.Serializable

/**
 * One loop step in a monitor's post-execution pipeline.
 *
 * A loop step fans out an indexed multi-value array (produced by a prior transformer step
 * such as `string_split`) into N separate executions of a downstream monitor — one
 * execution per array element.  Each element is injected as the value of [paramKey] in
 * the downstream monitor's parameter bindings.
 *
 * Array input convention (consistent with transformer output):
 *   metric.values["${inputKey}.count"] → number of elements N
 *   metric.values["${inputKey}.0"]     → first element
 *   metric.values["${inputKey}.1"]     → second element
 *   …
 *   metric.values["${inputKey}.${N-1}"]
 *
 * The host executor reads these keys, then calls InputQueueCoordinator.enqueue() N times,
 * once per element, in index order.  The coordinator's consumer dispatches each as a
 * MonitorExecutorService run — identical to the path used for alarm-triggered runs.
 *
 * If the count key is absent or zero, the step is a no-op.
 *
 * [inputKey] — the prefix key for the indexed array (e.g. "parts" reads "parts.count",
 *   "parts.0", …).
 *
 * [targetMonitorId] — the downstream monitor to trigger for each item.
 *
 * [paramKey] — the parameter binding key to inject the element value into on the
 *   downstream monitor.  Must correspond to a key in the downstream monitor's
 *   [MonitorDefinition.parameterBindings].
 *
 * [hwm] — high-water mark for the queue.  Items beyond this threshold are dropped and
 *   counted by InputQueueCoordinator.  Must match the value used when the queue was
 *   first created (set at first enqueue; subsequent changes have no effect).
 *   Default matches InputQueueCoordinator.DEFAULT_HWM (64).
 *
 * [throttle] — optional rate-limiting configuration; null means unthrottled serial dispatch.
 */
@Serializable
data class LoopStep(
    val inputKey: String,
    val targetMonitorId: String,
    val paramKey: String,
    val hwm: Int = DEFAULT_HWM,
    val throttle: QueueThrottleConfig? = null
) {
    companion object {
        const val DEFAULT_HWM = 64
    }
}
