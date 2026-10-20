package com.elementsplus.network;

import com.elementsplus.ElementsPlus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * 客户端向服务端同步光刻机当前标签页（设计/制造）。
 * 服务端菜单据此限制“快速移动”的目标槽位，避免设计页把物品移入不可见的制造输入槽。
 */
public record LithographyTabPayload(boolean designTab) implements CustomPacketPayload {
    public static final Type<LithographyTabPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ElementsPlus.MOD_ID, "lithography_tab"));
    public static final StreamCodec<FriendlyByteBuf, LithographyTabPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeBoolean(payload.designTab()),
            buf -> new LithographyTabPayload(buf.readBoolean())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
