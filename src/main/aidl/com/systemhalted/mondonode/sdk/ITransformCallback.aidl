package com.systemhalted.mondonode.sdk;

// Implemented by the host (or pipeline executor); transformer plugins call this to
// deliver results.  Declared oneway so transformer calls never block waiting for the host.
oneway interface ITransformCallback {
    // outputJson is a Map<String, String> encoded as a JSON object.
    // The primary result is always under key "value"; additional metadata keys
    // (e.g. "reason") may also be present.
    void onResult(String outputJson);
    void onError(String errorCode, String message);
}
