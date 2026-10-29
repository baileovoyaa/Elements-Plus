package com.elementsplus.item;

import com.elementsplus.ModDataComponents;
import com.elementsplus.core.circuit.CircuitComponent;
import com.elementsplus.core.circuit.CircuitComponentRegistry;
import com.elementsplus.core.circuit.CircuitItemData;
import com.elementsplus.core.circuit.storage.CircuitStorage;
import com.elementsplus.core.circuit.storage.StoredCircuit;
import net.minecraft.ChatFormatting;
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
        StoredCircuit stored = CircuitItemData.stored(itemStack);
        if (stored == null && CircuitItemData.compiledId(itemStack) != null) {
            list.add(Component.literal(CircuitItemData.compiledId(itemStack).toString()).withStyle(ChatFormatting.DARK_GRAY));
        }
        if (stored != null) {
            list.add(CircuitStorage.get().displayName(stored).copy()
                    .withStyle(style -> style.withColor(0xFFD700)));
            if (stored.custom() != null && stored.custom().description() != null
                    && !stored.custom().description().isBlank()) {
                list.add(Component.literal(stored.custom().description()).withStyle(ChatFormatting.GRAY));
            }
            list.add(Component.translatable("tooltip.elements-plus.circuit.author", stored.displayAuthor())
                    .withStyle(ChatFormatting.GRAY));
            list.add(Component.literal(stored.id().toString()).withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        ResourceLocation id = itemStack.get(ModDataComponents.EQUIVALENT_COMPONENT);
        if (id != null) {
            CircuitComponent component = CircuitComponentRegistry.byId(id);
            if (component != null) {
                list.add(component.getName().copy().withStyle(style -> style.withColor(0xFFD700)));
            }
        }
    }
}
