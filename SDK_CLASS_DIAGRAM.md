```mermaid
classDiagram
    %% ── AIDL INTERFACES ─────────────────────────────────────────────────

    class IMondoPlugin {
        <<interface>>
        +authenticate(nonce: byte[]) String
        +getPublicKey() String
        +getManifest() String
        +subscribe(callback: IDataCallback)
        +unsubscribe()
        +configure(configJson: String) Boolean
        +execute(configJson: String, callback: IDataCallback)
        +setHostBridge(bridge: IPluginHostBridge)
    }

    class IDataCallback {
        <<interface>>
        <<oneway>>
        +onMetricReceived(metricJson: String)
        +onPluginError(errorCode: String, message: String)
        +onPluginStatusChanged(status: String)
    }

    class IPluginHostBridge {
        <<interface>>
        +isDependencyAvailable(targetPluginId: String) Boolean
        +requestExecute(targetPluginId: String, configJson: String, callback: IPluginDependencyCallback)
    }

    class IPluginDependencyCallback {
        <<interface>>
        <<oneway>>
        +onResult(sourcePluginId: String, metricJson: String)
        +onError(sourcePluginId: String, errorCode: String, message: String)
    }

    class IManagementPlugin {
        <<interface>>
        +authenticate(nonce: byte[]) String
        +getPublicKey() String
        +getManifest() String
        +onHostConnected(host: IMondoNodeHost)
        +onHostDisconnected()
    }

    class IManagementCallback {
        <<interface>>
        <<oneway>>
        +onPluginConnected(manifestJson: String)
        +onPluginDisconnected(pluginId: String)
        +onMetricReceived(pluginId: String, metricJson: String)
        +onPluginError(pluginId: String, errorCode: String, message: String)
    }

    class IMondoNodeHost {
        <<interface>>
        +register(callback: IManagementCallback)
        +unregister(callback: IManagementCallback)
        +getConnectedPlugins() String
        +configurePlugin(pluginId: String, configJson: String) Boolean
        +setMonitors(monitorsJson: String)
        +getMonitors() String
        +getMonitorResults() String
        +getMonitorHistory(monitorId: String, limit: Int) String
        +getConnectedTransformers() String
        +getConnectedRouters() String
        +getConnectedOutputPlugins() String
        +getConnectedStorages() String
        +queryStorage(storageId: String, inputJson: String, configJson: String) String
        +getPipelineGraph() String
        +setPipelineGraph(graphJson: String) Boolean
        +subscribeOutput(callback: IManagementCallback, monitorId: String, metricKey: String)
        +unsubscribeOutput(callback: IManagementCallback, monitorId: String, metricKey: String)
        +subscribeMonitor(callback: IManagementCallback, monitorId: String)
        +unsubscribeMonitor(callback: IManagementCallback, monitorId: String)
    }

    class ITransformerPlugin {
        <<interface>>
        +authenticate(nonce: byte[]) String
        +getPublicKey() String
        +getManifest() String
        +transform(inputJson: String, configJson: String, callback: ITransformCallback)
    }

    class ITransformCallback {
        <<interface>>
        <<oneway>>
        +onResult(outputJson: String)
        +onError(errorCode: String, message: String)
    }

    class IRouterPlugin {
        <<interface>>
        +authenticate(nonce: byte[]) String
        +getPublicKey() String
        +getManifest() String
        +evaluate(inputJson: String, configJson: String, callback: IRouteCallback)
    }

    class IRouteCallback {
        <<interface>>
        <<oneway>>
        +onMatched(matchedIndicesJson: String)
        +onNoMatch()
        +onError(errorCode: String, message: String)
    }

    class IOutputPlugin {
        <<interface>>
        +authenticate(nonce: byte[]) String
        +getPublicKey() String
        +getManifest() String
        +write(inputJson: String, configJson: String, callback: IOutputCallback)
    }

    class IOutputCallback {
        <<interface>>
        <<oneway>>
        +onSuccess()
        +onError(errorCode: String, message: String)
    }

    class IStoragePlugin {
        <<interface>>
        +authenticate(nonce: byte[]) String
        +getPublicKey() String
        +getManifest() String
        +streamWrite(inputJson: String, configJson: String, callback: IStorageWriteCallback)
        +queryRead(inputJson: String, configJson: String, callback: IStorageQueryCallback)
        +queryWrite(inputJson: String, configJson: String, callback: IStorageQueryCallback)
        +startStream(configJson: String, callback: IStorageStreamCallback) Int
        +stopStream(handle: Int)
    }

    class IStorageWriteCallback {
        <<interface>>
        <<oneway>>
        +onSuccess()
        +onError(errorCode: String, message: String)
    }

    class IStorageQueryCallback {
        <<interface>>
        <<oneway>>
        +onResult(resultsJson: String)
        +onError(errorCode: String, message: String)
    }

    class IStorageStreamCallback {
        <<interface>>
        <<oneway>>
        +onData(dataJson: String)
        +onError(errorCode: String, message: String)
        +onEnd()
    }

    class IEventPlugin {
        <<interface>>
        +authenticate(nonce: byte[]) String
        +getPublicKey() String
        +getManifest() String
        +startEvent(configJson: String, callback: IEventCallback) Int
        +stopEvent(handle: Int)
    }

    class IEventCallback {
        <<interface>>
        <<oneway>>
        +onEvent(handle: Int, portId: String, value: String)
        +onError(handle: Int, errorCode: String, message: String)
        +onStopped(handle: Int)
    }

    %% ── SERVICE BASE CLASSES ─────────────────────────────────────────────

    class PluginServiceBase {
        <<abstract>>
        #hostBridge: IPluginHostBridge?
        #onGetManifest() PluginManifest
        #onSubscribe(callback: IDataCallback)
        #onUnsubscribe()
        #onConfigure(configJson: String) Boolean
        #onExecute(configJson: String, callback: IDataCallback)
    }

    class ManagementServiceBase {
        <<abstract>>
        #onGetManifest() ManagementPluginManifest
        #onHostConnected(host: IMondoNodeHost)
        #onHostDisconnected()
    }

    class TransformerServiceBase {
        <<abstract>>
        #onGetManifest() TransformerManifest
        #onTransform(capabilityId: String, inputValue: String, parameters: Map~String,String~, callback: ITransformCallback)
    }

    class RouterServiceBase {
        <<abstract>>
        #onGetManifest() RouterManifest
        #onEvaluate(capabilityId: String, inputValue: String, conditions: List~RouterCondition~, matchMode: MatchMode, callback: IRouteCallback)
    }

    class OutputServiceBase {
        <<abstract>>
        #onGetManifest() OutputManifest
        #onWrite(capabilityId: String, inputValues: Map~String,String~, parameters: Map~String,String~, callback: IOutputCallback)
    }

    class StorageServiceBase {
        <<abstract>>
        #onGetManifest() StorageManifest
        #onStreamWrite(capabilityId: String, inputValues: Map~String,String~, parameters: Map~String,String~, callback: IStorageWriteCallback)
        #onQueryRead(capabilityId: String, inputValues: Map~String,String~, parameters: Map~String,String~, callback: IStorageQueryCallback)
        #onQueryWrite(capabilityId: String, inputValues: Map~String,String~, parameters: Map~String,String~, callback: IStorageQueryCallback)
        #onStartStream(capabilityId: String, parameters: Map~String,String~, callback: IStorageStreamCallback) Int
        #onStopStream(handle: Int)
    }

    class EventServiceBase {
        <<abstract>>
        #onGetManifest() EventManifest
        #onStartEvent(capabilityId: String, parameters: Map~String,String~, callback: IEventCallback) Int
        #onStopEvent(handle: Int)
    }

    PluginServiceBase ..|> IMondoPlugin
    ManagementServiceBase ..|> IManagementPlugin
    TransformerServiceBase ..|> ITransformerPlugin
    RouterServiceBase ..|> IRouterPlugin
    OutputServiceBase ..|> IOutputPlugin
    StorageServiceBase ..|> IStoragePlugin
    EventServiceBase ..|> IEventPlugin

    IMondoPlugin --> IDataCallback : pushes metrics
    IMondoPlugin ..> IPluginHostBridge : receives via setHostBridge
    IPluginHostBridge --> IPluginDependencyCallback : delivers dependency result
    IManagementPlugin ..> IMondoNodeHost : receives via onHostConnected
    IMondoNodeHost --> IManagementCallback : pushes events
    ITransformerPlugin --> ITransformCallback : returns result
    IRouterPlugin --> IRouteCallback : returns match
    IOutputPlugin --> IOutputCallback : acks write
    IStoragePlugin --> IStorageWriteCallback : acks stream write
    IStoragePlugin --> IStorageQueryCallback : returns query result
    IStoragePlugin --> IStorageStreamCallback : pushes stream data
    IEventPlugin --> IEventCallback : pushes events

    %% ── MANIFEST DATA CLASSES ────────────────────────────────────────────

    class PluginManifest {
        +pluginId: String
        +name: String
        +version: String
        +author: String
        +description: String
        +capabilities: List~PluginCapability~
        +publicKeyBase64: String
        +pluginDependencies: List~PluginDependency~
    }

    class ManagementPluginManifest {
        +pluginId: String
        +label: String
        +version: String
        +accessLevel: ManagementAccessLevel
        +publicKeyBase64: String
    }

    class ManagementAccessLevel {
        <<enumeration>>
        READ_ONLY
        READ_WRITE
    }
    ManagementPluginManifest ..> ManagementAccessLevel

    class TransformerManifest {
        +transformerId: String
        +name: String
        +version: String
        +author: String
        +description: String
        +capabilities: List~TransformerCapability~
        +publicKeyBase64: String
    }

    class RouterManifest {
        +routerId: String
        +name: String
        +version: String
        +author: String
        +description: String
        +capabilities: List~RouterCapability~
        +publicKeyBase64: String
    }

    class OutputManifest {
        +outputId: String
        +name: String
        +version: String
        +author: String
        +description: String
        +capabilities: List~OutputCapability~
        +publicKeyBase64: String
    }

    class StorageManifest {
        +storageId: String
        +name: String
        +version: String
        +author: String
        +description: String
        +capabilities: List~StorageCapability~
        +publicKeyBase64: String
    }

    PluginServiceBase ..> PluginManifest : returns
    ManagementServiceBase ..> ManagementPluginManifest : returns
    TransformerServiceBase ..> TransformerManifest : returns
    RouterServiceBase ..> RouterManifest : returns
    OutputServiceBase ..> OutputManifest : returns
    StorageServiceBase ..> StorageManifest : returns
    EventServiceBase ..> EventManifest : returns
    EventManifest "1" *-- "1..*" EventCapability

    %% ── CAPABILITY DATA CLASSES ──────────────────────────────────────────

    class PluginCapability {
        +type: PluginCapabilityType
        +id: String
        +name: String
        +description: String
        +parameters: List~PluginParameterSpec~
        +inputQueueMode: InputQueueMode
        +outputPorts: List~PortSpec~
        +inputPorts: List~PortSpec~
    }

    class TransformerCapability {
        +id: String
        +name: String
        +description: String
        +inputDescription: String
        +outputDescription: String
        +parameters: List~PluginParameterSpec~
        +inputPorts: List~PortSpec~
        +outputPorts: List~PortSpec~
    }

    class RouterCapability {
        +id: String
        +name: String
        +description: String
        +supportedConditionTypes: List~String~
        +inputPorts: List~PortSpec~
        +outputPorts: List~PortSpec~
    }

    class OutputCapability {
        +id: String
        +name: String
        +description: String
        +inputDescription: String
        +parameters: List~PluginParameterSpec~
        +inputPorts: List~PortSpec~
    }

    class StorageCapability {
        +id: String
        +name: String
        +description: String
        +type: StorageCapabilityType
        +parameters: List~PluginParameterSpec~
        +inputPorts: List~PortSpec~
        +outputPorts: List~PortSpec~
    }

    class StorageCapabilityType {
        <<enumeration>>
        STREAM_WRITE
        QUERY_READ
        QUERY_WRITE
        STREAM_OUTPUT
    }

    class EventManifest {
        +eventId: String
        +name: String
        +version: String
        +author: String
        +description: String
        +capabilities: List~EventCapability~
        +publicKeyBase64: String
    }

    class EventCapability {
        +id: String
        +name: String
        +description: String
        +outputPorts: List~PortSpec~
        +parameters: List~PluginParameterSpec~
    }

    class PluginParameterSpec {
        +key: String
        +type: ParamType
        +shortName: String
        +description: String
        +required: Boolean
        +unit: String?
        +validRange: ValidRange?
        +acceptableValues: List~String~?
        +defaultValue: String?
    }

    class ValidRange {
        +min: Double
        +max: Double
    }

    class PluginDependency {
        +pluginId: String
        +name: String
        +required: Boolean
        +reason: String
    }

    PluginManifest "1" *-- "0..*" PluginCapability
    PluginManifest "1" *-- "0..*" PluginDependency
    TransformerManifest "1" *-- "1..*" TransformerCapability
    RouterManifest "1" *-- "1..*" RouterCapability
    OutputManifest "1" *-- "1..*" OutputCapability
    StorageManifest "1" *-- "1..*" StorageCapability
    PluginCapability "1" *-- "0..*" PluginParameterSpec
    TransformerCapability "1" *-- "0..*" PluginParameterSpec
    OutputCapability "1" *-- "0..*" PluginParameterSpec
    StorageCapability "1" *-- "0..*" PluginParameterSpec
    StorageCapability ..> StorageCapabilityType
    PluginParameterSpec "1" o-- "0..1" ValidRange

    %% ── MONITOR DEFINITION & METRIC ──────────────────────────────────────

    class MonitorDefinition {
        +id: String
        +name: String
        +pluginId: String
        +enabled: Boolean
        +schedulingMode: SchedulingMode
        +intervalSeconds: Int?
        +parameters: Map~String,String~
        +parameterBindings: Map~String,ParameterBinding~
        +inputTriggerKey: String?
        +inputQueueMode: InputQueueMode?
        +pipeline: List~PipelineStep~
        +effectivePipeline() List~PipelineStep~
    }

    class MetricData {
        +pluginId: String
        +capabilityId: String
        +timestampMs: Long
        +values: Map~String,String~
        +monitorId: String?
    }

    class ParameterBinding {
        <<@Deprecated · use PipelineGraphEdge>>
        +sourceMonitorId: String
        +metricKey: String
        +fallback: String?
    }

    MonitorDefinition "1" *-- "0..*" PipelineStep
    MonitorDefinition "1" o-- "0..*" ParameterBinding
    MonitorDefinition ..> SchedulingMode
    MonitorDefinition ..> InputQueueMode

    %% ── PIPELINE STEP HIERARCHY (DEPRECATED — replaced by PipelineGraph) ─

    class PipelineStep {
        <<sealed · @Deprecated>>
    }

    class TransformerPipelineStep {
        +step: TransformerStep
    }

    class RouterPipelineStep {
        +step: RouterStep
    }

    class LoopPipelineStep {
        +step: LoopStep
    }

    class QueueWritePipelineStep {
        +step: QueueStep
    }

    class QueueDrainPipelineStep {
        +step: QueueDrainStep
    }

    class OutputPipelineStep {
        +step: OutputStep
    }

    class StorageStreamWritePipelineStep {
        +step: StorageStreamWriteStep
    }

    class StorageQueryPipelineStep {
        +step: StorageQueryStep
    }

    PipelineStep <|-- TransformerPipelineStep
    PipelineStep <|-- RouterPipelineStep
    PipelineStep <|-- LoopPipelineStep
    PipelineStep <|-- QueueWritePipelineStep
    PipelineStep <|-- QueueDrainPipelineStep
    PipelineStep <|-- OutputPipelineStep
    PipelineStep <|-- StorageStreamWritePipelineStep
    PipelineStep <|-- StorageQueryPipelineStep

    %% ── STEP DATA CLASSES ────────────────────────────────────────────────

    class TransformerStep {
        +transformerId: String
        +capabilityId: String
        +inputKey: String
        +outputKey: String
        +parameters: Map~String,String~
    }

    class RouterStep {
        +routerId: String
        +capabilityId: String
        +inputKey: String
        +matchMode: MatchMode
        +cases: List~RouterCase~
        +defaultBranch: List~PipelineStep~?
    }

    class LoopStep {
        +inputKey: String
        +targetMonitorId: String
        +paramKey: String
        +hwm: Int
        +throttle: QueueThrottleConfig?
    }

    class QueueStep {
        +queueId: String
        +inputKey: String
        +hwm: Int
        +overflowPolicy: OverflowPolicy
        +diagnosticsKey: String?
    }

    class QueueDrainStep {
        +queueId: String
        +outputKey: String
        +drainPolicy: DrainPolicy
    }

    class OutputStep {
        +outputId: String
        +capabilityId: String
        +parameters: Map~String,String~
    }

    class StorageStreamWriteStep {
        +storageId: String
        +capabilityId: String
        +parameters: Map~String,String~
    }

    class StorageQueryStep {
        +storageId: String
        +capabilityId: String
        +resultKey: String
        +parameters: Map~String,String~
    }

    class QueueThrottleConfig {
        +interIterationDelayMs: Long
        +maxRatePerMinute: Int?
        +maxConcurrent: Int
    }

    TransformerPipelineStep "1" *-- "1" TransformerStep
    RouterPipelineStep "1" *-- "1" RouterStep
    LoopPipelineStep "1" *-- "1" LoopStep
    QueueWritePipelineStep "1" *-- "1" QueueStep
    QueueDrainPipelineStep "1" *-- "1" QueueDrainStep
    OutputPipelineStep "1" *-- "1" OutputStep
    StorageStreamWritePipelineStep "1" *-- "1" StorageStreamWriteStep
    StorageQueryPipelineStep "1" *-- "1" StorageQueryStep
    LoopStep "1" o-- "0..1" QueueThrottleConfig
    QueueStep ..> OverflowPolicy
    QueueDrainStep ..> DrainPolicy
    RouterStep ..> MatchMode

    %% ── ROUTER CONDITION HIERARCHY ───────────────────────────────────────

    class RouterCondition {
        <<sealed>>
    }

    class StringCondition {
        +operator: StringOperator
        +value: String
        +ignoreCase: Boolean
    }

    class NumericCondition {
        +operator: NumericOperator
        +value: Double
    }

    class RegexCondition {
        +pattern: String
        +ignoreCase: Boolean
    }

    class CompoundCondition {
        +logicalOperator: LogicalOperator
        +operands: List~RouterCondition~
    }

    class RouterCase {
        +condition: RouterCondition
        +branch: List~PipelineStep~
    }

    RouterCondition <|-- StringCondition
    RouterCondition <|-- NumericCondition
    RouterCondition <|-- RegexCondition
    RouterCondition <|-- CompoundCondition
    CompoundCondition "1" *-- "1..*" RouterCondition : operands
    RouterStep "1" *-- "1..*" RouterCase
    RouterCase "1" *-- "1" RouterCondition
    RouterCase "1" *-- "0..*" PipelineStep : branch
    RouterStep "1" o-- "0..*" PipelineStep : defaultBranch
    StringCondition ..> StringOperator
    NumericCondition ..> NumericOperator
    CompoundCondition ..> LogicalOperator

    %% ── ENUMS ────────────────────────────────────────────────────────────

    class DrainPolicy {
        <<enumeration>>
        SLIDING
        ACCUMULATE
    }

    class InputQueueMode {
        <<enumeration>>
        QUEUED
        UNQUEUED
        USER_CHOICE
    }

    class LogicalOperator {
        <<enumeration>>
        AND
        OR
        NOT
    }

    class MatchMode {
        <<enumeration>>
        FIRST
        MULTIPLE
    }

    class NumericOperator {
        <<enumeration>>
        EQUALS
        NOT_EQUALS
        LESS_THAN
        GREATER_THAN
        LESS_THAN_OR_EQUAL
        GREATER_THAN_OR_EQUAL
    }

    class OverflowPolicy {
        <<enumeration>>
        DROP_NEWEST
        DROP_OLDEST
    }

    class SchedulingMode {
        <<enumeration · @Deprecated>>
        EXACT
        INEXACT
        NONE
    }

    class StringOperator {
        <<enumeration>>
        EQUALS
        NOT_EQUALS
        STARTS_WITH
        ENDS_WITH
        CONTAINS
    }

    class PluginCapabilityType {
        <<enumeration>>
        NETWORK_MONITOR
        SYSTEM_MONITOR
        ANALYZER
        REPORTER
        CUSTOM
    }

    class ParamType {
        <<enumeration>>
        STRING
        INT
        FLOAT
        BOOLEAN
        ENUM
    }

    PluginCapability ..> PluginCapabilityType
    PluginCapability ..> InputQueueMode
    PluginParameterSpec ..> ParamType

    %% ── CONSTANTS / REGISTRY ─────────────────────────────────────────────

    class AuthConstants {
        <<object>>
        +PLUGIN_SERVICE_ACTION: String
        +MANAGEMENT_PLUGIN_SERVICE_ACTION: String
        +TRANSFORMER_SERVICE_ACTION: String
        +ROUTER_SERVICE_ACTION: String
        +OUTPUT_SERVICE_ACTION: String
        +STORAGE_SERVICE_ACTION: String
        +EVENT_SERVICE_ACTION: String
        +SENTINEL_SERVICE_ACTION: String
        +BIND_PLUGIN_PERMISSION: String
        +BIND_MANAGEMENT_PLUGIN_PERMISSION: String
        +BIND_TRANSFORMER_PERMISSION: String
        +BIND_ROUTER_PERMISSION: String
        +BIND_OUTPUT_PERMISSION: String
        +BIND_STORAGE_PERMISSION: String
        +BIND_EVENT_PERMISSION: String
        +BIND_SENTINEL_PERMISSION: String
        +KEY_ALGORITHM: String = "EC"
        +SIGNATURE_ALGORITHM: String = "SHA256withECDSA"
        +EC_CURVE: String = "secp256r1"
    }

    class OfficialPluginRegistry {
        <<object>>
        +PLUGIN_ID_ICMP: String
        +PLUGIN_ID_DNS: String
    }

    class MondoNodeSdk {
        <<object>>
        +init(devFingerprints: Set~String~)
        +isTrusted(fingerprint: String, officialSet: Set~String~) Boolean
    }

    class HostCallerCheck {
        <<top-level fun>>
        +requireCallerIsHost(context: Context)
    }

    class EncryptedPrefsHelper {
        <<top-level fun>>
        +createEncryptedPrefs(context: Context, name: String) SharedPreferences
    }

    class IntervalParser {
        <<top-level fun>>
        +parseIntervalSeconds(input: String) Int?
    }

    class RouterConditionEvaluator {
        <<object>>
        +evaluate(input: String, condition: RouterCondition) Boolean
    }
    RouterConditionEvaluator ..> RouterCondition

    %% ── PIPELINE GRAPH (replaces List~PipelineStep~ per-monitor model) ──

    class PipelineGraph {
        +id: String
        +nodes: List~PipelineGraphNode~
        +edges: List~PipelineGraphEdge~
    }
    class PipelineGraphNode {
        +id: String
        +type: PipelineNodeType
        +pluginId: String
        +capabilityId: String
        +label: String?
        +parameters: Map~String,String~
        +enabled: Boolean
        +schedulingMode: SchedulingMode?
        +intervalSeconds: Int?
        +fanInTriggerMode: FanInTriggerMode
        +instanceInputPorts: List~PortSpec~?
        +instanceOutputPorts: List~PortSpec~?
        +canvasX: Float?
        +canvasY: Float?
    }
    class PipelineGraphEdge {
        +id: String
        +fromNodeId: String
        +fromPortId: String
        +toNodeId: String
        +toPortId: String
    }
    class PortSpec {
        +id: String
        +name: String
        +dataType: PortDataType
        +isFailPath: Boolean
        +isDebugPath: Boolean
        +isDynamic: Boolean
        +description: String
    }
    class PipelineNodeType {
        <<enum>>
        MONITOR
        TRANSFORMER
        ROUTER
        OUTPUT
        STORAGE
        EVENT
    }
    class FanInTriggerMode {
        <<enum>>
        ANY_CHANGE
        ALL_CHANGE
    }
    class PortDataType {
        <<enum>>
        STRING
        JSON
        NUMBER
        BOOLEAN
        ANY
    }

    PipelineGraph "1" *-- "0..*" PipelineGraphNode
    PipelineGraph "1" *-- "0..*" PipelineGraphEdge
    PipelineGraphNode "0..1" *-- "0..*" PortSpec : instanceInputPorts
    PipelineGraphNode "0..1" *-- "0..*" PortSpec : instanceOutputPorts
    PipelineGraphNode --> PipelineNodeType
    PipelineGraphNode --> FanInTriggerMode
    PortSpec --> PortDataType
```
