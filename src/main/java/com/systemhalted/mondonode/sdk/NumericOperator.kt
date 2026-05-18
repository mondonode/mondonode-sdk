package com.systemhalted.mondonode.sdk

import kotlinx.serialization.Serializable

/**
 * Comparison operators for numeric-valued router conditions.
 *
 * Used in [RouterCondition.NumericCondition] to specify how the incoming pipeline value
 * (parsed as a [Double]) should be compared against a literal number. String comparisons
 * require [StringOperator] instead; logical composition uses [LogicalOperator].
 *
 * @see RouterCondition.NumericCondition
 * @see StringOperator
 * @see LogicalOperator
 */
@Serializable
enum class NumericOperator {
    /** Numeric equality (`value == literal`). */
    EQUALS,
    /** Numeric inequality (`value != literal`). */
    NOT_EQUALS,
    /** Strictly less than (`value < literal`). */
    LESS_THAN,
    /** Strictly greater than (`value > literal`). */
    GREATER_THAN,
    /** Less than or equal (`value <= literal`). */
    LESS_THAN_OR_EQUAL,
    /** Greater than or equal (`value >= literal`). */
    GREATER_THAN_OR_EQUAL
}
