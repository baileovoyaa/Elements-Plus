package com.elementsplus.network;

import com.elementsplus.ElementsPlus;
import com.elementsplus.core.file.FileEntry;
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
 * 服务端 -> 客户端：一个目录（或外接设备/共享空间的平铺列表）的内容。
 */
public record FileSystemDataPayload(BlockPos pos, FileRoot root, String path, List<FileEntry> entries,
                                    int memoryUsed, int memoryCapacity,
                                    int sharedBySelf, int sharedLimit,
                                    List<String> sharedPaths,
                                    List<PathEditors> pathEditors) implements CustomPacketPayload {
    /** 某个文件正在被哪些玩家编辑。 */
    public record PathEditors(String path, String names) {
    }

    public static final Type<FileSystemDataPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ElementsPlus.MOD_ID, "file_system_data"));
    public static final StreamCodec<FriendlyByteBuf, FileSystemDataPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeBlockPos(payload.pos());
                buf.writeVarInt(payload.root().ordinal());
                buf.writeUtf(payload.path() == null ? "" : payload.path());
                buf.writeVarInt(payload.entries().size());
                for (FileEntry entry : payload.entries()) {
                    writeEntry(buf, entry);
                }
                buf.writeVarInt(payload.memoryUsed());
                buf.writeVarInt(payload.memoryCapacity());
                buf.writeVarInt(payload.sharedBySelf());
                buf.writeVarInt(payload.sharedLimit());
                buf.writeVarInt(payload.sharedPaths().size());
                for (String shared : payload.sharedPaths()) {
                    buf.writeUtf(shared);
                }
                buf.writeVarInt(payload.pathEditors().size());
                for (PathEditors editor : payload.pathEditors()) {
                    buf.writeUtf(editor.path());
                    buf.writeUtf(editor.names());
                }
            },
            buf -> {
                BlockPos pos = buf.readBlockPos();
                FileRoot root = FileRoot.byOrdinal(buf.readVarInt());
                String path = buf.readUtf();
                int count = buf.readVarInt();
                List<FileEntry> entries = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    entries.add(readEntry(buf));
                }
                int used = buf.readVarInt();
                int capacity = buf.readVarInt();
                int sharedBySelf = buf.readVarInt();
                int sharedLimit = buf.readVarInt();
                int sharedCount = buf.readVarInt();
                List<String> sharedPaths = new ArrayList<>(sharedCount);
                for (int i = 0; i < sharedCount; i++) {
                    sharedPaths.add(buf.readUtf());
                }
                int editorCount = buf.readVarInt();
                List<PathEditors> pathEditors = new ArrayList<>(editorCount);
                for (int i = 0; i < editorCount; i++) {
                    pathEditors.add(new PathEditors(buf.readUtf(), buf.readUtf()));
                }
                return new FileSystemDataPayload(pos, root, path, entries, used, capacity,
                        sharedBySelf, sharedLimit, sharedPaths, pathEditors);
            }
    );

    public static void writeEntry(FriendlyByteBuf buf, FileEntry entry) {
        buf.writeUtf(entry.path());
        buf.writeUtf(entry.name());
        buf.writeBoolean(entry.folder());
        buf.writeVarInt(entry.size());
        buf.writeBoolean(entry.ownerId() != null);
        if (entry.ownerId() != null) {
            buf.writeUUID(entry.ownerId());
        }
        buf.writeUtf(entry.ownerName());
        buf.writeBoolean(entry.editable());
        buf.writeBoolean(entry.shared());
        buf.writeBoolean(entry.editing());
    }

    public static FileEntry readEntry(FriendlyByteBuf buf) {
        String path = buf.readUtf();
        String name = buf.readUtf();
        boolean folder = buf.readBoolean();
        int size = buf.readVarInt();
        UUID owner = buf.readBoolean() ? buf.readUUID() : null;
        String ownerName = buf.readUtf();
        boolean editable = buf.readBoolean();
        boolean shared = buf.readBoolean();
        boolean editing = buf.readBoolean();
        return new FileEntry(path, name, folder, size, owner, ownerName, editable, shared, editing);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
