package com.systemhalted.mondonode.sdk

object RouterConditionEvaluator {

    fun evaluate(input: String, condition: RouterCondition): Boolean = when (condition) {
        is StringCondition -> evaluateString(input, condition)
        is NumericCondition -> evaluateNumeric(input, condition)
        is RegexCondition -> evaluateRegex(input, condition)
        is CompoundCondition -> evaluateCompound(input, condition)
    }

    private fun evaluateString(input: String, condition: StringCondition): Boolean {
        val a = if (condition.ignoreCase) input.lowercase() else input
        val b = if (condition.ignoreCase) condition.value.lowercase() else condition.value
        return when (condition.operator) {
            StringOperator.EQUALS -> a == b
            StringOperator.NOT_EQUALS -> a != b
            StringOperator.STARTS_WITH -> a.startsWith(b)
            StringOperator.ENDS_WITH -> a.endsWith(b)
            StringOperator.CONTAINS -> a.contains(b)
        }
    }

    private fun evaluateNumeric(input: String, condition: NumericCondition): Boolean {
        val inputNum = input.toDoubleOrNull() ?: return false
        return when (condition.operator) {
            NumericOperator.EQUALS -> inputNum == condition.value
            NumericOperator.NOT_EQUALS -> inputNum != condition.value
            NumericOperator.LESS_THAN -> inputNum < condition.value
            NumericOperator.GREATER_THAN -> inputNum > condition.value
            NumericOperator.LESS_THAN_OR_EQUAL -> inputNum <= condition.value
            NumericOperator.GREATER_THAN_OR_EQUAL -> inputNum >= condition.value
        }
    }

    private fun evaluateRegex(input: String, condition: RegexCondition): Boolean {
        val options = if (condition.ignoreCase) setOf(RegexOption.IGNORE_CASE) else emptySet()
        return runCatching {
            Regex(condition.pattern, options).matches(input)
        }.getOrDefault(false)
    }

    private fun evaluateCompound(input: String, condition: CompoundCondition): Boolean =
        when (condition.logicalOperator) {
            LogicalOperator.AND -> condition.operands.all { evaluate(input, it) }
            LogicalOperator.OR -> condition.operands.any { evaluate(input, it) }
            LogicalOperator.NOT -> condition.operands.firstOrNull()?.let { !evaluate(input, it) } ?: false
        }
}
