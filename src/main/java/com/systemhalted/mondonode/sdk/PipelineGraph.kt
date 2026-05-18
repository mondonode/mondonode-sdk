package com.systemhalted.mondonode.sdk

import kotlinx.serialization.Serializable

/**
 * The workspace-level directed graph that defines the entire MondoNode data-flow pipeline.
 *
 * Every processing unit — monitor plugins (sources), transformers, validators, routers,
 * output plugins, and storage plugins — is a [PipelineGraphNode] with a stable UUID and
 * declared input/output ports.  [PipelineGraphEdge]s connect a named output port on one
 * node to a named input port on another, replacing the old linear
 * [MonitorDefinition.pipeline] + [ParameterBinding] approach.
 *
 * A single [PipelineGraph] is the canonical pipeline state for the entire host.
 * Management apps read it via [IMondoNodeHost.getPipelineGraph] and write it via
 * [IMondoNodeHost.setPipelineGraph].
 *
 * **Loop detection:** the host rejects graphs containing a pure data cycle (a directed
 * cycle with no storage node on every path in the cycle).  Cycles that pass through at
 * least one [PipelineNodeType.STORAGE] node on every path are warned but accepted —
 * they model intentional read-modify-write patterns (e.g. compare-and-set).
 *
 * **Fan-out** (one output port → many inputs) is modelled as multiple edges sharing the
 * same [PipelineGraphEdge.fromNodeId] / [PipelineGraphEdge.fromPortId] pair.
 *
 * **Fan-in** (many outputs → one input) is modelled as multiple edges sharing the same
 * [PipelineGraphEdge.toNodeId] / [PipelineGraphEdge.toPortId] pair; the target node's
 * [PipelineGraphNode.fanInTriggerMode] governs when it fires.
 */
@Serializable
data class PipelineGraph(
    val id: String = "",
    val nodes: List<PipelineGraphNode> = emptyList(),
    val edges: List<PipelineGraphEdge> = emptyList(),
)

/**
 * One node instance in the pipeline graph.
 *
 * [type] determines the execution semantics and which plugin category [pluginId] refers to.
 * [capabilityId] selects which specific capability of that plugin this node invokes.
 *
 * [parameters] holds static configuration for this node instance (e.g. REGEX pattern,
 * file path, JSON extraction paths).  Values arriving on input edges at runtime take
 * precedence over [parameters] for the matching port/parameter key.
 *
 * **MONITOR nodes — reserved input ports:**
 * - `"trigger"` — a boolean edge from an EVENT node (or any boolean source) that, when `"true"`,
 *   causes the monitor to execute one pass.  Multiple consecutive `"true"` values each trigger
 *   one additional execution.  If the trigger port is not connected, the monitor never runs
 *   automatically; it can still be triggered via the management API.
 * - `"enabled"` — a boolean edge whose latest value gates all trigger and parameter inputs.
 *   When `"false"`, incoming trigger values are silently dropped and the monitor does not run.
 *   When `"true"` (or not connected), the monitor behaves normally.  If not connected, the
 *   static [enabled] field governs the initial enabled state.
 * Any other incoming edge port ID is treated as a [PluginParameterSpec.key] that overrides the
 * matching static parameter for the monitor's next execution.
 *
 * **Fan-in:** when multiple edges arrive at a single input port, [fanInTriggerMode]
 * controls when the node fires.  [FanInTriggerMode.ANY_CHANGE] fires on each new value;
 * [FanInTriggerMode.ALL_CHANGE] waits until every connected input port has received at
 * least one new value since the last execution (e.g. for aggregation nodes like array_push).
 *
 * **Dynamic ports:** capabilities that expose a variable number of ports based on
 * configuration (e.g. json_extract_path with N configured output paths, or a router with
 * N cases) declare [PortSpec.isDynamic] on the relevant [PortSpec].  The actual port list
 * for this instance is stored in [instanceInputPorts] / [instanceOutputPorts], overriding
 * the capability manifest defaults.
 *
 * **Canvas layout:** [canvasX] / [canvasY] are editor-managed pixel coordinates persisted
 * opaquely by the host.  They have no meaning to the execution engine.
 */
@Serializable
data class PipelineGraphNode(
    val id: String,
    val type: PipelineNodeType,
    val pluginId: String,
    val capabilityId: String,
    val label: String? = null,
    val parameters: Map<String, String> = emptyMap(),
    // Static enabled flag for MONITOR nodes — overridden at runtime by an incoming "enabled" edge.
    val enabled: Boolean = true,
    // Fan-in synchronisation for nodes with multiple incoming edges
    val fanInTriggerMode: FanInTriggerMode = FanInTriggerMode.ANY_CHANGE,
    // Dynamic-port overrides: null means use the capability manifest's declared port list
    val instanceInputPorts: List<PortSpec>? = null,
    val instanceOutputPorts: List<PortSpec>? = null,
    // Editor canvas layout
    val canvasX: Float? = null,
    val canvasY: Float? = null,
)

/**
 * A directed data-flow connection from one node's output port to another node's input port.
 *
 * At runtime the host routes the value produced on [fromPortId] of [fromNodeId] to the
 * [toPortId] input of [toNodeId] immediately after [fromNodeId] completes execution.
 *
 * For [PipelineNodeType.MONITOR] targets, [toPortId] is a [PluginParameterSpec.key] —
 * the arriving value overrides that parameter for the monitor's next execution.
 */
@Serializable
data class PipelineGraphEdge(
    val id: String,
    val fromNodeId: String,
    val fromPortId: String,
    val toNodeId: String,
    val toPortId: String,
)

/**
 * Category of a pipeline graph node — controls execution semantics and which plugin
 * category [PipelineGraphNode.pluginId] belongs to.
 */
@Serializable
enum class PipelineNodeType {
    /** Monitoring plugin — executes when triggered via its "trigger" input port; primary data source. */
    MONITOR,
    /** Pure-function transformer — stateless; single input → one or more outputs. */
    TRANSFORMER,
    /** Routing plugin — evaluates conditions and fans out to one or more branch outputs. */
    ROUTER,
    /** Output plugin — data sink; writes to an external system; does not produce data. */
    OUTPUT,
    /** Storage plugin — persistent store; can both read and write; exempt from pure-cycle rejection. */
    STORAGE,
    /**
     * Event plugin — autonomous source node.  Has no input ports; emits boolean values from its
     * output ports when a condition is met (timer fires, sensor threshold crossed, etc.).
     * Connect an EVENT node's output to a MONITOR node's "trigger" input to drive execution.
     * Activated via startEvent() at pipeline activation; runs until stopEvent() or graph teardown.
     */
    EVENT,
}

/**
 * Controls when a node with multiple incoming edges fires.
 *
 * [ANY_CHANGE] — execute each time any connected input port receives a new value.
 * Appropriate for most transformers, outputs, and routers.
 *
 * [ALL_CHANGE] — wait until every connected input port has received at least one new
 * value since the last execution, then fire once with the latest value on each port.
 * Appropriate for aggregation nodes (e.g. array_push) that must combine values from
 * multiple independent upstream paths before they are meaningful together.
 */
@Serializable
enum class FanInTriggerMode {
    ANY_CHANGE,
    ALL_CHANGE,
}

/**
 * Declares one named connection point on a plugin capability.
 *
 * The host uses [id] to route values between nodes at runtime.  The editor uses [name],
 * [dataType], and [description] in the port tooltip, palette, and wire-compatibility UI.
 *
 * [isFailPath] marks ports that carry data when the capability's primary operation fails —
 * e.g. when a validator rejects its input or a converter cannot parse the value.  Only
 * capabilities with a meaningful failure mode declare a fail port.  The normal [id] ="output"
 * port carries a null/empty value on failure in those cases; downstream nodes connected to
 * the fail port receive the original unmodified input instead.
 *
 * [isDebugPath] marks ports that emit diagnostic or intermediate values for troubleshooting.
 * The editor hides debug ports by default; they can be revealed in a "debug view" mode.
 *
 * [isDynamic] marks ports whose count for a given node instance is determined by the
 * node's [PipelineGraphNode.parameters] at configuration time rather than being fixed in
 * the manifest.  For dynamic ports the instance's actual port list is stored in
 * [PipelineGraphNode.instanceInputPorts] / [PipelineGraphNode.instanceOutputPorts].
 * The manifest entry with [isDynamic] = true serves as a template / placeholder.
 *
 * [dataType] is advisory — the host does not enforce type compatibility at runtime (all
 * values are serialized as strings).  The editor uses it to color-code ports and warn the
 * user about potentially incompatible connections.
 */
@Serializable
data class PortSpec(
    val id: String,
    val name: String,
    val dataType: PortDataType = PortDataType.ANY,
    val isFailPath: Boolean = false,
    val isDebugPath: Boolean = false,
    val isDynamic: Boolean = false,
    val description: String = "",
)

/** Advisory data type on a port.  Values are always serialized as strings at runtime. */
@Serializable
enum class PortDataType {
    STRING,
    JSON,
    NUMBER,
    BOOLEAN,
    ANY,
}
