package com.systemhalted.mondonode.sdk;

import com.systemhalted.mondonode.sdk.IStorageWriteCallback;
import com.systemhalted.mondonode.sdk.IStorageQueryCallback;
import com.systemhalted.mondonode.sdk.IStorageStreamCallback;

interface IStoragePlugin {
    String authenticate(in byte[] nonce);
    String getPublicKey();
    String getManifest();
    void streamWrite(String inputJson, String configJson, IStorageWriteCallback callback);
    void queryRead(String inputJson, String configJson, IStorageQueryCallback callback);
    void queryWrite(String inputJson, String configJson, IStorageQueryCallback callback);
    // STREAM_OUTPUT capabilities: startStream returns a handle used to stop the stream later.
    // Returns -1 on failure. stopStream is a no-op for unknown handles.
    int startStream(String configJson, IStorageStreamCallback callback);
    void stopStream(int handle);
    // Trigger notification for STREAM_OUTPUT nodes that declare a "trigger" input port.
    // Called by the host when a boolean arrives on that port (e.g. from an EVENT node).
    // Default implementation in StorageServiceBase is a no-op; only queue-like plugins override it.
    oneway void onTrigger(int handle, boolean value);
}
