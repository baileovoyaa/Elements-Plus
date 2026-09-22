package com.elementsplus.item;

import com.elementsplus.ModDataComponents;
import com.elementsplus.core.circuit.BuiltinCircuitComponents;
import com.elementsplus.core.circuit.CircuitComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class ChipItem extends Item {
    public ChipItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack itemStack, TooltipContext tooltipContext, List<Component> list, TooltipFlag tooltipFlag) {
        ResourceLocation id = itemStack.get(ModDataComponents.EQUIVALENT_COMPONENT);
        if (id != null) {
            CircuitComponent component = BuiltinCircuitComponents.byId(id);
            if (component != null) {
                list.add(component.getName().copy().withStyle(style -> style.withColor(0xFFD700)));
            }
        }
    }
}
