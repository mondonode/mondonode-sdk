package com.systemhalted.mondonode.sdk

/**
 * SDK-level initialisation point. Call [init] once in [android.app.Application.onCreate]
 * before plugin discovery begins.
 *
 * In release builds omit the call (or call with an empty set) — only the official cert
 * fingerprint sets owned by the host's bundled Sentinels (e.g. `mondonode-sentinel-builtin`)
 * are trusted. The SDK itself ships no fingerprint sets; Sentinels own that list.
 *
 * In debug builds, pass the fingerprint printed by `scripts/gen-dev-signing-key.sh` so that
 * locally signed APKs are accepted by the host's Layer 1 cert check without populating the
 * official Sentinel fingerprint sets.
 */
object MondoNodeSdk {

    @Volatile private var devFingerprints: Set<String> = emptySet()

    /**
     * Register additional APK signing-cert fingerprints trusted alongside whatever official
     * set the calling Sentinel passes into [isTrusted].
     *
     * The dev fingerprints apply uniformly across **all** plugin categories — one dev key
     * signs every APK in the workspace.
     *
     * @param devFingerprints Colon-separated uppercase SHA-256 hex strings, as printed by
     *   `scripts/gen-dev-signing-key.sh`. Pass an empty set in release builds.
     */
    fun init(devFingerprints: Set<String> = emptySet()) {
        this.devFingerprints = devFingerprints
    }

    /**
     * Returns true if [fingerprint] is present in [officialSet] **or** in the dev fingerprints
     * registered via [init].
     *
     * Called by Sentinel plugins for every category's Layer 1 check. Public so external
     * Sentinel implementations can consult dev fingerprints without duplicating the check
     * logic.
     */
    fun isTrusted(fingerprint: String, officialSet: Set<String>): Boolean =
        officialSet.contains(fingerprint) || devFingerprints.contains(fingerprint)
}
