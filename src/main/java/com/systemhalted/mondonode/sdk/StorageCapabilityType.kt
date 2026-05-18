package com.systemhalted.mondonode.sdk

import kotlinx.serialization.Serializable

/**
 * Distinguishes the kinds of operation a storage capability can perform.
 *
 * Set on [StorageCapability.type] to tell the host how to wire the capability into the
 * pipeline graph:
 *
 * - [STREAM_WRITE] — data sink. The host calls `IStoragePlugin.streamWrite()`. The node has
 *   no output edges; write errors are swallowed. Suitable for append-only logging or
 *   best-effort persistence.
 * - [QUERY_READ] — read-only source. The host calls `IStoragePlugin.queryRead()` and routes
 *   the result back into the graph. The capability **must** declare an `"output"` port in
 *   [StorageCapability.outputPorts]. Suitable for lookups and enumeration operations.
 * - [QUERY_WRITE] — mutating source-and-sink. The host calls `IStoragePlugin.queryWrite()`
 *   and routes the result back into the graph. The capability **must** declare an `"output"`
 *   port. Suitable for set, delete, and compare-and-set operations.
 * - [STREAM_OUTPUT] — active push source. The host calls `IStoragePlugin.startStream()` at
 *   pipeline activation; the plugin pushes data points asynchronously via
 *   `IStorageStreamCallback.onData()`, which the host routes to downstream nodes exactly as
 *   a monitor metric would be routed. The capability **must** declare an `"output"` port.
 *   Standard parameters: `start_mode` (`LATEST` | `FROM_BEGINNING` | `FROM_TIME`),
 *   `start_time_ms` (required when `start_mode=FROM_TIME`).
 */
@Serializable
enum class StorageCapabilityType {
    /** Write-only sink; no result is returned to the pipeline. */
    STREAM_WRITE,
    /** Read-only query; result is returned to the pipeline via the `"output"` port. */
    QUERY_READ,
    /** Mutating query; result is returned to the pipeline via the `"output"` port. */
    QUERY_WRITE,
    /** Active push source; the host calls startStream() at pipeline activation. */
    STREAM_OUTPUT,
    /** @deprecated Use [QUERY_READ] or [QUERY_WRITE]. Kept for JSON deserialization safety. */
    @Deprecated("Use QUERY_READ or QUERY_WRITE")
    QUERY,
}
