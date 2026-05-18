package com.systemhalted.mondonode.sdk

import kotlinx.serialization.Serializable

/**
 * Formerly controlled AlarmManager-based scheduling.  All monitor scheduling is now
 * handled by EVENT plugin nodes connected to a MONITOR node's "trigger" input port.
 *
 * This enum is kept only for safe deserialization of legacy [MonitorDefinition] JSON that
 * was stored before the Event plugin architecture was introduced.  The values have no effect
 * on the execution engine.
 */
@Deprecated("AlarmManager scheduling replaced by Event plugin nodes. Connect an EVENT node to the monitor's trigger port.")
@Serializable
enum class SchedulingMode {
    EXACT,
    INEXACT,
    NONE
}
