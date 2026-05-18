# Known Issues — mondonode-sdk

Severity: **High** | **Medium** | **Low**
Status: **Open** | **Deferred** (named initiative) | **Accepted risk**

---

## Security

### IPC #4 — ManagementServiceBase cannot verify onHostConnected() caller identity

**Severity:** Medium
**Status:** Deferred — Sentinel architecture
**Tracking:** IPC audit 2026-05-10 · finding #4

**Description:**
`ManagementServiceBase.onHostConnected(IMondoNodeHost host)` receives an
`IMondoNodeHost` binder from its caller. The `requireCallerIsHost()` UID check
verifies that the calling process UID matches the host app's UID, but it cannot
verify that the `IMondoNodeHost` object passed as an argument is the genuine host
implementation rather than a counterfeit stub created by that process.

**Risk:**
Low in practice — a process that passes the UID check has already cleared the OS
signature permission gate (Layer 0), meaning it is signed with the host's cert.
A rogue app with that cert is already fully trusted. However it is a theoretical
gap in the chain of identity verification.

**Resolution:**
Planned as part of the Sentinel architecture. A Sentinel-issued capability token
could accompany the `onHostConnected()` call, allowing the management plugin to
verify it against a Sentinel before accepting the host reference.

---

### IPC #6 — startStream() handle is an enumerable integer

**Severity:** Medium
**Status:** Deferred — planned standalone AIDL fix (see issue #6)
**Tracking:** IPC audit 2026-05-10 · finding #6

**Description:**
`IStoragePlugin.startStream()` returns a plain `int` handle. There is no ownership
check — any caller that holds an `IStoragePlugin` binder (i.e. the host) can call
`stopStream(N)` for any `N`, including handles belonging to other logical streams
started by a different pipeline node or management plugin. Handles are issued as
simple counters and are therefore guessable.

**Risk:**
Low in practice — only the host can call `stopStream()` (enforced by
`requireCallerIsHost()` in `StorageServiceBase`). Within the host process, a buggy
pipeline executor or a future code path could accidentally stop an unrelated stream.

**Resolution:**
Replace the `int` handle with an opaque `IBinder` token or UUID string. The storage
plugin issues the token; only the caller that received it can usefully pass it back.
This is a standalone AIDL breaking change; can be done without Sentinel involvement.

---

### IPC #7 — One-way callbacks deliver plaintext metric data cross-process

**Severity:** Low
**Status:** Accepted risk — architectural constraint
**Tracking:** IPC audit 2026-05-10 · finding #7

**Description:**
`IDataCallback`, `IManagementCallback`, `IStorageStreamCallback`, and
`IStorageQueryCallback` are declared `oneway` and carry raw metric values as plain
JSON strings across Binder. Any process with access to the Binder transaction stream
(root, debuggable builds, etc.) could observe the data.

**Risk:**
Android Binder transactions are not observable by unprivileged processes — this
requires root or a debuggable build. In production (non-debuggable APK, no root),
the risk is negligible. On rooted devices or during development, metric values are
visible in Binder traces.

**Resolution:**
Accepted risk. Full callback encryption would require a session key negotiated
during authentication, significant latency overhead, and a custom serialization
layer. The threat model does not justify this in the current architecture.
If metrics are classified as sensitive in a future enterprise tier, revisit with an
end-to-end encrypted channel (e.g., per-session AES-GCM keyed from the
challenge-response exchange).

---

### IPC #8 — requestExecute() dependency declarations are not verified by the host

**Severity:** Medium
**Status:** Open
**Tracking:** IPC audit 2026-05-10 · finding #8

**Description:**
`IPluginHostBridge.requestExecute(targetPluginId, configJson, callback)` is gated
by the host on whether `targetPluginId` is in the requesting plugin's
`PluginManifest.pluginDependencies` list. However, the dependency list is
self-declared in the plugin's own manifest — the host accepts whatever the plugin
claims. A malicious plugin could declare every other plugin as a dependency and
then invoke all of them arbitrarily.

**Risk:**
Medium. Requires a plugin to clear Layer 0 (OS signature permission), Layer 1 (cert
fingerprint check), and Layer 2 (challenge-response). A plugin that clears all three
layers is already fully trusted. The risk is primarily against the principle of least
privilege — even official plugins should only be able to trigger their declared
dependencies.

**Resolution:**
The host should cross-check declared dependencies against an authoritative allowlist
rather than trusting the plugin's self-declaration. The allowlist could be embedded
in the builtin Sentinel or in a signed plugin registry. No AIDL change required;
this is a host-side enforcement change in `PluginManager`.

---

### OFFICIAL_*_CERT_FINGERPRINTS are empty — Layer 1 pinning inactive in release builds

**Severity:** High
**Status:** Open — blocked on production signing cert provisioning
**Tracking:** Deferred work item

**Description:**
The six `OFFICIAL_*_CERT_FINGERPRINTS` sets in `AuthConstants` (now living in
`mondonode-sentinel-builtin` as private constants after the Sentinel migration) are
empty. `MondoNodeSdk.isTrusted()` therefore never matches any official fingerprint.
Layer 1 cert pinning is entirely inactive in release builds until these are populated.

Dev builds are covered by `MondoNodeSdk.init(devFingerprints = ...)` behind a
`BuildConfig.DEBUG` guard, so development flows work. The gap is in release.

**Risk:**
High. Without Layer 1 pinning, any APK that clears the OS signature permission
(Layer 0) can connect as a plugin. Layer 0 requires the same signing certificate,
which limits the blast radius in practice — only apps signed with the release key
can connect. However, defence-in-depth is broken; if the release key is compromised,
there is no second check.

**Resolution:**
Populate fingerprint sets before the first production release. See the release
checklist in the root `CLAUDE.md`. Three automated guards prevent dev fingerprints
from reaching a release build; the forward blocker is provisioning the production
signing certs and running `scripts/gen-release-signing-key.sh` (or equivalent).
