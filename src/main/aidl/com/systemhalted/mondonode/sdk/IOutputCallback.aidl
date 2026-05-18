package com.systemhalted.mondonode.sdk;

// Implemented by the host; passed to IOutputPlugin.write() to receive the outcome.
// Declared oneway: the output plugin fires and forgets — the host callback is
// non-blocking from the plugin's perspective.
oneway interface IOutputCallback {

    // The write completed successfully.
    void onSuccess();

    // The write failed.  [errorCode] is a short machine-readable tag;
    // [message] is a human-readable description for logs and UI.
    void onError(String errorCode, String message);
}
