package com.elementsplus.network;

import com.elementsplus.ElementsPlus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** 客户端 -> 服务端：请求当前电路图册内容。 */
public record CircuitAlbumRequestPayload(int containerId) implements CustomPacketPayload {
    public static final Type<CircuitAlbumRequestPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ElementsPlus.MOD_ID, "circuit_album_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CircuitAlbumRequestPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> buf.writeInt(payload.containerId()),
                    buf -> new CircuitAlbumRequestPayload(buf.readInt())
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
