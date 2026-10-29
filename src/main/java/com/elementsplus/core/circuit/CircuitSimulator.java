package com.elementsplus.core.circuit;

import com.elementsplus.core.circuit.CircuitComponent.PinType;
import com.elementsplus.core.circuit.component.CapacitorComponentInstance;
import com.elementsplus.core.circuit.component.Const8ComponentInstance;
import com.elementsplus.core.circuit.component.ConstAnalog8ComponentInstance;
import com.elementsplus.core.circuit.component.Input8ComponentInstance;
import com.elementsplus.core.circuit.component.InputComponentInstance;
import com.elementsplus.core.circuit.component.Output8ComponentInstance;
import com.elementsplus.core.circuit.component.OutputComponentInstance;
import com.elementsplus.core.circuit.component.ResistorComponentInstance;
import com.elementsplus.core.circuit.component.ResonatorComponentInstance;
import com.elementsplus.core.circuit.diagram.CircuitDiagram;
import com.elementsplus.core.circuit.diagram.CircuitDiagram.Component;
import com.elementsplus.core.circuit.diagram.CircuitDiagram.Wire;
import com.elementsplus.core.circuit.diagram.CircuitDiagram.Wire.WireMaterial;
import com.elementsplus.core.circuit.storage.StoredCircuit;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 纯客户端电路模拟器。
 * <p>
 * 思路（参考 docs/参考算法描述.md）：
 * 1. 并查集把同材料、同位宽的导线四连通合并为"网络"节点：
 * 1 位网络携带单个 0~15 值；8 位总线网络携带 8 元组（每位仍为 0~15，
 * 信号强度得到保留），按位独立承载 8 路信号；
 * 2. 建依赖图：网络/组合元件/时序元件（电容 D 输入不建边，仅采样，切断时序环）；
 * 3. Kahn 拓扑排序；
 * 4. Tarjan SCC 把环内的网络标记为坏网；
 * 5. 按拓扑序传播求值：1 位网络取所有驱动源的最大值；
 * 总线网络有多个驱动源时按位取最大值（如 (12,11,10,...) | (10,11,12,...) = (12,11,12,...)）；
 * 6. 时钟步进：求值 -> 采样电容 D -> 统一更新状态 -> 重新求值；
 * 7. 复位：清空时序状态（电容 = 0，谐振器 = 初相）。
 * <p>
 * 位宽语义：
 * - 元件结点值按输出引脚布局拆成若干位段：1 位引脚占 4 位（0~15 模拟值），
 * 8 位引脚占 32 位（8 个 4 位通道，每个通道保存该位 0~15 的信号强度）；
 * - 不同位宽的线/引脚之间不传信号，并在连接处记录"位宽不匹配"告警；
 * - 加法器先从输入端读取 8 元组，按“位=非零”转成整数做二进制加法，
 * 再把结果按位转成 8 元组输出（高=15，低=0）。
 * <p>
 * 状态不持久化、不发送到服务端，仅用于屏幕内的模拟与渲染。
 */
public class CircuitSimulator {

    public record WirePoint(int x, int y, WireMaterial material) {
    }

    private enum NodeKind {
        NET, COMB, CAP, RES, SEQ
    }

    private enum DepKind {
        /**
         * 读取源结点值中的一个 4 位通道（0~15）。通道起点由 Dep.bit 指定。
         */
        WHOLE,
        /**
         * 读取源结点值中的一个 32 位（8×4 位）通道，即整个 8 元组。起点由 Dep.bit 指定。
         */
        TUPLE
    }

    /**
     * 一条入边依赖：源结点、取值方式、通道偏移。
     */
    private record Dep(int node, DepKind kind, int bit) {
    }

    /**
     * 一个输入引脚的若干通道依赖；引脚位宽 width 为 1 或 8。
     */
    private record InputPort(int width, Dep[] lanes) {
    }

    private static final double EPS = 1e-6;

    private CircuitDiagram diagram;

    private int totalNodes;
    private int numNets;
    private NodeKind[] nodeKind = new NodeKind[0];
    private long[] nodeVals = new long[0];
    /** 元件输出超过 64 位时使用的高位字（如高级 ALU 的 Y1 最高 4 位）。 */
    private long[] nodeValsHigh = new long[0];
    private long evalHighBits;
    private int[] netWidth = new int[0];
    private int[] topo = new int[0];
    private int topoLen;
    private boolean hasCycle;
    private boolean[] badNet = new boolean[0];

    private List<Component> comps = new ArrayList<>();
    private InputPort[][] inputPorts = new InputPort[0][];
    private int[] dSampleSource = new int[0];
    private List<Integer> capNodes = new ArrayList<>();
    private List<Integer> resNodes = new ArrayList<>();

    @SuppressWarnings("unchecked")
    private List<Dep>[] netInputDeps = new List[0];

    private Map<WirePoint, Integer> wireNet = new HashMap<>();
    private final Map<Long, Integer> compNodes = new HashMap<>();
    private Set<Long> mismatchCells = new HashSet<>();

    private final Map<WirePoint, Integer> wireValues = new HashMap<>();
    private final Map<WirePoint, Boolean> wireBad = new HashMap<>();
    private final Map<Long, Integer> compValues = new HashMap<>();

    private final Map<Long, Integer> capQ = new HashMap<>();
    private final Map<Long, Integer> resPhase = new HashMap<>();
    private final Map<Long, Integer> tffQ = new HashMap<>();
    private final Map<Long, Integer> tffPrevClock = new HashMap<>();
    private final List<Integer> tffNodes = new ArrayList<>();
    private final Map<Long, Long> registerQ = new HashMap<>();
    private final List<Integer> registerNodes = new ArrayList<>();
    private final Map<Long, CustomRuntime> customRuntimes = new HashMap<>();

    private static final class CustomRuntime {
        final CustomCircuitComponent component;
        final CircuitDiagram diagram;
        final CircuitSimulator simulator;
        final boolean stateful;

        CustomRuntime(CustomCircuitComponent component, CircuitDiagram diagram, CircuitSimulator simulator) {
            this.component = component;
            this.diagram = diagram;
            this.simulator = simulator;
            this.stateful = simulator.hasSequential();
        }
    }

    public void setDiagram(CircuitDiagram diagram) {
        this.diagram = diagram;
        rebuild();
        evaluate();
    }

    public void reset() {
        capQ.clear();
        resPhase.clear();
        tffQ.clear();
        tffPrevClock.clear();
        registerQ.clear();
        for (CustomRuntime runtime : customRuntimes.values()) {
            runtime.simulator.reset();
        }
        evaluate();
    }

    public boolean hasCycle() {
        return hasCycle;
    }

    public boolean hasMismatchWarning() {
        return !mismatchCells.isEmpty();
    }

    public Set<Long> getMismatchCells() {
        return mismatchCells;
    }

    public boolean hasMismatch(int x, int y) {
        for (long seam : mismatchCells) {
            int sx = seamCellX(seam);
            int sy = seamCellY(seam);
            if (seamVertical(seam)) {
                if ((x == sx || x == sx - 1) && y == sy) return true;
            } else {
                if (x == sx && (y == sy || y == sy - 1)) return true;
            }
        }
        return false;
    }

    public int getWireValue(int x, int y, WireMaterial material) {
        Integer v = wireValues.get(new WirePoint(x, y, material));
        return v == null ? 0 : v;
    }

    /**
     * 8 位总线打包值：bit b = 第 b 位（4 位通道）是否非零。
     */
    public int getWireBusValue(int x, int y, WireMaterial material) {
        Integer net = wireNet.get(new WirePoint(x, y, material));
        if (net == null) return 0;
        long v = nodeVals[net];
        int bits = 0;
        for (int b = 0; b < 8; b++) {
            if (((v >> (4 * b)) & 0xF) != 0) bits |= (1 << b);
        }
        return bits;
    }

    /**
     * 该格上 material 对应的导线网络是否为 8 位总线。
     */
    public boolean isWireBus(int x, int y, WireMaterial material) {
        Integer net = wireNet.get(new WirePoint(x, y, material));
        return net != null && net < netWidth.length && netWidth[net] == 8;
    }

    public boolean isWireBad(int x, int y, WireMaterial material) {
        Boolean b = wireBad.get(new WirePoint(x, y, material));
        return b != null && b;
    }

    public int getComponentValue(int x, int y) {
        Integer v = compValues.get(key(x, y));
        return v == null ? 0 : v;
    }

    /**
     * 8 位总线元件（INPUT_8 / OUTPUT_8）当前值：将 8 个 4 位通道打包的
     * 32 位值解码为 0~255 的整数（通道非 0 即该位为 1）。
     */
    public int getComponentBusValue(int x, int y) {
        Integer v = compValues.get(key(x, y));
        return v == null ? 0 : tupleToInt(v.intValue() & 0xFFFFFFFFL);
    }

    /**
     * 读取某个元件引脚当前各通道的信号强度（用于“检查”tooltip）。
     * 返回数组长度等于引脚位宽（1 或 8）：输入引脚读取其连接的来源，输出引脚读取元件自身输出。
     */
    public int[] getPinLanes(CircuitDiagram.Component component, Direction side, int offset) {
        if (component == null) {
            return new int[1];
        }
        int width = Math.max(1, component.component.getPinBitWidth(side, offset));
        int[] lanes = new int[width];
        if (diagram == null) {
            return lanes;
        }
        Integer nodeObj = compNodes.get(key(component.x, component.y));
        if (nodeObj == null || nodeObj >= nodeKind.length) {
            return lanes;
        }
        int node = nodeObj;
        PinType type = component.component.getPin(side, offset);
        if (type == PinType.INPUT) {
            int portIndex = inputPortIndex(component.component, side, offset);
            InputPort[] ports = node < inputPorts.length ? inputPorts[node] : null;
            if (ports == null || portIndex < 0 || portIndex >= ports.length) {
                return lanes;
            }
            Dep[] deps = ports[portIndex].lanes();
            for (int b = 0; b < width && b < deps.length; b++) {
                Dep d = deps[b];
                if (d != null) {
                    lanes[b] = (int) readLane(d.node(), d.bit());
                }
            }
        } else if (type == PinType.OUTPUT) {
            OutSlot slot = outputSlot(component.component, side, offset);
            for (int b = 0; b < width; b++) {
                lanes[b] = (int) readLane(node, slot.bitStart() + 4 * b);
            }
        }
        return lanes;
    }

    /**
     * 读取某格某材质导线当前各通道的信号强度（用于“检查”tooltip）。
     * 1 位导线返回长度 1，8 位总线返回长度 8。
     */
    public int[] getWireLanes(int x, int y, WireMaterial material) {
        int width = isWireBus(x, y, material) ? 8 : 1;
        int[] lanes = new int[width];
        int v = getWireValue(x, y, material);
        for (int b = 0; b < width; b++) {
            lanes[b] = (v >> (4 * b)) & 0xF;
        }
        return lanes;
    }

    /** 输入引脚在其元件输入端口列表中的序号（顺序同 rebuild：西、南、东、北）。 */
    private static int inputPortIndex(CircuitComponent cc, Direction side, int offset) {
        int index = 0;
        for (Direction s : new Direction[]{Direction.WEST, Direction.SOUTH, Direction.EAST, Direction.NORTH}) {
            int count = (s == Direction.NORTH || s == Direction.SOUTH) ? cc.getWidth() : cc.getHeight();
            for (int o = 0; o < count; o++) {
                if (cc.getPin(s, o) != PinType.INPUT) continue;
                if (s == side && o == offset) return index;
                index++;
            }
        }
        return -1;
    }

    /* ============================================================
     *  构建依赖图
     * ============================================================ */

    private void rebuild() {
        comps.clear();
        capNodes.clear();
        resNodes.clear();
        tffNodes.clear();
        registerNodes.clear();
        // 与电容状态一致：重建依赖图（如输入值变化触发）时保留触发器和寄存器状态，
        // 只有显式 reset() 才清空时序状态。
        wireNet.clear();
        compNodes.clear();
        wireValues.clear();
        wireBad.clear();
        compValues.clear();
        mismatchCells.clear();
        hasCycle = false;

        if (diagram == null) {
            totalNodes = 0;
            numNets = 0;
            nodeKind = new NodeKind[0];
            nodeVals = new long[0];
            nodeValsHigh = new long[0];
            netWidth = new int[0];
            topo = new int[0];
            topoLen = 0;
            badNet = new boolean[0];
            inputPorts = new InputPort[0][];
            dSampleSource = new int[0];
            netInputDeps = new List[0];
            return;
        }

        List<Wire> wires = new ArrayList<>();
        for (Map.Entry<Long, CircuitDiagram.Chunk> entry : diagram.chunks.entrySet()) {
            CircuitDiagram.Chunk chunk = entry.getValue();
            for (int i = 0; i < 16; i++) {
                for (int j = 0; j < 16; j++) {
                    if (chunk.wires[i][j] != null) {
                        wires.add(chunk.wires[i][j]);
                    }
                }
            }
            comps.addAll(chunk.components);
        }

        /* ---------- 1. 并查集：同材料、同位宽的导线合并为同一网络 ---------- */
        Map<WirePoint, Integer> elemIndex = new HashMap<>();
        Map<Integer, Integer> indexBw = new HashMap<>();
        UnionFind uf = new UnionFind();
        for (Wire wire : wires) {
            for (WireMaterial m : new WireMaterial[]{WireMaterial.COPPER, WireMaterial.GOLD}) {
                if (hasSideMaterial(wire, m)) {
                    int id = uf.add();
                    elemIndex.put(new WirePoint(wire.x, wire.y, m), id);
                    indexBw.put(id, wire.getBitWidth(m));
                }
            }
        }
        for (Wire wire : wires) {
            for (WireMaterial m : new WireMaterial[]{WireMaterial.COPPER, WireMaterial.GOLD}) {
                Integer self = elemIndex.get(new WirePoint(wire.x, wire.y, m));
                if (self == null) continue;
                for (Direction d : Direction.Plane.HORIZONTAL) {
                    if (materialAtSide(wire, d) != m) continue;
                    int nx = wire.x + d.getStepX();
                    int ny = wire.y + d.getStepZ();
                    Wire neighbor = findWire(nx, ny);
                    if (neighbor != null
                            && neighbor.getBitWidth(m) == wire.getBitWidth(m)
                            && materialAtSide(neighbor, d.getOpposite()) == m) {
                        Integer other = elemIndex.get(new WirePoint(nx, ny, m));
                        if (other != null) uf.union(self, other);
                    }
                }
            }
        }

        Map<Integer, Integer> rootToNet = new HashMap<>();
        netWidth = new int[elemIndex.size()];
        for (Map.Entry<WirePoint, Integer> e : elemIndex.entrySet()) {
            int root = uf.find(e.getValue());
            int net = rootToNet.computeIfAbsent(root, k -> rootToNet.size());
            netWidth[net] = indexBw.get(e.getValue());
            wireNet.put(e.getKey(), net);
        }
        numNets = rootToNet.size();
        netWidth = Arrays.copyOf(netWidth, numNets);
        int numComps = comps.size();
        totalNodes = numNets + numComps;

        nodeKind = new NodeKind[totalNodes];
        for (int i = 0; i < numNets; i++) nodeKind[i] = NodeKind.NET;
        for (int i = 0; i < numComps; i++) {
            CircuitComponent cc = comps.get(i).component;
            if (cc == BuiltinCircuitComponents.CAPACITOR) {
                nodeKind[numNets + i] = NodeKind.CAP;
                capNodes.add(numNets + i);
            } else if (cc == BuiltinCircuitComponents.RESONATOR) {
                nodeKind[numNets + i] = NodeKind.RES;
                resNodes.add(numNets + i);
            } else if (cc == BuiltinCircuitComponents.TFF || cc == BuiltinCircuitComponents.TFF_PRIME) {
                // 触发器是时序元件：输出只由内部状态决定，不应参与组合环检测。
                nodeKind[numNets + i] = NodeKind.SEQ;
                tffNodes.add(numNets + i);
            } else if (cc == BuiltinCircuitComponents.REGISTER || cc == BuiltinCircuitComponents.REGISTER_8
                    || cc == BuiltinCircuitComponents.REGISTER_ANALOG) {
                // 寄存器同样只在时钟步进时更新内部状态。
                nodeKind[numNets + i] = NodeKind.SEQ;
                registerNodes.add(numNets + i);
            } else if (cc instanceof CustomCircuitComponent custom) {
                CustomRuntime runtime = customRuntime(comps.get(i), custom);
                nodeKind[numNets + i] = runtime.stateful ? NodeKind.SEQ : NodeKind.COMB;
            } else {
                nodeKind[numNets + i] = NodeKind.COMB;
            }
        }

        for (int i = 0; i < numComps; i++) {
            compNodes.put(key(comps.get(i).x, comps.get(i).y), numNets + i);
        }

        /* ---------- 2. 建边（含位宽匹配检测） ---------- */
        @SuppressWarnings("unchecked")
        Set<Integer>[] predSets = new Set[totalNodes];
        for (int i = 0; i < totalNodes; i++) predSets[i] = new HashSet<>();
        netInputDeps = new List[totalNodes];
        for (int i = 0; i < totalNodes; i++) netInputDeps[i] = new ArrayList<>();
        inputPorts = new InputPort[totalNodes][];
        dSampleSource = new int[totalNodes];
        Arrays.fill(dSampleSource, -1);

        for (int i = 0; i < numComps; i++) {
            Component component = comps.get(i);
            int node = numNets + i;
            NodeKind kind = nodeKind[node];

            List<InputPort> ports = new ArrayList<>();
            int dSample = -1;
            for (Direction side : new Direction[]{Direction.WEST, Direction.SOUTH, Direction.EAST, Direction.NORTH}) {
                int count = (side == Direction.NORTH || side == Direction.SOUTH)
                        ? component.component.getWidth()
                        : component.component.getHeight();
                for (int offset = 0; offset < count; offset++) {
                    PinType type = component.component.getPin(side, offset);
                    if (type == null || type == PinType.NONE) continue;
                    PinInfo pin = pinExternal(component.component, component.direction,
                            component.mirrored, component.x, component.y, side, offset);
                    int pinWidth = component.component.getPinBitWidth(side, offset);

                    if (type == PinType.INPUT) {
                        Dep[] lanes = new Dep[pinWidth];
                        Wire wire = findWire(pin.ex, pin.ey);
                        if (wire != null) {
                            WireMaterial m = materialAtSide(wire, pin.facing.getOpposite());
                            if (m != null) {
                                if (wire.getBitWidth(m) == pinWidth) {
                                    Integer net = wireNet.get(new WirePoint(pin.ex, pin.ey, m));
                                    for (int b = 0; b < pinWidth; b++) {
                                        if (net != null) {
                                            lanes[b] = new Dep(net, DepKind.WHOLE, 4 * b);
                                            if (kind == NodeKind.CAP) {
                                                dSample = net;
                                            } else if (kind != NodeKind.SEQ) {
                                                predSets[node].add(net);
                                            }
                                        }
                                    }
                                } else {
                                    mismatchCells.add(seamKey(pin.ex, pin.ey, pin.facing));
                                }
                            }
                        } else {
                            Component other = compAt(pin.ex, pin.ey);
                            if (other != null) {
                                PinInfo op = otherPinAt(other, pin, PinType.OUTPUT);
                                if (op != null) {
                                    OutSlot slot = outputSlot(other.component, op.side, op.offset);
                                    int otherNode = compNodes.get(key(other.x, other.y));
                                    if (slot.width() == pinWidth) {
                                        for (int b = 0; b < pinWidth; b++) {
                                            lanes[b] = new Dep(otherNode, DepKind.WHOLE,
                                                    slot.bitStart() + 4 * b);
                                            if (kind == NodeKind.CAP) {
                                                dSample = otherNode;
                                            } else if (kind != NodeKind.SEQ) {
                                                predSets[node].add(otherNode);
                                            }
                                        }
                                    } else {
                                        mismatchCells.add(seamKey(pin.ex, pin.ey, pin.facing));
                                    }
                                }
                            }
                        }
                        ports.add(new InputPort(pinWidth, lanes));
                    } else { // OUTPUT
                        Wire wire = findWire(pin.ex, pin.ey);
                        if (wire != null) {
                            WireMaterial m = materialAtSide(wire, pin.facing.getOpposite());
                            if (m != null) {
                                if (wire.getBitWidth(m) == pinWidth) {
                                    OutSlot slot = outputSlot(component.component, side, offset);
                                    Integer net = wireNet.get(new WirePoint(pin.ex, pin.ey, m));
                                    if (net != null) {
                                        netInputDeps[net].add(new Dep(node,
                                                pinWidth == 8 ? DepKind.TUPLE : DepKind.WHOLE,
                                                slot.bitStart()));
                                        predSets[net].add(node);
                                    }
                                } else {
                                    mismatchCells.add(seamKey(pin.ex, pin.ey, pin.facing));
                                }
                            }
                        } else {
                            Component other = compAt(pin.ex, pin.ey);
                            if (other != null) {
                                PinInfo ip = otherPinAt(other, pin, PinType.INPUT);
                                if (ip != null) {
                                    int otherWidth = other.component.getPinBitWidth(ip.side, ip.offset);
                                    if (otherWidth != pinWidth) {
                                        mismatchCells.add(seamKey(pin.ex, pin.ey, pin.facing));
                                    } else {
                                        int otherNode = compNodes.get(key(other.x, other.y));
                                        if (nodeKind[otherNode] != NodeKind.CAP && nodeKind[otherNode] != NodeKind.SEQ) {
                                            predSets[otherNode].add(node);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            inputPorts[node] = ports.toArray(new InputPort[0]);
            if (kind == NodeKind.CAP) {
                dSampleSource[node] = dSample;
            }
        }

        /* ---------- 3. 拓扑排序 ---------- */
        @SuppressWarnings("unchecked")
        List<Integer>[] succs = new List[totalNodes];
        for (int i = 0; i < totalNodes; i++) succs[i] = new ArrayList<>();
        int[] indeg = new int[totalNodes];
        for (int i = 0; i < totalNodes; i++) {
            indeg[i] = predSets[i].size();
            for (int p : predSets[i]) succs[p].add(i);
        }
        int[] queue = new int[totalNodes];
        int head = 0, tail = 0;
        for (int i = 0; i < totalNodes; i++) if (indeg[i] == 0) queue[tail++] = i;
        topo = new int[totalNodes];
        topoLen = 0;
        while (head < tail) {
            int v = queue[head++];
            topo[topoLen++] = v;
            for (int w : succs[v]) {
                if (--indeg[w] == 0) queue[tail++] = w;
            }
        }

        /* ---------- 4. 组合环检测（迭代版 Tarjan SCC） ---------- */
        badNet = new boolean[totalNodes];
        hasCycle = topoLen < totalNodes;
        if (hasCycle) {
            tarjanMarkCycles(succs);
        }

        nodeVals = new long[totalNodes];
        nodeValsHigh = new long[totalNodes];
    }

    private Component compAt(int x, int y) {
        CircuitDiagram.Block block = diagram.getBlock(x, y);
        return block instanceof Component component ? component : null;
    }

    /**
     * other 组件上、与 me 的引脚 {@code pin} 正对相邻的某类型引脚。
     * <p>
     * 判定标准：other 该引脚的外侧格恰好等于 {@code pin} 所在格
     * （即 {@code pin} 的外侧格沿其朝向回退一格，落在 me 自身占据的格子上）。
     * 这样即使多个同面引脚直接相邻且彼此错位，也能一一对应，
     * 而不会像仅用包围盒判定那样让所有同面引脚都顶替成第一个引脚。
     */
    private static PinInfo otherPinAt(Component other, PinInfo pin, PinType type) {
        int ownX = pin.ex - pin.facing.getStepX();
        int ownY = pin.ey - pin.facing.getStepZ();
        for (Direction side : new Direction[]{Direction.WEST, Direction.SOUTH, Direction.EAST, Direction.NORTH}) {
            int count = (side == Direction.NORTH || side == Direction.SOUTH)
                    ? other.component.getWidth()
                    : other.component.getHeight();
            for (int offset = 0; offset < count; offset++) {
                if (other.component.getPin(side, offset) != type) continue;
                PinInfo p = pinExternal(other.component, other.direction,
                        other.mirrored, other.x, other.y, side, offset);
                if (p.ex == ownX && p.ey == ownY) {
                    return p;
                }
            }
        }
        return null;
    }

    /**
     * 元件输出引脚在节点值中的位段布局：1 位引脚占 4 位，8 位引脚占 32 位。
     */
    private record OutSlot(int bitStart, int width) {
    }

    private static OutSlot outputSlot(CircuitComponent cc, Direction side, int offset) {
        int bitStart = 0;
        for (Direction s : new Direction[]{Direction.WEST, Direction.SOUTH, Direction.EAST, Direction.NORTH}) {
            int count = (s == Direction.NORTH || s == Direction.SOUTH) ? cc.getWidth() : cc.getHeight();
            for (int o = 0; o < count; o++) {
                if (cc.getPin(s, o) != PinType.OUTPUT) continue;
                int w = cc.getPinBitWidth(s, o);
                if (s == side && o == offset) {
                    return new OutSlot(bitStart, w);
                }
                bitStart += (w == 8 ? 32 : 4);
            }
        }
        return new OutSlot(0, cc.getPinBitWidth(side, offset));
    }

    private void tarjanMarkCycles(List<Integer>[] succs) {
        boolean[] inTopo = new boolean[totalNodes];
        for (int i = 0; i < topoLen; i++) inTopo[topo[i]] = true;

        int[] index = new int[totalNodes];
        int[] low = new int[totalNodes];
        boolean[] onStack = new boolean[totalNodes];
        Arrays.fill(index, -1);
        List<Integer> stack = new ArrayList<>();
        int counter = 0;

        for (int start = 0; start < totalNodes; start++) {
            if (inTopo[start] || index[start] != -1) continue;

            List<int[]> callStack = new ArrayList<>();
            index[start] = low[start] = counter++;
            stack.add(start);
            onStack[start] = true;
            callStack.add(new int[]{start, 0});

            while (!callStack.isEmpty()) {
                int[] top = callStack.get(callStack.size() - 1);
                int v = top[0];
                List<Integer> ss = succs[v];
                if (top[1] < ss.size()) {
                    int w = ss.get(top[1]++);
                    if (inTopo[w]) continue;
                    if (index[w] == -1) {
                        index[w] = low[w] = counter++;
                        stack.add(w);
                        onStack[w] = true;
                        callStack.add(new int[]{w, 0});
                    } else if (onStack[w]) {
                        if (index[w] < low[v]) low[v] = index[w];
                    }
                } else {
                    if (low[v] == index[v]) {
                        List<Integer> component = new ArrayList<>();
                        int w;
                        do {
                            w = stack.remove(stack.size() - 1);
                            onStack[w] = false;
                            component.add(w);
                        } while (w != v);
                        if (component.size() > 1) {
                            for (int node : component) {
                                if (nodeKind[node] == NodeKind.NET) {
                                    badNet[node] = true;
                                }
                            }
                        }
                    }
                    callStack.remove(callStack.size() - 1);
                    if (!callStack.isEmpty()) {
                        int p = callStack.get(callStack.size() - 1)[0];
                        if (low[v] < low[p]) low[p] = low[v];
                    }
                }
            }
        }
    }

    /* ============================================================
     *  求值
     * ============================================================ */

    public void evaluate() {
        if (diagram == null || totalNodes == 0) {
            return;
        }
        evalOnce();
        collectResults();
    }

    private void evalOnce() {
        Arrays.fill(nodeVals, 0L);
        Arrays.fill(nodeValsHigh, 0L);
        for (int k = 0; k < topoLen; k++) {
            int node = topo[k];
            NodeKind kind = nodeKind[node];
            switch (kind) {
                case NET -> {
                    long v = 0L;
                    if (netWidth[node] == 8) {
                        for (Dep d : netInputDeps[node]) v = maxTuple(v, depValue(d));
                    } else {
                        for (Dep d : netInputDeps[node]) v = Math.max(v, depValue(d));
                    }
                    nodeVals[node] = v;
                }
                case CAP -> {
                    int idx = node - numNets;
                    Component component = comps.get(idx);
                    nodeVals[node] = capQ.getOrDefault(key(component.x, component.y), 0);
                }
                default -> {
                    int idx = node - numNets;
                    Component component = comps.get(idx);
                    if (kind == NodeKind.RES) {
                        nodeVals[node] = evalResonator(component, node);
                    } else {
                        evalHighBits = 0;
                        nodeVals[node] = evalCombinational(component, node);
                        nodeValsHigh[node] = evalHighBits;
                    }
                }
            }
        }
    }

    /** 读取某节点从 bit 开始的 4 位通道；bit >= 64 时落到高位字。 */
    private long readLane(int node, int bit) {
        if (bit >= 64) {
            return (nodeValsHigh[node] >>> (bit - 64)) & 0xF;
        }
        return (nodeVals[node] >>> bit) & 0xF;
    }

    private long depValue(Dep d) {
        return switch (d.kind()) {
            case WHOLE -> readLane(d.node(), d.bit());
            case TUPLE -> {
                int bit = d.bit();
                long high = nodeValsHigh[d.node()];
                long v = bit >= 64 ? high >>> (bit - 64) : nodeVals[d.node()] >>> bit;
                if (bit > 32 && bit < 64) {
                    v |= high << (64 - bit);
                }
                yield v & 0xFFFFFFFFL;
            }
        };
    }

    /**
     * 两个 8 元组（8×4 位通道）按位取最大值。
     */
    private static long maxTuple(long a, long b) {
        if (a == 0L) return b;
        if (b == 0L) return a;
        long r = 0L;
        for (int lane = 0; lane < 8; lane++) {
            int va = (int) ((a >> (4 * lane)) & 0xF);
            int vb = (int) ((b >> (4 * lane)) & 0xF);
            r |= ((long) Math.max(va, vb)) << (4 * lane);
        }
        return r;
    }

    /**
     * 8 元组按“位非零即真”转成 8 位整数。
     */
    private static int tupleToInt(long tuple) {
        int v = 0;
        for (int b = 0; b < 8; b++) {
            if (((tuple >> (4 * b)) & 0xF) != 0) v |= (1 << b);
        }
        return v;
    }

    /**
     * 8 元组放大：每个 4 位通道“非 0 即 15”。数字多路选择器/三态缓冲器的输出语义。
     */
    private static long amplifyTuple(long tuple) {
        long t = 0L;
        for (int b = 0; b < 8; b++) {
            if (((tuple >> (4 * b)) & 0xF) != 0) t |= 15L << (4 * b);
        }
        return t;
    }

    /**
     * 译码器输出：8 元组中第 index 通道为 15，其余为 0。
     */
    private static long decodeTuple(int index) {
        return 15L << (4 * index);
    }

    /**
     * 编码器输入综合：low/high 两个 8 元组共 lineCount 条输入线（下标 0~lineCount-1）。
     * 普通编码器返回所有非 0 输入下标的按位或；优先编码器返回最大非 0 下标（全 0 返回 0）。
     */
    private static int encodeIndex(long low, long high, int lineCount, boolean priority) {
        int acc = 0;
        int best = 0;
        for (int i = 0; i < lineCount; i++) {
            long tuple = i < 8 ? low : high;
            int lane = i < 8 ? i : i - 8;
            if (((tuple >> (4 * lane)) & 0xF) == 0) continue;
            acc |= i;
            best = i;
        }
        return priority ? best : acc;
    }

    /**
     * 4 选 1（数字/模拟，1 位/8 位同布局）被选中数据通道的值：
     * 输入 A1、A0 选择 D0~D3，即 index = A1*2 + A0 对应 D0..D3。
     */
    private long mux4Select(int node) {
        int a1 = (int) inPort(node, 4);
        int a0 = (int) inPort(node, 5);
        int index = (a1 != 0 ? 1 : 0) * 2 + (a0 != 0 ? 1 : 0);
        return switch (index) {
            case 0 -> inPort(node, 3);
            case 1 -> inPort(node, 2);
            case 2 -> inPort(node, 1);
            default -> inPort(node, 0);
        };
    }

    /**
     * 8 位整数转 8 元组：置位通道输出 15，其余 0。
     */
    private static long intToTuple(int bits) {
        long t = 0L;
        for (int b = 0; b < 8; b++) {
            if (((bits >> b) & 1) != 0) t |= 15L << (4 * b);
        }
        return t;
    }

    /**
     * 8 元组逐通道取反：通道非 0 变 0，通道为 0 变 15。按位非的输出语义。
     */
    private static long notTuple(long tuple) {
        long t = 0L;
        for (int b = 0; b < 8; b++) {
            if (((tuple >> (4 * b)) & 0xF) == 0) t |= 15L << (4 * b);
        }
        return t;
    }

    /**
     * 从 8 元组读取一个有符号 8 位整数（补码）：最高通道为符号位。
     */
    private static int signedFromTuple(long tuple) {
        int v = tupleToInt(tuple);
        return (v & 0x80) != 0 ? v - 0x100 : v;
    }

    /**
     * 8 元组按通道移动（保留通道信号强度）：shift>0 左移，shift<0 右移，空位补 0。
     * 目标通道 i 取源通道 i-shift，越界则填 0。
     */
    private static long analogShift(long tuple, int shift) {
        long t = 0L;
        for (int i = 0; i < 8; i++) {
            int src = i - shift;
            if (src >= 0 && src < 8) {
                t |= ((tuple >> (4 * src)) & 0xF) << (4 * i);
            }
        }
        return t;
    }

    /**
     * 8 元组算术移动：右移时越界通道补符号位（第 7 通道），左移补 0。
     * 目标通道 i 取源通道 i-shift；右移时若源通道 >= 8 则取符号位。
     */
    private static long analogArithmeticShift(long tuple, int shift) {
        long sign = (tuple >> (4 * 7)) & 0xF;
        long t = 0L;
        for (int i = 0; i < 8; i++) {
            int src = i - shift;
            long lane;
            if (src >= 0 && src < 8) {
                lane = (tuple >> (4 * src)) & 0xF;
            } else if (shift < 0) {
                lane = sign;
            } else {
                lane = 0L;
            }
            t |= lane << (4 * i);
        }
        return t;
    }

    /**
     * 8 元组按通道循环移动（保留通道信号强度）：shift>0 循环左移，shift<0 循环右移。
     * 目标通道 i 取源通道 (i-shift) mod 8。
     */
    private static long analogRotate(long tuple, int shift) {
        long t = 0L;
        for (int i = 0; i < 8; i++) {
            int src = ((i - shift) % 8 + 8) % 8;
            t |= ((tuple >> (4 * src)) & 0xF) << (4 * i);
        }
        return t;
    }

    /**
     * 读取元件第 k 个输入引脚：1 位返回 0~15，8 位返回 8 元组。
     */
    private long inPort(int node, int k) {
        InputPort[] ports = inputPorts[node];
        if (ports == null || k >= ports.length) return 0L;
        InputPort p = ports[k];
        if (p == null) return 0L;
        if (p.width() == 8) {
            long tuple = 0L;
            for (int b = 0; b < 8; b++) {
                Dep d = p.lanes()[b];
                if (d != null) tuple |= (depValue(d) & 0xF) << (4 * b);
            }
            return tuple;
        }
        Dep d = p.lanes()[0];
        return d == null ? 0L : depValue(d);
    }

    private CustomRuntime customRuntime(Component component, CustomCircuitComponent custom) {
        long stateKey = key(component.x, component.y);
        CustomRuntime existing = customRuntimes.get(stateKey);
        if (existing != null && existing.component == custom) {
            return existing;
        }
        CircuitDiagram source = custom.getCustomCircuit();
        CircuitDiagram diagram = source == null ? new CircuitDiagram() : source.copy();
        CircuitSimulator simulator = new CircuitSimulator();
        simulator.setDiagram(diagram);
        CustomRuntime runtime = new CustomRuntime(custom, diagram, simulator);
        customRuntimes.put(stateKey, runtime);
        return runtime;
    }

    private boolean hasSequential() {
        for (NodeKind kind : nodeKind) {
            if (kind == NodeKind.SEQ || kind == NodeKind.CAP || kind == NodeKind.RES) {
                return true;
            }
        }
        return false;
    }

    private long evalCustom(CustomRuntime runtime, int node) {
        CustomCircuitComponent custom = runtime.component;
        Map<String, Long> inputValues = new HashMap<>();
        for (StoredCircuit.PinBinding pin : custom.getRecord().custom().pins()) {
            if (pin.type() == PinType.INPUT) {
                inputValues.put(pin.label(), inPort(node, inputPortIndex(custom, pin.side(), pin.offset())));
            }
        }
        applyCustomInputs(runtime.diagram, inputValues);
        runtime.simulator.evaluate();

        long result = 0L;
        for (StoredCircuit.PinBinding pin : custom.getRecord().custom().pins()) {
            if (pin.type() != PinType.OUTPUT) {
                continue;
            }
            long value = readCustomOutput(runtime.diagram, runtime.simulator, pin.label());
            result |= value << outputSlot(custom, pin.side(), pin.offset()).bitStart();
        }
        return result;
    }

    private static void applyCustomInputs(CircuitDiagram diagram, Map<String, Long> inputValues) {
        for (CircuitDiagram.Chunk chunk : diagram.chunks.values()) {
            for (Component component : chunk.components) {
                if (component.component == BuiltinCircuitComponents.INPUT
                        && component.instance instanceof InputComponentInstance input) {
                    String label = input.getLabel();
                    long value = "VCC".equals(label) ? 15L : inputValues.getOrDefault(label, 0L);
                    input.setSignal((int) (value & 0xF));
                } else if (component.component == BuiltinCircuitComponents.INPUT_8
                        && component.instance instanceof Input8ComponentInstance input8) {
                    long value = inputValues.getOrDefault(input8.getLabel(), 0L);
                    for (int b = 0; b < 8; b++) {
                        input8.setBit(b, (int) ((value >> (4 * b)) & 0xF));
                    }
                }
            }
        }
    }

    private static long readCustomOutput(CircuitDiagram diagram, CircuitSimulator simulator, String label) {
        for (CircuitDiagram.Chunk chunk : diagram.chunks.values()) {
            for (Component component : chunk.components) {
                if (component.component == BuiltinCircuitComponents.OUTPUT
                        && component.instance instanceof OutputComponentInstance output
                        && label.equals(output.getLabel())) {
                    return simulator.getComponentValue(component.x, component.y);
                }
                if (component.component == BuiltinCircuitComponents.OUTPUT_8
                        && component.instance instanceof Output8ComponentInstance output8
                        && label.equals(output8.getLabel())) {
                    return simulator.getComponentValue(component.x, component.y);
                }
            }
        }
        return 0L;
    }

    private long evalCombinational(Component component, int node) {
        CircuitComponent cc = component.component;
        CustomRuntime runtime = customRuntimes.get(key(component.x, component.y));
        if (runtime != null && runtime.component == component.component) {
            return evalCustom(runtime, node);
        }
        if (cc == BuiltinCircuitComponents.TRANSISTOR) {
            int west = (int) inPort(node, 0);
            int south = (int) inPort(node, 1);
            return Math.min((int) Math.floor(2.0 * west * (1.0 - south / 15.0)), 15);
        } else if (cc == BuiltinCircuitComponents.DIODE) {
            return inPort(node, 0);
        } else if (cc == BuiltinCircuitComponents.RESISTOR) {
            int decay = ((ResistorComponentInstance) component.instance).getDecay();
            return Math.max(inPort(node, 0) - decay, 0);
        } else if (cc == BuiltinCircuitComponents.BATTERY) {
            return 15;
        } else if (cc == BuiltinCircuitComponents.INPUT) {
            return ((InputComponentInstance) component.instance).getSignal();
        } else if (cc == BuiltinCircuitComponents.OUTPUT) {
            return inPort(node, 0);
        } else if (cc == BuiltinCircuitComponents.INPUT_8) {
            long tuple = 0L;
            Input8ComponentInstance in8 = (Input8ComponentInstance) component.instance;
            for (int b = 0; b < 8; b++) {
                tuple |= ((long) in8.getBit(b) & 0xF) << (4 * b);
            }
            return tuple;
        } else if (cc == BuiltinCircuitComponents.OUTPUT_8) {
            return inPort(node, 0);
        } else if (cc == BuiltinCircuitComponents.TFF || cc == BuiltinCircuitComponents.TFF_PRIME) {
            return (long) tffQ.getOrDefault(key(component.x, component.y), 0)
                    << outputSlot(cc, Direction.EAST, 0).bitStart();
        } else if (cc == BuiltinCircuitComponents.REGISTER || cc == BuiltinCircuitComponents.REGISTER_8
                || cc == BuiltinCircuitComponents.REGISTER_ANALOG) {
            return registerQ.getOrDefault(key(component.x, component.y), 0L)
                    << outputSlot(cc, Direction.EAST, 0).bitStart();
        } else if (cc == BuiltinCircuitComponents.ADC) {
            return intToTuple((int) inPort(node, 0) & 0xF)
                    << outputSlot(cc, Direction.EAST, 0).bitStart();
        } else if (cc == BuiltinCircuitComponents.DAC) {
            return tupleToInt(inPort(node, 0)) & 0xF;
        } else if (cc == BuiltinCircuitComponents.AMPLIFIER) {
            return inPort(node, 0) > 0 ? 15 : 0;
        } else if (cc == BuiltinCircuitComponents.AND_GATE) {
            int a = (int) inPort(node, 0);
            int b = (int) inPort(node, 1);
            return a != 0 && b != 0 ? 15 : 0;
        } else if (cc == BuiltinCircuitComponents.AND_GATE_SC) {
            int a = (int) inPort(node, 0);
            int b = (int) inPort(node, 1);
            return a != 0 ? b : 0;
        } else if (cc == BuiltinCircuitComponents.OR_GATE) {
            int a = (int) inPort(node, 0);
            int b = (int) inPort(node, 1);
            return a != 0 || b != 0 ? 15 : 0;
        } else if (cc == BuiltinCircuitComponents.OR_GATE_SC) {
            int a = (int) inPort(node, 0);
            int b = (int) inPort(node, 1);
            return a != 0 ? a : b;
        } else if (cc == BuiltinCircuitComponents.NOT_GATE) {
            return inPort(node, 0) == 0 ? 15 : 0;
        } else if (cc == BuiltinCircuitComponents.ANALOG_NOT) {
            return 15 - inPort(node, 0);
        } else if (cc == BuiltinCircuitComponents.NAND_GATE) {
            int a = (int) inPort(node, 0);
            int b = (int) inPort(node, 1);
            return a != 0 && b != 0 ? 0 : 15;
        } else if (cc == BuiltinCircuitComponents.NOR_GATE) {
            int a = (int) inPort(node, 0);
            int b = (int) inPort(node, 1);
            return a != 0 || b != 0 ? 0 : 15;
        } else if (cc == BuiltinCircuitComponents.XOR_GATE) {
            int a = (int) inPort(node, 0);
            int b = (int) inPort(node, 1);
            return (a != 0) ^ (b != 0) ? 15 : 0;
        } else if (cc == BuiltinCircuitComponents.XNOR_GATE) {
            int a = (int) inPort(node, 0);
            int b = (int) inPort(node, 1);
            return (a != 0) ^ (b != 0) ? 0 : 15;
        } else if (cc == BuiltinCircuitComponents.AND_GATE_3) {
            int a = (int) inPort(node, 0);
            int b = (int) inPort(node, 1);
            int c = (int) inPort(node, 2);
            return a != 0 && b != 0 && c != 0 ? 15 : 0;
        } else if (cc == BuiltinCircuitComponents.OR_GATE_3) {
            int a = (int) inPort(node, 0);
            int b = (int) inPort(node, 1);
            int c = (int) inPort(node, 2);
            return a != 0 || b != 0 || c != 0 ? 15 : 0;
        } else if (cc == BuiltinCircuitComponents.XOR_GATE_3) {
            int a = (int) inPort(node, 0);
            int b = (int) inPort(node, 1);
            int c = (int) inPort(node, 2);
            return (a != 0) ^ (b != 0) ^ (c != 0) ? 15 : 0;
        } else if (cc == BuiltinCircuitComponents.XNOR_GATE_3) {
            int a = (int) inPort(node, 0);
            int b = (int) inPort(node, 1);
            int c = (int) inPort(node, 2);
            return (a != 0) ^ (b != 0) ^ (c != 0) ? 0 : 15;
        } else if (cc == BuiltinCircuitComponents.MUX_2) {
            int a = (int) inPort(node, 0);
            long d1 = inPort(node, 1);
            long d0 = inPort(node, 2);
            return (a != 0 ? d1 : d0) != 0 ? 15 : 0;
        } else if (cc == BuiltinCircuitComponents.MUX_2_8) {
            int a = (int) inPort(node, 0);
            long d1 = inPort(node, 1);
            long d0 = inPort(node, 2);
            return amplifyTuple(a != 0 ? d1 : d0);
        } else if (cc == BuiltinCircuitComponents.MUX_ANALOG_2 || cc == BuiltinCircuitComponents.MUX_ANALOG_2_8) {
            int a = (int) inPort(node, 0);
            long d1 = inPort(node, 1);
            long d0 = inPort(node, 2);
            return a != 0 ? d1 : d0;
        } else if (cc == BuiltinCircuitComponents.MUX_4 || cc == BuiltinCircuitComponents.MUX_4_8) {
            long sel = mux4Select(node);
            return cc == BuiltinCircuitComponents.MUX_4_8 ? amplifyTuple(sel) : (sel != 0 ? 15 : 0);
        } else if (cc == BuiltinCircuitComponents.MUX_ANALOG_4 || cc == BuiltinCircuitComponents.MUX_ANALOG_4_8) {
            return mux4Select(node);
        } else if (cc == BuiltinCircuitComponents.TRI_STATE_BUFFER_8) {
            long d = inPort(node, 0);
            int a = (int) inPort(node, 1);
            return a != 0 ? amplifyTuple(d) : 0L;
        } else if (cc == BuiltinCircuitComponents.TRI_STATE_BUFFER_ANALOG_8) {
            long d = inPort(node, 0);
            int a = (int) inPort(node, 1);
            return a != 0 ? d : 0L;
        } else if (cc == BuiltinCircuitComponents.DECODER_2_4) {
            int index = ((inPort(node, 0) != 0 ? 1 : 0) << 1) | (inPort(node, 1) != 0 ? 1 : 0);
            return decodeTuple(index) << outputSlot(cc, Direction.EAST, 0).bitStart();
        } else if (cc == BuiltinCircuitComponents.DECODER_3_8) {
            int index = ((inPort(node, 1) != 0 ? 1 : 0) << 2)
                    | ((inPort(node, 0) != 0 ? 1 : 0) << 1)
                    | (inPort(node, 2) != 0 ? 1 : 0);
            return decodeTuple(index) << outputSlot(cc, Direction.EAST, 0).bitStart();
        } else if (cc == BuiltinCircuitComponents.DECODER_4_16) {
            int index = ((inPort(node, 2) != 0 ? 1 : 0) << 3)
                    | ((inPort(node, 0) != 0 ? 1 : 0) << 2)
                    | ((inPort(node, 1) != 0 ? 1 : 0) << 1)
                    | (inPort(node, 3) != 0 ? 1 : 0);
            if (index < 8) {
                return decodeTuple(index) << outputSlot(cc, Direction.EAST, 0).bitStart();
            }
            return decodeTuple(index - 8) << outputSlot(cc, Direction.EAST, 1).bitStart();
        } else if (cc == BuiltinCircuitComponents.ENCODER_4_2 || cc == BuiltinCircuitComponents.PRIORITY_ENCODER_4_2) {
            boolean priority = cc == BuiltinCircuitComponents.PRIORITY_ENCODER_4_2;
            int index = encodeIndex(inPort(node, 0), 0L, 4, priority);
            long v = 0L;
            if ((index & 1) != 0) v |= 15L << outputSlot(cc, Direction.NORTH, 0).bitStart();  // Y0
            if ((index & 2) != 0) v |= 15L << outputSlot(cc, Direction.SOUTH, 0).bitStart();  // Y1
            return v;
        } else if (cc == BuiltinCircuitComponents.ENCODER_8_3 || cc == BuiltinCircuitComponents.PRIORITY_ENCODER_8_3) {
            boolean priority = cc == BuiltinCircuitComponents.PRIORITY_ENCODER_8_3;
            int index = encodeIndex(inPort(node, 0), 0L, 8, priority);
            long v = 0L;
            if ((index & 1) != 0) v |= 15L << outputSlot(cc, Direction.NORTH, 0).bitStart();  // Y0
            if ((index & 2) != 0) v |= 15L << outputSlot(cc, Direction.EAST, 0).bitStart();   // Y1
            if ((index & 4) != 0) v |= 15L << outputSlot(cc, Direction.SOUTH, 0).bitStart();  // Y2
            return v;
        } else if (cc == BuiltinCircuitComponents.ENCODER_16_4 || cc == BuiltinCircuitComponents.PRIORITY_ENCODER_16_4) {
            boolean priority = cc == BuiltinCircuitComponents.PRIORITY_ENCODER_16_4;
            int index = encodeIndex(inPort(node, 1), inPort(node, 0), 16, priority);
            long v = 0L;
            if ((index & 1) != 0) v |= 15L << outputSlot(cc, Direction.NORTH, 0).bitStart();  // Y0
            if ((index & 2) != 0) v |= 15L << outputSlot(cc, Direction.EAST, 0).bitStart();   // Y1
            if ((index & 4) != 0) v |= 15L << outputSlot(cc, Direction.EAST, 1).bitStart();   // Y2
            if ((index & 8) != 0) v |= 15L << outputSlot(cc, Direction.SOUTH, 0).bitStart();  // Y3
            return v;
        } else if (cc == BuiltinCircuitComponents.HALF_ADDER) {
            int a = (int) inPort(node, 0);
            int b = (int) inPort(node, 1);
            long v = 0L;
            v |= (long) ((a != 0) ^ (b != 0) ? 15 : 0) << outputSlot(cc, Direction.EAST, 0).bitStart();
            v |= (long) (a != 0 && b != 0 ? 15 : 0) << outputSlot(cc, Direction.SOUTH, 0).bitStart();
            return v;
        } else if (cc == BuiltinCircuitComponents.FULL_ADDER) {
            int a = (int) inPort(node, 0);
            int b = (int) inPort(node, 1);
            int cin = inPort(node, 2) != 0 ? 1 : 0;
            long v = 0L;
            v |= (long) ((a != 0) ^ (b != 0) ^ (cin != 0) ? 15 : 0) << outputSlot(cc, Direction.EAST, 0).bitStart();
            v |= (long) ((a != 0 && b != 0) || (b != 0 && cin != 0) || (cin != 0 && a != 0) ? 15 : 0) << outputSlot(cc, Direction.SOUTH, 0).bitStart();
            return v;
        } else if (cc == BuiltinCircuitComponents.BUS_JOINER_8) {
            long tuple = 0L;
            for (int b = 0; b < 8; b++) {
                tuple |= (inPort(node, 7 - b) & 0xF) << (4 * b);
            }
            return tuple << outputSlot(cc, Direction.EAST, 0).bitStart();
        } else if (cc == BuiltinCircuitComponents.BUS_SPLITTER_8) {
            long tuple = inPort(node, 0);
            long v = 0L;
            for (int b = 0; b < 8; b++) {
                v |= ((tuple >> (4 * b)) & 0xF) << outputSlot(cc, Direction.EAST, b).bitStart();
            }
            return v;
        } else if (cc == BuiltinCircuitComponents.ADDER_8) {
            int a = tupleToInt(inPort(node, 0));
            int b = tupleToInt(inPort(node, 1));
            int cin = inPort(node, 2) != 0 ? 1 : 0;
            int sum = a + b + cin;
            long v = 0L;
            v |= intToTuple(sum & 0xFF) << outputSlot(cc, Direction.EAST, 0).bitStart();
            v |= ((long) ((((sum >> 8) & 1) * 15) & 0xF)) << outputSlot(cc, Direction.SOUTH, 0).bitStart();
            return v;
        } else if (cc == BuiltinCircuitComponents.CONST_8 || cc == BuiltinCircuitComponents.MASK_8) {
            // 8 位常量/掩码：位为 1 的通道输出 15；掩码则输出输入 A 的强度。
            int strength = 15;
            if (cc == BuiltinCircuitComponents.MASK_8) {
                strength = (int) inPort(node, 0) & 0xF;
                if (strength == 0) {
                    return 0L;
                }
            }
            int mask = ((Const8ComponentInstance) component.instance).getValue();
            long tuple = 0L;
            for (int b = 0; b < 8; b++) {
                if (((mask >> b) & 1) != 0) {
                    tuple |= (long) strength << (4 * b);
                }
            }
            return tuple << outputSlot(cc, Direction.EAST, 0).bitStart();
        } else if (cc == BuiltinCircuitComponents.CONST_ANALOG_8) {
            ConstAnalog8ComponentInstance instance = (ConstAnalog8ComponentInstance) component.instance;
            long tuple = 0L;
            for (int b = 0; b < 8; b++) {
                tuple |= (long) (instance.getLane(b) & 0xF) << (4 * b);
            }
            return tuple << outputSlot(cc, Direction.EAST, 0).bitStart();
        } else if (cc == BuiltinCircuitComponents.COMPARATOR
                || cc == BuiltinCircuitComponents.COMPARATOR_8
                || cc == BuiltinCircuitComponents.COMPARATOR_SIGNED_8
                || cc == BuiltinCircuitComponents.COMPARATOR_ANALOG) {
            int i0 = (int) inPort(node, 2) & 0xF;
            int i1 = (int) inPort(node, 3) & 0xF;
            int comparison;
            if (cc == BuiltinCircuitComponents.COMPARATOR_8) {
                comparison = Integer.compare(tupleToInt(inPort(node, 0)), tupleToInt(inPort(node, 1)));
            } else if (cc == BuiltinCircuitComponents.COMPARATOR_SIGNED_8) {
                comparison = Integer.compare((byte) tupleToInt(inPort(node, 0)), (byte) tupleToInt(inPort(node, 1)));
            } else if (cc == BuiltinCircuitComponents.COMPARATOR_ANALOG) {
                comparison = Integer.compare((int) inPort(node, 0) & 0xF, (int) inPort(node, 1) & 0xF);
            } else {
                int a = inPort(node, 0) != 0 ? 1 : 0;
                int b = inPort(node, 1) != 0 ? 1 : 0;
                comparison = Integer.compare(a, b);
            }
            // A=B 时 I0/I1 按“非 0 即 15”直通，而不是透传模拟强度。
            int y0 = comparison < 0 ? 15 : (comparison == 0 && i0 != 0 ? 15 : 0);
            int y1 = comparison > 0 ? 15 : (comparison == 0 && i1 != 0 ? 15 : 0);
            long v = 0L;
            v |= (long) y0 << outputSlot(cc, Direction.SOUTH, 1).bitStart(); // Y0
            v |= (long) y1 << outputSlot(cc, Direction.SOUTH, 0).bitStart(); // Y1
            return v;
        } else if (cc == BuiltinCircuitComponents.BITWISE_AND
                || cc == BuiltinCircuitComponents.BITWISE_OR
                || cc == BuiltinCircuitComponents.BITWISE_XOR
                || cc == BuiltinCircuitComponents.BITWISE_NAND
                || cc == BuiltinCircuitComponents.BITWISE_NOR
                || cc == BuiltinCircuitComponents.BITWISE_XNOR) {
            // 8 位按位逻辑运算：逐通道对 A、B 进行对应逻辑运算，输出非 0 即 15。
            long a = inPort(node, 0);
            long b = inPort(node, 1);
            long t = 0L;
            for (int lane = 0; lane < 8; lane++) {
                boolean av = ((a >> (4 * lane)) & 0xF) != 0;
                boolean bv = ((b >> (4 * lane)) & 0xF) != 0;
                boolean r;
                if (cc == BuiltinCircuitComponents.BITWISE_AND) r = av && bv;
                else if (cc == BuiltinCircuitComponents.BITWISE_OR) r = av || bv;
                else if (cc == BuiltinCircuitComponents.BITWISE_XOR) r = av ^ bv;
                else if (cc == BuiltinCircuitComponents.BITWISE_NAND) r = !(av && bv);
                else if (cc == BuiltinCircuitComponents.BITWISE_NOR) r = !(av || bv);
                else r = !(av ^ bv);
                if (r) t |= 15L << (4 * lane);
            }
            return t << outputSlot(cc, Direction.EAST, 0).bitStart();
        } else if (cc == BuiltinCircuitComponents.BITWISE_NOT) {
            // 8 位按位非：逐通道取反，输出非 0 即 15。
            long t = notTuple(inPort(node, 0));
            return t << outputSlot(cc, Direction.EAST, 0).bitStart();
        } else if (cc == BuiltinCircuitComponents.NEG) {
            // 8 位取反器：输出 A 的相反数的补码表示。
            int a = tupleToInt(inPort(node, 0));
            return intToTuple((~a + 1) & 0xFF) << outputSlot(cc, Direction.EAST, 0).bitStart();
        } else if (cc == BuiltinCircuitComponents.OR_GATE_8
                || cc == BuiltinCircuitComponents.AND_GATE_8
                || cc == BuiltinCircuitComponents.XOR_GATE_8
                || cc == BuiltinCircuitComponents.XNOR_GATE_8) {
            // 8 路逻辑门：对 8 位输入 A 的 8 个通道进行逻辑运算，输出 1 位 Y。
            long a = inPort(node, 0);
            int count = 0;
            for (int lane = 0; lane < 8; lane++) {
                if (((a >> (4 * lane)) & 0xF) != 0) count++;
            }
            boolean r;
            if (cc == BuiltinCircuitComponents.OR_GATE_8) r = count > 0;
            else if (cc == BuiltinCircuitComponents.AND_GATE_8) r = count == 8;
            else if (cc == BuiltinCircuitComponents.XOR_GATE_8) r = (count & 1) == 1;
            else r = (count & 1) == 0;
            return (r ? 15L : 0L) << outputSlot(cc, Direction.EAST, 0).bitStart();
        } else if (cc == BuiltinCircuitComponents.BITWISE_SHIFT
                || cc == BuiltinCircuitComponents.BITWISE_SHIFT_ARITHMETIC
                || cc == BuiltinCircuitComponents.BITWISE_ROTATE
                || cc == BuiltinCircuitComponents.BITWISE_SHIFT_ANALOG
                || cc == BuiltinCircuitComponents.BITWISE_ROTATE_ANALOG) {
            // 8 位位移器 / 算术位移器 / 循环位移器：A 左移 B 位，B 视作有符号数（补码），为负则右移。
            long a = inPort(node, 0);
            int shift = signedFromTuple(inPort(node, 1));
            boolean arithmetic = cc == BuiltinCircuitComponents.BITWISE_SHIFT_ARITHMETIC;
            boolean rotate = cc == BuiltinCircuitComponents.BITWISE_ROTATE
                    || cc == BuiltinCircuitComponents.BITWISE_ROTATE_ANALOG;
            boolean analog = cc == BuiltinCircuitComponents.BITWISE_SHIFT_ANALOG
                    || cc == BuiltinCircuitComponents.BITWISE_ROTATE_ANALOG;
            long shifted = arithmetic ? analogArithmeticShift(a, shift)
                    : (rotate ? analogRotate(a, shift) : analogShift(a, shift));
            long t = analog ? shifted : amplifyTuple(shifted);
            return t << outputSlot(cc, Direction.EAST, 0).bitStart();
        } else if (cc == BuiltinCircuitComponents.MULTIPLIER || cc == BuiltinCircuitComponents.MULTIPLIER_LITE) {
            // 8 位无符号乘法：简单版只输出低 8 位，完整版再输出高 8 位。
            int a = tupleToInt(inPort(node, 0));
            int b = tupleToInt(inPort(node, 1));
            int product = a * b;
            long v = 0L;
            v |= intToTuple(product & 0xFF) << outputSlot(cc, Direction.EAST, 0).bitStart();
            if (cc == BuiltinCircuitComponents.MULTIPLIER) {
                v |= intToTuple((product >> 8) & 0xFF) << outputSlot(cc, Direction.EAST, 1).bitStart();
            }
            return v;
        } else if (cc == BuiltinCircuitComponents.DIVIDER) {
            // 8 位无符号除法：Q 为整数商，R 为余数；除数为 0 时输出 0。
            int a = tupleToInt(inPort(node, 0));
            int b = tupleToInt(inPort(node, 1));
            int q = 0;
            int r = 0;
            if (b != 0) {
                q = a / b;
                r = a % b;
            }
            long v = 0L;
            v |= intToTuple(q & 0xFF) << outputSlot(cc, Direction.EAST, 0).bitStart();
            v |= intToTuple(r & 0xFF) << outputSlot(cc, Direction.SOUTH, 0).bitStart();
            return v;
        } else if (cc == BuiltinCircuitComponents.ALU || cc == BuiltinCircuitComponents.ALU_ADVANCED) {
            // 多功能运算器：输入端口顺序为 A、B、OP、CI。
            boolean advanced = cc == BuiltinCircuitComponents.ALU_ADVANCED;
            int a = tupleToInt(inPort(node, 0));
            int b = tupleToInt(inPort(node, 1));
            int op = tupleToInt(inPort(node, 2)) & 0x7;
            int ci = inPort(node, 3) != 0 ? 1 : 0;
            int y;
            int y1 = 0;
            int co = 0;
            switch (op) {
                case 0 -> {
                    int sum = a + b + ci;
                    y = sum & 0xFF;
                    co = (sum >> 8) & 1;
                }
                case 1 -> {
                    // 减法复用 CI/CO 作为借位输入/输出：Y = A - B - CI，CO = 1 表示不够减。
                    y = (a - b - ci) & 0xFF;
                    co = a < b + ci ? 1 : 0;
                }
                case 2 -> y = a & b;
                case 3 -> y = a | b;
                case 4 -> y = ~a & 0xFF;
                case 5 -> y = a ^ b;
                case 6 -> {
                    // 逻辑移位，B 按 8 位补码解释：非负左移、负数右移，空位补 0。
                    int shift = (byte) (b & 0xFF);
                    if (shift >= 0) {
                        y = shift >= 8 ? 0 : (a << shift) & 0xFF;
                    } else {
                        int s = -shift;
                        y = s >= 8 ? 0 : (a & 0xFF) >>> s;
                    }
                }
                case 7 -> {
                    if (advanced) {
                        int product = a * b;
                        y = product & 0xFF;
                        y1 = (product >> 8) & 0xFF;
                    } else {
                        y = 0;
                    }
                }
                default -> y = 0;
            }
            long v = 0L;
            v |= intToTuple(y & 0xFF) << outputSlot(cc, Direction.EAST, 0).bitStart();
            if (advanced) {
                long y1Tuple = intToTuple(y1 & 0xFF);
                v |= (y1Tuple & 0x0FFFFFFFL) << outputSlot(cc, Direction.EAST, 1).bitStart();
                // Y1 的槽位跨过第 64 位，最高 4 位写入高位字。
                evalHighBits = (y1Tuple >>> 28) & 0xF;
            }
            v |= (long) (co != 0 ? 15 : 0) << outputSlot(cc, Direction.SOUTH, 0).bitStart();
            return v;
        }
        return 0;
    }

    private int evalResonator(Component component, int node) {
        ResonatorComponentInstance instance = (ResonatorComponentInstance) component.instance;
        int phase = resPhase.getOrDefault(key(component.x, component.y), instance.getInitialPhase());
        int period = Math.max(1, instance.getHighDuration() + instance.getLowDuration());
        boolean high = phase % period < instance.getHighDuration();
        return high ? (int) inPort(node, 0) : 0;
    }

    /* ============================================================
     *  时钟步进 / 复位
     * ============================================================ */

    public void step() {
        if (diagram == null || totalNodes == 0) {
            return;
        }
        evaluate();

        Map<Integer, Integer> dSample = new HashMap<>();
        for (int node : capNodes) {
            int src = dSampleSource[node];
            dSample.put(node, src >= 0 ? (int) nodeVals[src] : 0);
        }
        for (int node : capNodes) {
            int idx = node - numNets;
            Component component = comps.get(idx);
            int q = capQ.getOrDefault(key(component.x, component.y), 0);
            int d = dSample.getOrDefault(node, 0);
            int charge = ((CapacitorComponentInstance) component.instance).getChargeSpeed();
            int discharge = ((CapacitorComponentInstance) component.instance).getDischargeSpeed();
            int q1;
            if (d > q) {
                q1 = Math.min(q + charge, d);
            } else if (d < q) {
                q1 = Math.max(q - discharge, d);
            } else {
                q1 = q;
            }
            capQ.put(key(component.x, component.y), q1);
        }
        for (int node : resNodes) {
            int idx = node - numNets;
            Component component = comps.get(idx);
            ResonatorComponentInstance instance = (ResonatorComponentInstance) component.instance;
            int period = Math.max(1, instance.getHighDuration() + instance.getLowDuration());
            int phase = resPhase.getOrDefault(key(component.x, component.y), instance.getInitialPhase());
            resPhase.put(key(component.x, component.y), (phase + 1) % period);
        }
        for (int node : tffNodes) {
            int idx = node - numNets;
            Component component = comps.get(idx);
            long stateKey = key(component.x, component.y);
            int clock = (int) inPort(node, 0);
            int q = tffQ.getOrDefault(stateKey, 0);
            if (component.component == BuiltinCircuitComponents.TFF) {
                if (clock != 0) {
                    q = q == 0 ? 15 : 0;
                }
            } else {
                int previous = tffPrevClock.getOrDefault(stateKey, 0);
                if (clock != 0 && previous == 0) {
                    q = q == 0 ? 15 : 0;
                }
                tffPrevClock.put(stateKey, clock != 0 ? 1 : 0);
            }
            tffQ.put(stateKey, q);
        }
        for (int node : registerNodes) {
            int idx = node - numNets;
            Component component = comps.get(idx);
            long stateKey = key(component.x, component.y);
            if (inPort(node, 1) != 0) {
                long value;
                if (component.component == BuiltinCircuitComponents.REGISTER) {
                    value = inPort(node, 0) != 0 ? 15 : 0;
                } else if (component.component == BuiltinCircuitComponents.REGISTER_ANALOG) {
                    value = inPort(node, 0) & 0xF;
                } else {
                    value = amplifyTuple(inPort(node, 0));
                }
                registerQ.put(stateKey, value);
            }
        }
        for (CustomRuntime runtime : customRuntimes.values()) {
            if (runtime.stateful) {
                runtime.simulator.step();
            }
        }

        evaluate();
    }

    private void collectResults() {
        wireValues.clear();
        wireBad.clear();
        compValues.clear();
        for (Map.Entry<WirePoint, Integer> e : wireNet.entrySet()) {
            int net = e.getValue();
            wireValues.put(e.getKey(), (int) nodeVals[net]);
            wireBad.put(e.getKey(), badNet[net]);
        }
        for (int i = 0; i < comps.size(); i++) {
            Component component = comps.get(i);
            compValues.put(key(component.x, component.y), (int) nodeVals[numNets + i]);
        }
    }

    /* ============================================================
     *  工具方法
     * ============================================================ */

    private record PinInfo(Direction facing, int ex, int ey, Direction side, int offset) {
    }

    private static PinInfo pinExternal(CircuitComponent comp, Direction rot, boolean mirrored, int cx, int cy, Direction side, int offset) {
        int w0 = comp.getWidth();
        int h0 = comp.getHeight();
        double[] local = CircuitComponent.rotatedPinOffset(w0, h0, rot, mirrored, side, offset);
        double rx = local[0];
        double ry = local[1];
        int wRot = (rot == Direction.EAST || rot == Direction.WEST) ? h0 : w0;
        int hRot = (rot == Direction.EAST || rot == Direction.WEST) ? w0 : h0;
        Direction facing;
        if (ry <= EPS) facing = Direction.NORTH;
        else if (ry >= hRot - EPS) facing = Direction.SOUTH;
        else if (rx <= EPS) facing = Direction.WEST;
        else facing = Direction.EAST;
        int ex, ey;
        switch (facing) {
            case NORTH -> {
                ex = cx + (int) Math.floor(rx);
                ey = cy - 1;
            }
            case SOUTH -> {
                ex = cx + (int) Math.floor(rx);
                ey = cy + hRot;
            }
            case WEST -> {
                ex = cx - 1;
                ey = cy + (int) Math.floor(ry);
            }
            default -> {
                ex = cx + wRot;
                ey = cy + (int) Math.floor(ry);
            }
        }
        return new PinInfo(facing, ex, ey, side, offset);
    }

    private Wire findWire(int x, int y) {
        if (diagram == null) return null;
        CircuitDiagram.Block block = diagram.getBlock(x, y);
        return block instanceof Wire wire ? wire : null;
    }

    private static boolean hasSideMaterial(Wire wire, WireMaterial m) {
        return wire.north == m || wire.east == m || wire.south == m || wire.west == m;
    }

    private static WireMaterial materialAtSide(Wire wire, Direction d) {
        return switch (d) {
            case DOWN, UP -> null;
            case NORTH -> wire.north;
            case EAST -> wire.east;
            case SOUTH -> wire.south;
            case WEST -> wire.west;
        };
    }

    private static long key(int x, int y) {
        return ((long) x << 32) | (y & 0xFFFFFFFFL);
    }

    /**
     * 位宽不匹配接缝键：编码接缝中点（网格顶点的平行/垂直边）。
     * 坐标意义：
     * <ul>
     * <li>水平接缝（NORTH/SOUTH 引脚）：中点 = (x0 + 0.5, y0)</li>
     * <li>垂直接缝（WEST/EAST 引脚）：中点 = (x0, y0 + 0.5)</li>
     * </ul>
     */
    private static long seamKey(int ex, int ey, Direction facing) {
        int x0, y0;
        boolean vertical;
        switch (facing) {
            case NORTH -> {
                x0 = ex;
                y0 = ey + 1;
                vertical = false;
            }
            case SOUTH -> {
                x0 = ex;
                y0 = ey;
                vertical = false;
            }
            case WEST -> {
                x0 = ex + 1;
                y0 = ey;
                vertical = true;
            }
            default -> {
                x0 = ex;
                y0 = ey;
                vertical = true;
            }
        }
        return ((long) x0 << 33) | ((y0 & 0xFFFFFFFFL) << 1) | (vertical ? 1 : 0);
    }

    private static int seamCellX(long seam) {
        return (int) (seam >> 33);
    }

    private static int seamCellY(long seam) {
        return (int) ((seam >> 1) & 0xFFFFFFFFL);
    }

    private static boolean seamVertical(long seam) {
        return (seam & 1) == 1;
    }

    private static final class UnionFind {
        private final List<Integer> parent = new ArrayList<>();

        int add() {
            parent.add(parent.size());
            return parent.size() - 1;
        }

        int find(int x) {
            int r = x;
            while (parent.get(r) != r) r = parent.get(r);
            while (parent.get(x) != r) {
                int next = parent.get(x);
                parent.set(x, r);
                x = next;
            }
            return r;
        }

        void union(int a, int b) {
            int ra = find(a);
            int rb = find(b);
            if (ra != rb) parent.set(ra, rb);
        }
    }
}