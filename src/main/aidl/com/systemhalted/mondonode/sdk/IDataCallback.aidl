package com.systemhalted.mondonode.sdk;

// Implemented by the host app; plugins call this to push metric data.
// Declared oneway so plugin calls never block waiting for the host.
oneway interface IDataCallback {
    void onMetricReceived(String metricJson);
    void onPluginError(String errorCode, String message);
    void onPluginStatusChanged(String status);
}
