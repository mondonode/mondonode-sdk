package com.systemhalted.mondonode.sdk;

// Implemented by a plugin that calls IPluginHostBridge.requestExecute().
// The host invokes these methods when the target dependency plugin produces
// a result or reports an error.
oneway interface IPluginDependencyCallback {

    // Delivers a successful metric result from the dependency plugin.
    void onResult(String sourcePluginId, String metricJson);

    // Reports that the dependency plugin's execution failed.
    void onError(String sourcePluginId, String errorCode, String message);
}
