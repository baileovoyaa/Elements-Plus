package com.elementsplus.network;

import com.elementsplus.ElementsPlus;
import com.elementsplus.ModDataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** 服务端 -> 客户端：文件夹内容、双容量与当前鼠标物品。 */
public record FileFolderDataPayload(int containerId, List<ItemStack> items,
                                    int capacityItems, int capacityBytes,
                                    ItemStack carried, int lockedInventoryIndex)
        implements CustomPacketPayload {
    public static final Type<FileFolderDataPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ElementsPlus.MOD_ID, "file_folder_data"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FileFolderDataPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeInt(payload.containerId());
                        List<ItemStack> items = new ArrayList<>();
                        for (ItemStack stack : payload.items()) {
                            if (stack != null && !stack.isEmpty()) {
                                items.add(stack);
                            }
                        }
                        buf.writeVarInt(items.size());
                        for (ItemStack stack : items) {
                            ItemStack.STREAM_CODEC.encode(buf, stack);
                        }
                        buf.writeVarInt(payload.capacityItems());
                        buf.writeVarInt(payload.capacityBytes());
                        boolean hasCarried = payload.carried() != null && !payload.carried().isEmpty();
                        buf.writeBoolean(hasCarried);
                        if (hasCarried) {
                            ItemStack.STREAM_CODEC.encode(buf, payload.carried());
                        }
                        buf.writeVarInt(payload.lockedInventoryIndex());
                    },
                    buf -> {
                        int containerId = buf.readInt();
                        int count = buf.readVarInt();
                        List<ItemStack> items = new ArrayList<>(count);
                        for (int i = 0; i < count; i++) {
                            items.add(ItemStack.STREAM_CODEC.decode(buf));
                        }
                        int capacityItems = buf.readVarInt();
                        int capacityBytes = buf.readVarInt();
                        ItemStack carried = buf.readBoolean()
                                ? ItemStack.STREAM_CODEC.decode(buf) : ItemStack.EMPTY;
                        int locked = buf.readVarInt();
                        return new FileFolderDataPayload(containerId, items, capacityItems,
                                capacityBytes, carried, locked);
                    }
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
