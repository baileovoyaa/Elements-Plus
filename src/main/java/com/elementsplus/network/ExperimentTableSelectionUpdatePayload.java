package com.elementsplus.network;

import com.elementsplus.ElementsPlus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public record ExperimentTableSelectionUpdatePayload(BlockPos pos, @Nullable String chapterName, @Nullable String experimentName) implements CustomPacketPayload {
    public static final Type<ExperimentTableSelectionUpdatePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ElementsPlus.MOD_ID, "experiment_table_selection_update"));
    public static final StreamCodec<FriendlyByteBuf, ExperimentTableSelectionUpdatePayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeBlockPos(payload.pos());
                buf.writeNullable(payload.chapterName(), FriendlyByteBuf::writeUtf);
                buf.writeNullable(payload.experimentName(), FriendlyByteBuf::writeUtf);
            },
            buf -> new ExperimentTableSelectionUpdatePayload(buf.readBlockPos(), buf.readNullable(FriendlyByteBuf::readUtf), buf.readNullable(FriendlyByteBuf::readUtf))
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}