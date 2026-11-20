package com.elementsplus.core.file;

import com.elementsplus.core.circuit.CircuitComponentRegistry;
import com.elementsplus.core.circuit.diagram.CircuitDiagram;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 电路图与外接设备之间的 {@code .dat} 文件格式：内容是 {@code CIRCUIT_DIAGRAM}
 * 的 NBT 序列化，不包含编译信息。
 */
public final class CircuitDiagramFile {
    /** 防止恶意/损坏文件消耗过多内存。 */
    private static final int MAX_FILE_BYTES = 1024 * 1024;
    private static final int MAX_COMPONENTS = 4096;
    private static final int MAX_CHUNKS = 4096;

    private CircuitDiagramFile() {
    }

    /** 把电路图序列化为 {@code .dat} 字节；失败返回 {@code null}。 */
    public static @Nullable byte[] encode(CircuitDiagram diagram) {
        if (diagram == null) {
            return null;
        }
        Tag tag = CircuitDiagram.CODEC.encodeStart(NbtOps.INSTANCE, diagram)
                .result().orElse(null);
        if (!(tag instanceof CompoundTag compound)) {
            return null;
        }
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream output = new DataOutputStream(bytes);
            NbtIo.write(compound, output);
            output.flush();
            return bytes.toByteArray();
        } catch (IOException e) {
            return null;
        }
    }

    /** 解析 {@code .dat} 字节；失败返回 {@code null}。 */
    public static @Nullable CircuitDiagram decode(byte[] data) {
        CompoundTag compound = readCompound(data);
        if (compound == null) {
            return null;
        }
        return CircuitDiagram.CODEC.parse(NbtOps.INSTANCE, compound).result().orElse(null);
    }

    private static @Nullable CompoundTag readCompound(byte[] data) {
        if (data == null || data.length == 0 || data.length > MAX_FILE_BYTES) {
            return null;
        }
        try {
            DataInputStream input = new DataInputStream(new ByteArrayInputStream(data));
            Tag tag = NbtIo.read(input, NbtAccounter.create(MAX_FILE_BYTES));
            return tag instanceof CompoundTag compound ? compound : null;
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    /**
     * 上传时的安全校验。返回错误信息列表；为空表示可以写入。
     */
    public static List<String> validate(byte[] data, @Nullable CircuitDiagram diagram) {
        List<String> errors = new ArrayList<>();
        if (diagram == null) {
            errors.add("gui.elements-plus.file.error.invalid_diagram");
            return errors;
        }
        if (diagram.scale == null) {
            errors.add("gui.elements-plus.file.error.invalid_diagram");
            return errors;
        }
        if (diagram.chunks == null || diagram.chunks.size() > MAX_CHUNKS) {
            errors.add("gui.elements-plus.file.error.invalid_diagram");
            return errors;
        }
        int components = 0;
        for (CircuitDiagram.Chunk chunk : diagram.chunks.values()) {
            if (chunk == null || chunk.components == null) {
                errors.add("gui.elements-plus.file.error.invalid_diagram");
                return errors;
            }
            components += chunk.components.size();
            for (CircuitDiagram.Component component : chunk.components) {
                if (component == null || component.component == null) {
                    errors.add("gui.elements-plus.file.error.invalid_diagram");
                    return errors;
                }
                if (CircuitComponentRegistry.byId(component.component.getId()) == null) {
                    errors.add("gui.elements-plus.file.error.unknown_component");
                    return errors;
                }
            }
        }
        if (components > MAX_COMPONENTS) {
            errors.add("gui.elements-plus.file.error.invalid_diagram");
        }
        return errors;
    }
}
