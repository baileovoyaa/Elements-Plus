package com.elementsplus.item;

import com.elementsplus.menu.CircuitAlbumMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

/** 电路图册：右键打开电路图收纳界面。 */
public class CircuitAlbumItem extends Item {
    public CircuitAlbumItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new SimpleMenuProvider(
                    (id, inventory, p) -> new CircuitAlbumMenu(id, inventory, hand),
                    Component.translatable("container.elements-plus.circuit_album")));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
