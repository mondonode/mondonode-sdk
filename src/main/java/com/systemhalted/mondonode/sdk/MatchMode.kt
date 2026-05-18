package com.systemhalted.mondonode.sdk

import kotlinx.serialization.Serializable

/**
 * Controls how many matching cases a router evaluates before stopping.
 *
 * [FIRST]    — stop after the first case whose condition matches; equivalent to a
 *              traditional switch/case with break semantics.
 * [MULTIPLE] — evaluate all cases and collect every match; branches for all matched
 *              cases are executed in order.
 */
@Serializable
enum class MatchMode { FIRST, MULTIPLE }
