package com.elementsplus.item;

import com.elementsplus.ModItems;
import com.elementsplus.ModPotions;
import com.elementsplus.entity.SyringeProjectile;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.ProjectileItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * 可携带药水效果的注射器。
 *
 * <p>注射器是消耗品：右键长按 1 秒注射自己或生物，左键蓄力撞击，也可以作为弩/发射器的弹药。</p>
 */
public class SyringeItem extends PotionItem implements ProjectileItem {
    /** 右键长按时间：20 tick = 1 秒。 */
    private static final int RIGHT_CLICK_USE_TICKS = 20;
    /** 原版使用动作的最大时长；实际注射由 onUseTick 自行计时。 */
    private static final int USE_DURATION = 72000;

    /** 仅服务端使用；按玩家实例记录目标与已持续时间。 */
    private static final Map<Player, UseState> USE_STATES = new IdentityHashMap<>();

    public SyringeItem(Properties properties) {
        super(properties);
    }

    private static final class UseState {
        private LivingEntity target;
        private int ticks;
        private final InteractionHand hand;

        private UseState(LivingEntity target, InteractionHand hand) {
            this.target = target;
            this.hand = hand;
        }
    }

    @Override
    public @NotNull ItemStack getDefaultInstance() {
        return PotionContents.createItemStack(this, ModPotions.RUST_PROOF);
    }

    /** 注射器默认携带防锈药水，保证旧存档中的空注射器仍然可用。 */
    public static PotionContents contents(ItemStack stack) {
        return stack.getOrDefault(DataComponents.POTION_CONTENTS, new PotionContents(ModPotions.RUST_PROOF));
    }

    public static boolean isFilled(ItemStack stack) {
        PotionContents contents = contents(stack);
        return contents.potion().isPresent() || !contents.customEffects().isEmpty();
    }

    /** 返回手持注射器的手，主手优先。 */
    @Nullable
    public static InteractionHand findHand(Player player) {
        if (player.getMainHandItem().is(ModItems.SYRINGE)) {
            return InteractionHand.MAIN_HAND;
        }
        if (player.getOffhandItem().is(ModItems.SYRINGE)) {
            return InteractionHand.OFF_HAND;
        }
        return null;
    }

    /** 开始右键长按注射。目标为 null 时按当前视线解析。 */
    public static void beginUse(Player player, InteractionHand hand, @Nullable LivingEntity target) {
        if (!player.level().isClientSide()) {
            LivingEntity resolved = target != null ? target : findUseTarget(player);
            USE_STATES.put(player, new UseState(resolved, hand));
        }
        player.startUsingItem(hand);
    }

    /** 清空玩家正在进行的注射计时。 */
    public static void clearUseState(Player player) {
        USE_STATES.remove(player);
    }

    /** 仅主手注射器可进入左键蓄力平举姿态。 */
    public static void startChargePose(Player player) {
        if (!player.getMainHandItem().is(ModItems.SYRINGE)) {
            return;
        }
        clearUseState(player);
        if (!player.getUseItem().isEmpty()) {
            player.stopUsingItem();
        }
        player.startUsingItem(InteractionHand.MAIN_HAND);
    }

    /** 左键松开时结束持矛平举姿态。 */
    public static void stopChargePose(Player player) {
        if (player.getUseItem().is(ModItems.SYRINGE)) {
            player.stopUsingItem();
        }
    }

    /**
     * 解析当前右键目标：视线内最近的可注射生物，否则返回玩家自己。
     * 该方法只在服务端用于长按计时和最终注射。
     */
    public static LivingEntity findUseTarget(Player player) {
        HitResult hit = ProjectileUtil.getHitResultOnViewVector(
                player,
                entity -> entity instanceof LivingEntity living
                        && living != player
                        && !(living instanceof ArmorStand)
                        && living.isAlive()
                        && !living.isSpectator(),
                player.entityInteractionRange()
        );
        if (hit instanceof EntityHitResult entityHit
                && entityHit.getEntity() instanceof LivingEntity living) {
            return living;
        }
        return player;
    }

    /**
     * 将物品上的药水效果施加给目标。
     *
     * @return 是否成功读取到有效药水内容
     */
    public static boolean applyEffects(ItemStack stack, LivingEntity target, @Nullable Entity source) {
        return applyEffects(stack, target, source, 1.0D);
    }

    /**
     * 将药水效果施加给目标，并按倍率缩放持续时间。
     *
     * @param durationMultiplier 持续时间倍率；弹射物会随飞行时间指数衰减
     */
    public static boolean applyEffects(ItemStack stack, LivingEntity target, @Nullable Entity source,
                                       double durationMultiplier) {
        PotionContents contents = contents(stack);
        if (!isFilled(stack)) {
            return false;
        }
        double multiplier = Math.max(0.0D, Math.min(1.0D, durationMultiplier));
        contents.forEachEffect(effect -> {
            MobEffect mobEffect = effect.getEffect().value();
            if (mobEffect.isInstantenous()) {
                mobEffect.applyInstantenousEffect(source, source, target, effect.getAmplifier(), 1.0D);
                return;
            }

            int baseDuration = effect.getDuration();
            int duration = baseDuration <= 0
                    ? baseDuration
                    : Math.max(1, (int) Math.round(baseDuration * multiplier));
            target.addEffect(new MobEffectInstance(effect.getEffect(), duration, effect.getAmplifier(),
                    effect.isAmbient(), effect.isVisible(), effect.showIcon()), source);
        });
        return true;
    }

    /** 执行一次注射并消耗一支注射器。 */
    public static boolean inject(Player player, ItemStack stack, LivingEntity target, InteractionHand hand) {
        if (!applyEffects(stack, target, player)) {
            return false;
        }
        consume(player, stack);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.HONEY_DRINK, SoundSource.PLAYERS, 0.7F, 1.35F);
        player.swing(hand, true);
        return true;
    }

    private static void consume(Player player, ItemStack stack) {
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!isFilled(stack) || SyringeChargeManager.isCharging(player)) {
            return InteractionResultHolder.pass(stack);
        }
        beginUse(player, hand, null);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public @NotNull InteractionResult interactLivingEntity(ItemStack stack, Player player,
                                                           LivingEntity target, InteractionHand hand) {
        if (target instanceof ArmorStand || !isFilled(stack) || !player.hasLineOfSight(target)
                || SyringeChargeManager.isCharging(player)) {
            return InteractionResult.PASS;
        }
        beginUse(player, hand, target);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        if (level.isClientSide() || !(entity instanceof Player player)) {
            return;
        }
        // 左键蓄力使用同一个 UseAnim 来显示平举姿态，不参与右键注射计时。
        if (SyringeChargeManager.isCharging(player)) {
            return;
        }

        UseState state = USE_STATES.get(player);
        if (state == null) {
            state = new UseState(findUseTarget(player), player.getUsedItemHand());
            USE_STATES.put(player, state);
        }

        LivingEntity currentTarget = findUseTarget(player);
        if (currentTarget != state.target) {
            // 目标变化时重新计时。
            state.target = currentTarget;
            state.ticks = 0;
            return;
        }

        state.ticks++;
        if (state.ticks < RIGHT_CLICK_USE_TICKS) {
            return;
        }

        inject(player, stack, state.target, state.hand);
        player.stopUsingItem();
        USE_STATES.remove(player);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (entity instanceof Player player) {
            USE_STATES.remove(player);
        }
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return USE_DURATION;
    }

    @Override
    public @NotNull UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.SPEAR;
    }

    @Override
    public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) {
        // 手持注射器时左键用于蓄力冲锋，不进行普通攻击/挖掘。
        return false;
    }

    @Override
    public @NotNull Projectile asProjectile(Level level, Position position, ItemStack stack, Direction direction) {
        return new SyringeProjectile(level, position.x(), position.y(), position.z(), stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        PotionContents contents = contents(stack);
        if (isFilled(stack)) {
            contents.addPotionTooltip(tooltip::add, 1.0F, 1.0F);
        }
        tooltip.add(Component.translatable("tooltip.elements-plus.syringe.one_use")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.elements-plus.syringe.usage")
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.elements-plus.syringe.projectile_decay")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public @NotNull Component getName(ItemStack stack) {
        PotionContents contents = contents(stack);
        if (contents.potion().isPresent()) {
            String potionKey = Potion.getName(contents.potion(), "item.minecraft.potion.effect.");
            return Component.translatable("item.elements-plus.syringe.filled", Component.translatable(potionKey));
        }
        return super.getName(stack);
    }
}
