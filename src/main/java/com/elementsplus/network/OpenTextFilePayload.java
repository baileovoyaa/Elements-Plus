package com.elementsplus.network;

import com.elementsplus.ElementsPlus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** 服务端 -> 客户端：打开文本文件只读阅读界面。 */
public record OpenTextFilePayload(String title, String text) implements CustomPacketPayload {
    public static final Type<OpenTextFilePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ElementsPlus.MOD_ID, "open_text_file"));
    public static final StreamCodec<FriendlyByteBuf, OpenTextFilePayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeUtf(payload.title() == null ? "" : payload.title(), 256);
                        buf.writeUtf(payload.text() == null ? "" : payload.text(), 262144);
                    },
                    buf -> new OpenTextFilePayload(buf.readUtf(256), buf.readUtf(262144))
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
