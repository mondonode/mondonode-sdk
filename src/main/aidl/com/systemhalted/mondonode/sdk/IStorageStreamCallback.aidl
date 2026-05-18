package com.systemhalted.mondonode.sdk;

/**
 * Implemented by the host; passed to IStoragePlugin.startStream().
 *
 * All methods are oneway — the host never blocks the plugin's stream thread.
 *
 * onData   — one data point: {"ts": <epoch_ms>, "v": <double_as_string>}
 * onError  — stream terminated abnormally; handle is invalidated.
 * onEnd    — stream terminated cleanly (e.g. end of historical replay); handle is invalidated.
 */
oneway interface IStorageStreamCallback {
    void onData(String dataJson);
    void onError(String code, String message);
    void onEnd();
}
