package com.elementsplus.network;

import com.elementsplus.ElementsPlus;
import com.elementsplus.blocks.entity.RedstoneInterfaceBlockEntity;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public record RedstoneInterfaceActionPayload(BlockPos pos, Action action, int pinIndex, Direction face)
        implements CustomPacketPayload {

    public enum Action {
        ASSIGN, UNASSIGN, RUN, PAUSE, STEP, RESET
    }

    public static final Type<RedstoneInterfaceActionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ElementsPlus.MOD_ID, "redstone_interface_action"));
    public static final StreamCodec<FriendlyByteBuf, RedstoneInterfaceActionPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> {
                        buf.writeBlockPos(payload.pos());
                        buf.writeVarInt(payload.action().ordinal());
                        buf.writeVarInt(payload.pinIndex());
                        buf.writeVarInt(payload.face() == null ? -1 : payload.face().ordinal());
                    },
                    buf -> {
                        BlockPos pos = buf.readBlockPos();
                        int actionIndex = buf.readVarInt();
                        Action action = actionIndex >= 0 && actionIndex < Action.values().length
                                ? Action.values()[actionIndex] : Action.PAUSE;
                        int pin = buf.readVarInt();
                        int face = buf.readVarInt();
                        return new RedstoneInterfaceActionPayload(pos, action, pin,
                                face < 0 || face >= Direction.values().length
                                        ? null : Direction.values()[face]);
                    }
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RedstoneInterfaceActionPayload payload, ServerPlayNetworking.Context context) {
        ServerPlayer player = context.player();
        if (!(player.level().getBlockEntity(payload.pos()) instanceof RedstoneInterfaceBlockEntity entity)) {
            return;
        }
        if (player.distanceToSqr(payload.pos().getX() + 0.5, payload.pos().getY() + 0.5,
                payload.pos().getZ() + 0.5) > 64 * 64) {
            return;
        }
        switch (payload.action()) {
            case ASSIGN -> {
                if (payload.face() != null) {
                    entity.assignPin(payload.pinIndex(), payload.face());
                }
            }
            case UNASSIGN -> {
                if (payload.face() != null) {
                    entity.unassignPin(payload.face());
                }
            }
            case RUN -> entity.setRunning(true);
            case PAUSE -> entity.setRunning(false);
            case STEP -> entity.singleStep();
            case RESET -> entity.resetCircuit();
        }
    }
}
