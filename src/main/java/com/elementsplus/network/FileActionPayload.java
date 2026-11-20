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

/**
 * 客户端 -> 服务端：文件系统写操作。
 */
public record FileActionPayload(BlockPos pos, FileRoot root, Action action, List<String> paths,
                                String target, String name, byte[] data,
                                boolean copy, boolean overwrite) implements CustomPacketPayload {
    public enum Action {
        CREATE_FILE,
        CREATE_FOLDER,
        RENAME,
        DELETE,
        /** 在本根节点内移动/复制；target 是目标目录。 */
        TRANSFER,
        /** 在本根节点内复制/移动到明确的目标路径（用于重命名粘贴）。 */
        TRANSFER_TO,
        /** 写入文件内容；paths[0] 是路径。 */
        WRITE_FILE,
        SHARE,
        UNSHARE,
        /** 把 data 写入外接设备槽位物品。 */
        PRINT,
        /** 上传电路图（.dat 内容写入未编译电路图）。 */
        UPLOAD_CIRCUIT
    }

    public static final Type<FileActionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ElementsPlus.MOD_ID, "file_action"));
    public static final StreamCodec<FriendlyByteBuf, FileActionPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeBlockPos(payload.pos());
                buf.writeVarInt(payload.root().ordinal());
                buf.writeVarInt(payload.action().ordinal());
                buf.writeVarInt(payload.paths().size());
                for (String path : payload.paths()) {
                    buf.writeUtf(path);
                }
                buf.writeUtf(payload.target() == null ? "" : payload.target());
                buf.writeUtf(payload.name() == null ? "" : payload.name());
                byte[] data = payload.data() == null ? new byte[0] : payload.data();
                buf.writeVarInt(data.length);
                buf.writeBytes(data);
                buf.writeBoolean(payload.copy());
                buf.writeBoolean(payload.overwrite());
            },
            buf -> {
                BlockPos pos = buf.readBlockPos();
                FileRoot root = FileRoot.byOrdinal(buf.readVarInt());
                Action action = Action.values()[Math.min(Action.values().length - 1, buf.readVarInt())];
                int count = buf.readVarInt();
                List<String> paths = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    paths.add(buf.readUtf());
                }
                String target = buf.readUtf();
                String name = buf.readUtf();
                byte[] data = new byte[buf.readVarInt()];
                buf.readBytes(data);
                boolean copy = buf.readBoolean();
                boolean overwrite = buf.readBoolean();
                return new FileActionPayload(pos, root, action, paths, target, name, data, copy, overwrite);
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
