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
 * 1. 并查集把同材料、同位宽的导线四连通合并为"网络"节点：
 *    1 位网络携带单个 0~15 值；8 位总线网络携带 8 元组（每位仍为 0~15，
 *    信号强度得到保留），按位独立承载 8 路信号；
 * 2. 建依赖图：网络/组合元件/时序元件（电容 D 输入不建边，仅采样，切断时序环）；
 * 3. Kahn 拓扑排序；
 * 4. Tarjan SCC 把环内的网络标记为坏网；
 * 5. 按拓扑序传播求值：1 位网络取所有驱动源的最大值；
 *    总线网络有多个驱动源时按位取最大值（如 (12,11,10,...) | (10,11,12,...) = (12,11,12,...)）；
 * 6. 时钟步进：求值 -> 采样电容 D -> 统一更新状态 -> 重新求值；
 * 7. 复位：清空时序状态（电容 = 0，谐振器 = 初相）。
 *
 * 位宽语义：
 * - 元件结点值按输出引脚布局拆成若干位段：1 位引脚占 4 位（0~15 模拟值），
 *   8 位引脚占 32 位（8 个 4 位通道，每个通道保存该位 0~15 的信号强度）；
 * - 不同位宽的线/引脚之间不传信号，并在连接处记录"位宽不匹配"告警；
 * - 加法器先从输入端读取 8 元组，按“位=非零”转成整数做二进制加法，
 *   再把结果按位转成 8 元组输出（高=15，低=0）。
 *
 * 状态不持久化、不发送到服务端，仅用于屏幕内的模拟与渲染。
 */
public class CircuitSimulator {

    public record WirePoint(int x, int y, WireMaterial material) {
    }

    private enum NodeKind {
        NET, COMB, CAP, RES
    }

    private enum DepKind {
        /** 读取源结点值中的一个 4 位通道（0~15）。通道起点由 Dep.bit 指定。 */
        WHOLE,
        /** 读取源结点值中的一个 32 位（8×4 位）通道，即整个 8 元组。起点由 Dep.bit 指定。 */
        TUPLE
    }

    /** 一条入边依赖：源结点、取值方式、通道偏移。 */
    private record Dep(int node, DepKind kind, int bit) {
    }

    /** 一个输入引脚的若干通道依赖；引脚位宽 width 为 1 或 8。 */
    private record InputPort(int width, Dep[] lanes) {
    }

    private static final double EPS = 1e-6;

    private CircuitDiagram diagram;

    private int totalNodes;
    private int numNets;
    private NodeKind[] nodeKind = new NodeKind[0];
    private long[] nodeVals = new long[0];
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

    /** 8 位总线打包值：bit b = 第 b 位（4 位通道）是否非零。 */
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

    /** 该格上 material 对应的导线网络是否为 8 位总线。 */
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
            nodeVals = new long[0];
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
                    indexBw.put(id, wire.bitWidth);
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
                            && neighbor.bitWidth == wire.bitWidth
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
                                    Integer net = wireNet.get(new WirePoint(pin.ex, pin.ey, m));
                                    for (int b = 0; b < pinWidth; b++) {
                                        if (net != null) {
                                            lanes[b] = new Dep(net, DepKind.WHOLE, 4 * b);
                                            if (kind != NodeKind.CAP) predSets[node].add(net);
                                            else dSample = net;
                                        }
                                    }
                                } else {
                                    mismatchCells.add(seamKey(pin.ex, pin.ey, pin.facing));
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
                                            lanes[b] = new Dep(otherNode, DepKind.WHOLE,
                                                    slot.bitStart() + 4 * b);
                                            if (kind != NodeKind.CAP) predSets[node].add(otherNode);
                                            else dSample = otherNode;
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
                                if (wire.bitWidth == pinWidth) {
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
                                PinInfo ip = otherPinAt(other, component, PinType.INPUT);
                                if (ip != null) {
                                    int otherWidth = other.component.getPinBitWidth(ip.side, ip.offset);
                                    if (otherWidth != pinWidth) {
                                        mismatchCells.add(seamKey(pin.ex, pin.ey, pin.facing));
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

        nodeVals = new long[totalNodes];
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

    /** 元件输出引脚在节点值中的位段布局：1 位引脚占 4 位，8 位引脚占 32 位。 */
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
                    nodeVals[node] = kind == NodeKind.RES ? evalResonator(component, node) : evalCombinational(component, node);
                }
            }
        }
    }

    private long depValue(Dep d) {
        return switch (d.kind()) {
            case WHOLE -> (nodeVals[d.node()] >> d.bit()) & 0xF;
            case TUPLE -> (nodeVals[d.node()] >> d.bit()) & 0xFFFFFFFFL;
        };
    }

    /** 两个 8 元组（8×4 位通道）按位取最大值。 */
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

    /** 8 元组按“位非零即真”转成 8 位整数。 */
    private static int tupleToInt(long tuple) {
        int v = 0;
        for (int b = 0; b < 8; b++) {
            if (((tuple >> (4 * b)) & 0xF) != 0) v |= (1 << b);
        }
        return v;
    }

    /** 8 位整数转 8 元组：置位通道输出 15，其余 0。 */
    private static long intToTuple(int bits) {
        long t = 0L;
        for (int b = 0; b < 8; b++) {
            if (((bits >> b) & 1) != 0) t |= 15L << (4 * b);
        }
        return t;
    }

    /** 读取元件第 k 个输入引脚：1 位返回 0~15，8 位返回 8 元组。 */
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

    private long evalCombinational(Component component, int node) {
        CircuitComponent cc = component.component;
        if (cc == BuiltinCircuitComponents.TRANSISTOR) {
            int west = (int) inPort(node, 0);
            int south = (int) inPort(node, 1);
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
        if (cc == BuiltinCircuitComponents.AMPLIFIER) {
            return inPort(node, 0) > 0 ? 15 : 0;
        }
        if (cc == BuiltinCircuitComponents.AND_GATE) {
            int a = (int) inPort(node, 0);
            int b = (int) inPort(node, 1);
            return a != 0 ? b : 0;
        }
        if (cc == BuiltinCircuitComponents.BUS_JOINER_8) {
            long tuple = 0L;
            for (int b = 0; b < 8; b++) {
                tuple |= (inPort(node, 7 - b) & 0xF) << (4 * b);
            }
            return tuple << outputSlot(cc, Direction.EAST, 0).bitStart();
        }
        if (cc == BuiltinCircuitComponents.BUS_SPLITTER_8) {
            long tuple = inPort(node, 0);
            long v = 0L;
            for (int b = 0; b < 8; b++) {
                v |= ((tuple >> (4 * b)) & 0xF) << outputSlot(cc, Direction.EAST, b).bitStart();
            }
            return v;
        }
        if (cc == BuiltinCircuitComponents.ADDER_8) {
            int a = tupleToInt(inPort(node, 0));
            int b = tupleToInt(inPort(node, 1));
            int cin = inPort(node, 2) != 0 ? 1 : 0;
            int sum = a + b + cin;
            long v = 0L;
            v |= intToTuple(sum & 0xFF) << outputSlot(cc, Direction.EAST, 0).bitStart();
            v |= ((long) ((((sum >> 8) & 1) * 15) & 0xF)) << outputSlot(cc, Direction.SOUTH, 0).bitStart();
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
            case NORTH -> { x0 = ex; y0 = ey + 1; vertical = false; }
            case SOUTH -> { x0 = ex; y0 = ey; vertical = false; }
            case WEST -> { x0 = ex + 1; y0 = ey; vertical = true; }
            default -> { x0 = ex; y0 = ey; vertical = true; }
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