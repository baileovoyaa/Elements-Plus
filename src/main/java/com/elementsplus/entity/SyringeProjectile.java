package com.elementsplus.entity;

import com.elementsplus.ModEntities;
import com.elementsplus.ModItems;
import com.elementsplus.item.SyringeItem;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.NotNull;

/**
 * 弩发射的注射器弹射物。命中生物时施加药水效果，命中方块或生物后破碎消失。
 */
public class SyringeProjectile extends ThrowableItemProjectile {
    /** 飞行 100 tick（5 秒）后药水效果时长减半。 */
    private static final double HALF_LIFE_TICKS = 100.0D;

    public SyringeProjectile(EntityType<? extends SyringeProjectile> type, Level level) {
        super(type, level);
    }

    public SyringeProjectile(Level level, LivingEntity owner, ItemStack syringe) {
        super(ModEntities.SYRINGE_PROJECTILE, owner, level);
        setItem(syringe.copyWithCount(1));
    }

    public SyringeProjectile(Level level, double x, double y, double z, ItemStack syringe) {
        super(ModEntities.SYRINGE_PROJECTILE, x, y, z, level);
        setItem(syringe.copyWithCount(1));
    }

    @Override
    protected @NotNull Item getDefaultItem() {
        return ModItems.SYRINGE;
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);

        if (level().isClientSide()) {
            discard();
            return;
        }

        if (result.getType() == HitResult.Type.ENTITY && result instanceof EntityHitResult entityHit) {
            Entity target = entityHit.getEntity();
            if (target instanceof LivingEntity living) {
                Entity source = getOwner() != null ? getOwner() : this;
                SyringeItem.applyEffects(getItem(), living, source, durationMultiplier());
            }
        }

        if (level() instanceof ServerLevel serverLevel) {
            ItemStack stack = getItem();
            serverLevel.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, stack),
                    getX(), getY(), getZ(), 10, 0.15D, 0.15D, 0.15D, 0.06D);
            serverLevel.playSound(null, getX(), getY(), getZ(),
                    SoundEvents.GLASS_BREAK, SoundSource.NEUTRAL, 0.8F, 1.25F);
        }
        discard();
    }

    /** 飞行时间越长，药水持续时间倍率越小：0.5^(tickCount / 100)。 */
    private double durationMultiplier() {
        return Math.pow(0.5D, Math.max(0, tickCount) / HALF_LIFE_TICKS);
    }
}
