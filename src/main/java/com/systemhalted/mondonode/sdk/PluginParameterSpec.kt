package com.systemhalted.mondonode.sdk

import kotlinx.serialization.Serializable

@Serializable
enum class ParamType { STRING, INT, FLOAT, BOOLEAN, ENUM }

@Serializable
data class ValidRange(val min: Double, val max: Double)

/**
 * Describes a single configuration parameter that a plugin capability accepts.
 *
 * Plugins declare these inside [PluginCapability.parameters] so that management
 * apps can build configuration UIs and validate input without hardcoding per-plugin
 * field names or types.
 *
 * [key] is the JSON field name used in the configJson blob sent via
 * IMondoPlugin.configure(). All other fields are informational / for validation.
 */
@Serializable
data class PluginParameterSpec(
    val key: String,
    val type: ParamType,
    val shortName: String,
    val description: String,
    val required: Boolean = true,
    val unit: String? = null,
    val validRange: ValidRange? = null,
    val acceptableValues: List<String>? = null,
    val defaultValue: String? = null
)
