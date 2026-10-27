package com.elementsplus.network;

import com.elementsplus.ElementsPlus;
import com.elementsplus.blocks.entity.ExperimentStatus;
import com.elementsplus.core.experiment.ExperimentFailureInfo;
import com.elementsplus.core.experiment.SequentialFailureInfo;
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
 * failureInfo 为失败测例的原始数值；客户端会按本地数字格式重新生成失败信息文本。
 */
public record ExperimentTableStatusPayload(BlockPos pos, ExperimentStatus status, boolean paused,
                                           @Nullable String experimentName, float progress,
                                           List<Integer> testResults, List<String> tooltipLines,
                                           @Nullable ExperimentFailureInfo failureInfo,
                                           int currentTestIndex,
                                           @Nullable SequentialFailureInfo sequentialFailure) implements CustomPacketPayload {

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
                writeFailureInfo(buf, payload.failureInfo());
                buf.writeInt(payload.currentTestIndex());
                writeSequentialFailure(buf, payload.sequentialFailure());
            },
            buf -> new ExperimentTableStatusPayload(
                    buf.readBlockPos(),
                    ExperimentStatus.values()[buf.readVarInt()],
                    buf.readBoolean(),
                    buf.readNullable(FriendlyByteBuf::readUtf),
                    buf.readFloat(),
                    buf.readCollection(ArrayList::new, FriendlyByteBuf::readVarInt),
                    buf.readCollection(ArrayList::new, FriendlyByteBuf::readUtf),
                    readFailureInfo(buf),
                    buf.readInt(),
                    readSequentialFailure(buf)
            )
    );

    public ExperimentTableStatusPayload(BlockPos pos, ExperimentStatus status, boolean paused,
                                        @Nullable String experimentName, float progress,
                                        List<Integer> testResults, List<String> tooltipLines) {
        this(pos, status, paused, experimentName, progress, testResults, tooltipLines, null, -1, null);
    }

    private static void writeSequentialFailure(FriendlyByteBuf buf, @Nullable SequentialFailureInfo info) {
        buf.writeBoolean(info != null);
        if (info == null) {
            return;
        }
        buf.writeVarInt(info.testIndex());
        buf.writeVarInt(info.totalTicks());
        buf.writeVarInt(info.failedTick());
        buf.writeCollection(info.rows(), (b, row) -> {
            b.writeVarInt(row.tick());
            writePins(b, row.inputs());
            writePins(b, row.expected());
            writePins(b, row.actual());
        });
    }

    private static SequentialFailureInfo readSequentialFailure(FriendlyByteBuf buf) {
        if (!buf.readBoolean()) {
            return null;
        }
        int testIndex = buf.readVarInt();
        int totalTicks = buf.readVarInt();
        int failedTick = buf.readVarInt();
        List<SequentialFailureInfo.Row> rows = buf.readCollection(ArrayList::new, b ->
                new SequentialFailureInfo.Row(
                        b.readVarInt(),
                        readPins(b),
                        readPins(b),
                        readPins(b)));
        return new SequentialFailureInfo(testIndex, totalTicks, failedTick, rows);
    }

    private static void writeFailureInfo(FriendlyByteBuf buf, @Nullable ExperimentFailureInfo info) {
        buf.writeBoolean(info != null);
        if (info == null) {
            return;
        }
        writePins(buf, info.inputs());
        writePins(buf, info.expected());
        writePins(buf, info.actual());
    }

    private static ExperimentFailureInfo readFailureInfo(FriendlyByteBuf buf) {
        if (!buf.readBoolean()) {
            return null;
        }
        List<ExperimentFailureInfo.Pin> inputs = readPins(buf);
        List<ExperimentFailureInfo.Pin> expected = readPins(buf);
        List<ExperimentFailureInfo.Pin> actual = readPins(buf);
        return new ExperimentFailureInfo(inputs, expected, actual);
    }

    private static void writePins(FriendlyByteBuf buf, List<ExperimentFailureInfo.Pin> pins) {
        buf.writeCollection(pins, (b, pin) -> {
            b.writeUtf(pin.label());
            b.writeVarInt(pin.bitWidth());
            b.writeVarInt(pin.values().length);
            for (byte value : pin.values()) {
                b.writeByte(value);
            }
        });
    }

    private static List<ExperimentFailureInfo.Pin> readPins(FriendlyByteBuf buf) {
        return buf.readCollection(ArrayList::new, b -> {
            String label = b.readUtf();
            int bitWidth = b.readVarInt();
            int length = b.readVarInt();
            byte[] values = new byte[length];
            for (int i = 0; i < length; i++) {
                values[i] = b.readByte();
            }
            return new ExperimentFailureInfo.Pin(label, bitWidth, values);
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
