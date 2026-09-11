package com.elementsplus.core.circuit.diagram;

import com.elementsplus.core.circuit.BuiltinCircuitComponents;
import com.elementsplus.core.circuit.CircuitComponent;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CircuitDiagram {
    public static class Chunk {
        Wire[][] wires = new Wire[16][16];
        List<Component> components = new ArrayList<>();
    }

    public static class Block {
        public int x;
        public int y;
    }

    public static class Wire extends Block {
        public enum WireMaterial {
            COPPER,
            GOLD
        }

        public WireMaterial north, east, south, west;

        public Wire(WireMaterial north, WireMaterial east, WireMaterial south, WireMaterial west) {
            this.north = north;
            this.east = east;
            this.south = south;
            this.west = west;
        }
    }

    public static class Component extends Block {
        public CircuitComponent component;
        public Direction direction;

        public Component(CircuitComponent component, Direction direction) {
            this.component = component;
            this.direction = direction;
        }

        public int getHeight() {
            return direction == Direction.NORTH || direction == Direction.SOUTH ? component.getHeight() : component.getWidth();
        }

        public int getWidth() {
            return direction == Direction.NORTH || direction == Direction.SOUTH ? component.getWidth() : component.getHeight();
        }
    }

    public Map<Long, Chunk> chunks = new HashMap<>();

    public Block getBlock(int x, int y) {
        int chunkX = Math.floorDiv(x, 16);
        int chunkY = Math.floorDiv(y, 16);
        int localX = Math.floorMod(x, 16);
        int localY = Math.floorMod(y, 16);

        // 1. 检查当前 chunk 的导线
        Chunk currentChunk = chunks.get(key(chunkX, chunkY));
        if (currentChunk != null) {
            Wire wire = currentChunk.wires[localX][localY];
            if (wire != null) {
                return wire;
            }
        }

        // 2. 检查可能覆盖该点的组件。组件原点可能在当前 chunk 或左/上/左上 chunk。
        // 因为组件大小不超过 16x16，所以原点坐标 ox 在 [x-15, x]，oy 在 [y-15, y]。
        for (int cx = chunkX - 1; cx <= chunkX; cx++) {
            for (int cy = chunkY - 1; cy <= chunkY; cy++) {
                Chunk chunk = chunks.get(key(cx, cy));
                if (chunk == null) continue;
                for (Component comp : chunk.components) {
                    int w = comp.getWidth();
                    int h = comp.getHeight();
                    if (x >= comp.x && x < comp.x + w && y >= comp.y && y < comp.y + h) {
                        return comp;
                    }
                }
            }
        }
        return null;
    }

    private static long key(int cx, int cy) {
        return ((long) cx << 32) | (cy & 0xFFFFFFFFL);
    }

    public boolean setBlock(int x, int y, Block block, boolean force) {
        if (block == null) {
            Block existing = getBlock(x, y);
            if (existing == null) {
                return false;
            }
            removeBlock(existing);
            return true;
        }

        block.x = x;
        block.y = y;

        // 计算占用区域
        int w = 1, h = 1;
        if (block instanceof Component) {
            Component c = (Component) block;
            w = c.getWidth();
            h = c.getHeight();
        }

        // 检查冲突
        List<Block> toRemove = new ArrayList<>();
        for (int dx = 0; dx < w; dx++) {
            for (int dy = 0; dy < h; dy++) {
                Block existing = getBlock(x + dx, y + dy);
                if (existing != null && !toRemove.contains(existing)) {
                    toRemove.add(existing);
                }
            }
        }

        if (!toRemove.isEmpty()) {
            if (!force) {
                return false;
            }
            for (Block b : toRemove) {
                removeBlock(b);
            }
        }

        // 放置新块
        if (block instanceof Wire) {
            int chunkX = Math.floorDiv(x, 16);
            int chunkY = Math.floorDiv(y, 16);
            int localX = Math.floorMod(x, 16);
            int localY = Math.floorMod(y, 16);
            Chunk chunk = chunks.computeIfAbsent(key(chunkX, chunkY), k -> new Chunk());
            chunk.wires[localX][localY] = (Wire) block;
        } else if (block instanceof Component) {
            int chunkX = Math.floorDiv(x, 16);
            int chunkY = Math.floorDiv(y, 16);
            Chunk chunk = chunks.computeIfAbsent(key(chunkX, chunkY), k -> new Chunk());
            chunk.components.add((Component) block);
        } else {
            throw new IllegalArgumentException("Unknown block type: " + block.getClass());
        }
        return true;
    }

    public boolean setBlock(int x, int y, Block block) {
        return setBlock(x, y, block, false);
    }

    private void removeBlock(Block block) {
        if (block instanceof Wire) {
            int chunkX = Math.floorDiv(block.x, 16);
            int chunkY = Math.floorDiv(block.y, 16);
            int localX = Math.floorMod(block.x, 16);
            int localY = Math.floorMod(block.y, 16);
            Chunk chunk = chunks.get(key(chunkX, chunkY));
            if (chunk != null) {
                if (chunk.wires[localX][localY] == block) {
                    chunk.wires[localX][localY] = null;
                }
            }
        } else if (block instanceof Component) {
            Component comp = (Component) block;
            int chunkX = Math.floorDiv(comp.x, 16);
            int chunkY = Math.floorDiv(comp.y, 16);
            Chunk chunk = chunks.get(key(chunkX, chunkY));
            if (chunk != null) {
                chunk.components.remove(comp);
            }
        }
    }

    public static final CircuitDiagram EXAMPLE = new CircuitDiagram();

    static {
        EXAMPLE.setBlock(5, 5, new Wire(null, Wire.WireMaterial.COPPER, Wire.WireMaterial.COPPER, Wire.WireMaterial.COPPER));
        EXAMPLE.setBlock(6, 5, new Component(BuiltinCircuitComponents.AND_GATE, Direction.NORTH));
    }
}