package com.elementsplus.network;

import com.elementsplus.ElementsPlus;
import com.elementsplus.core.circuit.diagram.CircuitDiagram;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record UpdateCircuitDiagramPayload(CircuitDiagram diagram) implements CustomPacketPayload {
    public static final Type<UpdateCircuitDiagramPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ElementsPlus.MOD_ID, "update_circuit_diagram"));
    public static final StreamCodec<FriendlyByteBuf, UpdateCircuitDiagramPayload> STREAM_CODEC = StreamCodec.of(
            UpdateCircuitDiagramPayload::write,
            UpdateCircuitDiagramPayload::read
    );

    private static void write(FriendlyByteBuf buf, UpdateCircuitDiagramPayload payload) {
        CircuitDiagram.PAYLOAD_STREAM_CODEC.encode(buf, payload.diagram());
    }

    private static UpdateCircuitDiagramPayload read(FriendlyByteBuf buf) {
        return new UpdateCircuitDiagramPayload(CircuitDiagram.PAYLOAD_STREAM_CODEC.decode(buf));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}