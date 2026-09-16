package com.elementsplus.network;

import com.elementsplus.ElementsPlus;
import com.elementsplus.player.PlayerToolbox;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ToolboxSyncPayload(PlayerToolbox toolbox) implements CustomPacketPayload {
    public static final Type<ToolboxSyncPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ElementsPlus.MOD_ID, "toolbox_sync"));
    public static final StreamCodec<FriendlyByteBuf, ToolboxSyncPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> PlayerToolbox.STREAM_CODEC.encode(buf, payload.toolbox()),
            buf -> new ToolboxSyncPayload(PlayerToolbox.STREAM_CODEC.decode(buf))
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}