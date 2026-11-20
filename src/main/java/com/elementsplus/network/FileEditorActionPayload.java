package com.elementsplus.network;

import com.elementsplus.ElementsPlus;
import com.elementsplus.core.file.FileRoot;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * 客户端 -> 服务端：打开/关闭编辑器、自动保存、光标同步与模式切换。
 */
public record FileEditorActionPayload(BlockPos pos, FileRoot root, String path, Mode mode,
                                      byte[] data, int cursorStart, int cursorEnd,
                                      boolean textMode) implements CustomPacketPayload {
    public enum Mode {
        OPEN,
        CLOSE,
        /** 自动保存：把编辑器内容写回文件。 */
        UPDATE,
        /** 只同步光标位置。 */
        CURSOR,
        /** 文本 / 十六进制模式切换，用于协作显示。 */
        MODE
    }

    public static final Type<FileEditorActionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ElementsPlus.MOD_ID, "file_editor_action"));
    public static final StreamCodec<FriendlyByteBuf, FileEditorActionPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeBlockPos(payload.pos());
                buf.writeVarInt(payload.root().ordinal());
                buf.writeUtf(payload.path() == null ? "" : payload.path());
                buf.writeVarInt(payload.mode().ordinal());
                byte[] data = payload.data() == null ? new byte[0] : payload.data();
                buf.writeVarInt(data.length);
                buf.writeBytes(data);
                buf.writeVarInt(payload.cursorStart());
                buf.writeVarInt(payload.cursorEnd());
                buf.writeBoolean(payload.textMode());
            },
            buf -> {
                BlockPos pos = buf.readBlockPos();
                FileRoot root = FileRoot.byOrdinal(buf.readVarInt());
                String path = buf.readUtf();
                Mode mode = Mode.values()[Math.min(Mode.values().length - 1, buf.readVarInt())];
                byte[] data = new byte[buf.readVarInt()];
                buf.readBytes(data);
                return new FileEditorActionPayload(pos, root, path, mode, data,
                        buf.readVarInt(), buf.readVarInt(), buf.readBoolean());
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
