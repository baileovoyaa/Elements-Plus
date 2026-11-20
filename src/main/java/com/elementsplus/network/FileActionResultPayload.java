package com.elementsplus.network;

import com.elementsplus.ElementsPlus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * 服务端 -> 客户端：文件操作结果。失败原因显示在右上角状态图标与控制台中，
 * 客户端收到失败结果时播放失败音效。
 */
public record FileActionResultPayload(BlockPos pos, boolean success, String messageKey,
                                      List<String> args, List<Integer> errorLines,
                                      String path) implements CustomPacketPayload {
    public static final Type<FileActionResultPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ElementsPlus.MOD_ID, "file_action_result"));
    public static final StreamCodec<FriendlyByteBuf, FileActionResultPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeBlockPos(payload.pos());
                buf.writeBoolean(payload.success());
                buf.writeUtf(payload.messageKey() == null ? "" : payload.messageKey());
                buf.writeVarInt(payload.args().size());
                for (String arg : payload.args()) {
                    buf.writeUtf(arg);
                }
                buf.writeVarInt(payload.errorLines().size());
                for (int line : payload.errorLines()) {
                    buf.writeVarInt(line);
                }
                buf.writeUtf(payload.path() == null ? "" : payload.path());
            },
            buf -> {
                BlockPos pos = buf.readBlockPos();
                boolean success = buf.readBoolean();
                String messageKey = buf.readUtf();
                int argCount = buf.readVarInt();
                List<String> args = new ArrayList<>(argCount);
                for (int i = 0; i < argCount; i++) {
                    args.add(buf.readUtf());
                }
                int lineCount = buf.readVarInt();
                List<Integer> lines = new ArrayList<>(lineCount);
                for (int i = 0; i < lineCount; i++) {
                    lines.add(buf.readVarInt());
                }
                return new FileActionResultPayload(pos, success, messageKey, args, lines, buf.readUtf());
            }
    );

    /** 成功通知，不需要额外消息。 */
    public static FileActionResultPayload ok(BlockPos pos, String path) {
        return new FileActionResultPayload(pos, true, "", List.of(), List.of(), path);
    }

    public static FileActionResultPayload fail(BlockPos pos, String path, String messageKey, String... args) {
        return new FileActionResultPayload(pos, false, messageKey, List.of(args), List.of(), path);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
