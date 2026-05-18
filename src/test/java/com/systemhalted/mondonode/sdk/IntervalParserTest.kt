package com.systemhalted.mondonode.sdk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IntervalParserTest {

    /**
     * Why: blank and empty strings are valid "no override" inputs; the host uses null to mean
     *      "fall back to the plugin default interval".
     * Pass: returns null for empty string, whitespace-only string.
     * Fail: returns any non-null value.
     * Origin: Happy-path coverage
     */
    @Test
    fun blankAndEmpty_returnNull() {
        assertNull(parseIntervalSeconds(""))
        assertNull(parseIntervalSeconds("   "))
    }

    /**
     * Why: bare integers are kept for backward compatibility with configs written before suffix
     *      notation was introduced; they are treated as seconds.
     * Pass: "300" → 300.
     * Fail: returns null or a different value.
     * Origin: Happy-path coverage
     */
    @Test
    fun plainInteger_returnsSeconds() {
        assertEquals(300, parseIntervalSeconds("300"))
    }

    /**
     * Why: single-token minute suffix is the most common human-readable form.
     * Pass: "5m" → 300.
     * Fail: returns null or a different value.
     * Origin: Happy-path coverage
     */
    @Test
    fun minuteSuffix_parsesCorrectly() {
        assertEquals(300, parseIntervalSeconds("5m"))
    }

    /**
     * Why: multi-token suffix strings (hours + minutes) must accumulate all tokens correctly.
     * Pass: "1h30m" → 5400.
     * Fail: returns null or a different value.
     * Origin: Happy-path coverage
     */
    @Test
    fun hoursAndMinutes_accumulateCorrectly() {
        assertEquals(5400, parseIntervalSeconds("1h30m"))
    }

    /**
     * Why: tokens can appear in any order; the parser must not assume h > m > s ordering.
     *      "30m1800s" = 1800 + 1800 = 3600.
     * Pass: "30m1800s" → 3600.
     * Fail: returns null or a different value.
     * Origin: Happy-path coverage
     */
    @Test
    fun mixedOrderTokens_accumulateCorrectly() {
        assertEquals(3600, parseIntervalSeconds("30m1800s"))
    }

    /**
     * Why: garbage strings must not silently produce a nonsense interval; returning null lets
     *      the caller fall back to the plugin default rather than scheduling at a wrong rate.
     * Pass: returns null for "abc", "5x", "1h 30m" (space inside).
     * Fail: returns any non-null value.
     * Origin: Happy-path coverage
     */
    @Test
    fun invalidString_returnsNull() {
        assertNull(parseIntervalSeconds("abc"))
        assertNull(parseIntervalSeconds("5x"))
        assertNull(parseIntervalSeconds("1h 30m"))
    }

    /**
     * Why: a token combination that sums to zero (e.g. "0m") is indistinguishable from "no
     *      interval set" and would cause divide-by-zero or instant-repeat behaviour in the
     *      scheduler; null signals the caller to reject or substitute a default.
     * Pass: "0m" → null, "0s" → null.
     * Fail: returns 0 or any non-null value.
     * Origin: Happy-path coverage
     */
    @Test
    fun zeroResult_returnsNull() {
        assertNull(parseIntervalSeconds("0m"))
        assertNull(parseIntervalSeconds("0s"))
    }
}
