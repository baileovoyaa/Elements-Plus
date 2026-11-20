package com.elementsplus.item;

import com.elementsplus.ModDataComponents;
import com.elementsplus.core.config.ElementsPlusServerConfig;
import com.elementsplus.core.file.FileText;
import com.elementsplus.network.OpenTextFilePayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 文本文件物品：内容为 UTF-8 文本，最大 1 堆叠。容量由服务端配置决定。
 */
public class TextFileItem extends Item {
    public TextFileItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static byte[] getData(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return new byte[0];
        }
        byte[] data = stack.get(ModDataComponents.FILE_DATA);
        return data == null ? new byte[0] : data.clone();
    }

    public static int maxBytes() {
        return ElementsPlusServerConfig.textFileMaxBytes();
    }

    /** 写入文本内容；超过容量时失败。 */
    public static boolean setData(ItemStack stack, byte[] data) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        byte[] content = data == null ? new byte[0] : data;
        if (content.length > maxBytes()) {
            return false;
        }
        stack.set(ModDataComponents.FILE_DATA, content.clone());
        return true;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player,
                                                          InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            String text = FileText.decodeLossless(getData(stack));
            if (text == null) {
                serverPlayer.displayClientMessage(Component.translatable(
                        "gui.elements-plus.file.error.not_text"), true);
            } else {
                ServerPlayNetworking.send(serverPlayer, new OpenTextFilePayload(
                        stack.getHoverName().getString(), text));
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip,
                                TooltipFlag flag) {
        int size = getData(stack).length;
        tooltip.add(Component.translatable("tooltip.elements-plus.file.size", size)
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.elements-plus.text_file.capacity", maxBytes())
                .withStyle(ChatFormatting.DARK_GRAY));
        String text = FileText.decodeLossless(getData(stack));
        if (text != null && !text.isEmpty()) {
            String preview = text.lines().findFirst().orElse("");
            if (preview.length() > 40) {
                preview = preview.substring(0, 40) + "...";
            }
            tooltip.add(Component.literal(preview).withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
