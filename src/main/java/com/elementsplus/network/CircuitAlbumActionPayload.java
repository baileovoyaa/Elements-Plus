package com.elementsplus.network;

import com.elementsplus.ElementsPlus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** 客户端 -> 服务端：对电路图册列表执行放入/取出操作。 */
public record CircuitAlbumActionPayload(int containerId, Action action, int index)
        implements CustomPacketPayload {
    public enum Action {
        INSERT_CARRIED,
        TAKE_TO_CURSOR,
        TAKE_TO_INVENTORY
    }

    public static final Type<CircuitAlbumActionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ElementsPlus.MOD_ID, "circuit_album_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CircuitAlbumActionPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeInt(payload.containerId());
                        buf.writeEnum(payload.action());
                        buf.writeInt(payload.index());
                    },
                    buf -> new CircuitAlbumActionPayload(
                            buf.readInt(), buf.readEnum(Action.class), buf.readInt())
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
