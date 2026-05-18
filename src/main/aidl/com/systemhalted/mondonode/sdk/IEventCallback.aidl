package com.systemhalted.mondonode.sdk;

/**
 * Implemented by the host.  Event plugins call these methods to push events and
 * lifecycle notifications back to the host.  All methods are declared oneway so
 * the plugin never blocks waiting for the host to process the event.
 */
oneway interface IEventCallback {
    /**
     * The event condition was met.  The host routes the boolean value from the
     * declared output port (portId) to all connected downstream nodes in the graph.
     *
     * handle: the value returned by IEventPlugin.startEvent().
     * portId: the output port ID on this event capability (usually "output").
     * value:  true = condition met / fire; false = condition cleared.
     */
    void onEvent(int handle, String portId, boolean value);

    /**
     * A non-recoverable error occurred.  The host removes the active event record
     * for this handle; the event will not fire again until the graph is reactivated.
     */
    void onError(int handle, int code, String message);

    /**
     * The event instance has fully stopped (called after IEventPlugin.stopEvent()).
     */
    void onStopped(int handle);
}
