package com.systemhalted.mondonode.sdk

import kotlinx.serialization.Serializable

/**
 * Boolean composition operators for compound router conditions.
 *
 * Used in [RouterCondition.CompoundCondition] to combine two or more child conditions into a
 * single logical expression. [AND] and [OR] require at least two operands; [NOT] requires
 * exactly one.
 *
 * @see RouterCondition.CompoundCondition
 * @see StringOperator
 * @see NumericOperator
 */
@Serializable
enum class LogicalOperator {
    /** All operands must be true. */
    AND,
    /** At least one operand must be true. */
    OR,
    /** The single operand must be false. */
    NOT
}
