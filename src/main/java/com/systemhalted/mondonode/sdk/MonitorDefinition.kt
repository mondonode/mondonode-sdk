package com.systemhalted.mondonode.sdk

import kotlinx.serialization.Serializable

/**
 * A configured monitoring instance managed by a management app.
 *
 * [pluginId] identifies which plugin executes the monitor.
 * [parameters] maps each [PluginParameterSpec.key] to its serialized value.
 * [enabled] is the static default enabled state; overridden at runtime by an incoming
 * "enabled" edge in the [PipelineGraph].
 *
 * Monitors are driven exclusively by [PipelineNodeType.EVENT] nodes connected to the
 * monitor node's "trigger" input port.  [schedulingMode] and [intervalSeconds] are
 * deprecated — they have no effect on the execution engine.
 *
 * [parameterBindings] maps parameter keys to dynamic [ParameterBinding] descriptors.
 * At execution time, each binding is resolved from the upstream monitor's latest result
 * and merged over [parameters] (bindings take precedence over static values).
 *
 * [inputTriggerKey] optionally names one key in [parameterBindings] whose upstream
 * value change should immediately trigger this monitor to run.
 *
 * [inputQueueMode] controls how trigger values are buffered when they arrive faster than
 * this monitor can execute.  Null means use the plugin capability's declared mode.
 *
 * [pipeline] is the authoritative post-execution pipeline: an ordered list of
 * [PipelineStep]s applied after the monitor plugin runs.
 *
 * [transformerPipeline] is the legacy field kept for backward compatibility with
 * monitors saved before router support was added.
 */
@Serializable
data class MonitorDefinition(
    val id: String,
    val name: String,
    val pluginId: String,
    val enabled: Boolean = true,
    @Deprecated("AlarmManager scheduling replaced by Event plugin nodes. Has no effect on execution.")
    val schedulingMode: SchedulingMode = SchedulingMode.NONE,
    @Deprecated("AlarmManager scheduling replaced by Event plugin nodes. Has no effect on execution.")
    val intervalSeconds: Int? = null,
    val parameters: Map<String, String> = emptyMap(),
    val parameterBindings: Map<String, ParameterBinding> = emptyMap(),
    val inputTriggerKey: String? = null,
    val inputQueueMode: InputQueueMode? = null,
    val pipeline: List<PipelineStep> = emptyList(),
    @Deprecated("Use pipeline instead; kept for backward compatibility with pre-router monitors")
    val transformerPipeline: List<TransformerStep> = emptyList()
) {
    /** Returns the effective pipeline, migrating legacy transformerPipeline if needed. */
    @Suppress("DEPRECATION")
    fun effectivePipeline(): List<PipelineStep> =
        if (pipeline.isNotEmpty()) pipeline
        else transformerPipeline.map { TransformerPipelineStep(it) }
}
