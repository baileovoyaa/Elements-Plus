package com.elementsplus.network;

import com.elementsplus.ElementsPlus;
import com.elementsplus.blocks.entity.ExperimentStatus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 服务端 -> 客户端：实验桌状态同步。
 * tooltipLines 为 JSON 序列化的文本组件（由客户端解码），避免 Codec 层需要注册表上下文。
 */
public record ExperimentTableStatusPayload(BlockPos pos, ExperimentStatus status, boolean paused, @Nullable String experimentName, float progress, List<Integer> testResults, List<String> tooltipLines) implements CustomPacketPayload {

    public static final Type<ExperimentTableStatusPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ElementsPlus.MOD_ID, "experiment_table_status"));
    public static final StreamCodec<FriendlyByteBuf, ExperimentTableStatusPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeBlockPos(payload.pos());
                buf.writeVarInt(payload.status().ordinal());
                buf.writeBoolean(payload.paused());
                buf.writeNullable(payload.experimentName(), FriendlyByteBuf::writeUtf);
                buf.writeFloat(payload.progress());
                buf.writeCollection(payload.testResults(), FriendlyByteBuf::writeVarInt);
                buf.writeCollection(payload.tooltipLines(), FriendlyByteBuf::writeUtf);
            },
            buf -> new ExperimentTableStatusPayload(
                    buf.readBlockPos(),
                    ExperimentStatus.values()[buf.readVarInt()],
                    buf.readBoolean(),
                    buf.readNullable(FriendlyByteBuf::readUtf),
                    buf.readFloat(),
                    buf.readCollection(ArrayList::new, FriendlyByteBuf::readVarInt),
                    buf.readCollection(ArrayList::new, FriendlyByteBuf::readUtf)
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}