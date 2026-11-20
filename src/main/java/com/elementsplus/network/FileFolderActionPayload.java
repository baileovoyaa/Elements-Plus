package com.elementsplus.network;

import com.elementsplus.ElementsPlus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** 客户端 -> 服务端：文件夹操作。 */
public record FileFolderActionPayload(int containerId, Action action, int index)
        implements CustomPacketPayload {
    public enum Action {
        INSERT_CARRIED,
        TAKE_TO_CURSOR,
        TAKE_TO_INVENTORY
    }

    public static final Type<FileFolderActionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ElementsPlus.MOD_ID, "file_folder_action"));
    public static final StreamCodec<FriendlyByteBuf, FileFolderActionPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeInt(payload.containerId());
                        buf.writeVarInt(payload.action().ordinal());
                        buf.writeVarInt(payload.index());
                    },
                    buf -> new FileFolderActionPayload(buf.readInt(),
                            Action.values()[Math.min(Action.values().length - 1, buf.readVarInt())],
                            buf.readVarInt())
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
