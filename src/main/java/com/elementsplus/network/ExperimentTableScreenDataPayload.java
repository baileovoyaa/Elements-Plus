package com.elementsplus.network;

import com.elementsplus.ElementsPlus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

public record ExperimentTableScreenDataPayload(BlockPos pos, @Nullable String selectedChapter, @Nullable String selectedExperiment, Set<String> unlockedChapters) implements CustomPacketPayload {
    public static final Type<ExperimentTableScreenDataPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ElementsPlus.MOD_ID, "experiment_table_screen_data"));
    public static final StreamCodec<FriendlyByteBuf, ExperimentTableScreenDataPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeBlockPos(payload.pos());
                buf.writeNullable(payload.selectedChapter(), FriendlyByteBuf::writeUtf);
                buf.writeNullable(payload.selectedExperiment(), FriendlyByteBuf::writeUtf);
                buf.writeCollection(payload.unlockedChapters(), FriendlyByteBuf::writeUtf);
            },
            buf -> new ExperimentTableScreenDataPayload(
                    buf.readBlockPos(),
                    buf.readNullable(FriendlyByteBuf::readUtf),
                    buf.readNullable(FriendlyByteBuf::readUtf),
                    buf.readCollection(HashSet::new, FriendlyByteBuf::readUtf)
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}