package com.elementsplus.network;

import com.elementsplus.ElementsPlus;
import com.elementsplus.ModDataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** 服务端 -> 客户端：电路图册的容量、当前内容、光标物品和锁定的物品栏格。 */
public record CircuitAlbumDataPayload(int containerId, int capacity, List<ItemStack> contents,
                                      ItemStack carried, int lockedInventoryIndex)
        implements CustomPacketPayload {
    public static final Type<CircuitAlbumDataPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ElementsPlus.MOD_ID, "circuit_album_data"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CircuitAlbumDataPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeInt(payload.containerId());
                buf.writeVarInt(payload.capacity());
                ModDataComponents.ITEM_STACK_LIST_STREAM_CODEC.encode(buf, payload.contents());
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, payload.carried());
                buf.writeInt(payload.lockedInventoryIndex());
            },
            buf -> new CircuitAlbumDataPayload(
                    buf.readInt(),
                    buf.readVarInt(),
                    ModDataComponents.ITEM_STACK_LIST_STREAM_CODEC.decode(buf),
                    ItemStack.OPTIONAL_STREAM_CODEC.decode(buf),
                    buf.readInt())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
