package com.elementsplus.network;

import com.elementsplus.ElementsPlus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** 客户端 -> 服务端：上报光刻机界面中完成的进度判定。 */
public record AdvancementActionPayload(String advancement, String criterion) implements CustomPacketPayload {
    public static final Type<AdvancementActionPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ElementsPlus.MOD_ID, "advancement_action"));

    public static final StreamCodec<FriendlyByteBuf, AdvancementActionPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeUtf(payload.advancement());
                buf.writeUtf(payload.criterion());
            },
            buf -> new AdvancementActionPayload(buf.readUtf(), buf.readUtf())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
