package com.systemhalted.mondonode.sdk;

import com.systemhalted.mondonode.sdk.IOutputCallback;

// Implemented by output plugin apps; the host binds to this service.
// Output plugins are data sinks: they consume metric values and write them to
// an external destination (file, network, message queue, etc.).
// They do not return a value — the metric is unchanged after an output step.
interface IOutputPlugin {

    // --- Authentication (two-layer) ---

    // Layer 1 (pre-bind): host checks APK signing cert fingerprint via PackageManager.
    // Layer 2 (post-bind): host issues a random nonce; output plugin signs it with its
    // Android Keystore EC private key and returns the Base64-encoded ECDSA signature.
    String authenticate(in byte[] nonce);

    // Returns the Base64-encoded DER EC public key matching the Keystore key used
    // in authenticate(). The host uses this to verify the signature.
    String getPublicKey();

    // --- Metadata ---

    // Returns a JSON-serialized OutputManifest describing this plugin's capabilities
    // and parameter schemas.
    String getManifest();

    // --- Output ---

    // Write metric data asynchronously to the output destination.
    //
    // inputJson:  JSON-encoded Map<String, String> containing all metric values
    //             produced by the monitor and its pipeline steps.
    //             Example: {"host":"8.8.8.8","reachable":"true","avg_rtt_ms":"5"}
    //
    // configJson: JSON object with key "capability_id" (selects which output
    //             operation to perform) plus any capability-specific parameters.
    //             Example: {"capability_id": "file_write", "file_path": "icmp.log"}
    //
    // Delivers exactly one onSuccess() or onError() call via [callback], then stops.
    // Returns immediately; all work must be asynchronous.
    void write(String inputJson, String configJson, IOutputCallback callback);
}
