package com.elementsplus.network;

import com.elementsplus.ElementsPlus;
import com.elementsplus.core.circuit.diagram.CircuitDiagram;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** 服务端拒绝电路板编辑时，把旧电路图回传给客户端。 */
public record BoardEditResultPayload(CircuitDiagram diagram) implements CustomPacketPayload {
    public static final Type<BoardEditResultPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ElementsPlus.MOD_ID, "board_edit_result"));
    public static final StreamCodec<FriendlyByteBuf, BoardEditResultPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> CircuitDiagram.PAYLOAD_STREAM_CODEC.encode(buf, payload.diagram()),
                    buf -> new BoardEditResultPayload(CircuitDiagram.PAYLOAD_STREAM_CODEC.decode(buf))
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
