package com.systemhalted.mondonode.sdk

/**
 * Compile-time constants shared by the host and all plugin APKs.
 *
 * Contains:
 * - **Intent actions** — each plugin category declares its service with a specific action so the
 *   host can discover it via `PackageManager.queryIntentServices()`.
 * - **Keystore aliases** — stable per-category aliases under which each plugin type stores its
 *   runtime EC authentication key in the Android Keystore. The SDK base classes use these aliases
 *   automatically; plugin authors do not need to reference them directly.
 * - **Cryptographic constants** — the algorithm suite used for challenge-response authentication
 *   (`secp256r1` EC key, `SHA256withECDSA` signature).
 *
 * Official cert fingerprint sets have moved to `mondonode-sentinel-builtin` as private
 * constants. The Sentinel chain owns all cert-tier policy; the host no longer checks
 * fingerprints directly.
 */
object AuthConstants {
    /** Intent action that all MondoNode plugin services must declare in their manifest. */
    const val PLUGIN_SERVICE_ACTION = "com.systemhalted.mondonode.PLUGIN"

    /** Intent action that the host management service declares in its manifest. */
    const val MANAGER_SERVICE_ACTION = "com.systemhalted.mondonode.MANAGER"

    /** Intent action that management plugin services must declare in their manifest. */
    const val MANAGEMENT_PLUGIN_SERVICE_ACTION = "com.systemhalted.mondonode.MANAGEMENT_PLUGIN"

    /** Intent action that transformer plugin services must declare in their manifest. */
    const val TRANSFORMER_SERVICE_ACTION = "com.systemhalted.mondonode.TRANSFORMER"

    /** Intent action that router plugin services must declare in their manifest. */
    const val ROUTER_SERVICE_ACTION = "com.systemhalted.mondonode.ROUTER"

    /** Intent action that output plugin services must declare in their manifest. */
    const val OUTPUT_SERVICE_ACTION = "com.systemhalted.mondonode.OUTPUT"

    /** Intent action that storage plugin services must declare in their manifest. */
    const val STORAGE_SERVICE_ACTION = "com.systemhalted.mondonode.STORAGE"

    /** Intent action that Event plugin services must declare in their manifest. */
    const val EVENT_SERVICE_ACTION = "com.systemhalted.mondonode.EVENT"

    /**
     * Intent action that Sentinel plugin services must declare in their manifest.
     * Also available as [com.systemhalted.mondonode.sentinel.sdk.SentinelConstants.SENTINEL_SERVICE_ACTION].
     * Mirrored here so the host app can reference it without importing the sentinel-sdk directly,
     * but SentinelConstants is the authoritative declaration.
     */
    const val SENTINEL_SERVICE_ACTION = "com.systemhalted.mondonode.SENTINEL"

    /**
     * Package name of the MondoNode host application. Used by plugin service base classes to
     * validate that AIDL callers are the host (finding #5 — per-call UID check).
     */
    const val HOST_PACKAGE_NAME = "com.systemhalted.mondonode"

    /**
     * Signature-level Android permission names declared by the host app. Plugin services require
     * the relevant permission on their `<service>` element so only the host (signed with the same
     * certificate as the permission declaration) can bind to them (finding #2 — OS binding gate).
     *
     * These constants are provided here so plugin authors can reference them without hard-coding
     * strings. The `<permission>` declarations themselves live only in `mondonode-app`'s manifest.
     */
    const val BIND_PLUGIN_PERMISSION = "com.systemhalted.mondonode.BIND_PLUGIN"
    const val BIND_MANAGEMENT_PLUGIN_PERMISSION = "com.systemhalted.mondonode.BIND_MANAGEMENT_PLUGIN"
    const val BIND_TRANSFORMER_PERMISSION = "com.systemhalted.mondonode.BIND_TRANSFORMER"
    const val BIND_ROUTER_PERMISSION = "com.systemhalted.mondonode.BIND_ROUTER"
    const val BIND_OUTPUT_PERMISSION = "com.systemhalted.mondonode.BIND_OUTPUT"
    const val BIND_STORAGE_PERMISSION = "com.systemhalted.mondonode.BIND_STORAGE"
    const val BIND_SENTINEL_PERMISSION = "com.systemhalted.mondonode.BIND_SENTINEL"
    const val BIND_EVENT_PERMISSION = "com.systemhalted.mondonode.BIND_EVENT"

    /** Android Keystore alias used by plugins for their runtime EC signing key. */
    const val PLUGIN_KEYSTORE_ALIAS = "mondonode_plugin_auth_key"

    /** Android Keystore alias used by management plugins for their runtime EC signing key. */
    const val MANAGEMENT_PLUGIN_KEYSTORE_ALIAS = "mondonode_mgmt_plugin_auth_key"

    /** Android Keystore alias used by transformer plugins for their runtime EC signing key. */
    const val TRANSFORMER_PLUGIN_KEYSTORE_ALIAS = "mondonode_transformer_auth_key"

    /** Android Keystore alias used by router plugins for their runtime EC signing key. */
    const val ROUTER_PLUGIN_KEYSTORE_ALIAS = "mondonode_router_auth_key"

    /** Android Keystore alias used by output plugins for their runtime EC signing key. */
    const val OUTPUT_PLUGIN_KEYSTORE_ALIAS = "mondonode_output_auth_key"

    /** Android Keystore alias used by storage plugins for their runtime EC signing key. */
    const val STORAGE_PLUGIN_KEYSTORE_ALIAS = "mondonode_storage_auth_key"

    /** Android Keystore alias used by event plugins for their runtime EC signing key. */
    const val EVENT_PLUGIN_KEYSTORE_ALIAS = "mondonode_event_auth_key"

    /** JCA key algorithm used for plugin authentication keys (`"EC"`). */
    const val KEY_ALGORITHM = "EC"
    /** JCA signature algorithm used for challenge-response authentication. */
    const val SIGNATURE_ALGORITHM = "SHA256withECDSA"
    /** Named elliptic curve used for all plugin authentication keys. */
    const val EC_CURVE = "secp256r1"

}
