# mondonode-sdk — Architecture

## 1. Base Service Class Hierarchy

Each plugin category extends one abstract base class from the SDK. The base class handles Keystore key generation, nonce signing, `IBinder.linkToDeath()` wiring, and per-call UID validation via `requireCallerIsHost()`.

```mermaid
classDiagram
    direction TB

    class Service {
        <<Android>>
        +onBind(intent) IBinder
    }

    class PluginServiceBase {
        <<SDK · abstract>>
        #hostBridge IPluginHostBridge?
        +onBind(intent) IBinder
        #onGetManifest()* PluginManifest
        #onSubscribe(callback IDataCallback)*
        #onUnsubscribe()*
        #onConfigure(configJson String) Boolean*
        #onExecute(configJson String, callback IDataCallback)*
        #onHostBridgeDied() open
        -generateOrLoadKeyPair()
        -signNonce(nonce) String
    }

    class TransformerServiceBase {
        <<SDK · abstract>>
        +onBind(intent) IBinder
        #onGetManifest()* TransformerManifest
        #onTransform(capabilityId, inputValue, parameters, callback)*
    }

    class RouterServiceBase {
        <<SDK · abstract>>
        +onBind(intent) IBinder
        #onGetManifest()* RouterManifest
        #onEvaluate(capabilityId, inputValue, conditions, matchMode, callback)*
    }

    class OutputServiceBase {
        <<SDK · abstract>>
        +onBind(intent) IBinder
        #onGetManifest()* OutputManifest
        #onWrite(capabilityId, inputValues, parameters, callback)*
    }

    class StorageServiceBase {
        <<SDK · abstract>>
        +onBind(intent) IBinder
        #onGetManifest()* StorageManifest
        #onStreamWrite(capabilityId, inputValues, parameters, callback)*
        #onQueryRead(capabilityId, inputValues, parameters, callback)*
        #onQueryWrite(capabilityId, inputValues, parameters, callback)*
        #onStartStream(capabilityId, parameters, callback) Int open
        #onStopStream(handle Int) open
    }

    class ManagementServiceBase {
        <<SDK · abstract>>
        +onBind(intent) IBinder
        #onGetManifest()* ManagementPluginManifest
        #onHostConnected(host IMondoNodeHost)*
        #onHostDisconnected()*
        -hostDeathRecipient IBinder.DeathRecipient
    }

    class EventServiceBase {
        <<SDK · abstract>>
        +onBind(intent) IBinder
        #onGetManifest()* EventManifest
        #onStartEvent(capabilityId String, parameters Map~String,String~, callback IEventCallback)* Int
        #onStopEvent(handle Int)*
    }

    Service <|-- PluginServiceBase
    Service <|-- TransformerServiceBase
    Service <|-- RouterServiceBase
    Service <|-- OutputServiceBase
    Service <|-- StorageServiceBase
    Service <|-- ManagementServiceBase
    Service <|-- EventServiceBase
```

---

## 2. AIDL Interface Pairs

Every category uses one plugin-side interface (implemented by the APK, called by the host) and one or more host-side callbacks (implemented by the host, declared `oneway`).

```mermaid
classDiagram
    direction LR

    class IMondoPlugin {
        <<AIDL · plugin implements>>
        +authenticate(nonce) String
        +getPublicKey() String
        +getManifest() String
        +subscribe(IDataCallback)
        +unsubscribe()
        +configure(configJson) Boolean
        +execute(configJson, IDataCallback)
        +setHostBridge(IPluginHostBridge)
    }
    class IDataCallback {
        <<AIDL oneway · host implements>>
        +onMetricReceived(metricJson)
        +onPluginError(code, message)
        +onPluginStatusChanged(status)
    }
    IMondoPlugin --> IDataCallback : pushes to

    class ITransformerPlugin {
        <<AIDL · plugin implements>>
        +authenticate(nonce) String
        +getPublicKey() String
        +getManifest() String
        +transform(inputJson, configJson, ITransformCallback)
    }
    class ITransformCallback {
        <<AIDL oneway · host implements>>
        +onResult(outputJson)
        +onError(errorCode, message)
    }
    ITransformerPlugin --> ITransformCallback : returns to

    class IRouterPlugin {
        <<AIDL · plugin implements>>
        +authenticate(nonce) String
        +getPublicKey() String
        +getManifest() String
        +evaluate(inputJson, configJson, IRouteCallback)
    }
    class IRouteCallback {
        <<AIDL oneway · host implements>>
        +onMatched(matchedIndicesJson)
        +onNoMatch()
        +onError(errorCode, message)
    }
    IRouterPlugin --> IRouteCallback : returns to

    class IOutputPlugin {
        <<AIDL · plugin implements>>
        +authenticate(nonce) String
        +getPublicKey() String
        +getManifest() String
        +write(inputJson, configJson, IOutputCallback)
    }
    class IOutputCallback {
        <<AIDL oneway · host implements>>
        +onSuccess()
        +onError(errorCode, message)
    }
    IOutputPlugin --> IOutputCallback : acks to

    class IStoragePlugin {
        <<AIDL · plugin implements>>
        +authenticate(nonce) String
        +getPublicKey() String
        +getManifest() String
        +streamWrite(inputJson, configJson, IStorageWriteCallback)
        +queryRead(inputJson, configJson, IStorageQueryCallback)
        +queryWrite(inputJson, configJson, IStorageQueryCallback)
        +startStream(configJson, IStorageStreamCallback) Int
        +stopStream(handle Int)
    }
    class IStorageWriteCallback {
        <<AIDL oneway · host implements>>
        +onSuccess()
        +onError(errorCode, message)
    }
    class IStorageQueryCallback {
        <<AIDL oneway · host implements>>
        +onResult(resultsJson)
        +onError(errorCode, message)
    }
    class IStorageStreamCallback {
        <<AIDL oneway · host implements>>
        +onData(dataJson)
        +onError(code, message)
        +onEnd()
    }
    IStoragePlugin --> IStorageWriteCallback : acks stream write
    IStoragePlugin --> IStorageQueryCallback : returns query result
    IStoragePlugin --> IStorageStreamCallback : pushes stream data

    class IManagementPlugin {
        <<AIDL · management app implements>>
        +authenticate(nonce) String
        +getPublicKey() String
        +getManifest() String
        +onHostConnected(IMondoNodeHost)
        +onHostDisconnected()
    }
    class IMondoNodeHost {
        <<AIDL · host implements>>
        +register(IManagementCallback)
        +unregister(IManagementCallback)
        +getConnectedPlugins() String
        +getConnectedTransformers() String
        +getConnectedRouters() String
        +getConnectedOutputPlugins() String
        +getConnectedStorages() String
        +configurePlugin(pluginId, configJson) Boolean
        +setMonitors(monitorsJson)
        +getMonitors() String
        +getMonitorResults() String
        +getMonitorHistory(monitorId, limit) String
        +queryStorage(storageId, inputJson, configJson) String
        +getPipelineGraph() String
        +setPipelineGraph(graphJson) Boolean
        +subscribeOutput(callback, monitorId, metricKey)
        +unsubscribeOutput(callback, monitorId, metricKey)
        +subscribeMonitor(callback, monitorId)
        +unsubscribeMonitor(callback, monitorId)
    }
    class IManagementCallback {
        <<AIDL oneway · mgmt app implements>>
        +onPluginConnected(manifestJson)
        +onPluginDisconnected(pluginId)
        +onMetricReceived(pluginId, metricJson)
        +onPluginError(pluginId, errorCode, message)
    }
    IManagementPlugin --> IMondoNodeHost : receives on connect
    IMondoNodeHost --> IManagementCallback : pushes events

    class IPluginHostBridge {
        <<AIDL · host implements>>
        +isDependencyAvailable(targetPluginId) Boolean
        +requestExecute(targetPluginId, configJson, IPluginDependencyCallback)
    }
    class IPluginDependencyCallback {
        <<AIDL oneway · plugin implements>>
        +onResult(sourcePluginId, metricJson)
        +onError(sourcePluginId, errorCode, message)
    }
    IMondoPlugin --> IPluginHostBridge : calls for cross-plugin deps
    IPluginHostBridge --> IPluginDependencyCallback : delivers result

    class IEventPlugin {
        <<AIDL · plugin implements>>
        +authenticate(nonce) String
        +getPublicKey() String
        +getManifest() String
        +startEvent(configJson, IEventCallback) Int
        +stopEvent(handle Int)
    }
    class IEventCallback {
        <<AIDL oneway · host implements>>
        +onEvent(handle Int, portId String, value String)
        +onError(handle Int, errorCode String, message String)
        +onStopped(handle Int)
    }
    IEventPlugin --> IEventCallback : pushes events to
```

---

## 3. Manifest and Capability Types

Each plugin category has a manifest type (returned by `getManifest()`) and one or more capability types.

```mermaid
classDiagram
    direction TB

    class PluginManifest {
        +pluginId String
        +name String
        +version String
        +author String
        +description String
        +capabilities List~PluginCapability~
        +publicKeyBase64 String
        +pluginDependencies List~PluginDependency~
    }
    class PluginCapability {
        +id String
        +name String
        +description String
        +type PluginCapabilityType
        +parameters List~PluginParameterSpec~
        +inputQueueMode InputQueueMode
        +inputPorts List~PortSpec~
        +outputPorts List~PortSpec~
    }
    PluginManifest "1" *-- "1..*" PluginCapability

    class TransformerManifest {
        +transformerId String
        +name String
        +version String
        +author String
        +description String
        +capabilities List~TransformerCapability~
        +publicKeyBase64 String
    }
    class TransformerCapability {
        +id String
        +name String
        +description String
        +parameters List~PluginParameterSpec~
        +inputPorts List~PortSpec~
        +outputPorts List~PortSpec~
    }
    TransformerManifest "1" *-- "1..*" TransformerCapability

    class RouterManifest {
        +routerId String
        +name String
        +version String
        +author String
        +description String
        +capabilities List~RouterCapability~
        +publicKeyBase64 String
    }
    class RouterCapability {
        +id String
        +name String
        +description String
        +supportedConditionTypes List~String~
        +inputPorts List~PortSpec~
        +outputPorts List~PortSpec~
    }
    RouterManifest "1" *-- "1..*" RouterCapability

    class OutputManifest {
        +outputId String
        +name String
        +version String
        +author String
        +description String
        +capabilities List~OutputCapability~
        +publicKeyBase64 String
    }
    class OutputCapability {
        +id String
        +name String
        +description String
        +parameters List~PluginParameterSpec~
        +inputPorts List~PortSpec~
    }
    OutputManifest "1" *-- "1..*" OutputCapability

    class StorageManifest {
        +storageId String
        +name String
        +version String
        +author String
        +description String
        +capabilities List~StorageCapability~
        +publicKeyBase64 String
    }
    class StorageCapability {
        +id String
        +name String
        +description String
        +type StorageCapabilityType
        +parameters List~PluginParameterSpec~
        +inputPorts List~PortSpec~
        +outputPorts List~PortSpec~
    }
    class StorageCapabilityType {
        <<enum>>
        STREAM_WRITE
        QUERY_READ
        QUERY_WRITE
        STREAM_OUTPUT
    }
    StorageManifest "1" *-- "1..*" StorageCapability
    StorageCapability --> StorageCapabilityType

    class EventManifest {
        +eventId String
        +name String
        +version String
        +author String
        +description String
        +capabilities List~EventCapability~
        +publicKeyBase64 String
    }
    class EventCapability {
        +id String
        +name String
        +description String
        +outputPorts List~PortSpec~
        +parameters List~PluginParameterSpec~
    }
    EventManifest "1" *-- "1..*" EventCapability

    class ManagementPluginManifest {
        +pluginId String
        +label String
        +version String
        +accessLevel ManagementAccessLevel
        +publicKeyBase64 String
    }
    class ManagementAccessLevel {
        <<enum>>
        READ_ONLY
        READ_WRITE
    }
    ManagementPluginManifest --> ManagementAccessLevel

    class PluginParameterSpec {
        +key String
        +type ParamType
        +shortName String
        +description String
        +required Boolean
        +unit String?
        +validRange ValidRange?
        +acceptableValues List~String~?
        +defaultValue String?
    }
    class PortSpec {
        +id String
        +name String
        +dataType PortDataType
        +isFailPath Boolean
        +isDebugPath Boolean
        +isDynamic Boolean
        +description String
    }
    class ParamType {
        <<enum>>
        STRING · INT · FLOAT · BOOLEAN · ENUM
    }
    class PortDataType {
        <<enum>>
        STRING · JSON · NUMBER · BOOLEAN · ANY
    }
```

---

## 4. RouterCondition Sealed Hierarchy

Used by router plugins and `RouterConditionEvaluator` (SDK `object`).

```mermaid
classDiagram
    class RouterCondition {
        <<sealed>>
    }
    class StringCondition {
        +operator StringOperator
        +value String
        +ignoreCase Boolean
    }
    class NumericCondition {
        +operator NumericOperator
        +value Double
    }
    class RegexCondition {
        +pattern String
        +ignoreCase Boolean
    }
    class CompoundCondition {
        +logicalOperator LogicalOperator
        +operands List~RouterCondition~
    }
    class StringOperator {
        <<enum>>
        EQUALS · NOT_EQUALS
        STARTS_WITH · ENDS_WITH · CONTAINS
    }
    class NumericOperator {
        <<enum>>
        EQUALS · NOT_EQUALS
        LESS_THAN · GREATER_THAN
        LESS_THAN_OR_EQUAL · GREATER_THAN_OR_EQUAL
    }
    class LogicalOperator {
        <<enum>>
        AND · OR · NOT
    }
    class MatchMode {
        <<enum>>
        FIRST
        MULTIPLE
    }
    class RouterConditionEvaluator {
        <<SDK object>>
        +evaluate(input String, condition RouterCondition) Boolean
    }

    RouterCondition <|-- StringCondition
    RouterCondition <|-- NumericCondition
    RouterCondition <|-- RegexCondition
    RouterCondition <|-- CompoundCondition
    CompoundCondition "1" *-- "1..*" RouterCondition : operands (recursive)
    StringCondition --> StringOperator
    NumericCondition --> NumericOperator
    CompoundCondition --> LogicalOperator
    RouterConditionEvaluator ..> RouterCondition : dispatches on sealed type
```

---

## 5. Auth Utilities

```mermaid
classDiagram
    class MondoNodeSdk {
        <<SDK object>>
        +init(devFingerprints Set~String~)
        +isTrusted(fingerprint, officialSet) Boolean
        note: called by PluginValidator and Sentinels
        note: caller passes the per-category official set
        note: official sets now live in mondonode-sentinel-builtin
    }

    class HostCallerCheck {
        <<SDK · HostCallerCheck.kt>>
        +requireCallerIsHost(context Context)
        note: checks Binder.getCallingUid()
        note: against com.systemhalted.mondonode UID
        note: no-op for same-process calls
    }

    class AuthConstants {
        <<SDK object>>
        PLUGIN_SERVICE_ACTION String
        TRANSFORMER_SERVICE_ACTION String
        ROUTER_SERVICE_ACTION String
        OUTPUT_SERVICE_ACTION String
        STORAGE_SERVICE_ACTION String
        EVENT_SERVICE_ACTION String
        MANAGEMENT_PLUGIN_SERVICE_ACTION String
        MANAGER_SERVICE_ACTION String
        SENTINEL_SERVICE_ACTION String
        BIND_PLUGIN_PERMISSION String
        BIND_TRANSFORMER_PERMISSION String
        BIND_ROUTER_PERMISSION String
        BIND_OUTPUT_PERMISSION String
        BIND_STORAGE_PERMISSION String
        BIND_EVENT_PERMISSION String
        BIND_MANAGEMENT_PLUGIN_PERMISSION String
        BIND_SENTINEL_PERMISSION String
        HOST_PACKAGE_NAME String
        KEY_ALGORITHM String
        SIGNATURE_ALGORITHM String
        EC_CURVE String
        *_KEYSTORE_ALIAS String (per category)
    }

    class EncryptedPrefsHelper {
        <<SDK · top-level function>>
        +createEncryptedPrefs(context, name) SharedPreferences
        note: AES256_SIV key encryption
        note: AES256_GCM value encryption
        note: Android Keystore MasterKey
    }

    class IntervalParser {
        <<SDK · top-level function>>
        +parseIntervalSeconds(input String) Int?
        note: "5m" → 300, "1h30m" → 5400
        note: bare integer = seconds, blank = null
    }
```
