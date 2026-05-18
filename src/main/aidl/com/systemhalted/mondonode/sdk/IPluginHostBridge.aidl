package com.systemhalted.mondonode.sdk;

import com.systemhalted.mondonode.sdk.IPluginDependencyCallback;

// Implemented by the host; passed to each authenticated plugin via
// IMondoPlugin.setHostBridge(). Allows a plugin to invoke its declared
// dependencies without binding to them directly.
//
// The host enforces that targetPluginId must appear in the calling plugin's
// PluginManifest.pluginDependencies. Calls to undeclared plugin IDs are
// rejected via IPluginDependencyCallback.onError() with errorCode "UNAUTHORIZED".
interface IPluginHostBridge {

    // Returns true if the declared dependency plugin is currently connected
    // and authenticated. Returns false for undeclared plugin IDs.
    boolean isDependencyAvailable(String targetPluginId);

    // Request a single execution cycle of a declared dependency plugin.
    // The result (or error) arrives asynchronously via [callback].
    // Returns immediately; the host enforces its own timeout on the target.
    void requestExecute(String targetPluginId, String configJson, IPluginDependencyCallback callback);
}
