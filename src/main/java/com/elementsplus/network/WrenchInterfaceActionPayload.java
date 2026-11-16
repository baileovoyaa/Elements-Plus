package com.elementsplus.network;

import com.elementsplus.ElementsPlus;
import com.elementsplus.blocks.entity.RedstoneInterfaceBlockEntity;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * 扳手对红石接口执行的操作。模式与剪贴板只存在于客户端，
 * 只有真正执行操作时才把必要数据发送到服务端。
 */
public record WrenchInterfaceActionPayload(BlockPos pos, Direction face, Action action,
                                           int pinIndex, List<String> clipboardLabels)
        implements CustomPacketPayload {

    public enum Action {
        ROTATE_CW,
        ROTATE_CCW,
        SWAP_FACE,
        MIRROR,
        ASSIGN,
        UNASSIGN,
        PASTE,
        RUN,
        PAUSE,
        RESET
    }

    public static final Type<WrenchInterfaceActionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ElementsPlus.MOD_ID, "wrench_interface_action"));
    public static final StreamCodec<FriendlyByteBuf, WrenchInterfaceActionPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeBlockPos(payload.pos());
                        buf.writeVarInt(payload.action().ordinal());
                        buf.writeVarInt(payload.face() == null ? -1 : payload.face().ordinal());
                        buf.writeVarInt(payload.pinIndex());
                        List<String> labels = payload.clipboardLabels();
                        buf.writeVarInt(labels.size());
                        for (String label : labels) {
                            buf.writeUtf(label == null ? "" : label);
                        }
                    },
                    buf -> {
                        BlockPos pos = buf.readBlockPos();
                        int actionIndex = buf.readVarInt();
                        Action action = actionIndex >= 0 && actionIndex < Action.values().length
                                ? Action.values()[actionIndex] : Action.PAUSE;
                        int faceIndex = buf.readVarInt();
                        Direction face = faceIndex >= 0 && faceIndex < Direction.values().length
                                ? Direction.values()[faceIndex] : null;
                        int pin = buf.readVarInt();
                        int size = buf.readVarInt();
                        List<String> labels = new ArrayList<>(Math.max(0, Math.min(size, 64)));
                        for (int i = 0; i < size && i < 64; i++) {
                            labels.add(buf.readUtf());
                        }
                        return new WrenchInterfaceActionPayload(pos, face, action, pin, labels);
                    }
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(WrenchInterfaceActionPayload payload, ServerPlayNetworking.Context context) {
        ServerPlayer player = context.player();
        if (!(player.level().getBlockEntity(payload.pos()) instanceof RedstoneInterfaceBlockEntity entity)) {
            return;
        }
        if (player.distanceToSqr(payload.pos().getX() + 0.5, payload.pos().getY() + 0.5,
                payload.pos().getZ() + 0.5) > 64 * 64) {
            return;
        }
        Direction face = payload.face();
        switch (payload.action()) {
            case ROTATE_CW -> {
                if (face != null) {
                    entity.rotatePins(face, true);
                    actionBar(player, Component.translatable("wrench.elements-plus.rotated", faceName(face)));
                }
            }
            case ROTATE_CCW -> {
                if (face != null) {
                    entity.rotatePins(face, false);
                    actionBar(player, Component.translatable("wrench.elements-plus.rotated", faceName(face)));
                }
            }
            case SWAP_FACE -> {
                if (face != null) {
                    entity.swapFace(face);
                    actionBar(player, Component.translatable("wrench.elements-plus.swapped", faceName(face)));
                }
            }
            case MIRROR -> {
                entity.mirrorPins();
                actionBar(player, Component.translatable("wrench.elements-plus.mirrored"));
            }
            case ASSIGN -> {
                if (face != null) {
                    RedstoneInterfaceBlockEntity.PinInfo pin = entity.getPin(payload.pinIndex());
                    if (pin != null) {
                        entity.assignPin(pin.index(), face);
                        actionBar(player, Component.translatable("wrench.elements-plus.assigned",
                                faceName(face), pinComponent(pin)));
                    }
                }
            }
            case UNASSIGN -> {
                if (face != null) {
                    entity.unassignPin(face);
                    actionBar(player, Component.translatable("wrench.elements-plus.unassigned", faceName(face)));
                }
            }
            case PASTE -> {
                boolean pasted = entity.pasteAssignments(payload.clipboardLabels());
                actionBar(player, Component.translatable(pasted
                        ? "wrench.elements-plus.pasted"
                        : "wrench.elements-plus.paste_failed"));
            }
            case RUN -> {
                entity.setRunning(true);
                actionBar(player, Component.translatable("wrench.elements-plus.running"));
            }
            case PAUSE -> {
                entity.setRunning(false);
                actionBar(player, Component.translatable("wrench.elements-plus.paused"));
            }
            case RESET -> {
                entity.resetCircuit();
                actionBar(player, Component.translatable("wrench.elements-plus.reset"));
            }
        }
    }

    private static Component faceName(Direction face) {
        return Component.translatable("gui.elements-plus.direction." + face.getSerializedName());
    }

    private static Component pinComponent(RedstoneInterfaceBlockEntity.PinInfo pin) {
        int typeColor = pin.type() == com.elementsplus.core.circuit.CircuitComponent.PinType.INPUT
                ? 0xFF4488FF : 0xFF44FF88;
        int widthColor = pin.bitWidth() >= 8 ? 0xFFFF55FF : 0xFFAAAAAA;
        String label = pin.label() == null || pin.label().isBlank() ? "?" : pin.label();
        return Component.literal("■ ").withStyle(style -> style.withColor(typeColor))
                .append(Component.literal(label).withStyle(style -> style.withColor(widthColor)));
    }

    private static void actionBar(ServerPlayer player, Component component) {
        player.displayClientMessage(component, true);
    }
}
