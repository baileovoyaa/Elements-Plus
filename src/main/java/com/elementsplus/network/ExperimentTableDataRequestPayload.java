package com.elementsplus.network;

import com.elementsplus.ElementsPlus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ExperimentTableDataRequestPayload() implements CustomPacketPayload {
    public static final Type<ExperimentTableDataRequestPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ElementsPlus.MOD_ID, "experiment_table_data_request"));
    public static final StreamCodec<FriendlyByteBuf, ExperimentTableDataRequestPayload> STREAM_CODEC = StreamCodec.unit(new ExperimentTableDataRequestPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}