package com.systemhalted.mondonode.sdk;

import com.systemhalted.mondonode.sdk.IMondoNodeHost;

// Implemented by management plugin apps; the host app discovers and binds to this service.
interface IManagementPlugin {

    // --- Authentication (two-layer) ---

    // Layer 1 (pre-bind): host checks APK signing cert fingerprint via PackageManager.
    // Layer 2 (post-bind): host issues a random nonce; management plugin signs it with its
    // Android Keystore EC private key and returns the Base64-encoded ECDSA signature.
    String authenticate(in byte[] nonce);

    // Returns the Base64-encoded DER EC public key matching the Keystore key used
    // in authenticate(). The host uses this to verify the signature.
    String getPublicKey();

    // Returns a JSON-serialized ManagementPluginManifest.
    String getManifest();

    // --- Host handoff ---

    // Called by the host after auth passes. The management plugin stores the provided
    // IMondoNodeHost reference and uses it to register callbacks and issue commands.
    oneway void onHostConnected(IMondoNodeHost host);

    // Called by the host when it is shutting down or revoking the connection.
    oneway void onHostDisconnected();
}
