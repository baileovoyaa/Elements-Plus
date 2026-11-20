package com.elementsplus.item;

import com.elementsplus.core.file.DiskData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * 磁盘物品：固定 256 字节容量，最大 1 堆叠。
 */
public class DiskItem extends Item {
    public DiskItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip,
                                TooltipFlag flag) {
        int size = DiskData.read(stack).length;
        tooltip.add(Component.translatable("tooltip.elements-plus.disk.size", size, DiskData.CAPACITY)
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.elements-plus.disk.hint")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
