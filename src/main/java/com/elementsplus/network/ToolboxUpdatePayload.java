package com.elementsplus.network;

import com.elementsplus.ElementsPlus;
import com.elementsplus.player.PlayerToolbox;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ToolboxUpdatePayload(PlayerToolbox toolbox) implements CustomPacketPayload {
    public static final Type<ToolboxUpdatePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ElementsPlus.MOD_ID, "toolbox_update"));
    public static final StreamCodec<FriendlyByteBuf, ToolboxUpdatePayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> PlayerToolbox.STREAM_CODEC.encode(buf, payload.toolbox()),
            buf -> new ToolboxUpdatePayload(PlayerToolbox.STREAM_CODEC.decode(buf))
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}