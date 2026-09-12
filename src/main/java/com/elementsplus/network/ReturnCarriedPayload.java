package com.elementsplus.network;

import com.elementsplus.ElementsPlus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ReturnCarriedPayload() implements CustomPacketPayload {
    public static final Type<ReturnCarriedPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ElementsPlus.MOD_ID, "return_carried"));
    public static final StreamCodec<FriendlyByteBuf, ReturnCarriedPayload> STREAM_CODEC = StreamCodec.unit(new ReturnCarriedPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}