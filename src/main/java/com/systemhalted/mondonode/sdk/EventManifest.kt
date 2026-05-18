package com.systemhalted.mondonode.sdk

import kotlinx.serialization.Serializable

/**
 * Self-description returned by an Event plugin service via IEventPlugin.getManifest().
 *
 * [eventId] is the stable, reverse-DNS identifier for this plugin (e.g.
 * "com.example.events.motion_sensor").  It is the key used by the host to look up
 * the plugin in the PipelineGraph.
 *
 * [capabilities] lists each distinct event type the plugin can produce.  A single
 * plugin may expose multiple capabilities (e.g. "timer_interval" and "timer_cron"
 * in the same APK).
 *
 * [publicKeyBase64] is the Base64-encoded EC public key used for Layer 2
 * challenge-response authentication.  Populated automatically by EventServiceBase.
 */
@Serializable
data class EventManifest(
    val eventId: String,
    val name: String,
    val version: String,
    val author: String = "",
    val description: String = "",
    val capabilities: List<EventCapability> = emptyList(),
    val publicKeyBase64: String = "",
)
