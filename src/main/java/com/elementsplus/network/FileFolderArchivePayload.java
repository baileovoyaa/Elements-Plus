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

/** 服务端 -> 客户端：一个文件夹的完整只读快照，用于跨根复制到本地。 */
public record FileFolderArchivePayload(BlockPos pos, FileRoot root, String path,
                                       List<Entry> entries) implements CustomPacketPayload {
    public record Entry(String path, boolean folder, byte[] data) {
    }

    public static final Type<FileFolderArchivePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ElementsPlus.MOD_ID, "file_folder_archive"));
    public static final StreamCodec<FriendlyByteBuf, FileFolderArchivePayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeBlockPos(payload.pos());
                        buf.writeVarInt(payload.root().ordinal());
                        buf.writeUtf(payload.path() == null ? "" : payload.path());
                        buf.writeVarInt(payload.entries().size());
                        for (Entry entry : payload.entries()) {
                            buf.writeUtf(entry.path());
                            buf.writeBoolean(entry.folder());
                            byte[] data = entry.data() == null ? new byte[0] : entry.data();
                            buf.writeVarInt(data.length);
                            buf.writeBytes(data);
                        }
                    },
                    buf -> {
                        BlockPos pos = buf.readBlockPos();
                        FileRoot root = FileRoot.byOrdinal(buf.readVarInt());
                        String path = buf.readUtf();
                        int count = buf.readVarInt();
                        List<Entry> entries = new ArrayList<>(count);
                        for (int i = 0; i < count; i++) {
                            String entryPath = buf.readUtf();
                            boolean folder = buf.readBoolean();
                            byte[] data = new byte[buf.readVarInt()];
                            buf.readBytes(data);
                            entries.add(new Entry(entryPath, folder, data));
                        }
                        return new FileFolderArchivePayload(pos, root, path, entries);
                    }
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
