package com.elementsplus.network;

import com.elementsplus.ElementsPlus;
import com.elementsplus.core.circuit.CircuitComponent;
import com.elementsplus.core.circuit.storage.StoredCircuit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** 客户端 -> 服务端：提交自定义元件配置并编译。 */
public record CustomComponentCompilePayload(BlockPos pos,
                                            String name,
                                            String description,
                                            @Nullable ResourceLocation iconItem,
                                            @Nullable String iconChar,
                                            int color,
                                            int width,
                                            int height,
                                            List<StoredCircuit.PinBinding> pins) implements CustomPacketPayload {

    public static final Type<CustomComponentCompilePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ElementsPlus.MOD_ID, "custom_component_compile"));

    public static final StreamCodec<FriendlyByteBuf, CustomComponentCompilePayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeBlockPos(payload.pos());
                buf.writeUtf(payload.name());
                buf.writeUtf(payload.description());
                buf.writeBoolean(payload.iconItem() != null);
                if (payload.iconItem() != null) {
                    ResourceLocation.STREAM_CODEC.encode(buf, payload.iconItem());
                }
                buf.writeBoolean(payload.iconChar() != null);
                if (payload.iconChar() != null) {
                    buf.writeUtf(payload.iconChar());
                }
                buf.writeInt(payload.color());
                buf.writeVarInt(payload.width());
                buf.writeVarInt(payload.height());
                buf.writeVarInt(payload.pins().size());
                for (StoredCircuit.PinBinding pin : payload.pins()) {
                    buf.writeVarInt(pin.side().ordinal());
                    buf.writeVarInt(pin.offset());
                    buf.writeVarInt(pin.type().ordinal());
                    buf.writeVarInt(pin.bitWidth());
                    buf.writeUtf(pin.label());
                }
            },
            buf -> {
                BlockPos pos = buf.readBlockPos();
                String name = buf.readUtf();
                String description = buf.readUtf();
                ResourceLocation iconItem = buf.readBoolean() ? ResourceLocation.STREAM_CODEC.decode(buf) : null;
                String iconChar = buf.readBoolean() ? buf.readUtf() : null;
                int color = buf.readInt();
                int width = buf.readVarInt();
                int height = buf.readVarInt();
                int count = buf.readVarInt();
                List<StoredCircuit.PinBinding> pins = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    Direction side = Direction.values()[buf.readVarInt()];
                    int offset = buf.readVarInt();
                    CircuitComponent.PinType type = CircuitComponent.PinType.values()[buf.readVarInt()];
                    int bitWidth = buf.readVarInt();
                    String label = buf.readUtf();
                    pins.add(new StoredCircuit.PinBinding(side, offset, type, bitWidth, label));
                }
                return new CustomComponentCompilePayload(pos, name, description, iconItem, iconChar,
                        color, width, height, pins);
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
