package com.systemhalted.mondonode.sdk;

oneway interface IStorageWriteCallback {
    void onSuccess();
    void onError(String errorCode, String message);
}
