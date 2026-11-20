package com.elementsplus.item;

import com.elementsplus.ModDataComponents;
import com.elementsplus.ModMenuTypes;
import com.elementsplus.menu.PaperTapeReaderMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 纸带阅读器：右键打开 GUI，GUI 中可放入打孔文件。
 */
public class PaperTapeReaderItem extends Item {
    public PaperTapeReaderItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static ItemStack getContents(ItemStack reader) {
        if (reader == null || reader.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack contents = reader.get(ModDataComponents.PAPER_TAPE_CONTENTS);
        // 必须返回副本：Slot/Container 取出内容时会 split/修改返回的 ItemStack。
        // 直接返回组件实例会让复制出的阅读器共享同一个嵌套堆叠，取出后组件中
        // 留下 count=0 的非法堆叠，最终在保存玩家数据时崩溃。
        return contents == null || contents.isEmpty() ? ItemStack.EMPTY : contents.copy();
    }

    /** 纸带阅读器只读：读出打孔文件内容；没有文件时为 0 字节。 */
    public static byte[] getData(ItemStack reader) {
        ItemStack contents = getContents(reader);
        return contents.isEmpty() ? new byte[0] : PunchedFileItem.getData(contents);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(new SimpleMenuProvider(
                    (containerId, inventory, p) -> new PaperTapeReaderMenu(containerId, inventory, hand),
                    Component.translatable("container.elements-plus.paper_tape_reader")));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip,
                                TooltipFlag flag) {
        ItemStack contents = getContents(stack);
        if (contents.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.elements-plus.paper_tape_reader.empty")
                    .withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("tooltip.elements-plus.paper_tape_reader.loaded",
                            contents.getHoverName(), PunchedFileItem.getData(contents).length)
                    .withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable("tooltip.elements-plus.paper_tape_reader.readonly")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
