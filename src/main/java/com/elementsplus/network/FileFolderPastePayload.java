package com.elementsplus.network;

import com.elementsplus.ElementsPlus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/** 客户端 -> 服务端：把整个文件夹写入实验桌内存。 */
public record FileFolderPastePayload(BlockPos pos, String targetDir, String folderName,
                                     List<FileFolderArchivePayload.Entry> entries,
                                     boolean overwrite) implements CustomPacketPayload {
    public static final Type<FileFolderPastePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ElementsPlus.MOD_ID, "file_folder_paste"));
    public static final StreamCodec<FriendlyByteBuf, FileFolderPastePayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeBlockPos(payload.pos());
                        buf.writeUtf(payload.targetDir() == null ? "" : payload.targetDir());
                        buf.writeUtf(payload.folderName() == null ? "" : payload.folderName());
                        buf.writeVarInt(payload.entries().size());
                        for (FileFolderArchivePayload.Entry entry : payload.entries()) {
                            buf.writeUtf(entry.path());
                            buf.writeBoolean(entry.folder());
                            byte[] data = entry.data() == null ? new byte[0] : entry.data();
                            buf.writeVarInt(data.length);
                            buf.writeBytes(data);
                        }
                        buf.writeBoolean(payload.overwrite());
                    },
                    buf -> {
                        BlockPos pos = buf.readBlockPos();
                        String targetDir = buf.readUtf();
                        String folderName = buf.readUtf();
                        int count = buf.readVarInt();
                        List<FileFolderArchivePayload.Entry> entries = new ArrayList<>(count);
                        for (int i = 0; i < count; i++) {
                            String path = buf.readUtf();
                            boolean folder = buf.readBoolean();
                            byte[] data = new byte[buf.readVarInt()];
                            buf.readBytes(data);
                            entries.add(new FileFolderArchivePayload.Entry(path, folder, data));
                        }
                        return new FileFolderPastePayload(pos, targetDir, folderName, entries,
                                buf.readBoolean());
                    }
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
