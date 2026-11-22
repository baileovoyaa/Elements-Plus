package com.elementsplus.network;

import com.elementsplus.ElementsPlus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public record ExperimentTableScreenDataPayload(BlockPos pos, @Nullable String selectedChapter,
                                               @Nullable String selectedExperiment,
                                               Set<String> unlockedChapters, Set<String> forcedChapters,
                                               Set<String> completedExperiments,
                                               Map<String, Integer> experimentScores,
                                               long worldSeed) implements CustomPacketPayload {
    public static final Type<ExperimentTableScreenDataPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ElementsPlus.MOD_ID, "experiment_table_screen_data"));
    public static final StreamCodec<FriendlyByteBuf, ExperimentTableScreenDataPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeBlockPos(payload.pos());
                buf.writeNullable(payload.selectedChapter(), FriendlyByteBuf::writeUtf);
                buf.writeNullable(payload.selectedExperiment(), FriendlyByteBuf::writeUtf);
                buf.writeCollection(payload.unlockedChapters(), FriendlyByteBuf::writeUtf);
                buf.writeCollection(payload.forcedChapters(), FriendlyByteBuf::writeUtf);
                buf.writeCollection(payload.completedExperiments(), FriendlyByteBuf::writeUtf);
                writeScores(buf, payload.experimentScores());
                buf.writeLong(payload.worldSeed());
            },
            buf -> new ExperimentTableScreenDataPayload(
                    buf.readBlockPos(),
                    buf.readNullable(FriendlyByteBuf::readUtf),
                    buf.readNullable(FriendlyByteBuf::readUtf),
                    buf.readCollection(HashSet::new, FriendlyByteBuf::readUtf),
                    buf.readCollection(HashSet::new, FriendlyByteBuf::readUtf),
                    buf.readCollection(HashSet::new, FriendlyByteBuf::readUtf),
                    readScores(buf),
                    buf.readLong()
            )
    );

    private static void writeScores(FriendlyByteBuf buf, Map<String, Integer> scores) {
        Map<String, Integer> safe = scores == null ? Map.of() : scores;
        buf.writeVarInt(safe.size());
        for (Map.Entry<String, Integer> entry : safe.entrySet()) {
            buf.writeUtf(entry.getKey());
            buf.writeVarInt(entry.getValue());
        }
    }

    private static Map<String, Integer> readScores(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        Map<String, Integer> scores = new LinkedHashMap<>();
        for (int i = 0; i < size; i++) {
            scores.put(buf.readUtf(), buf.readVarInt());
        }
        return scores;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}