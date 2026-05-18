package com.systemhalted.mondonode.sdk

import kotlinx.serialization.Serializable

/**
 * Controls how a monitor's reactive input queue behaves when upstream trigger values
 * arrive faster than the monitor can process them.
 *
 * Declared on [PluginCapability] to express the plugin's requirements.  Can be
 * overridden per-monitor in [MonitorDefinition.inputQueueMode] when the capability
 * declares [USER_CHOICE].  If the capability declares [QUEUED] or [UNQUEUED] the
 * host enforces that mode regardless of the monitor setting.
 *
 * [QUEUED] — buffer all trigger values; each is dispatched as a separate execution
 *   (default).  No values are lost as long as the queue is below HWM.  Use when the
 *   monitor must process every upstream value (e.g. event counting, audit logging).
 *
 * [UNQUEUED] — latest-value-only; if multiple triggers arrive before the monitor runs,
 *   only the most recent value is used (CONFLATED channel semantics).  Use when only
 *   the current state matters and intermediate values are irrelevant (e.g. polling the
 *   latest reading, debouncing rapidly-changing sensor data).
 *
 * [USER_CHOICE] — the plugin has no requirement; the user configures it per-monitor.
 *   Defaults to [QUEUED] if the monitor does not specify.
 */
@Serializable
enum class InputQueueMode {
    QUEUED,
    UNQUEUED,
    USER_CHOICE
}
