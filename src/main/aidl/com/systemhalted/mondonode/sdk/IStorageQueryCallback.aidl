package com.systemhalted.mondonode.sdk;

oneway interface IStorageQueryCallback {
    void onResult(String resultsJson);
    void onError(String errorCode, String message);
}
