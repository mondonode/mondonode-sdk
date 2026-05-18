package com.systemhalted.mondonode.sdk;

import com.systemhalted.mondonode.sdk.IDataCallback;
import com.systemhalted.mondonode.sdk.IPluginHostBridge;

// Implemented by plugin apps; the host app binds to this service.
interface IMondoPlugin {

    // --- Authentication (two-layer) ---

    // Layer 1 (pre-bind): host checks APK signing cert fingerprint via PackageManager.
    // Layer 2 (post-bind): host issues a random nonce; plugin signs it with its
    // Android Keystore EC private key and returns the Base64-encoded ECDSA signature.
    String authenticate(in byte[] nonce);

    // Returns the Base64-encoded DER EC public key matching the Keystore key used
    // in authenticate(). The host uses this to verify the signature.
    String getPublicKey();

    // --- Metadata ---

    // Returns a JSON-serialized PluginManifest describing this plugin's capabilities.
    String getManifest();

    // --- Data streaming ---

    // Begin pushing metric data via the provided callback.
    void subscribe(IDataCallback callback);

    // Stop all metric data delivery.
    void unsubscribe();

    // --- Configuration ---

    // Push a JSON configuration blob to the plugin. Returns true on success.
    boolean configure(String configJson);

    // --- On-demand execution (alarm-triggered monitoring) ---

    // Run a single measurement cycle with the given parameter map JSON and deliver
    // exactly one result via [callback], then stop.  Returns immediately; the result
    // arrives asynchronously.  The host enforces a timeout and unbinds if it is exceeded.
    void execute(String configJson, IDataCallback callback);

    // --- Plugin dependency bridge ---

    // Called by the host immediately after successful authentication to provide
    // a bridge for cross-plugin communication. The plugin may retain this reference
    // to call declared dependencies via the host. Only declared dependencies
    // (PluginManifest.pluginDependencies) may be invoked through the bridge.
    void setHostBridge(IPluginHostBridge bridge);
}
