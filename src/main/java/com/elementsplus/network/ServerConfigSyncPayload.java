package com.elementsplus.network;

import com.elementsplus.ElementsPlus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** 将存档级服务端配置同步给客户端，用于与服务器一致的本地预览校验等。 */
public record ServerConfigSyncPayload(int customComponentNestingLimit,
                                      boolean allowInfiniteInFiniteCircuit,
                                      boolean asyncExperimentTests,
                                      boolean allowSurvivalCreativeComponent) implements CustomPacketPayload {
    public static final Type<ServerConfigSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ElementsPlus.MOD_ID, "server_config_sync"));
    public static final StreamCodec<FriendlyByteBuf, ServerConfigSyncPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeInt(payload.customComponentNestingLimit());
                buf.writeBoolean(payload.allowInfiniteInFiniteCircuit());
                buf.writeBoolean(payload.asyncExperimentTests());
                buf.writeBoolean(payload.allowSurvivalCreativeComponent());
            },
            buf -> new ServerConfigSyncPayload(buf.readInt(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
