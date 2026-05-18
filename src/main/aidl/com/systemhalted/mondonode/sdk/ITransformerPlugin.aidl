package com.systemhalted.mondonode.sdk;

import com.systemhalted.mondonode.sdk.ITransformCallback;

// Implemented by transformer plugin apps; the host binds to this service.
// Transformers are pure functions: they accept a string input and configuration,
// and produce a string output.  All inputs and outputs are JSON-encoded maps.
interface ITransformerPlugin {

    // --- Authentication (two-layer) ---

    // Layer 1 (pre-bind): host checks APK signing cert fingerprint via PackageManager.
    // Layer 2 (post-bind): host issues a random nonce; transformer signs it with its
    // Android Keystore EC private key and returns the Base64-encoded ECDSA signature.
    String authenticate(in byte[] nonce);

    // Returns the Base64-encoded DER EC public key matching the Keystore key used
    // in authenticate(). The host uses this to verify the signature.
    String getPublicKey();

    // --- Metadata ---

    // Returns a JSON-serialized TransformerManifest describing this transformer's
    // capabilities, parameter schemas, and public key.
    String getManifest();

    // --- Transformation ---

    // Apply a transformation asynchronously.
    //
    // inputJson:  JSON object with key "value" containing the string to transform.
    //             Example: {"value": "192.168.1.1"}
    //
    // configJson: JSON object with key "capability_id" (selects which transformation
    //             to perform) plus any capability-specific parameters.
    //             Example: {"capability_id": "validate_ipv4"}
    //
    // Delivers exactly one onResult() or onError() call via [callback], then stops.
    // Returns immediately; all work must be asynchronous.
    void transform(String inputJson, String configJson, ITransformCallback callback);
}
