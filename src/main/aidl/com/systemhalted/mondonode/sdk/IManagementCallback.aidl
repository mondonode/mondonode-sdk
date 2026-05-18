package com.systemhalted.mondonode.sdk;

// Implemented by management apps (e.g. the GUI plugin); the host calls this to push
// plugin state changes and metric data. Declared oneway so host calls never block.
oneway interface IManagementCallback {
    void onPluginConnected(String manifestJson);
    void onPluginDisconnected(String pluginId);
    void onMetricReceived(String pluginId, String metricJson);
    void onPluginError(String pluginId, String errorCode, String message);
}
