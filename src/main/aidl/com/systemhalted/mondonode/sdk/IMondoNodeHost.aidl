package com.systemhalted.mondonode.sdk;

import com.systemhalted.mondonode.sdk.IManagementCallback;

// Exposed by the host app; management apps (e.g. the GUI plugin) bind to this service
// using the intent action AuthConstants.MANAGER_SERVICE_ACTION.
interface IMondoNodeHost {

    // Register to receive plugin connect/disconnect events and metric data.
    // Immediately fires onPluginConnected for each currently connected plugin.
    void register(IManagementCallback callback);

    // Unregister a previously registered callback.
    void unregister(IManagementCallback callback);

    // Returns a JSON array of PluginManifest for all auth-passed plugins.
    String getConnectedPlugins();

    // Push a JSON configuration blob to the plugin identified by pluginId.
    // Returns true if the plugin accepted the configuration.
    boolean configurePlugin(String pluginId, String configJson);

    // Register the full list of MonitorDefinitions (JSON-serialized List<MonitorDefinition>)
    // with the host.  The host persists them and schedules an alarm for each enabled monitor.
    // Call this on connect and whenever the list changes (add / edit / delete / enable / disable).
    void setMonitors(String monitorsJson);

    // Returns the host's persisted JSON array of MonitorDefinitions.
    // Management apps should call this on first connect if their local storage is empty
    // (e.g. after a fresh install or reinstall) to recover monitors configured by a
    // previously installed management app rather than overwriting them with an empty list.
    String getMonitors();

    // Returns a JSON array of MetricData for the most recent successful execution of each monitor.
    // Management apps should call this on connect to hydrate their results view without waiting
    // for the next alarm cycle to fire.
    String getMonitorResults();

    // Returns a JSON array of MetricData for the most recent `limit` successful executions of
    // the given monitor, ordered newest-first.  Returns "[]" if the monitor has no history yet.
    // Maximum useful limit is 100 (the host's rolling window size).
    String getMonitorHistory(String monitorId, int limit);

    // Returns a JSON array of TransformerManifest for all authenticated transformer plugins.
    String getConnectedTransformers();

    // Returns a JSON array of RouterManifest for all authenticated router plugins.
    String getConnectedRouters();

    // Returns a JSON array of OutputManifest for all authenticated output plugins.
    String getConnectedOutputPlugins();

    // Returns a JSON array of StorageManifest for all authenticated storage plugins.
    String getConnectedStorages();

    // Proxies a query to the storage plugin identified by storageId.  The host inspects
    // the plugin's cached StorageManifest and dispatches to IStoragePlugin.queryRead() for
    // QUERY_READ capabilities or IStoragePlugin.queryWrite() for QUERY_WRITE capabilities.
    // inputJson and configJson follow the same format as those AIDL methods.
    // Returns the resultsJson from the plugin on success, or a JSON error object
    // {"error":"<code>","message":"<text>"} if the plugin is not found, times out, etc.
    // Blocks the caller's Binder thread until the storage callback fires (timeout: 10 s).
    String queryStorage(String storageId, String inputJson, String configJson);

    // ---- Pipeline graph (replaces the linear per-monitor pipeline model) -------

    // Returns the host's current PipelineGraph as JSON.
    // The graph is the canonical pipeline state: all monitor nodes, transformer nodes,
    // router nodes, output nodes, and storage nodes, plus all edges connecting them.
    // Returns an empty PipelineGraph JSON if no graph has been set yet.
    String getPipelineGraph();

    // Replaces the host's PipelineGraph with the supplied JSON.
    // The host validates the graph (loop detection), persists it, and re-schedules
    // all monitor nodes.  Returns true if the graph was accepted and applied,
    // false if validation failed (e.g. pure data cycle detected).
    boolean setPipelineGraph(in String graphJson);

    // Subscribe [callback] to metric_received events for a specific (monitorId, metricKey) pair.
    // Callbacks with no subscriptions receive all events (firehose default).
    void subscribeOutput(IManagementCallback callback, String monitorId, String metricKey);

    // Remove a specific (monitorId, metricKey) subscription added via subscribeOutput.
    void unsubscribeOutput(IManagementCallback callback, String monitorId, String metricKey);

    // Subscribe [callback] to all metric_received events for every key produced by [monitorId].
    void subscribeMonitor(IManagementCallback callback, String monitorId);

    // Remove a whole-monitor subscription added via subscribeMonitor.
    void unsubscribeMonitor(IManagementCallback callback, String monitorId);
}
