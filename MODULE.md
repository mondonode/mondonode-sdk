# Module mondonode-sdk

Shared library for [MondoNode](https://systemhalted.com) plugin development.

Provides everything a plugin author needs to integrate with the MondoNode host:

- **AIDL interfaces** — `IMondoPlugin`, `ITransformerPlugin`, `IRouterPlugin`, `IOutputPlugin`,
  `IStoragePlugin`, `IEventPlugin`, `IManagementPlugin`, `IMondoNodeHost`, `IPluginHostBridge`,
  and their corresponding callback interfaces (`IDataCallback`, `ITransformCallback`,
  `IRouteCallback`, `IOutputCallback`, `IStorageWriteCallback`, `IStorageQueryCallback`,
  `IStorageStreamCallback`, `IEventCallback`, `IManagementCallback`,
  `IPluginDependencyCallback`).
- **Base service classes** — `PluginServiceBase`, `TransformerServiceBase`, `RouterServiceBase`,
  `OutputServiceBase`, `StorageServiceBase`, `EventServiceBase`, `ManagementServiceBase`. Extend
  one of these in your `Service` instead of implementing the raw AIDL interface; authentication,
  Keystore key management, per-call UID validation, and binder death detection are handled for
  you.
- **Data models** — `PluginManifest`, `TransformerManifest`, `RouterManifest`, `OutputManifest`,
  `StorageManifest`, `EventManifest`, `ManagementPluginManifest`, `MetricData`,
  `MonitorDefinition`, `PipelineGraph` / `PipelineGraphNode` / `PipelineGraphEdge`, `PortSpec`,
  `RouterCondition` (sealed), and all supporting types — all serializable via
  `kotlinx.serialization`.
- **Constants** — `AuthConstants` (intent actions, bind permissions, Keystore aliases,
  cryptographic algorithm names, host package name), `OfficialPluginRegistry` (official plugin
  ID constants).
- **Utilities** — `parseIntervalSeconds()` for human-readable schedule strings (`"5m"`,
  `"1h30m"`); `createEncryptedPrefs()` for encrypted `SharedPreferences`;
  `requireCallerIsHost()` for per-call UID validation; `RouterConditionEvaluator` for
  evaluating `RouterCondition` against a string value.

> **Note on cert fingerprints:** The `OFFICIAL_*_CERT_FINGERPRINTS` sets that previously lived
> in `AuthConstants` have moved to `mondonode-sentinel-builtin` as private constants. The host
> no longer checks fingerprints directly; all trust decisions go through the Sentinel chain.

## Quick start

Add the dependency (once published to Maven Central):

```kotlin
// build.gradle.kts
dependencies {
    implementation("com.systemhalted:mondonode-sdk:<version>")
}
```

Declare your service in `AndroidManifest.xml`:

```xml
<service
    android:name=".MyMonitorService"
    android:exported="true"
    android:permission="com.systemhalted.mondonode.BIND_PLUGIN">
    <intent-filter>
        <action android:name="com.systemhalted.mondonode.PLUGIN" />
    </intent-filter>
</service>
```

Extend the appropriate base class:

```kotlin
class MyMonitorService : PluginServiceBase() {
    override fun onGetManifest(): PluginManifest = PluginManifest(
        pluginId    = "com.example.myplugin",
        name        = "My Plugin",
        version     = "1.0.0",
        author      = "Example Corp",
        description = "Does something useful.",
        capabilities = listOf(/* ... */),
        publicKeyBase64 = "" // filled by base class after Keystore key generation
    )

    override fun onSubscribe(callback: IDataCallback) { /* push metrics via callback */ }
    override fun onUnsubscribe() { /* stop all metric delivery */ }
    override fun onConfigure(configJson: String): Boolean = true
}
```

See the [mondonode-plugin-template](https://github.com/systemhalted) repository for a complete
working example.

## Package structure

| Package | Contents |
|---|---|
| `com.systemhalted.mondonode.sdk` | All types — interfaces, base classes, data models, constants |
