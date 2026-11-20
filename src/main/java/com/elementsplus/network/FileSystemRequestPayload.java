package com.elementsplus.network;

import com.elementsplus.ElementsPlus;
import com.elementsplus.core.file.FileRoot;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * 客户端 -> 服务端：请求文件系统列表或读取文件内容。
 */
public record FileSystemRequestPayload(BlockPos pos, FileRoot root, String path,
                                       Request request) implements CustomPacketPayload {
    public enum Request {
        LIST,
        READ,
        FOLDER
    }

    public static final Type<FileSystemRequestPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ElementsPlus.MOD_ID, "file_system_request"));
    public static final StreamCodec<FriendlyByteBuf, FileSystemRequestPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeBlockPos(payload.pos());
                buf.writeVarInt(payload.root().ordinal());
                buf.writeUtf(payload.path() == null ? "" : payload.path());
                buf.writeVarInt(payload.request().ordinal());
            },
            buf -> new FileSystemRequestPayload(
                    buf.readBlockPos(),
                    FileRoot.byOrdinal(buf.readVarInt()),
                    buf.readUtf(),
                    Request.values()[Math.min(Request.values().length - 1, buf.readVarInt())])
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
