package com.elementsplus.core.circuit.diagram;

import com.elementsplus.core.circuit.BuiltinCircuitComponents;
import com.elementsplus.core.circuit.CircuitComponent;
import com.elementsplus.core.circuit.component.CircuitComponentInstance;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CircuitDiagram {
    /**
     * 电路图可用范围规模。以"区块"（16x16 单元格）为单位：
     * 小=2x2、中=4x4、大=8x8、超大=16x16、无限=无边界。
     */
    public enum Scale implements StringRepresentable {
        SMALL("small", 2),
        MEDIUM("medium", 4),
        LARGE("large", 8),
        HUGE("huge", 16),
        INFINITE("infinite", 0);

        public static final Codec<Scale> CODEC = StringRepresentable.fromEnum(Scale::values);

        public final String serializedName;
        public final int chunks;

        Scale(String serializedName, int chunks) {
            this.serializedName = serializedName;
            this.chunks = chunks;
        }

        /** 无限规模没有边界限制。 */
        public boolean isInfinite() {
            return chunks == 0;
        }

        /** 边长的单元格数；无限规模为 -1。 */
        public int sizeInCells() {
            return isInfinite() ? -1 : chunks * 16;
        }

        /** 能否在工作台用纸升级（无限与超大不可再升级）。 */
        public boolean canUpgrade() {
            return !isInfinite() && this != HUGE;
        }

        public Scale upgrade() {
            return switch (this) {
                case SMALL -> MEDIUM;
                case MEDIUM -> LARGE;
                case LARGE -> HUGE;
                default -> this;
            };
        }

        @Override
        public String getSerializedName() {
            return serializedName;
        }
    }

    /** 可用范围规模，默认小（2x2 区块）。 */
    public Scale scale = Scale.SMALL;

    public static class Chunk {
        public Wire[][] wires = new Wire[16][16];
        public List<Component> components = new ArrayList<>();
    }

    public static class Block {
        public int x;
        public int y;
    }

    public static class Wire extends Block {
        public enum WireMaterial {
            COPPER,
            GOLD;

            public static final Codec<WireMaterial> CODEC = Codec.STRING.xmap(WireMaterial::valueOf, WireMaterial::name);
        }

        public WireMaterial north, east, south, west;
        public int bitWidth = 1;

        public Wire(WireMaterial north, WireMaterial east, WireMaterial south, WireMaterial west) {
            this(north, east, south, west, 1);
        }

        public Wire(WireMaterial north, WireMaterial east, WireMaterial south, WireMaterial west, int bitWidth) {
            this.north = north;
            this.east = east;
            this.south = south;
            this.west = west;
            this.bitWidth = bitWidth;
        }
    }

    public static class Component extends Block {
        public CircuitComponent component;
        public CircuitComponentInstance instance;
        public Direction direction;

        public Component(CircuitComponent component, Direction direction, CircuitComponentInstance instance) {
            this.component = component;
            this.direction = direction;
            this.instance = instance != null ? instance : component.createInstance();
        }

        public Component(CircuitComponent component, Direction direction) {
            this(component, direction, component.createInstance());
        }

        public Component copy() {
            return new Component(component, direction, instance.copy());
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
        if (block instanceof Component c) {
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
        } else if (block instanceof Component comp) {
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
        EXAMPLE.setBlock(5, 6, new Wire(Wire.WireMaterial.GOLD, Wire.WireMaterial.GOLD, null, Wire.WireMaterial.GOLD));
        EXAMPLE.setBlock(6, 5, new Component(BuiltinCircuitComponents.AND_GATE, Direction.NORTH));
    }

    public CircuitDiagram copy() {
        CircuitDiagram copy = new CircuitDiagram();
        copy.scale = this.scale;
        for (Map.Entry<Long, Chunk> entry : chunks.entrySet()) {
            Chunk source = entry.getValue();
            Chunk target = new Chunk();
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    Wire wire = source.wires[x][y];
                    if (wire != null) {
                        Wire newWire = new Wire(wire.north, wire.east, wire.south, wire.west, wire.bitWidth);
                        newWire.x = wire.x;
                        newWire.y = wire.y;
                        target.wires[x][y] = newWire;
                    }
                }
            }
            for (Component component : source.components) {
                Component newComponent = component.copy();
                newComponent.x = component.x;
                newComponent.y = component.y;
                target.components.add(newComponent);
            }
            copy.chunks.put(entry.getKey(), target);
        }
        return copy;
    }

    public static final Codec<Direction> DIRECTION_CODEC = StringRepresentable.fromEnum(Direction::values);

    public static final Codec<Wire> WIRE_CODEC = new Codec<Wire>() {
        @Override
        public <T> DataResult<T> encode(Wire input, DynamicOps<T> ops, T prefix) {
            RecordBuilder<T> builder = ops.mapBuilder()
                    .add("x", ops.createInt(input.x))
                    .add("y", ops.createInt(input.y));
            addMaterial(builder, ops, "north", input.north);
            addMaterial(builder, ops, "east", input.east);
            addMaterial(builder, ops, "south", input.south);
            addMaterial(builder, ops, "west", input.west);
            if (input.bitWidth > 1) {
                builder.add("bitWidth", ops.createInt(input.bitWidth));
            }
            return builder.build(prefix);
        }

        @Override
        public <T> DataResult<Pair<Wire, T>> decode(DynamicOps<T> ops, T input) {
            return ops.getMap(input).flatMap(map -> {
                Optional<Integer> x = getField(Codec.INT, ops, map, "x");
                Optional<Integer> y = getField(Codec.INT, ops, map, "y");
                Optional<Integer> bitWidth = getField(Codec.INT, ops, map, "bitWidth");
                Wire wire = new Wire(
                        getMaterial(ops, map, "north"),
                        getMaterial(ops, map, "east"),
                        getMaterial(ops, map, "south"),
                        getMaterial(ops, map, "west"),
                        bitWidth.orElse(1));
                wire.x = x.orElse(0);
                wire.y = y.orElse(0);
                return DataResult.success(new Pair<>(wire, input));
            });
        }
    };

    private static <T, R> Optional<R> getField(Codec<R> codec, DynamicOps<T> ops, MapLike<T> map, String key) {
        T value = map.get(key);
        return value == null ? Optional.empty() : codec.parse(ops, value).result();
    }

    private static <T> void addMaterial(RecordBuilder<T> builder, DynamicOps<T> ops, String key, Wire.WireMaterial material) {
        if (material != null) {
            builder.add(key, Wire.WireMaterial.CODEC.encodeStart(ops, material));
        }
    }

    private static <T> Wire.WireMaterial getMaterial(DynamicOps<T> ops, MapLike<T> map, String key) {
        T value = map.get(key);
        return value == null ? null : Wire.WireMaterial.CODEC.parse(ops, value).result().orElse(null);
    }

    public static final Codec<Component> COMPONENT_CODEC = new Codec<Component>() {
        @Override
        public <T> DataResult<T> encode(Component input, DynamicOps<T> ops, T prefix) {
            ResourceLocation id = input.component.getId();
            if (id == null) {
                return DataResult.error(() -> "Circuit component has no registered id: " + input.component.getName().getString());
            }
            RecordBuilder<T> builder = ops.mapBuilder()
                    .add("x", ops.createInt(input.x))
                    .add("y", ops.createInt(input.y))
                    .add("id", ResourceLocation.CODEC.encodeStart(ops, id))
                    .add("direction", DIRECTION_CODEC.encodeStart(ops, input.direction));
            for (CircuitComponentInstance.Config config : input.instance.getConfigs()) {
                if (config instanceof CircuitComponentInstance.IntConfig) {
                    builder.add(config.key, ops.createInt(input.instance.getInt(config.key)));
                } else if (config instanceof CircuitComponentInstance.FloatConfig) {
                    builder.add(config.key, ops.createFloat(input.instance.getFloat(config.key)));
                }
            }
            return builder.build(prefix);
        }

        @Override
        public <T> DataResult<Pair<Component, T>> decode(DynamicOps<T> ops, T input) {
            return ops.getMap(input).flatMap(map -> {
                Optional<ResourceLocation> id = getField(ResourceLocation.CODEC, ops, map, "id");
                if (id.isEmpty()) {
                    return DataResult.error(() -> "Component: missing or invalid 'id'");
                }
                CircuitComponent circuitComponent = BuiltinCircuitComponents.byId(id.get());
                if (circuitComponent == null) {
                    return DataResult.error(() -> "Unknown circuit component id: " + id.get());
                }
                Optional<Integer> x = getField(Codec.INT, ops, map, "x");
                Optional<Integer> y = getField(Codec.INT, ops, map, "y");
                Optional<Direction> direction = getField(DIRECTION_CODEC, ops, map, "direction");
                CircuitComponentInstance instance = circuitComponent.createInstance();
                for (CircuitComponentInstance.Config config : instance.getConfigs()) {
                    if (config instanceof CircuitComponentInstance.IntConfig) {
                        getField(Codec.INT, ops, map, config.key).ifPresent(v -> instance.setInt(config.key, v));
                    } else if (config instanceof CircuitComponentInstance.FloatConfig) {
                        getField(Codec.FLOAT, ops, map, config.key).ifPresent(v -> instance.setFloat(config.key, v));
                    }
                }
                Component component = new Component(circuitComponent, direction.orElse(Direction.NORTH), instance);
                component.x = x.orElse(0);
                component.y = y.orElse(0);
                return DataResult.success(new Pair<>(component, input));
            });
        }
    };

    public static final Codec<Chunk> CHUNK_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            WIRE_CODEC.listOf().optionalFieldOf("wires", List.of()).forGetter(chunk -> {
                List<Wire> wires = new ArrayList<>();
                for (Wire[] row : chunk.wires) {
                    for (Wire wire : row) {
                        if (wire != null) {
                            wires.add(wire);
                        }
                    }
                }
                return wires;
            }),
            COMPONENT_CODEC.listOf().optionalFieldOf("components", List.of()).forGetter(chunk -> chunk.components)
    ).apply(instance, (wires, components) -> {
        Chunk chunk = new Chunk();
        for (Wire wire : wires) {
            chunk.wires[Math.floorMod(wire.x, 16)][Math.floorMod(wire.y, 16)] = wire;
        }
        chunk.components.addAll(components);
        return chunk;
    }));

    public record ChunkEntry(int cx, int cy, Chunk chunk) {
    }

    public static final Codec<ChunkEntry> CHUNK_ENTRY_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("cx").forGetter(ChunkEntry::cx),
            Codec.INT.fieldOf("cy").forGetter(ChunkEntry::cy),
            CHUNK_CODEC.fieldOf("chunk").forGetter(ChunkEntry::chunk)
    ).apply(instance, ChunkEntry::new));

    public static final Codec<CircuitDiagram> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Scale.CODEC.optionalFieldOf("scale", Scale.SMALL).forGetter(diagram -> diagram.scale),
            CHUNK_ENTRY_CODEC.listOf().optionalFieldOf("chunks", List.of()).forGetter(diagram -> {
                List<ChunkEntry> entries = new ArrayList<>();
                diagram.chunks.forEach((key, chunk) ->
                        entries.add(new ChunkEntry((int) (key >> 32), (int) (key & 0xFFFFFFFFL), chunk)));
                return entries;
            })
    ).apply(instance, (scale, chunks) -> {
        CircuitDiagram diagram = new CircuitDiagram();
        diagram.scale = scale;
        for (ChunkEntry entry : chunks) {
            diagram.chunks.put((((long) entry.cx()) << 32) | (entry.cy() & 0xFFFFFFFFL), entry.chunk());
        }
        return diagram;
    }));

    public static final StreamCodec<RegistryFriendlyByteBuf, CircuitDiagram> STREAM_CODEC = StreamCodec.of(
            CircuitDiagram::writeToBuf,
            CircuitDiagram::readFromBuf
    );

    public static final StreamCodec<FriendlyByteBuf, CircuitDiagram> PAYLOAD_STREAM_CODEC = StreamCodec.of(
            CircuitDiagram::writeToBuf,
            CircuitDiagram::readFromBuf
    );

    private static void writeMaterial(FriendlyByteBuf buf, Wire.WireMaterial material) {
        buf.writeVarInt(material == null ? -1 : material.ordinal());
    }

    private static Wire.WireMaterial readMaterial(FriendlyByteBuf buf) {
        int ordinal = buf.readVarInt();
        return ordinal == -1 ? null : Wire.WireMaterial.values()[ordinal];
    }

    private static void writeToBuf(FriendlyByteBuf buf, CircuitDiagram diagram) {
        buf.writeVarInt(diagram.scale.ordinal());
        buf.writeVarInt(diagram.chunks.size());
        for (Map.Entry<Long, Chunk> entry : diagram.chunks.entrySet()) {
            buf.writeVarInt((int) (entry.getKey() >> 32));
            buf.writeVarInt((int) (entry.getKey() & 0xFFFFFFFFL));
            Chunk chunk = entry.getValue();
            List<Wire> wires = new ArrayList<>();
            for (Wire[] row : chunk.wires) {
                for (Wire wire : row) {
                    if (wire != null) {
                        wires.add(wire);
                    }
                }
            }
            buf.writeVarInt(wires.size());
            for (Wire wire : wires) {
                buf.writeVarInt(wire.x);
                buf.writeVarInt(wire.y);
                writeMaterial(buf, wire.north);
                writeMaterial(buf, wire.east);
                writeMaterial(buf, wire.south);
                writeMaterial(buf, wire.west);
                buf.writeVarInt(wire.bitWidth);
            }
            buf.writeVarInt(chunk.components.size());
            for (Component component : chunk.components) {
                buf.writeVarInt(component.x);
                buf.writeVarInt(component.y);
                buf.writeVarInt(component.direction.ordinal());
                ResourceLocation id = component.component.getId();
                if (id == null) {
                    throw new IllegalArgumentException("Circuit component has no registered id: " + component.component.getName().getString());
                }
                ResourceLocation.STREAM_CODEC.encode(buf, id);
                for (CircuitComponentInstance.Config config : component.instance.getConfigs()) {
                    if (config instanceof CircuitComponentInstance.IntConfig) {
                        buf.writeInt(component.instance.getInt(config.key));
                    } else if (config instanceof CircuitComponentInstance.FloatConfig) {
                        buf.writeFloat(component.instance.getFloat(config.key));
                    }
                }
            }
        }
    }

    private static CircuitDiagram readFromBuf(FriendlyByteBuf buf) {
        CircuitDiagram diagram = new CircuitDiagram();
        Scale[] scales = Scale.values();
        diagram.scale = scales[Math.max(0, Math.min(buf.readVarInt(), scales.length - 1))];
        int chunkCount = buf.readVarInt();
        for (int i = 0; i < chunkCount; i++) {
            long cx = buf.readVarInt();
            long cy = buf.readVarInt();
            long key = (cx << 32) | (cy & 0xFFFFFFFFL);
            Chunk chunk = new Chunk();
            int wireCount = buf.readVarInt();
            for (int j = 0; j < wireCount; j++) {
                int x = buf.readVarInt();
                int y = buf.readVarInt();
                Wire wire = new Wire(
                        readMaterial(buf), readMaterial(buf), readMaterial(buf), readMaterial(buf));
                wire.bitWidth = buf.readVarInt();
                wire.x = x;
                wire.y = y;
                chunk.wires[Math.floorMod(wire.x, 16)][Math.floorMod(wire.y, 16)] = wire;
            }
            int componentCount = buf.readVarInt();
            for (int j = 0; j < componentCount; j++) {
                int x = buf.readVarInt();
                int y = buf.readVarInt();
                Direction direction = Direction.values()[buf.readVarInt()];
                CircuitComponent circuitComponent = BuiltinCircuitComponents.byId(ResourceLocation.STREAM_CODEC.decode(buf));
                if (circuitComponent == null) {
                    throw new IllegalArgumentException("Unknown circuit component id");
                }
                CircuitComponentInstance instance = circuitComponent.createInstance();
                for (CircuitComponentInstance.Config config : instance.getConfigs()) {
                    if (config instanceof CircuitComponentInstance.IntConfig) {
                        instance.setInt(config.key, buf.readInt());
                    } else if (config instanceof CircuitComponentInstance.FloatConfig) {
                        instance.setFloat(config.key, buf.readFloat());
                    }
                }
                Component component = new Component(circuitComponent, direction, instance);
                component.x = x;
                component.y = y;
                chunk.components.add(component);
            }
            diagram.chunks.put(key, chunk);
        }
        return diagram;
    }
}