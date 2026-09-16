package com.elementsplus.network;

import com.elementsplus.ElementsPlus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ToolboxRequestPayload() implements CustomPacketPayload {
    public static final Type<ToolboxRequestPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ElementsPlus.MOD_ID, "toolbox_request"));
    public static final StreamCodec<FriendlyByteBuf, ToolboxRequestPayload> STREAM_CODEC = StreamCodec.unit(new ToolboxRequestPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}