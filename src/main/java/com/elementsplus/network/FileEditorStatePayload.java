package com.elementsplus.network;

import com.elementsplus.ElementsPlus;
import com.elementsplus.core.file.FileRoot;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 服务端 -> 客户端：编辑器内容、修订号与所有正在编辑的玩家。
 */
public record FileEditorStatePayload(BlockPos pos, FileRoot root, String path, byte[] data,
                                     long revision, List<EditorInfo> editors, boolean readOnly,
                                     int maxBytes, String errorKey, boolean reopen)
        implements CustomPacketPayload {
    public record EditorInfo(UUID player, String name, int start, int end, boolean textMode) {
    }

    public static final Type<FileEditorStatePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ElementsPlus.MOD_ID, "file_editor_state"));
    public static final StreamCodec<FriendlyByteBuf, FileEditorStatePayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeBlockPos(payload.pos());
                buf.writeVarInt(payload.root().ordinal());
                buf.writeUtf(payload.path() == null ? "" : payload.path());
                byte[] data = payload.data() == null ? new byte[0] : payload.data();
                buf.writeVarInt(data.length);
                buf.writeBytes(data);
                buf.writeLong(payload.revision());
                buf.writeVarInt(payload.editors().size());
                for (EditorInfo editor : payload.editors()) {
                    buf.writeUUID(editor.player());
                    buf.writeUtf(editor.name());
                    buf.writeVarInt(editor.start());
                    buf.writeVarInt(editor.end());
                    buf.writeBoolean(editor.textMode());
                }
                buf.writeBoolean(payload.readOnly());
                buf.writeInt(payload.maxBytes());
                buf.writeUtf(payload.errorKey() == null ? "" : payload.errorKey());
                buf.writeBoolean(payload.reopen());
            },
            buf -> {
                BlockPos pos = buf.readBlockPos();
                FileRoot root = FileRoot.byOrdinal(buf.readVarInt());
                String path = buf.readUtf();
                byte[] data = new byte[buf.readVarInt()];
                buf.readBytes(data);
                long revision = buf.readLong();
                int count = buf.readVarInt();
                List<EditorInfo> editors = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    editors.add(new EditorInfo(buf.readUUID(), buf.readUtf(), buf.readVarInt(),
                            buf.readVarInt(), buf.readBoolean()));
                }
                boolean readOnly = buf.readBoolean();
                int maxBytes = buf.readInt();
                String errorKey = buf.readUtf();
                boolean reopen = buf.readBoolean();
                return new FileEditorStatePayload(pos, root, path, data, revision, editors,
                        readOnly, maxBytes, errorKey, reopen);
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
