package com.elementsplus.item;

import com.elementsplus.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * 二进制打孔文件物品：固定 256 字节容量，最大 1 堆叠。
 */
public class PunchedFileItem extends Item {
    public static final int CAPACITY = 256;

    public PunchedFileItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static byte[] getData(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return new byte[0];
        }
        byte[] data = stack.get(ModDataComponents.FILE_DATA);
        return data == null ? new byte[0] : data.clone();
    }

    public static boolean setData(ItemStack stack, byte[] data) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        byte[] content = data == null ? new byte[0] : data;
        if (content.length > CAPACITY) {
            return false;
        }
        stack.set(ModDataComponents.FILE_DATA, content.clone());
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip,
                                TooltipFlag flag) {
        int size = getData(stack).length;
        tooltip.add(Component.translatable("tooltip.elements-plus.file.size", size)
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.elements-plus.punched_file.capacity", CAPACITY)
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
