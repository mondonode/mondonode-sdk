package com.systemhalted.mondonode.sdk

private val SUFFIX_PATTERN = Regex("""^(\d+[smh])+$""")
private val TOKEN_PATTERN = Regex("""(\d+)([smh])""")

/**
 * Parses a human-readable interval string into a total number of seconds.
 *
 * Accepts:
 *   - Blank/empty → null (plugin default)
 *   - Plain integer → seconds (e.g. "300" → 300)
 *   - One or more <number><unit> tokens concatenated, where unit is:
 *       s = seconds, m = minutes, h = hours
 *     e.g. "5m" → 300, "1h30m" → 5400, "30m1800s" → 3600
 *
 * Returns null for blank input or any input that cannot be parsed.
 */
fun parseIntervalSeconds(input: String): Int? {
    val trimmed = input.trim()
    if (trimmed.isBlank()) return null

    // Plain integer — treat as seconds (backward compatibility)
    trimmed.toIntOrNull()?.let { return it }

    // Suffix notation: one or more <number><unit> tokens
    if (!SUFFIX_PATTERN.matches(trimmed)) return null

    var total = 0
    for (match in TOKEN_PATTERN.findAll(trimmed)) {
        val value = match.groupValues[1].toIntOrNull() ?: return null
        total += when (match.groupValues[2]) {
            "s" -> value
            "m" -> value * 60
            "h" -> value * 3600
            else -> return null
        }
    }
    return total.takeIf { it > 0 }
}
