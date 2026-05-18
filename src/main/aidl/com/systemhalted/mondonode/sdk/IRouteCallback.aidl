package com.systemhalted.mondonode.sdk;

// Implemented by the host pipeline executor; router plugins call this to deliver
// evaluation results.  Declared oneway so router calls never block waiting for the host.
oneway interface IRouteCallback {
    // matchedIndicesJson is a JSON array of integer case indices that matched,
    // in evaluation order.  Example: [0] or [0, 2]
    void onMatched(String matchedIndicesJson);

    // No conditions matched.  The host will execute the default branch if one is defined,
    // or skip output entirely if there is no default.
    void onNoMatch();

    void onError(String errorCode, String message);
}
