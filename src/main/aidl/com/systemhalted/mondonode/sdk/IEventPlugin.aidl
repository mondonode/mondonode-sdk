package com.systemhalted.mondonode.sdk;

import com.systemhalted.mondonode.sdk.IEventCallback;

/**
 * Implemented by Event plugin services.  The host binds to this interface and calls
 * startEvent() for each capability instance wired in the PipelineGraph.
 *
 * Event plugins are source nodes — they have no pipeline inputs.  They run autonomously,
 * waiting for a condition, and emit a boolean value to their output port via
 * IEventCallback.onEvent() when that condition fires.  Connected edges route the
 * emitted value to downstream nodes (typically a MONITOR node's "trigger" port).
 *
 * Lifecycle: startEvent() → (one or more onEvent callbacks) → stopEvent().
 * A single plugin service may host multiple concurrent event instances; each is
 * identified by the handle returned from startEvent().
 */
interface IEventPlugin {
    /** Layer 2 challenge-response: sign nonce with the Keystore EC private key. */
    String authenticate(in byte[] nonce);
    /** Return the Base64-encoded public key for the Keystore auth key pair. */
    String getPublicKey();
    /** Return a JSON-serialised EventManifest describing this plugin's capabilities. */
    String getManifest();

    /**
     * Start one event instance for the given capability.
     *
     * configJson: JSON object containing at minimum "capability_id" and any
     * capability-specific parameters.
     *
     * Returns a non-negative integer handle on success, or -1 on failure.
     * The handle is stable for the lifetime of the event instance and is
     * passed to all IEventCallback calls and to stopEvent().
     */
    int startEvent(String configJson, IEventCallback callback);

    /**
     * Stop the event instance identified by handle.
     * The plugin must call IEventCallback.onStopped(handle) after cleanup.
     * No-op for unknown handles.
     */
    void stopEvent(int handle);
}
