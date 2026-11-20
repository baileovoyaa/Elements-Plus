package com.elementsplus.network;

import com.elementsplus.ElementsPlus;
import com.elementsplus.core.file.FileRoot;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * 服务端 -> 客户端：读取文件内容的结果（例如从外接设备复制到客户端文件）。
 */
public record FileContentPayload(BlockPos pos, FileRoot root, String path, byte[] data,
                                 Purpose purpose, String message) implements CustomPacketPayload {
    public enum Purpose {
        COPY,
        OPEN
    }

    public static final Type<FileContentPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ElementsPlus.MOD_ID, "file_content"));
    public static final StreamCodec<FriendlyByteBuf, FileContentPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeBlockPos(payload.pos());
                buf.writeVarInt(payload.root().ordinal());
                buf.writeUtf(payload.path() == null ? "" : payload.path());
                byte[] data = payload.data() == null ? new byte[0] : payload.data();
                buf.writeVarInt(data.length);
                buf.writeBytes(data);
                buf.writeVarInt(payload.purpose().ordinal());
                buf.writeUtf(payload.message() == null ? "" : payload.message());
            },
            buf -> {
                BlockPos pos = buf.readBlockPos();
                FileRoot root = FileRoot.byOrdinal(buf.readVarInt());
                String path = buf.readUtf();
                byte[] data = new byte[buf.readVarInt()];
                buf.readBytes(data);
                Purpose purpose = Purpose.values()[Math.min(Purpose.values().length - 1, buf.readVarInt())];
                return new FileContentPayload(pos, root, path, data, purpose, buf.readUtf());
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
