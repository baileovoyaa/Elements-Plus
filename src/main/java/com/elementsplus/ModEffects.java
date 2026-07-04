package com.elementsplus;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

public class ModEffects {
    private static final List<Holder<MobEffect>> EFFECTS = new ArrayList<>();
    private static Holder.Reference<MobEffect> registerEffect(String id, MobEffect effect) {
        Holder.Reference<MobEffect> holder = Registry.registerForHolder(
                BuiltInRegistries.MOB_EFFECT,
                ResourceLocation.fromNamespaceAndPath(ElementsPlus.MOD_ID, id),
                effect
        );
        EFFECTS.add(holder);
        return holder;
    }
    public static Holder<MobEffect> STEEL_PIPE_SHIELD;
    public static List<Holder<MobEffect>> getEffects() {
        return EFFECTS;
    }
    public static void initialize() {
        STEEL_PIPE_SHIELD = registerEffect("steel_pipe_shield",
                new MobEffect(MobEffectCategory.BENEFICIAL, 0xFFAA00) {
                    @Override
                    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
                        return true;
                    }

                    @Override
                    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
                        if (!entity.level().isClientSide() && entity instanceof Player player) {
                            player.addEffect(new MobEffectInstance(
                                    MobEffects.DAMAGE_RESISTANCE,
                                    2, 1, false, false, false
                            ));
                            player.addEffect(new MobEffectInstance(
                                    MobEffects.FIRE_RESISTANCE,
                                    2, 0, false, false, false
                            ));
                        }
                        return super.applyEffectTick(entity, amplifier);
                    }
                }
        );
        ElementsPlus.LOGGER.info("Registered {} effect(s)", EFFECTS.size());
    }
}