package com.elementsplus.core.circuit.storage;

import com.elementsplus.ElementsPlus;
import com.elementsplus.core.circuit.BuiltinCircuitComponents;
import com.elementsplus.core.circuit.CircuitComponent;
import com.elementsplus.core.circuit.CustomCircuitComponent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 已编译电路的分离存储。服务端启动时从 data/elements-plus/circuits 读取，
 * 客户端通过同步包获得同一份索引。物品只保存 UUID。
 */
public final class CircuitStorage {
    private static final CircuitStorage INSTANCE = new CircuitStorage();

    private final Map<UUID, StoredCircuit> circuits = new LinkedHashMap<>();
    private final Map<ResourceLocation, CustomCircuitComponent> customComponents = new HashMap<>();
    @Nullable private Path directory;

    private CircuitStorage() {
    }

    public static CircuitStorage get() {
        return INSTANCE;
    }

    public synchronized void load(MinecraftServer server) {
        this.directory = server.getWorldPath(LevelResource.ROOT)
                .resolve("data").resolve("elements-plus").resolve("circuits");
        circuits.clear();
        customComponents.clear();
        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            ElementsPlus.LOGGER.error("Failed to create circuit storage directory", e);
            return;
        }

        List<CompoundTag> pending = new ArrayList<>();
        try (var stream = Files.list(directory)) {
            stream.filter(path -> path.getFileName().toString().endsWith(".dat"))
                    .forEach(path -> {
                        try {
                            pending.add(NbtIo.readCompressed(path, NbtAccounter.unlimitedHeap()));
                        } catch (IOException e) {
                            ElementsPlus.LOGGER.error("Failed to read circuit {}", path, e);
                        }
                    });
        } catch (IOException e) {
            ElementsPlus.LOGGER.error("Failed to list circuit storage", e);
        }

        // 自定义元件可能嵌套引用其他自定义元件；反复尝试直到不再有进展。
        boolean progress = true;
        while (progress && !pending.isEmpty()) {
            progress = false;
            for (int i = pending.size() - 1; i >= 0; i--) {
                StoredCircuit record = decode(pending.get(i));
                if (record == null) {
                    continue;
                }
                circuits.put(record.id(), record);
                pending.remove(i);
                progress = true;
            }
        }
        if (!pending.isEmpty()) {
            ElementsPlus.LOGGER.warn("{} compiled circuit(s) could not be loaded (missing nested custom component?)", pending.size());
        }
        ElementsPlus.LOGGER.info("Loaded {} compiled circuits", circuits.size());
    }

    @Nullable
    private static StoredCircuit decode(CompoundTag tag) {
        try {
            return StoredCircuit.CODEC.parse(NbtOps.INSTANCE, tag).result().orElse(null);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * 客户端收到完整索引后合并本地缓存。集成服务器与客户端共用 JVM，
     * 因此这里不能清空服务端已经加载的索引；按 UUID 合并即可。
     */
    public synchronized void replaceAll(Collection<StoredCircuit> records) {
        for (StoredCircuit record : records) {
            circuits.put(record.id(), record);
            customComponents.remove(record.componentId());
        }
    }

    public synchronized void put(StoredCircuit record) {
        circuits.put(record.id(), record);
        customComponents.remove(record.componentId());
        save(record);
    }

    /** 客户端网络同步使用：只更新内存，不写文件。 */
    public synchronized void putTransient(StoredCircuit record) {
        circuits.put(record.id(), record);
        customComponents.remove(record.componentId());
    }

    private void save(StoredCircuit record) {
        if (directory == null) {
            return;
        }
        try {
            Files.createDirectories(directory);
            net.minecraft.nbt.Tag encoded = StoredCircuit.CODEC.encodeStart(NbtOps.INSTANCE, record)
                    .result().orElseThrow(() -> new IllegalStateException("Failed to encode circuit"));
            if (!(encoded instanceof CompoundTag tag)) {
                throw new IllegalStateException("Encoded circuit is not a compound tag");
            }
            NbtIo.writeCompressed(tag, directory.resolve(record.id() + ".dat"));
        } catch (Exception e) {
            ElementsPlus.LOGGER.error("Failed to save circuit {}", record.id(), e);
        }
    }

    @Nullable
    public synchronized StoredCircuit get(@Nullable UUID id) {
        return id == null ? null : circuits.get(id);
    }

    public synchronized Collection<StoredCircuit> all() {
        return List.copyOf(circuits.values());
    }

    /** 按依赖顺序排序，保证客户端按顺序解码时嵌套自定义元件已经存在。 */
    public synchronized List<StoredCircuit> ordered() {
        List<StoredCircuit> source = new ArrayList<>(circuits.values());
        source.sort(java.util.Comparator.comparingInt(record ->
                record.diagram().scale.isInfinite() ? Integer.MAX_VALUE : record.diagram().scale.chunks));
        List<StoredCircuit> result = new ArrayList<>(source.size());
        Set<UUID> visiting = new HashSet<>();
        Set<UUID> done = new HashSet<>();
        for (StoredCircuit record : source) {
            visit(record, circuits, visiting, done, result);
        }
        return result;
    }

    private static void visit(StoredCircuit record, Map<UUID, StoredCircuit> all,
                              Set<UUID> visiting, Set<UUID> done, List<StoredCircuit> result) {
        if (done.contains(record.id()) || !visiting.add(record.id())) {
            return;
        }
        for (var chunk : record.diagram().chunks.values()) {
            for (var component : chunk.components) {
                if (!component.component.isCustom() || component.component.getCircuitId() == null) {
                    continue;
                }
                StoredCircuit dependency = all.get(component.component.getCircuitId());
                if (dependency != null) {
                    visit(dependency, all, visiting, done, result);
                }
            }
        }
        visiting.remove(record.id());
        if (done.add(record.id())) {
            result.add(record);
        }
    }

    public synchronized List<StoredCircuit> byName(String name) {
        List<StoredCircuit> result = new ArrayList<>();
        for (StoredCircuit record : circuits.values()) {
            if (record.matchesName(name)) {
                result.add(record);
            }
        }
        return result;
    }

    @Nullable
    public synchronized StoredCircuit byComponentId(@Nullable ResourceLocation id) {
        if (id == null) {
            return null;
        }
        String path = id.getPath();
        if (!id.getNamespace().equals(ElementsPlus.MOD_ID) || !path.startsWith("custom/")) {
            return null;
        }
        try {
            return circuits.get(UUID.fromString(path.substring("custom/".length())));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Nullable
    public synchronized CircuitComponent customComponent(@Nullable ResourceLocation id) {
        StoredCircuit record = byComponentId(id);
        if (record == null || !record.isCustom()) {
            return null;
        }
        return customComponents.computeIfAbsent(id, ignored -> new CustomCircuitComponent(record));
    }

    public synchronized List<CircuitComponent> customComponents() {
        List<CircuitComponent> result = new ArrayList<>();
        for (StoredCircuit record : circuits.values()) {
            if (!record.isCustom()) {
                continue;
            }
            ResourceLocation id = record.componentId();
            if (id != null) {
                result.add(customComponents.computeIfAbsent(id, ignored -> new CustomCircuitComponent(record)));
            }
        }
        return result;
    }

    public synchronized void clear() {
        circuits.clear();
        customComponents.clear();
    }

    @Nullable
    public synchronized Component displayName(StoredCircuit record) {
        if (record.isCustom()) {
            return net.minecraft.network.chat.Component.literal(record.custom().name())
                    .withStyle(net.minecraft.ChatFormatting.ITALIC);
        }
        CircuitComponent builtin = BuiltinCircuitComponents.byId(record.equivalentId());
        return builtin == null ? net.minecraft.network.chat.Component.literal(record.lookupName()) : builtin.getName();
    }
}
