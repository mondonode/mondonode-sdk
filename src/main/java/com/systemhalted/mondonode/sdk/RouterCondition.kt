package com.systemhalted.mondonode.sdk

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A condition evaluated by a router plugin against an input string value.
 *
 * The sealed class hierarchy lets the host and plugin exchange structured condition
 * data as JSON without a separate type registry.
 *
 * [StringCondition]   — string comparison; [ignoreCase] defaults to true.
 * [NumericCondition]  — parses both sides as Double before comparing.
 *                       If the input cannot be parsed, the condition evaluates to false.
 * [RegexCondition]    — full regex match against the entire input string.
 * [CompoundCondition] — logical AND/OR/NOT composition of other conditions.
 *                       NOT uses only the first operand; AND/OR short-circuit.
 */
@Serializable
sealed class RouterCondition

@Serializable
@SerialName("string")
data class StringCondition(
    val operator: StringOperator,
    val value: String,
    val ignoreCase: Boolean = true
) : RouterCondition()

@Serializable
@SerialName("numeric")
data class NumericCondition(
    val operator: NumericOperator,
    val value: Double
) : RouterCondition()

@Serializable
@SerialName("regex")
data class RegexCondition(
    val pattern: String,
    val ignoreCase: Boolean = true
) : RouterCondition()

@Serializable
@SerialName("compound")
data class CompoundCondition(
    val logicalOperator: LogicalOperator,
    val operands: List<RouterCondition>
) : RouterCondition()
