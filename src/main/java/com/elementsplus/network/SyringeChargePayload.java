package com.elementsplus.network;

import com.elementsplus.ElementsPlus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** 客户端 -> 服务端：同步玩家是否正在按住左键蓄力注射器。 */
public record SyringeChargePayload(boolean charging) implements CustomPacketPayload {
    public static final Type<SyringeChargePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ElementsPlus.MOD_ID, "syringe_charge"));

    public static final StreamCodec<FriendlyByteBuf, SyringeChargePayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeBoolean(payload.charging()),
            buf -> new SyringeChargePayload(buf.readBoolean())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
