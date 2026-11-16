package com.elementsplus.item;

import com.elementsplus.ModDataComponents;
import com.elementsplus.core.circuit.CircuitItemData;
import com.elementsplus.core.circuit.diagram.CircuitDiagram;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * 电路板：可以直接编辑的小型实际电路。内部使用与电路图相同的
 * {@link ModDataComponents#CIRCUIT_DIAGRAM} 数据，但不会被编译成芯片。
 */
public class CircuitBoardItem extends Item {
    public CircuitBoardItem(Properties properties) {
        super(properties.stacksTo(1)
                .component(ModDataComponents.CIRCUIT_DIAGRAM, CircuitDiagram.EMPTY.copy()));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        CircuitDiagram diagram = CircuitItemData.diagram(stack);
        if (diagram != null) {
            tooltip.add(Component.translatable("tooltip.elements-plus.circuit_board.scale",
                    Component.translatable("circuit_diagram.scale." + diagram.scale.getSerializedName()))
                    .withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable("tooltip.elements-plus.circuit_board.editable")
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.elements-plus.circuit_board.limit")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
