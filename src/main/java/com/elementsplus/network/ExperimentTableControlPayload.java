package com.elementsplus.network;

import com.elementsplus.ElementsPlus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * 客户端 -> 服务端：实验桌控制指令（开始/暂停/恢复/停止）。
 */
public record ExperimentTableControlPayload(BlockPos pos, Action action, @Nullable String experimentName) implements CustomPacketPayload {

    public enum Action {
        START,
        PAUSE,
        RESUME,
        STOP
    }

    public static final Type<ExperimentTableControlPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ElementsPlus.MOD_ID, "experiment_table_control"));
    public static final StreamCodec<FriendlyByteBuf, ExperimentTableControlPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeBlockPos(payload.pos());
                buf.writeVarInt(payload.action().ordinal());
                buf.writeNullable(payload.experimentName(), FriendlyByteBuf::writeUtf);
            },
            buf -> new ExperimentTableControlPayload(
                    buf.readBlockPos(),
                    Action.values()[buf.readVarInt()],
                    buf.readNullable(FriendlyByteBuf::readUtf)
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}