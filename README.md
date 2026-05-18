# MondoNode SDK

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Maven Central](https://img.shields.io/maven-central/v/com.systemhalted/mondonode-sdk)](https://central.sonatype.com/artifact/com.systemhalted/mondonode-sdk)

Shared library for [MondoNode](https://systemhalted.com) plugin development.

Provides AIDL interfaces, base service classes, authentication helpers, and serializable data
models used by the MondoNode host and all plugin APKs.

---

## Requirements

- Android **minSdk 26** (Android 8.0 Oreo); built against `compileSdk 35`
- Kotlin **2.0.21**, `jvmTarget = 17` / `sourceCompatibility = 17`
- Built with AGP **8.5.2** and Gradle **8.9**

---

## Installation

```kotlin
// build.gradle.kts
dependencies {
    implementation("com.systemhalted:mondonode-sdk:<version>")
}
```

Replace `<version>` with the latest release from
[Maven Central](https://central.sonatype.com/artifact/com.systemhalted/mondonode-sdk).

---

## Plugin types

MondoNode supports seven plugin categories. Each has a corresponding base class in this SDK:

| Plugin type | Base class | Intent action | Bind permission |
|---|---|---|---|
| Monitoring | `PluginServiceBase` | `com.systemhalted.mondonode.PLUGIN` | `com.systemhalted.mondonode.BIND_PLUGIN` |
| Transformer | `TransformerServiceBase` | `com.systemhalted.mondonode.TRANSFORMER` | `com.systemhalted.mondonode.BIND_TRANSFORMER` |
| Router | `RouterServiceBase` | `com.systemhalted.mondonode.ROUTER` | `com.systemhalted.mondonode.BIND_ROUTER` |
| Output | `OutputServiceBase` | `com.systemhalted.mondonode.OUTPUT` | `com.systemhalted.mondonode.BIND_OUTPUT` |
| Storage | `StorageServiceBase` | `com.systemhalted.mondonode.STORAGE` | `com.systemhalted.mondonode.BIND_STORAGE` |
| Event | `EventServiceBase` | `com.systemhalted.mondonode.EVENT` | `com.systemhalted.mondonode.BIND_EVENT` |
| Management app | `ManagementServiceBase` | `com.systemhalted.mondonode.MANAGEMENT_PLUGIN` | `com.systemhalted.mondonode.BIND_MANAGEMENT_PLUGIN` |

The bind permission is declared at `protectionLevel="signature"` by the host application; plugin
services must declare `android:permission="…"` on their `<service>` element so that only the host
(signed with the same certificate as the permission declaration) can bind to them.

---

## Quick start: monitoring plugin

### 1. Add the dependency

```kotlin
dependencies {
    implementation("com.systemhalted:mondonode-sdk:<version>")
}
```

### 2. Declare the service

```xml
<!-- AndroidManifest.xml -->
<service
    android:name=".MyMonitorService"
    android:exported="true"
    android:permission="com.systemhalted.mondonode.BIND_PLUGIN">
    <intent-filter>
        <action android:name="com.systemhalted.mondonode.PLUGIN" />
    </intent-filter>
</service>
```

### 3. Extend `PluginServiceBase`

```kotlin
class MyMonitorService : PluginServiceBase() {

    override fun onGetManifest() = PluginManifest(
        pluginId     = "com.example.myplugin",
        name         = "My Plugin",
        version      = "1.0.0",
        author       = "Example Corp",
        description  = "Measures something useful.",
        capabilities = listOf(
            PluginCapability(
                type        = PluginCapabilityType.NETWORK_MONITOR,
                id          = "my_metric",
                name        = "My Metric",
                description = "Measures something.",
                parameters  = listOf(
                    PluginParameterSpec(
                        key         = "target",
                        type        = ParamType.STRING,
                        shortName   = "Target",
                        description = "The thing to measure.",
                        required    = true
                    )
                )
            )
        ),
        publicKeyBase64 = "" // filled by base class after Keystore key generation
    )

    override fun onSubscribe(callback: IDataCallback) {
        // start periodic work; push MetricData JSON to callback.onMetricReceived(...)
    }

    override fun onUnsubscribe() {
        // cancel all in-flight metric delivery
    }

    override fun onConfigure(configJson: String): Boolean {
        // parse and apply the JSON config; return true on success
        return true
    }
}
```

`PluginServiceBase` handles:
- Android Keystore EC key generation on first run (`secp256r1`)
- Challenge-response authentication with the host (`SHA256withECDSA`)
- Per-call UID validation via `requireCallerIsHost()` on every AIDL method
- Routing the `IPluginHostBridge` to the `hostBridge` property and tracking host process
  death via `IBinder.linkToDeath` (override `onHostBridgeDied()` to react)
- A default `onExecute()` for alarm-triggered single-shot runs (override for a more
  efficient one-off path)

---

## Quick start: transformer plugin

```kotlin
class MyTransformerService : TransformerServiceBase() {

    override fun onGetManifest() = TransformerManifest(
        transformerId = "com.example.mytransformer",
        name          = "My Transformer",
        version       = "1.0.0",
        author        = "Example Corp",
        description   = "Transforms a string value.",
        capabilities  = listOf(
            TransformerCapability(
                id                = "reverse",
                name              = "Reverse",
                description       = "Reverses the input string.",
                inputDescription  = "Any string",
                outputDescription = "The input string reversed"
            )
        ),
        publicKeyBase64 = ""
    )

    override fun onTransform(
        capabilityId: String,
        inputValue: String,
        parameters: Map<String, String>,
        callback: ITransformCallback
    ) {
        // runs on Dispatchers.IO; call callback exactly once
        callback.onResult("""{"value":"${inputValue.reversed()}"}""")
    }
}
```

Output must be a JSON object. The `"value"` key maps to the `"output"` port. Additional keys
map to ports with matching IDs.

---

## Pipeline graph model

The host represents all processing as a `PipelineGraph` — a directed graph of
`PipelineGraphNode` instances connected by `PipelineGraphEdge` instances. Edges carry string
values between named ports on nodes.

```
EventNode ──"output"──► MonitorNode ──"output"──► TransformerNode ──"output"──► OutputNode
```

Key types:

| Type | Description |
|---|---|
| `PipelineGraph` | The full workspace graph; stored and managed by the host |
| `PipelineGraphNode` | One processing unit instance (`MONITOR`, `TRANSFORMER`, `ROUTER`, `OUTPUT`, `STORAGE`, `EVENT`) |
| `PipelineGraphEdge` | A directed connection from `fromNodeId:fromPortId` to `toNodeId:toPortId` |
| `PortSpec` | Declares a named port on a capability (id, name, data type, flags) |
| `FanInTriggerMode` | Controls when a multi-input node fires: `ANY_CHANGE` or `ALL_CHANGE` |

Monitors no longer have built-in scheduling. To run a monitor periodically, connect an
`EVENT` node's `"output"` port to the monitor node's `"trigger"` input port. Each `"true"`
value arriving on the trigger port enqueues one monitor execution.

The legacy `MonitorDefinition.pipeline: List<PipelineStep>` model and the `SchedulingMode`
enum are `@Deprecated` and retained only for safe deserialization of legacy JSON. New code
should use `PipelineGraph`.

---

## Router conditions

Router plugins evaluate a `RouterCondition` against an incoming value:

```kotlin
// Match if value is "ok" or "OK"
val cond = RouterCondition.StringCondition(
    operator   = StringOperator.EQUALS,
    value      = "ok",
    ignoreCase = true
)

// Match if numeric value > 100
val numCond = RouterCondition.NumericCondition(
    operator = NumericOperator.GREATER_THAN,
    value    = 100.0
)

// Compound: value starts with "err" AND length > 3
val compound = RouterCondition.CompoundCondition(
    logicalOperator = LogicalOperator.AND,
    operands = listOf(
        RouterCondition.StringCondition(StringOperator.STARTS_WITH, "err"),
        RouterCondition.RegexCondition(pattern = ".{4,}")
    )
)
```

---

## Storage and event plugins

Storage plugins (`StorageServiceBase`) implement `IStoragePlugin`, which has four entry
points selected by the capability's `StorageCapabilityType`:

| Capability type | Method | Purpose |
|---|---|---|
| `STREAM_WRITE` | `onStreamWrite()` | Fire-and-forget writes (e.g. append a time-series sample) |
| `QUERY_READ` | `onQueryRead()` | Read-only queries that return data |
| `QUERY_WRITE` | `onQueryWrite()` | Mutating operations (set, delete, CAS) that also return a result |
| `STREAM_OUTPUT` | `onStartStream()` / `onStopStream()` | Long-lived stream source; pushes values via `IStorageStreamCallback.onData()` |

Read/write separation is enforced structurally by the AIDL: the host invokes
`queryRead()` or `queryWrite()` based on the manifest-declared capability type.

Event plugins (`EventServiceBase`) implement `IEventPlugin`. Override `onStartEvent()` to
begin emitting values and return a non-negative handle; override `onStopEvent(handle)` to
clean up. The host activates events at pipeline activation time and routes each
`IEventCallback.onEvent()` value through the graph from the declared output port.

---

## Cross-plugin dependencies

A monitoring plugin may declare dependencies on other monitoring plugins in
`PluginManifest.pluginDependencies`. After authentication, the host calls
`IMondoPlugin.setHostBridge(bridge)` and `PluginServiceBase` exposes the bridge through
the `hostBridge` property:

```kotlin
hostBridge?.requestExecute(
    targetPluginId = "com.example.other-plugin",
    configJson     = """{"capability_id":"do_thing"}""",
    callback       = object : IPluginDependencyCallback.Stub() {
        override fun onResult(sourcePluginId: String, metricJson: String) { /* ... */ }
        override fun onError(sourcePluginId: String, errorCode: String, message: String) { /* ... */ }
    }
)
```

The host only routes calls to plugin IDs the requesting plugin has declared in
`pluginDependencies` — undeclared calls are rejected.

---

## Schedule strings

`parseIntervalSeconds(input)` (top-level function in `IntervalParser.kt`) parses
human-readable interval strings into seconds — used by event plugins such as
`mondonode-plugin-simple-timer`:

```kotlin
parseIntervalSeconds("30")     // → 30
parseIntervalSeconds("5m")     // → 300
parseIntervalSeconds("1h30m")  // → 5400
parseIntervalSeconds("")       // → null
```

---

## Encrypted preferences

`createEncryptedPrefs(context, name)` (top-level function in `EncryptedPrefsHelper.kt`) is
the only approved way to obtain a `SharedPreferences` in a plugin. It returns an
`androidx.security.crypto.EncryptedSharedPreferences` backed by an Android Keystore
`MasterKey` (AES-256-SIV for keys, AES-256-GCM for values):

```kotlin
val prefs = createEncryptedPrefs(context, "my_plugin_prefs")
```

Note: `EncryptedSharedPreferences` does not support `getAll()` — if you need to enumerate
entries, maintain an explicit index key inside the store.

---

## Timestamps

All timestamps in this SDK are **UTC epoch milliseconds** (`Long`). Never use
`TimeZone.getDefault()` or locale-aware formatters when working with `MetricData.timestampMs`.
Use `System.currentTimeMillis()` or `Instant.now().toEpochMilli()` as sources.

---

## Plugin template

See the
[mondonode-plugin-template]([github repo])
repository for a complete, buildable starting point.

---

## R8 / ProGuard

The SDK ships a `consumer-rules.pro` file that is automatically merged into your APK's R8
configuration. It keeps all SDK AIDL interfaces, generated Stub/Proxy classes, and
kotlinx.serialization infrastructure — you do not need to copy any SDK rules into your own
`proguard-rules.pro`.

You **do** need to add rules for classes defined in your own plugin. Copy the template below
into your module's `proguard-rules.pro` and replace `com.example.yourplugin` with your actual
package:

```proguard
# Keep your plugin Service subclass so the host can discover and bind to it.
-keep class com.example.yourplugin.**Service { *; }

# kotlinx.serialization — keep generated serializers for every @Serializable class
# you define in your plugin. R8 cannot statically see the descriptor lookups the
# serialization runtime performs at startup.
-keepclassmembers class com.example.yourplugin.** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep class com.example.yourplugin.**$$serializer { *; }

# If your plugin uses third-party libraries that rely on reflection (e.g. OkHttp, Ktor,
# Gson, Moshi, Retrofit), add their recommended keep rules here. The
# mondonode-plugin-template repository includes examples for common networking libraries.
```

---

## Third-party dependencies

| Library | Version | License |
|---|---|---|
| `org.jetbrains.kotlinx:kotlinx-serialization-json` | 1.7.3 | Apache 2.0 |
| `org.jetbrains.kotlinx:kotlinx-coroutines-android` | 1.9.0 | Apache 2.0 |
| `androidx.security:security-crypto` | (project catalog) | Apache 2.0 |

---

## License

```
MIT License

Copyright (c) 2025 SystemHalted

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
