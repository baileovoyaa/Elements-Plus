package com.elementsplus.core.config;

import com.elementsplus.ElementsPlus;
import com.elementsplus.core.circuit.diagram.CircuitDiagram;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 服务端（存档级）配置。文件位于存档根目录的 serverconfig 文件夹中。
 *
 * <p>客户端在加入服务器时会收到一份同步副本，用于在放置/编译自定义元件时提前显示
 * 与服务器一致的规模嵌套校验结果；真正以服务端校验为准。</p>
 */
public final class ElementsPlusServerConfig {
    public static final String CUSTOM_COMPONENT_NESTING_LIMIT = "custom_component_nesting_limit";
    public static final String ALLOW_INFINITE_IN_FINITE_CIRCUIT = "allow_infinite_in_finite_circuit";
    public static final String ASYNC_EXPERIMENT_TESTS = "async_experiment_tests";
    public static final String ALLOW_SURVIVAL_CREATIVE_COMPONENT = "allow_survival_creative_component";
    public static final String CIRCUIT_ALBUM_CAPACITY = "circuit_album_capacity";
    public static final String INTERFACE_SAMPLE_TICKS = "interface_sample_ticks";
    public static final String INTERFACE_AVG_MSPT_LIMIT = "interface_avg_mspt_limit";
    public static final String INTERFACE_MAX_MSPT_LIMIT = "interface_max_mspt_limit";

    public static final List<String> OPTIONS = List.of(
            CUSTOM_COMPONENT_NESTING_LIMIT,
            ALLOW_INFINITE_IN_FINITE_CIRCUIT,
            ASYNC_EXPERIMENT_TESTS,
            ALLOW_SURVIVAL_CREATIVE_COMPONENT,
            CIRCUIT_ALBUM_CAPACITY,
            INTERFACE_SAMPLE_TICKS,
            INTERFACE_AVG_MSPT_LIMIT,
            INTERFACE_MAX_MSPT_LIMIT
    );

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int DEFAULT_NESTING_LIMIT = 1;
    private static final boolean DEFAULT_ALLOW_INFINITE_IN_FINITE = false;
    private static final boolean DEFAULT_ASYNC_EXPERIMENT_TESTS = false;
    private static final boolean DEFAULT_ALLOW_SURVIVAL_CREATIVE_COMPONENT = false;
    private static final int DEFAULT_CIRCUIT_ALBUM_CAPACITY = 50;
    private static final int DEFAULT_INTERFACE_SAMPLE_TICKS = 100;
    private static final int DEFAULT_INTERFACE_AVG_MSPT_LIMIT = 10;
    private static final int DEFAULT_INTERFACE_MAX_MSPT_LIMIT = 100;

    private static volatile int customComponentNestingLimit = DEFAULT_NESTING_LIMIT;
    private static volatile boolean allowInfiniteInFiniteCircuit = DEFAULT_ALLOW_INFINITE_IN_FINITE;
    private static volatile boolean asyncExperimentTests = DEFAULT_ASYNC_EXPERIMENT_TESTS;
    private static volatile boolean allowSurvivalCreativeComponent = DEFAULT_ALLOW_SURVIVAL_CREATIVE_COMPONENT;
    private static volatile int circuitAlbumCapacity = DEFAULT_CIRCUIT_ALBUM_CAPACITY;
    private static volatile int interfaceSampleTicks = DEFAULT_INTERFACE_SAMPLE_TICKS;
    private static volatile int interfaceAvgMsptLimit = DEFAULT_INTERFACE_AVG_MSPT_LIMIT;
    private static volatile int interfaceMaxMsptLimit = DEFAULT_INTERFACE_MAX_MSPT_LIMIT;
    private static volatile @Nullable Path configFile;

    private ElementsPlusServerConfig() {
    }

    public static int customComponentNestingLimit() {
        return customComponentNestingLimit;
    }

    public static boolean allowInfiniteInFiniteCircuit() {
        return allowInfiniteInFiniteCircuit;
    }

    public static boolean asyncExperimentTests() {
        return asyncExperimentTests;
    }

    public static boolean allowSurvivalCreativeComponent() {
        return allowSurvivalCreativeComponent;
    }

    public static int circuitAlbumCapacity() {
        return circuitAlbumCapacity;
    }

    public static int interfaceSampleTicks() {
        return interfaceSampleTicks;
    }

    public static int interfaceAvgMsptLimit() {
        return interfaceAvgMsptLimit;
    }

    public static int interfaceMaxMsptLimit() {
        return interfaceMaxMsptLimit;
    }

    public static boolean isOption(String option) {
        return OPTIONS.contains(option);
    }

    public static String valueOf(String option) {
        return switch (option) {
            case CUSTOM_COMPONENT_NESTING_LIMIT -> Integer.toString(customComponentNestingLimit);
            case ALLOW_INFINITE_IN_FINITE_CIRCUIT -> Boolean.toString(allowInfiniteInFiniteCircuit);
            case ASYNC_EXPERIMENT_TESTS -> Boolean.toString(asyncExperimentTests);
            case ALLOW_SURVIVAL_CREATIVE_COMPONENT -> Boolean.toString(allowSurvivalCreativeComponent);
            case CIRCUIT_ALBUM_CAPACITY -> Integer.toString(circuitAlbumCapacity);
            case INTERFACE_SAMPLE_TICKS -> Integer.toString(interfaceSampleTicks);
            case INTERFACE_AVG_MSPT_LIMIT -> Integer.toString(interfaceAvgMsptLimit);
            case INTERFACE_MAX_MSPT_LIMIT -> Integer.toString(interfaceMaxMsptLimit);
            default -> "";
        };
    }

    public static boolean canNest(CircuitDiagram.Scale parent, CircuitDiagram.Scale child) {
        if (parent == null || child == null) {
            return false;
        }
        // 无限规模永远可以容纳任意元件，包括其他无限规模元件。
        if (parent.isInfinite()) {
            return true;
        }
        // 无限规模元件放进有限规模电路由单独配置控制。
        if (child.isInfinite()) {
            return allowInfiniteInFiniteCircuit;
        }
        // 父电路规模至少要比子元件大 nestingLimit 级。
        return parent.ordinal() - child.ordinal() >= customComponentNestingLimit;
    }

    public static boolean setNestingLimit(int value) {
        int previous = customComponentNestingLimit;
        customComponentNestingLimit = value;
        if (save()) {
            return true;
        }
        customComponentNestingLimit = previous;
        return false;
    }

    public static boolean setAllowInfiniteInFiniteCircuit(boolean value) {
        boolean previous = allowInfiniteInFiniteCircuit;
        allowInfiniteInFiniteCircuit = value;
        if (save()) {
            return true;
        }
        allowInfiniteInFiniteCircuit = previous;
        return false;
    }

    public static boolean setAsyncExperimentTests(boolean value) {
        boolean previous = asyncExperimentTests;
        asyncExperimentTests = value;
        if (save()) {
            return true;
        }
        asyncExperimentTests = previous;
        return false;
    }

    public static boolean setAllowSurvivalCreativeComponent(boolean value) {
        boolean previous = allowSurvivalCreativeComponent;
        allowSurvivalCreativeComponent = value;
        if (save()) {
            return true;
        }
        allowSurvivalCreativeComponent = previous;
        return false;
    }

    public static boolean setCircuitAlbumCapacity(int value) {
        int previous = circuitAlbumCapacity;
        circuitAlbumCapacity = Math.max(0, value);
        if (save()) {
            return true;
        }
        circuitAlbumCapacity = previous;
        return false;
    }

    public static boolean setInterfaceSampleTicks(int value) {
        int previous = interfaceSampleTicks;
        interfaceSampleTicks = Math.max(0, value);
        if (save()) {
            return true;
        }
        interfaceSampleTicks = previous;
        return false;
    }

    public static boolean setInterfaceAvgMsptLimit(int value) {
        int previous = interfaceAvgMsptLimit;
        interfaceAvgMsptLimit = Math.max(0, value);
        if (save()) {
            return true;
        }
        interfaceAvgMsptLimit = previous;
        return false;
    }

    public static boolean setInterfaceMaxMsptLimit(int value) {
        int previous = interfaceMaxMsptLimit;
        interfaceMaxMsptLimit = Math.max(0, value);
        if (save()) {
            return true;
        }
        interfaceMaxMsptLimit = previous;
        return false;
    }

    /**
     * 收到服务端同步的配置时调用。客户端只使用这些值进行预览校验。
     */
    public static void applySynced(int nestingLimit, boolean allowInfiniteInFinite, boolean asyncTests,
                                   boolean allowSurvivalCreative) {
        customComponentNestingLimit = nestingLimit;
        allowInfiniteInFiniteCircuit = allowInfiniteInFinite;
        asyncExperimentTests = asyncTests;
        allowSurvivalCreativeComponent = allowSurvivalCreative;
    }

    /**
     * 从存档目录加载配置。文件不存在时会创建默认配置。
     */
    public static void load(MinecraftServer server) {
        Path directory = server.getWorldPath(LevelResource.ROOT).resolve("serverconfig");
        configFile = directory.resolve("elements-plus-server.json");
        customComponentNestingLimit = DEFAULT_NESTING_LIMIT;
        allowInfiniteInFiniteCircuit = DEFAULT_ALLOW_INFINITE_IN_FINITE;
        asyncExperimentTests = DEFAULT_ASYNC_EXPERIMENT_TESTS;
        allowSurvivalCreativeComponent = DEFAULT_ALLOW_SURVIVAL_CREATIVE_COMPONENT;
        circuitAlbumCapacity = DEFAULT_CIRCUIT_ALBUM_CAPACITY;
        interfaceSampleTicks = DEFAULT_INTERFACE_SAMPLE_TICKS;
        interfaceAvgMsptLimit = DEFAULT_INTERFACE_AVG_MSPT_LIMIT;
        interfaceMaxMsptLimit = DEFAULT_INTERFACE_MAX_MSPT_LIMIT;

        if (!Files.exists(configFile)) {
            save();
            return;
        }

        try (Reader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            if (root.has(CUSTOM_COMPONENT_NESTING_LIMIT)
                    && root.get(CUSTOM_COMPONENT_NESTING_LIMIT).isJsonPrimitive()) {
                customComponentNestingLimit = root.get(CUSTOM_COMPONENT_NESTING_LIMIT).getAsInt();
            }
            if (root.has(ALLOW_INFINITE_IN_FINITE_CIRCUIT)
                    && root.get(ALLOW_INFINITE_IN_FINITE_CIRCUIT).isJsonPrimitive()) {
                allowInfiniteInFiniteCircuit = root.get(ALLOW_INFINITE_IN_FINITE_CIRCUIT).getAsBoolean();
            }
            if (root.has(ASYNC_EXPERIMENT_TESTS)
                    && root.get(ASYNC_EXPERIMENT_TESTS).isJsonPrimitive()) {
                asyncExperimentTests = root.get(ASYNC_EXPERIMENT_TESTS).getAsBoolean();
            }
            if (root.has(ALLOW_SURVIVAL_CREATIVE_COMPONENT)
                    && root.get(ALLOW_SURVIVAL_CREATIVE_COMPONENT).isJsonPrimitive()) {
                allowSurvivalCreativeComponent = root.get(ALLOW_SURVIVAL_CREATIVE_COMPONENT).getAsBoolean();
            }
            if (root.has(CIRCUIT_ALBUM_CAPACITY)
                    && root.get(CIRCUIT_ALBUM_CAPACITY).isJsonPrimitive()) {
                circuitAlbumCapacity = Math.max(0, root.get(CIRCUIT_ALBUM_CAPACITY).getAsInt());
            }
            if (root.has(INTERFACE_SAMPLE_TICKS)
                    && root.get(INTERFACE_SAMPLE_TICKS).isJsonPrimitive()) {
                interfaceSampleTicks = Math.max(0, root.get(INTERFACE_SAMPLE_TICKS).getAsInt());
            }
            if (root.has(INTERFACE_AVG_MSPT_LIMIT)
                    && root.get(INTERFACE_AVG_MSPT_LIMIT).isJsonPrimitive()) {
                interfaceAvgMsptLimit = Math.max(0, root.get(INTERFACE_AVG_MSPT_LIMIT).getAsInt());
            }
            if (root.has(INTERFACE_MAX_MSPT_LIMIT)
                    && root.get(INTERFACE_MAX_MSPT_LIMIT).isJsonPrimitive()) {
                interfaceMaxMsptLimit = Math.max(0, root.get(INTERFACE_MAX_MSPT_LIMIT).getAsInt());
            }
        } catch (Exception e) {
            ElementsPlus.LOGGER.warn("Failed to load server config {}", configFile, e);
            return;
        }

        // 补全旧文件/新版本中缺失的配置项。
        save();
    }

    public static boolean save() {
        Path file = configFile;
        if (file == null) {
            return false;
        }
        JsonObject root = new JsonObject();
        root.addProperty(CUSTOM_COMPONENT_NESTING_LIMIT, customComponentNestingLimit);
        root.addProperty(ALLOW_INFINITE_IN_FINITE_CIRCUIT, allowInfiniteInFiniteCircuit);
        root.addProperty(ASYNC_EXPERIMENT_TESTS, asyncExperimentTests);
        root.addProperty(ALLOW_SURVIVAL_CREATIVE_COMPONENT, allowSurvivalCreativeComponent);
        root.addProperty(CIRCUIT_ALBUM_CAPACITY, circuitAlbumCapacity);
        root.addProperty(INTERFACE_SAMPLE_TICKS, interfaceSampleTicks);
        root.addProperty(INTERFACE_AVG_MSPT_LIMIT, interfaceAvgMsptLimit);
        root.addProperty(INTERFACE_MAX_MSPT_LIMIT, interfaceMaxMsptLimit);
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(root) + System.lineSeparator(), StandardCharsets.UTF_8);
            return true;
        } catch (IOException e) {
            ElementsPlus.LOGGER.warn("Failed to save server config {}", file, e);
            return false;
        }
    }

    /** 服务端停止时清空存档路径，避免切换存档后继续读写旧文件。 */
    public static void unload() {
        configFile = null;
        customComponentNestingLimit = DEFAULT_NESTING_LIMIT;
        allowInfiniteInFiniteCircuit = DEFAULT_ALLOW_INFINITE_IN_FINITE;
        asyncExperimentTests = DEFAULT_ASYNC_EXPERIMENT_TESTS;
        allowSurvivalCreativeComponent = DEFAULT_ALLOW_SURVIVAL_CREATIVE_COMPONENT;
        circuitAlbumCapacity = DEFAULT_CIRCUIT_ALBUM_CAPACITY;
        interfaceSampleTicks = DEFAULT_INTERFACE_SAMPLE_TICKS;
        interfaceAvgMsptLimit = DEFAULT_INTERFACE_AVG_MSPT_LIMIT;
        interfaceMaxMsptLimit = DEFAULT_INTERFACE_MAX_MSPT_LIMIT;
    }
}
