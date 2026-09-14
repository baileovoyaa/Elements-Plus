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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

/**
 * 纯客户端电路模拟器。
 *
 * 思路（参考 docs/参考算法描述.md）：
 * 1. 并查集把同材料的四连通导线合并为"网络"节点；
 * 2. 建依赖图：网络/组合元件/时序元件（电容 D 输入不建边，仅采样，切断时序环）；
 * 3. Kahn 拓扑排序；
 * 4. Tarjan SCC 把环内的网络标记为坏网；
 * 5. 按拓扑序传播求值（网络取所有驱动源的最大值，0~15 信号）；
 * 6. 时钟步进：求值 -> 采样电容 D -> 统一更新状态 -> 重新求值；
 * 7. 复位：清空时序状态（电容 = 0，谐振器 = 初相）。
 *
 * 状态不持久化、不发送到服务端，仅用于屏幕内的模拟与渲染。
 */
public class CircuitSimulator {

    public record WirePoint(int x, int y, WireMaterial material) {
    }

    private enum NodeKind {
        NET, COMB, CAP, RES
    }

    private static final double EPS = 1e-6;

    private CircuitDiagram diagram;

    private int totalNodes;
    private int numNets;
    private NodeKind[] nodeKind = new NodeKind[0];
    private int[] nodeVals = new int[0];
    private int[][] preds = new int[0][];
    private int[] topo = new int[0];
    private int topoLen;
    private boolean hasCycle;
    private boolean[] badNet = new boolean[0];

    private List<Component> comps = new ArrayList<>();
    private int[][] inputNodes = new int[0][];
    private int[] dSampleSource = new int[0];
    private List<Integer> capNodes = new ArrayList<>();
    private List<Integer> resNodes = new ArrayList<>();

    private Map<WirePoint, Integer> wireNet = new HashMap<>();

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

    public int getWireValue(int x, int y, WireMaterial material) {
        Integer v = wireValues.get(new WirePoint(x, y, material));
        return v == null ? 0 : v;
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
        hasCycle = false;

        if (diagram == null) {
            totalNodes = 0;
            numNets = 0;
            nodeKind = new NodeKind[0];
            nodeVals = new int[0];
            preds = new int[0][];
            topo = new int[0];
            topoLen = 0;
            badNet = new boolean[0];
            inputNodes = new int[0][];
            dSampleSource = new int[0];
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

        /* ---------- 1. 并查集合并同材料导线网络 ---------- */
        Map<WirePoint, Integer> elemIndex = new HashMap<>();
        UnionFind uf = new UnionFind();
        for (Wire wire : wires) {
            for (WireMaterial m : new WireMaterial[]{WireMaterial.COPPER, WireMaterial.GOLD}) {
                if (hasSideMaterial(wire, m)) {
                    int id = uf.add();
                    elemIndex.put(new WirePoint(wire.x, wire.y, m), id);
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
                    if (neighbor != null && materialAtSide(neighbor, d.getOpposite()) == m) {
                        Integer other = elemIndex.get(new WirePoint(nx, ny, m));
                        if (other != null) {
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

        /* ---------- 2. 建边 ---------- */
        @SuppressWarnings("unchecked")
        Set<Integer>[] predSets = new Set[totalNodes];
        for (int i = 0; i < totalNodes; i++) predSets[i] = new HashSet<>();

        inputNodes = new int[totalNodes][];
        dSampleSource = new int[totalNodes];
        Arrays.fill(dSampleSource, -1);

        for (int i = 0; i < numComps; i++) {
            Component component = comps.get(i);
            int node = numNets + i;
            NodeKind kind = nodeKind[node];

            List<Integer> inputs = new ArrayList<>();
            int dSample = -1;
            for (Direction side : new Direction[]{Direction.WEST, Direction.SOUTH, Direction.EAST, Direction.NORTH}) {
                int count = (side == Direction.NORTH || side == Direction.SOUTH)
                        ? component.component.getWidth()
                        : component.component.getHeight();
                for (int offset = 0; offset < count; offset++) {
                    PinType type = component.component.getPin(side, offset);
                    if (type == null || type == PinType.NONE) continue;
                    PinInfo pin = pinExternal(component.component, component.direction, component.x, component.y, side, offset);
                    if (type == PinType.INPUT) {
                        int src = sourceNode(pin, compIndex);
                        if (src >= 0) {
                            inputs.add(src);
                            if (kind != NodeKind.CAP) predSets[node].add(src);
                            if (kind == NodeKind.CAP) dSample = src;
                        } else {
                            inputs.add(-1);
                        }
                    } else { // OUTPUT
                        Integer net = netAt(pin.ex, pin.ey, pin.facing);
                        if (net != null) {
                            predSets[net].add(node);
                        } else {
                            Integer other = compIndex.get(key(pin.ex, pin.ey));
                            if (other != null && nodeKind[other] != NodeKind.CAP) {
                                predSets[other].add(node);
                            }
                        }
                    }
                }
            }
            inputNodes[node] = inputs.stream().mapToInt(Integer::intValue).toArray();
            if (kind == NodeKind.CAP) {
                dSampleSource[node] = inputs.isEmpty() ? -1 : inputs.get(0);
            }
        }

        /* ---------- 3. 拓扑排序 ---------- */
        preds = new int[totalNodes][];
        @SuppressWarnings("unchecked")
        List<Integer>[] succs = new List[totalNodes];
        for (int i = 0; i < totalNodes; i++) succs[i] = new ArrayList<>();
        int[] indeg = new int[totalNodes];
        for (int i = 0; i < totalNodes; i++) {
            preds[i] = predSets[i].stream().mapToInt(Integer::intValue).toArray();
            indeg[i] = preds[i].length;
            for (int p : preds[i]) succs[p].add(i);
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

    private int sourceNode(PinInfo pin, Map<Long, Integer> compIndex) {
        Integer net = netAt(pin.ex, pin.ey, pin.facing);
        if (net != null) return net;
        Integer other = compIndex.get(key(pin.ex, pin.ey));
        return other != null ? other : -1;
    }

    /** 外部格子朝向元件引脚一侧的导线，其对应材料网络；无则 null */
    private Integer netAt(int x, int y, Direction facing) {
        Wire wire = findWire(x, y);
        if (wire == null) return null;
        WireMaterial m = materialAtSide(wire, facing.getOpposite());
        if (m == null) return null;
        return wireNet.get(new WirePoint(x, y, m));
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
                    for (int p : preds[node]) v = Math.max(v, nodeVals[p]);
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

    private int in(int node, int i) {
        int[] inputs = inputNodes[node];
        if (inputs == null || i >= inputs.length) return 0;
        int src = inputs[i];
        return src >= 0 ? nodeVals[src] : 0;
    }

    private int evalCombinational(Component component, int node) {
        CircuitComponent cc = component.component;
        if (cc == BuiltinCircuitComponents.TRANSISTOR) {
            int west = in(node, 0);
            int south = in(node, 1);
            return Math.min((int) Math.floor(2.0 * west * (1.0 - south / 15.0)), 15);
        }
        if (cc == BuiltinCircuitComponents.DIODE) {
            return in(node, 0);
        }
        if (cc == BuiltinCircuitComponents.RESISTOR) {
            int decay = ((ResistorComponentInstance) component.instance).getDecay();
            return Math.max(in(node, 0) - decay, 0);
        }
        if (cc == BuiltinCircuitComponents.BATTERY) {
            return 15;
        }
        if (cc == BuiltinCircuitComponents.INPUT) {
            return ((InputComponentInstance) component.instance).getSignal();
        }
        if (cc == BuiltinCircuitComponents.OUTPUT) {
            return in(node, 0);
        }
        if (cc == BuiltinCircuitComponents.AND_GATE) {
            int a = in(node, 0);
            int b = in(node, 1);
            return a != 0 ? b : 0;
        }
        return 0;
    }

    private int evalResonator(Component component, int node) {
        ResonatorComponentInstance instance = (ResonatorComponentInstance) component.instance;
        int phase = resPhase.getOrDefault(key(component.x, component.y), instance.getInitialPhase());
        int period = Math.max(1, instance.getHighDuration() + instance.getLowDuration());
        boolean high = phase % period < instance.getHighDuration();
        return high ? in(node, 0) : 0;
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

    private record PinInfo(Direction facing, int ex, int ey) {
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
        return new PinInfo(facing, ex, ey);
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