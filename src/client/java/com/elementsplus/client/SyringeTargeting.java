package com.elementsplus.client;

import com.elementsplus.item.SyringeItem;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * 客户端视线目标高亮。只保存目标实体 id，由客户端渲染 Mixin 让该实体显示发光轮廓。
 *
 * <p>注意：不要通过覆盖 Entity#isCurrentlyGlowing 实现高亮，因为 LivingEntity
 * 会把该方法的返回值写回实体同步标志，导致轮廓在移开视线后仍然残留。</p>
 */
public final class SyringeTargeting {
    private static int targetId = -1;

    private SyringeTargeting() {
    }

    public static void tick(Minecraft client) {
        Player player = client.player;
        targetId = -1;
        if (player == null || !player.isAlive() || client.level == null || client.screen != null
                || ElementsPlusClient.isSyringeCharging()
                || SyringeItem.findHand(player) == null) {
            return;
        }

        double reach = player.entityInteractionRange();
        HitResult hit = ProjectileUtil.getHitResultOnViewVector(
                player,
                entity -> entity instanceof LivingEntity living
                        && !(living instanceof ArmorStand)
                        && living != player
                        && living.isAlive()
                        && !living.isSpectator(),
                reach
        );
        targetId = hit instanceof EntityHitResult entityHit ? entityHit.getEntity().getId() : -1;
    }

    public static boolean isTarget(Entity entity) {
        return targetId >= 0 && entity.getId() == targetId;
    }
}
