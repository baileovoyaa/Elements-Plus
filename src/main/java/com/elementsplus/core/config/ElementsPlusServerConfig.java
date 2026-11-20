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
    public static final String FILE_FOLDER_CAPACITY = "file_folder_capacity";
    public static final String FILE_FOLDER_MAX_BYTES = "file_folder_max_bytes";
    public static final String INTERFACE_SAMPLE_TICKS = "interface_sample_ticks";
    public static final String INTERFACE_AVG_MSPT_LIMIT = "interface_avg_mspt_limit";
    public static final String INTERFACE_MAX_MSPT_LIMIT = "interface_max_mspt_limit";
    public static final String TEXT_FILE_MAX_BYTES = "text_file_max_bytes";
    public static final String SHARED_FILE_MAX_BYTES = "shared_file_max_bytes";
    public static final String MEMORY_MAX_BYTES = "memory_max_bytes";
    public static final String MAX_SHARED_PER_PLAYER = "max_shared_per_player";
    public static final String ALLOW_CIRCUIT_UPLOAD = "allow_circuit_upload";

    public static final List<String> OPTIONS = List.of(
            CUSTOM_COMPONENT_NESTING_LIMIT,
            ALLOW_INFINITE_IN_FINITE_CIRCUIT,
            ASYNC_EXPERIMENT_TESTS,
            ALLOW_SURVIVAL_CREATIVE_COMPONENT,
            CIRCUIT_ALBUM_CAPACITY,
            FILE_FOLDER_CAPACITY,
            FILE_FOLDER_MAX_BYTES,
            INTERFACE_SAMPLE_TICKS,
            INTERFACE_AVG_MSPT_LIMIT,
            INTERFACE_MAX_MSPT_LIMIT,
            TEXT_FILE_MAX_BYTES,
            SHARED_FILE_MAX_BYTES,
            MEMORY_MAX_BYTES,
            MAX_SHARED_PER_PLAYER,
            ALLOW_CIRCUIT_UPLOAD
    );

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int DEFAULT_NESTING_LIMIT = 1;
    private static final boolean DEFAULT_ALLOW_INFINITE_IN_FINITE = false;
    private static final boolean DEFAULT_ASYNC_EXPERIMENT_TESTS = false;
    private static final boolean DEFAULT_ALLOW_SURVIVAL_CREATIVE_COMPONENT = false;
    private static final int DEFAULT_CIRCUIT_ALBUM_CAPACITY = 50;
    private static final int DEFAULT_FILE_FOLDER_CAPACITY = 50;
    private static final int DEFAULT_FILE_FOLDER_MAX_BYTES = 1024 * 1024;
    private static final int DEFAULT_INTERFACE_SAMPLE_TICKS = 100;
    private static final int DEFAULT_INTERFACE_AVG_MSPT_LIMIT = 10;
    private static final int DEFAULT_INTERFACE_MAX_MSPT_LIMIT = 100;
    private static final int DEFAULT_TEXT_FILE_MAX_BYTES = 32 * 1024;
    private static final int DEFAULT_SHARED_FILE_MAX_BYTES = 32 * 1024;
    private static final int DEFAULT_MEMORY_MAX_BYTES = 32 * 1024;
    private static final int DEFAULT_MAX_SHARED_PER_PLAYER = 4;
    private static final boolean DEFAULT_ALLOW_CIRCUIT_UPLOAD = false;

    private static volatile int customComponentNestingLimit = DEFAULT_NESTING_LIMIT;
    private static volatile boolean allowInfiniteInFiniteCircuit = DEFAULT_ALLOW_INFINITE_IN_FINITE;
    private static volatile boolean asyncExperimentTests = DEFAULT_ASYNC_EXPERIMENT_TESTS;
    private static volatile boolean allowSurvivalCreativeComponent = DEFAULT_ALLOW_SURVIVAL_CREATIVE_COMPONENT;
    private static volatile int circuitAlbumCapacity = DEFAULT_CIRCUIT_ALBUM_CAPACITY;
    private static volatile int fileFolderCapacity = DEFAULT_FILE_FOLDER_CAPACITY;
    private static volatile int fileFolderMaxBytes = DEFAULT_FILE_FOLDER_MAX_BYTES;
    private static volatile int interfaceSampleTicks = DEFAULT_INTERFACE_SAMPLE_TICKS;
    private static volatile int interfaceAvgMsptLimit = DEFAULT_INTERFACE_AVG_MSPT_LIMIT;
    private static volatile int interfaceMaxMsptLimit = DEFAULT_INTERFACE_MAX_MSPT_LIMIT;
    private static volatile int textFileMaxBytes = DEFAULT_TEXT_FILE_MAX_BYTES;
    private static volatile int sharedFileMaxBytes = DEFAULT_SHARED_FILE_MAX_BYTES;
    private static volatile int memoryMaxBytes = DEFAULT_MEMORY_MAX_BYTES;
    private static volatile int maxSharedPerPlayer = DEFAULT_MAX_SHARED_PER_PLAYER;
    private static volatile boolean allowCircuitUpload = DEFAULT_ALLOW_CIRCUIT_UPLOAD;
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

    public static int fileFolderCapacity() {
        return fileFolderCapacity;
    }

    public static int fileFolderMaxBytes() {
        return fileFolderMaxBytes;
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

    /** 文本文件物品的最大字节数。 */
    public static int textFileMaxBytes() {
        return textFileMaxBytes;
    }

    /** 单个共享文件的最大字节数。 */
    public static int sharedFileMaxBytes() {
        return sharedFileMaxBytes;
    }

    /** 实验桌内存的总容量（包含文件头）。 */
    public static int memoryMaxBytes() {
        return memoryMaxBytes;
    }

    /** 单玩家同时共享文件数上限。 */
    public static int maxSharedPerPlayer() {
        return maxSharedPerPlayer;
    }

    /** 是否允许通过实验桌上传电路图。 */
    public static boolean allowCircuitUpload() {
        return allowCircuitUpload;
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
            case FILE_FOLDER_CAPACITY -> Integer.toString(fileFolderCapacity);
            case FILE_FOLDER_MAX_BYTES -> Integer.toString(fileFolderMaxBytes);
            case INTERFACE_SAMPLE_TICKS -> Integer.toString(interfaceSampleTicks);
            case INTERFACE_AVG_MSPT_LIMIT -> Integer.toString(interfaceAvgMsptLimit);
            case INTERFACE_MAX_MSPT_LIMIT -> Integer.toString(interfaceMaxMsptLimit);
            case TEXT_FILE_MAX_BYTES -> Integer.toString(textFileMaxBytes);
            case SHARED_FILE_MAX_BYTES -> Integer.toString(sharedFileMaxBytes);
            case MEMORY_MAX_BYTES -> Integer.toString(memoryMaxBytes);
            case MAX_SHARED_PER_PLAYER -> Integer.toString(maxSharedPerPlayer);
            case ALLOW_CIRCUIT_UPLOAD -> Boolean.toString(allowCircuitUpload);
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

    public static boolean setFileFolderCapacity(int value) {
        int previous = fileFolderCapacity;
        fileFolderCapacity = Math.max(0, value);
        if (save()) {
            return true;
        }
        fileFolderCapacity = previous;
        return false;
    }

    public static boolean setFileFolderMaxBytes(int value) {
        int previous = fileFolderMaxBytes;
        fileFolderMaxBytes = Math.max(0, value);
        if (save()) {
            return true;
        }
        fileFolderMaxBytes = previous;
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

    public static boolean setTextFileMaxBytes(int value) {
        int previous = textFileMaxBytes;
        textFileMaxBytes = clampFileSize(value);
        if (save()) {
            return true;
        }
        textFileMaxBytes = previous;
        return false;
    }

    public static boolean setSharedFileMaxBytes(int value) {
        int previous = sharedFileMaxBytes;
        sharedFileMaxBytes = clampFileSize(value);
        if (save()) {
            return true;
        }
        sharedFileMaxBytes = previous;
        return false;
    }

    public static boolean setMemoryMaxBytes(int value) {
        int previous = memoryMaxBytes;
        memoryMaxBytes = clampFileSize(value);
        if (save()) {
            return true;
        }
        memoryMaxBytes = previous;
        return false;
    }

    public static boolean setMaxSharedPerPlayer(int value) {
        int previous = maxSharedPerPlayer;
        maxSharedPerPlayer = Math.max(0, value);
        if (save()) {
            return true;
        }
        maxSharedPerPlayer = previous;
        return false;
    }

    public static boolean setAllowCircuitUpload(boolean value) {
        boolean previous = allowCircuitUpload;
        allowCircuitUpload = value;
        if (save()) {
            return true;
        }
        allowCircuitUpload = previous;
        return false;
    }

    private static int clampFileSize(int value) {
        return Math.max(0, Math.min(com.elementsplus.core.file.FileHeader.MAX_DATA_LENGTH, value));
    }

    /**
     * 收到服务端同步的配置时调用。客户端只使用这些值进行预览校验。
     */
    public static void applySynced(int nestingLimit, boolean allowInfiniteInFinite, boolean asyncTests,
                                   boolean allowSurvivalCreative, int textFileLimit, int sharedFileLimit,
                                   int memoryLimit, int maxShared, boolean circuitUpload) {
        customComponentNestingLimit = nestingLimit;
        allowInfiniteInFiniteCircuit = allowInfiniteInFinite;
        asyncExperimentTests = asyncTests;
        allowSurvivalCreativeComponent = allowSurvivalCreative;
        textFileMaxBytes = clampFileSize(textFileLimit);
        sharedFileMaxBytes = clampFileSize(sharedFileLimit);
        memoryMaxBytes = clampFileSize(memoryLimit);
        maxSharedPerPlayer = Math.max(0, maxShared);
        allowCircuitUpload = circuitUpload;
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
        fileFolderCapacity = DEFAULT_FILE_FOLDER_CAPACITY;
        fileFolderMaxBytes = DEFAULT_FILE_FOLDER_MAX_BYTES;
        interfaceSampleTicks = DEFAULT_INTERFACE_SAMPLE_TICKS;
        interfaceAvgMsptLimit = DEFAULT_INTERFACE_AVG_MSPT_LIMIT;
        interfaceMaxMsptLimit = DEFAULT_INTERFACE_MAX_MSPT_LIMIT;
        textFileMaxBytes = DEFAULT_TEXT_FILE_MAX_BYTES;
        sharedFileMaxBytes = DEFAULT_SHARED_FILE_MAX_BYTES;
        memoryMaxBytes = DEFAULT_MEMORY_MAX_BYTES;
        maxSharedPerPlayer = DEFAULT_MAX_SHARED_PER_PLAYER;
        allowCircuitUpload = DEFAULT_ALLOW_CIRCUIT_UPLOAD;

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
            if (root.has(FILE_FOLDER_CAPACITY)
                    && root.get(FILE_FOLDER_CAPACITY).isJsonPrimitive()) {
                fileFolderCapacity = Math.max(0, root.get(FILE_FOLDER_CAPACITY).getAsInt());
            }
            if (root.has(FILE_FOLDER_MAX_BYTES)
                    && root.get(FILE_FOLDER_MAX_BYTES).isJsonPrimitive()) {
                fileFolderMaxBytes = Math.max(0, root.get(FILE_FOLDER_MAX_BYTES).getAsInt());
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
            if (root.has(TEXT_FILE_MAX_BYTES)
                    && root.get(TEXT_FILE_MAX_BYTES).isJsonPrimitive()) {
                textFileMaxBytes = clampFileSize(root.get(TEXT_FILE_MAX_BYTES).getAsInt());
            }
            if (root.has(SHARED_FILE_MAX_BYTES)
                    && root.get(SHARED_FILE_MAX_BYTES).isJsonPrimitive()) {
                sharedFileMaxBytes = clampFileSize(root.get(SHARED_FILE_MAX_BYTES).getAsInt());
            }
            if (root.has(MEMORY_MAX_BYTES)
                    && root.get(MEMORY_MAX_BYTES).isJsonPrimitive()) {
                memoryMaxBytes = clampFileSize(root.get(MEMORY_MAX_BYTES).getAsInt());
            }
            if (root.has(MAX_SHARED_PER_PLAYER)
                    && root.get(MAX_SHARED_PER_PLAYER).isJsonPrimitive()) {
                maxSharedPerPlayer = Math.max(0, root.get(MAX_SHARED_PER_PLAYER).getAsInt());
            }
            if (root.has(ALLOW_CIRCUIT_UPLOAD)
                    && root.get(ALLOW_CIRCUIT_UPLOAD).isJsonPrimitive()) {
                allowCircuitUpload = root.get(ALLOW_CIRCUIT_UPLOAD).getAsBoolean();
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
        root.addProperty(FILE_FOLDER_CAPACITY, fileFolderCapacity);
        root.addProperty(FILE_FOLDER_MAX_BYTES, fileFolderMaxBytes);
        root.addProperty(INTERFACE_SAMPLE_TICKS, interfaceSampleTicks);
        root.addProperty(INTERFACE_AVG_MSPT_LIMIT, interfaceAvgMsptLimit);
        root.addProperty(INTERFACE_MAX_MSPT_LIMIT, interfaceMaxMsptLimit);
        root.addProperty(TEXT_FILE_MAX_BYTES, textFileMaxBytes);
        root.addProperty(SHARED_FILE_MAX_BYTES, sharedFileMaxBytes);
        root.addProperty(MEMORY_MAX_BYTES, memoryMaxBytes);
        root.addProperty(MAX_SHARED_PER_PLAYER, maxSharedPerPlayer);
        root.addProperty(ALLOW_CIRCUIT_UPLOAD, allowCircuitUpload);
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
        fileFolderCapacity = DEFAULT_FILE_FOLDER_CAPACITY;
        fileFolderMaxBytes = DEFAULT_FILE_FOLDER_MAX_BYTES;
        interfaceSampleTicks = DEFAULT_INTERFACE_SAMPLE_TICKS;
        interfaceAvgMsptLimit = DEFAULT_INTERFACE_AVG_MSPT_LIMIT;
        interfaceMaxMsptLimit = DEFAULT_INTERFACE_MAX_MSPT_LIMIT;
        textFileMaxBytes = DEFAULT_TEXT_FILE_MAX_BYTES;
        sharedFileMaxBytes = DEFAULT_SHARED_FILE_MAX_BYTES;
        memoryMaxBytes = DEFAULT_MEMORY_MAX_BYTES;
        maxSharedPerPlayer = DEFAULT_MAX_SHARED_PER_PLAYER;
        allowCircuitUpload = DEFAULT_ALLOW_CIRCUIT_UPLOAD;
    }
}
