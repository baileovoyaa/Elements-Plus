package com.elementsplus.core.circuit;

import com.elementsplus.core.circuit.CircuitComponent.PinType;
import com.elementsplus.core.circuit.component.CapacitorComponentInstance;
import com.elementsplus.core.circuit.component.InputComponentInstance;
import com.elementsplus.core.circuit.component.ResistorComponentInstance;
import com.elementsplus.core.circuit.component.ResonatorComponentInstance;
import com.elementsplus.core.circuit.diagram.CircuitDiagram;
import com.elementsplus.core.circuit.diagram.CircuitDiagram.Component;
import com.elementsplus.core.circuit.diagram.CircuitDiagram.Wire;
import com.elementsplus.core.circuit.diagram.CircuitDiagram.Wire.WireMaterial;
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
 *
 * 思路（参考 docs/参考算法描述.md）：
 * 1. 并查集把同材料、同位宽的四连通导线按"每一位"合并为网络节点：
 *    1 位导线只有 1 条信号道，8 位总线拆成 8 条相互独立的信号道；
 * 2. 建依赖图：网络/组合元件/时序元件（电容 D 输入不建边，仅采样，切断时序环）；
 * 3. Kahn 拓扑排序；
 * 4. Tarjan SCC 把环内的网络标记为坏网；
 * 5. 按拓扑序传播求值（网络取所有驱动源对应信号道的最大值）；
 * 6. 时钟步进：求值 -> 采样电容 D -> 统一更新状态 -> 重新求值；
 * 7. 复位：清空时序状态（电容 = 0，谐振器 = 初相）。
 *
 * 位宽语义：
 * - 元件结点值按输出引脚布局被拆成若干个位段，1 位引脚占 4 位（0~15 模拟值），
 *   8 位引脚占 8 位（每一位取值 0 或 1）；
 * - 网络结点值 = 该信号道上的信号（1 位模拟 0~15，8 位总线信号道 0 或 15）；
 * - 不同位宽的线/引脚之间不传信号，并在连接处记录"位宽不匹配"告警；
 * - 8 位总线按位独立承载 8 路 1 位信号。
 *
 * 状态不持久化、不发送到服务端，仅用于屏幕内的模拟与渲染。
 */
public class CircuitSimulator {

    public record WirePoint(int x, int y, WireMaterial material, int bit) {
    }

    private enum NodeKind {
        NET, COMB, CAP, RES
    }

    private enum DepKind {
        /** 读取源结点值中的 4 位段（0~15）。位段起点由 Dep.bit 指定。 */
        WHOLE,
        /** 读取源结点值中的 1 位（0/15 数字信号）。位由 Dep.bit 指定。 */
        BIT
    }

    /** 一条入边依赖：源结点、取值方式、位段/位位置。 */
    private record Dep(int node, DepKind kind, int bit) {
    }

    /** 一个输入引脚的若干信号道依赖；位数 width 为 1 或 8。 */
    private record InputPort(int width, Dep[] lanes) {
    }

    private static final double EPS = 1e-6;

    private CircuitDiagram diagram;

    private int totalNodes;
    private int numNets;
    private NodeKind[] nodeKind = new NodeKind[0];
    private int[] nodeVals = new int[0];
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
    private Set<Long> mismatchCells = new HashSet<>();

    private final Map<WirePoint, Integer> wireValues = new HashMap<>();
    private final Map<WirePoint, Boolean> wireBad = new HashMap<>();
    private final Map<Long, Integer> compValues = new HashMap<>();

    private final Map<Long, Integer> capQ = new HashMap<>();
    private final Map<Long, Integer> resPhase = new HashMap<>();

    public void setDiagram(CircuitDiagram diagram) {
        this.diagram = diagram;
        rebuild();
        evaluate();
    }

    public void reset() {
        capQ.clear();
        resPhase.clear();
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
        return mismatchCells.contains(key(x, y));
    }

    public int getWireValue(int x, int y, WireMaterial material) {
        Integer v = wireValues.get(new WirePoint(x, y, material, 0));
        return v == null ? 0 : v;
    }

    /** 8 位总线打包值：bit b = 第 b 位信号是否为高（0/1）。 */
    public int getWireBusValue(int x, int y, WireMaterial material) {
        int v = 0;
        for (int b = 0; b < 8; b++) {
            Integer lane = wireValues.get(new WirePoint(x, y, material, b));
            if (lane != null && lane != 0) {
                v |= (1 << b);
            }
        }
        return v;
    }

    /** 该格上 material 对应的导线是否为 8 位总线。 */
    public boolean isWireBus(int x, int y, WireMaterial material) {
        for (int b = 1; b < 8; b++) {
            if (wireValues.containsKey(new WirePoint(x, y, material, b))) {
                return true;
            }
        }
        return wireValues.containsKey(new WirePoint(x, y, material, 0));
    }

    public boolean isWireBad(int x, int y, WireMaterial material) {
        Boolean b = wireBad.get(new WirePoint(x, y, material, 0));
        return b != null && b;
    }

    public int getComponentValue(int x, int y) {
        Integer v = compValues.get(key(x, y));
        return v == null ? 0 : v;
    }

    /* ============================================================
     *  构建依赖图
     * ============================================================ */

    private void rebuild() {
        comps.clear();
        capNodes.clear();
        resNodes.clear();
        wireNet.clear();
        wireValues.clear();
        wireBad.clear();
        compValues.clear();
        mismatchCells.clear();
        hasCycle = false;

        if (diagram == null) {
            totalNodes = 0;
            numNets = 0;
            nodeKind = new NodeKind[0];
            nodeVals = new int[0];
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

        /* ---------- 1. 并查集：同材料、同位宽的导线按每一位合并网络 ---------- */
        Map<WirePoint, Integer> elemIndex = new HashMap<>();
        UnionFind uf = new UnionFind();
        for (Wire wire : wires) {
            for (WireMaterial m : new WireMaterial[]{WireMaterial.COPPER, WireMaterial.GOLD}) {
                if (!hasSideMaterial(wire, m)) continue;
                for (int b = 0; b < wire.bitWidth; b++) {
                    int id = uf.add();
                    elemIndex.put(new WirePoint(wire.x, wire.y, m, b), id);
                }
            }
        }
        for (Wire wire : wires) {
            for (WireMaterial m : new WireMaterial[]{WireMaterial.COPPER, WireMaterial.GOLD}) {
                if (!hasSideMaterial(wire, m)) continue;
                for (Direction d : Direction.Plane.HORIZONTAL) {
                    if (materialAtSide(wire, d) != m) continue;
                    int nx = wire.x + d.getStepX();
                    int ny = wire.y + d.getStepZ();
                    Wire neighbor = findWire(nx, ny);
                    if (neighbor == null
                            || neighbor.bitWidth != wire.bitWidth
                            || materialAtSide(neighbor, d.getOpposite()) != m) {
                        continue;
                    }
                    for (int b = 0; b < wire.bitWidth; b++) {
                        Integer self = elemIndex.get(new WirePoint(wire.x, wire.y, m, b));
                        Integer other = elemIndex.get(new WirePoint(nx, ny, m, b));
                        if (self != null && other != null) {
                            uf.union(self, other);
                        }
                    }
                }
            }
        }

        Map<Integer, Integer> rootToNet = new HashMap<>();
        for (Map.Entry<WirePoint, Integer> e : elemIndex.entrySet()) {
            int root = uf.find(e.getValue());
            int net = rootToNet.computeIfAbsent(root, k -> rootToNet.size());
            wireNet.put(e.getKey(), net);
        }
        numNets = rootToNet.size();
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
            } else {
                nodeKind[numNets + i] = NodeKind.COMB;
            }
        }

        Map<Long, Integer> compIndex = new HashMap<>();
        for (int i = 0; i < numComps; i++) {
            compIndex.put(key(comps.get(i).x, comps.get(i).y), numNets + i);
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
                    PinInfo pin = pinExternal(component.component, component.direction, component.x, component.y, side, offset);
                    int pinWidth = component.component.getPinBitWidth(side, offset);

                    if (type == PinType.INPUT) {
                        Dep[] lanes = new Dep[pinWidth];
                        Wire wire = findWire(pin.ex, pin.ey);
                        if (wire != null) {
                            WireMaterial m = materialAtSide(wire, pin.facing.getOpposite());
                            if (m != null) {
                                if (wire.bitWidth == pinWidth) {
                                    for (int b = 0; b < pinWidth; b++) {
                                        Integer net = wireNet.get(new WirePoint(pin.ex, pin.ey, m, b));
                                        if (net != null) {
                                            lanes[b] = new Dep(net, DepKind.WHOLE, 0);
                                            if (kind != NodeKind.CAP) predSets[node].add(net);
                                            else dSample = net;
                                        }
                                    }
                                } else {
                                    mismatchCells.add(key(pin.ex, pin.ey));
                                }
                            }
                        } else {
                            Component other = compAt(pin.ex, pin.ey);
                            if (other != null) {
                                PinInfo op = otherPinAt(other, component, PinType.OUTPUT);
                                if (op != null) {
                                    OutSlot slot = outputSlot(other.component, op.side, op.offset);
                                    int otherNode = compIndex.get(key(other.x, other.y));
                                    if (slot.width() == pinWidth) {
                                        for (int b = 0; b < pinWidth; b++) {
                                            lanes[b] = new Dep(otherNode,
                                                    slot.width() == 8 ? DepKind.BIT : DepKind.WHOLE,
                                                    slot.bitStart() + (slot.width() == 8 ? b : 0));
                                            if (kind != NodeKind.CAP) predSets[node].add(otherNode);
                                            else dSample = otherNode;
                                        }
                                    } else {
                                        mismatchCells.add(key(pin.ex, pin.ey));
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
                                if (wire.bitWidth == pinWidth) {
                                    OutSlot slot = outputSlot(component.component, side, offset);
                                    for (int b = 0; b < pinWidth; b++) {
                                        Integer net = wireNet.get(new WirePoint(pin.ex, pin.ey, m, b));
                                        if (net != null) {
                                            netInputDeps[net].add(new Dep(node,
                                                    pinWidth == 8 ? DepKind.BIT : DepKind.WHOLE,
                                                    slot.bitStart() + (pinWidth == 8 ? b : 0)));
                                            predSets[net].add(node);
                                        }
                                    }
                                } else {
                                    mismatchCells.add(key(pin.ex, pin.ey));
                                }
                            }
                        } else {
                            Component other = compAt(pin.ex, pin.ey);
                            if (other != null) {
                                PinInfo ip = otherPinAt(other, component, PinType.INPUT);
                                if (ip != null) {
                                    int otherWidth = other.component.getPinBitWidth(ip.side, ip.offset);
                                    if (otherWidth != pinWidth) {
                                        mismatchCells.add(key(pin.ex, pin.ey));
                                    } else {
                                        int otherNode = compIndex.get(key(other.x, other.y));
                                        if (nodeKind[otherNode] != NodeKind.CAP) predSets[otherNode].add(node);
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

        nodeVals = new int[totalNodes];
    }

    private Component compAt(int x, int y) {
        CircuitDiagram.Block block = diagram.getBlock(x, y);
        return block instanceof Component component ? component : null;
    }

    /** other 组件上、朝向 me 组件所在区域（旋转后包围盒）的某类型引脚。 */
    private static PinInfo otherPinAt(Component other, Component me, PinType type) {
        Direction rot = me.direction;
        boolean ns = rot == Direction.NORTH || rot == Direction.SOUTH;
        int wRot = ns ? me.component.getWidth() : me.component.getHeight();
        int hRot = ns ? me.component.getHeight() : me.component.getWidth();
        for (Direction side : new Direction[]{Direction.WEST, Direction.SOUTH, Direction.EAST, Direction.NORTH}) {
            int count = (side == Direction.NORTH || side == Direction.SOUTH)
                    ? other.component.getWidth()
                    : other.component.getHeight();
            for (int offset = 0; offset < count; offset++) {
                if (other.component.getPin(side, offset) != type) continue;
                PinInfo p = pinExternal(other.component, other.direction, other.x, other.y, side, offset);
                if (p.ex >= me.x && p.ex < me.x + wRot && p.ey >= me.y && p.ey < me.y + hRot) {
                    return p;
                }
            }
        }
        return null;
    }

    /** 元件输出引脚在节点值中的位段布局：1 位引脚占 4 位，8 位引脚占 8 位。 */
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
                bitStart += (w == 8 ? 8 : 4);
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
        Arrays.fill(nodeVals, 0);
        for (int k = 0; k < topoLen; k++) {
            int node = topo[k];
            NodeKind kind = nodeKind[node];
            switch (kind) {
                case NET -> {
                    int v = 0;
                    for (Dep d : netInputDeps[node]) v = Math.max(v, depValue(d));
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
                    nodeVals[node] = kind == NodeKind.RES ? evalResonator(component, node) : evalCombinational(component, node);
                }
            }
        }
    }

    private int depValue(Dep d) {
        return switch (d.kind()) {
            case WHOLE -> (nodeVals[d.node()] >> d.bit()) & 0xF;
            case BIT -> ((nodeVals[d.node()] >> d.bit()) & 1) * 15;
        };
    }

    /** 读取元件第 k 个输入引脚的值：1 位返回 0~15，8 位返回打包的 0~255。 */
    private int inPort(int node, int k) {
        InputPort[] ports = inputPorts[node];
        if (ports == null || k >= ports.length) return 0;
        InputPort p = ports[k];
        if (p == null) return 0;
        if (p.width() == 8) {
            int v = 0;
            for (int b = 0; b < 8; b++) {
                Dep d = p.lanes()[b];
                if (d != null && depValue(d) != 0) v |= (1 << b);
            }
            return v;
        }
        Dep d = p.lanes()[0];
        return d == null ? 0 : depValue(d);
    }

    private int evalCombinational(Component component, int node) {
        CircuitComponent cc = component.component;
        if (cc == BuiltinCircuitComponents.TRANSISTOR) {
            int west = inPort(node, 0);
            int south = inPort(node, 1);
            return Math.min((int) Math.floor(2.0 * west * (1.0 - south / 15.0)), 15);
        }
        if (cc == BuiltinCircuitComponents.DIODE) {
            return inPort(node, 0);
        }
        if (cc == BuiltinCircuitComponents.RESISTOR) {
            int decay = ((ResistorComponentInstance) component.instance).getDecay();
            return Math.max(inPort(node, 0) - decay, 0);
        }
        if (cc == BuiltinCircuitComponents.BATTERY) {
            return 15;
        }
        if (cc == BuiltinCircuitComponents.INPUT) {
            return ((InputComponentInstance) component.instance).getSignal();
        }
        if (cc == BuiltinCircuitComponents.OUTPUT) {
            return inPort(node, 0);
        }
        if (cc == BuiltinCircuitComponents.AND_GATE) {
            int a = inPort(node, 0);
            int b = inPort(node, 1);
            return a != 0 ? b : 0;
        }
        if (cc == BuiltinCircuitComponents.BUS_JOINER_8) {
            int v = 0;
            for (int b = 0; b < 8; b++) {
                if (inPort(node, 7 - b) != 0) v |= (1 << b);
            }
            return v;
        }
        if (cc == BuiltinCircuitComponents.BUS_SPLITTER_8) {
            int bus = inPort(node, 0);
            int v = 0;
            for (int b = 0; b < 8; b++) {
                if ((bus & (1 << b)) != 0) v |= (15 << (b * 4));
            }
            return v;
        }
        if (cc == BuiltinCircuitComponents.ADDER_8) {
            int a = inPort(node, 0);
            int b = inPort(node, 1);
            int cin = inPort(node, 2) != 0 ? 1 : 0;
            int sum = a + b + cin;
            return (sum & 0xFF) | ((((sum >> 8) & 1) * 15) << 8);
        }
        return 0;
    }

    private int evalResonator(Component component, int node) {
        ResonatorComponentInstance instance = (ResonatorComponentInstance) component.instance;
        int phase = resPhase.getOrDefault(key(component.x, component.y), instance.getInitialPhase());
        int period = Math.max(1, instance.getHighDuration() + instance.getLowDuration());
        boolean high = phase % period < instance.getHighDuration();
        return high ? inPort(node, 0) : 0;
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
            dSample.put(node, src >= 0 ? nodeVals[src] : 0);
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

        evaluate();
    }

    private void collectResults() {
        wireValues.clear();
        wireBad.clear();
        compValues.clear();
        for (Map.Entry<WirePoint, Integer> e : wireNet.entrySet()) {
            int net = e.getValue();
            wireValues.put(e.getKey(), nodeVals[net]);
            wireBad.put(e.getKey(), badNet[net]);
        }
        for (int i = 0; i < comps.size(); i++) {
            Component component = comps.get(i);
            compValues.put(key(component.x, component.y), nodeVals[numNets + i]);
        }
    }

    /* ============================================================
     *  工具方法
     * ============================================================ */

    private record PinInfo(Direction facing, int ex, int ey, Direction side, int offset) {
    }

    private static PinInfo pinExternal(CircuitComponent comp, Direction rot, int cx, int cy, Direction side, int offset) {
        int w0 = comp.getWidth();
        int h0 = comp.getHeight();
        double nx, ny;
        switch (side) {
            case NORTH -> {
                nx = offset + 0.5;
                ny = 0;
            }
            case EAST -> {
                nx = w0;
                ny = offset + 0.5;
            }
            case SOUTH -> {
                nx = w0 - 0.5 - offset;
                ny = h0;
            }
            default -> { // WEST
                nx = 0;
                ny = h0 - 0.5 - offset;
            }
        }
        double rx, ry;
        switch (rot) {
            case EAST -> {
                rx = h0 - ny;
                ry = nx;
            }
            case SOUTH -> {
                rx = w0 - nx;
                ry = h0 - ny;
            }
            case WEST -> {
                rx = ny;
                ry = w0 - nx;
            }
            default -> { // NORTH
                rx = nx;
                ry = ny;
            }
        }
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