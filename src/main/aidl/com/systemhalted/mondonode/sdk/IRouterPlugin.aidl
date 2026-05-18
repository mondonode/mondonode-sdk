package com.systemhalted.mondonode.sdk;

import com.systemhalted.mondonode.sdk.IRouteCallback;

// Implemented by router plugin apps; the host binds to this service.
// Routers evaluate a set of conditions against a string input value and return
// the indices of the cases whose conditions matched.  Branch execution is
// performed by the host — the plugin is responsible only for condition evaluation.
interface IRouterPlugin {

    // --- Authentication (two-layer) ---

    // Layer 1 (pre-bind): host checks APK signing cert fingerprint via PackageManager.
    // Layer 2 (post-bind): host issues a random nonce; router signs it with its
    // Android Keystore EC private key and returns the Base64-encoded ECDSA signature.
    String authenticate(in byte[] nonce);

    // Returns the Base64-encoded DER EC public key matching the Keystore key used
    // in authenticate(). The host uses this to verify the signature.
    String getPublicKey();

    // --- Metadata ---

    // Returns a JSON-serialized RouterManifest describing this router's
    // capabilities and public key.
    String getManifest();

    // --- Evaluation ---

    // Evaluate conditions against an input value and return matched case indices.
    //
    // inputJson:  JSON object with key "value" containing the string to test.
    //             Example: {"value": "192.168.1.1"}
    //
    // configJson: JSON object with:
    //   "capability_id"  — selects which routing capability to use
    //   "match_mode"     — "FIRST" (stop after first match) or "MULTIPLE" (all matches)
    //   "conditions"     — JSON array of serialized RouterCondition objects, one per case,
    //                      in the same order as RouterStep.cases
    //             Example: {"capability_id": "switch_evaluate",
    //                        "match_mode": "FIRST",
    //                        "conditions": [
    //                          {"type": "string", "operator": "EQUALS", "value": "ok"},
    //                          {"type": "numeric", "operator": "GREATER_THAN", "value": 0.0}
    //                        ]}
    //
    // Delivers exactly one onMatched(), onNoMatch(), or onError() via [callback].
    // Returns immediately; all work must be asynchronous.
    void evaluate(String inputJson, String configJson, IRouteCallback callback);
}
