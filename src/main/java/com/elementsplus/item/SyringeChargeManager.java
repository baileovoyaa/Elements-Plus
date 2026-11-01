package com.elementsplus.item;

import com.elementsplus.ModItems;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 服务端蓄力状态与碰撞检测。
 *
 * <p>客户端只在按住左键的状态变化时发送包，服务端每 tick 检测是否撞到生物，
 * 从而模拟 1.21.11 长矛的蓄力突刺。</p>
 */
public final class SyringeChargeManager {
    /** 略低于玩家疾跑速度（约 0.28 方块/tick）的阈值（单位：方块/tick）。 */
    private static final double MIN_RELATIVE_SPEED = 0.20D;

    private static final Set<UUID> CHARGING = new HashSet<>();

    private SyringeChargeManager() {
    }

    public static void setCharging(ServerPlayer player, boolean charging) {
        UUID id = player.getUUID();
        if (charging) {
            CHARGING.add(id);
            SyringeItem.startChargePose(player);
        } else {
            CHARGING.remove(id);
            SyringeItem.stopChargePose(player);
        }
    }

    public static boolean isCharging(Player player) {
        return CHARGING.contains(player.getUUID());
    }

    public static void remove(ServerPlayer player) {
        CHARGING.remove(player.getUUID());
    }

    public static void tick(MinecraftServer server) {
        if (CHARGING.isEmpty()) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UUID id = player.getUUID();
            if (!CHARGING.contains(id) || !player.isAlive() || player.isSpectator()) {
                continue;
            }
            ItemStack stack = player.getMainHandItem();
            if (!stack.is(ModItems.SYRINGE) || !SyringeItem.isFilled(stack)) {
                continue;
            }
            InteractionHand hand = InteractionHand.MAIN_HAND;
            LivingEntity target = findTouchingTarget(player);
            if (target == null) {
                continue;
            }
            double relativeSpeed = player.getDeltaMovement().subtract(target.getDeltaMovement()).length();
            if (relativeSpeed < MIN_RELATIVE_SPEED) {
                continue;
            }
            if (SyringeItem.inject(player, stack, target, hand)) {
                // 左键命中后结束本次蓄力和平举姿态，需要松开左键后重新蓄力。
                CHARGING.remove(id);
                SyringeItem.clearUseState(player);
                player.stopUsingItem();
            }
        }
    }

    private static LivingEntity findTouchingTarget(ServerPlayer player) {
        AABB box = player.getBoundingBox().inflate(0.35D, 0.2D, 0.35D);
        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Entity entity : player.level().getEntities(player, box, SyringeChargeManager::canHit)) {
            if (!(entity instanceof LivingEntity living)) {
                continue;
            }
            double distance = player.distanceToSqr(living);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = living;
            }
        }
        return best;
    }

    private static boolean canHit(Entity entity) {
        return entity instanceof LivingEntity living
                && !(living instanceof ArmorStand)
                && living.isAlive()
                && !living.isSpectator();
    }
}
