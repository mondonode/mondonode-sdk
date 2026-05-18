package com.systemhalted.mondonode.sdk

import kotlinx.serialization.Serializable

/**
 * Comparison operators for string-valued router conditions.
 *
 * Used in [RouterCondition.StringCondition] to specify how the incoming pipeline value
 * should be compared against a literal string. All comparisons operate on the value as a
 * plain string; numeric comparisons require [NumericOperator] instead.
 *
 * @see RouterCondition.StringCondition
 * @see NumericOperator
 * @see LogicalOperator
 */
@Serializable
enum class StringOperator {
    /** Exact equality (or case-insensitive equality when `ignoreCase = true`). */
    EQUALS,
    /** Negation of [EQUALS]. */
    NOT_EQUALS,
    /** Value begins with the literal string. */
    STARTS_WITH,
    /** Value ends with the literal string. */
    ENDS_WITH,
    /** Value contains the literal string as a substring. */
    CONTAINS
}
