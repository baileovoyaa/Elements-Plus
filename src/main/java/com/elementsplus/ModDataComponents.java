package com.elementsplus;

import com.elementsplus.core.circuit.diagram.CircuitDiagram;
import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ModDataComponents {

    public static final Codec<UUID> UUID_CODEC = Codec.STRING.xmap(UUID::fromString, UUID::toString);
    public static final StreamCodec<FriendlyByteBuf, UUID> UUID_STREAM_CODEC = StreamCodec.of(
            (buf, uuid) -> buf.writeUUID(uuid),
            buf -> buf.readUUID()
    );

    public static final DataComponentType<CircuitDiagram> CIRCUIT_DIAGRAM = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            ElementsPlus.id("circuit_diagram"),
            DataComponentType.<CircuitDiagram>builder()
                    .persistent(CircuitDiagram.CODEC)
                    .networkSynchronized(CircuitDiagram.STREAM_CODEC)
                    .build()
    );

    /** 已编译电路在分离存储中的 UUID；物品不再直接保存电路本体。 */
    public static final DataComponentType<UUID> COMPILED_CIRCUIT = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            ElementsPlus.id("compiled_circuit"),
            DataComponentType.<UUID>builder()
                    .persistent(UUID_CODEC)
                    .networkSynchronized(UUID_STREAM_CODEC)
                    .build()
    );

    public static final DataComponentType<ResourceLocation> EQUIVALENT_COMPONENT = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            ElementsPlus.id("equivalent_component"),
            DataComponentType.<ResourceLocation>builder()
                    .persistent(ResourceLocation.CODEC.fieldOf("id").codec())
                    .networkSynchronized(ResourceLocation.STREAM_CODEC)
                    .build()
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, List<ItemStack>> ITEM_STACK_LIST_STREAM_CODEC =
            StreamCodec.of(
                    (buf, stacks) -> {
                        buf.writeVarInt(stacks.size());
                        for (ItemStack stack : stacks) {
                            ItemStack.STREAM_CODEC.encode(buf, stack);
                        }
                    },
                    buf -> {
                        int size = buf.readVarInt();
                        List<ItemStack> stacks = new ArrayList<>(size);
                        for (int i = 0; i < size; i++) {
                            stacks.add(ItemStack.STREAM_CODEC.decode(buf));
                        }
                        return stacks;
                    }
            );

    /** 电路图册中保存的电路图物品列表。 */
    public static final DataComponentType<List<ItemStack>> CIRCUIT_ALBUM_CONTENTS = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            ElementsPlus.id("circuit_album_contents"),
            DataComponentType.<List<ItemStack>>builder()
                    .persistent(ItemStack.CODEC.listOf())
                    .networkSynchronized(ITEM_STACK_LIST_STREAM_CODEC)
                    .build()
    );

    /** 文件夹物品中保存的文本文件/打孔文件列表。 */
    public static final DataComponentType<List<ItemStack>> FILE_FOLDER_CONTENTS = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            ElementsPlus.id("file_folder_contents"),
            DataComponentType.<List<ItemStack>>builder()
                    .persistent(ItemStack.CODEC.listOf())
                    .networkSynchronized(ITEM_STACK_LIST_STREAM_CODEC)
                    .build()
    );

    public static final StreamCodec<FriendlyByteBuf, byte[]> BYTE_ARRAY_STREAM_CODEC = StreamCodec.of(
            (buf, data) -> {
                buf.writeVarInt(data.length);
                buf.writeBytes(data);
            },
            buf -> {
                byte[] data = new byte[buf.readVarInt()];
                buf.readBytes(data);
                return data;
            }
    );

    public static final Codec<byte[]> BYTE_ARRAY_CODEC = Codec.STRING.xmap(
            text -> java.util.Base64.getDecoder().decode(text),
            data -> java.util.Base64.getEncoder().encodeToString(data)
    );

    /** 磁盘物品中的 256 字节数据。 */
    public static final DataComponentType<byte[]> DISK_DATA = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            ElementsPlus.id("disk_data"),
            DataComponentType.<byte[]>builder()
                    .persistent(BYTE_ARRAY_CODEC)
                    .networkSynchronized(BYTE_ARRAY_STREAM_CODEC)
                    .build()
    );

    /** 文本文件 / 打孔文件物品中的文件内容。 */
    public static final DataComponentType<byte[]> FILE_DATA = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            ElementsPlus.id("file_data"),
            DataComponentType.<byte[]>builder()
                    .persistent(BYTE_ARRAY_CODEC)
                    .networkSynchronized(BYTE_ARRAY_STREAM_CODEC)
                    .build()
    );

    /** 纸带阅读器中放入的打孔文件。 */
    public static final DataComponentType<ItemStack> PAPER_TAPE_CONTENTS = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            ElementsPlus.id("paper_tape_contents"),
            DataComponentType.<ItemStack>builder()
                    .persistent(ItemStack.CODEC)
                    .networkSynchronized(ItemStack.STREAM_CODEC)
                    .build()
    );

    public static void initialize() {
    }
}