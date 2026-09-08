package com.elementsplus.compat.jei;

import com.elementsplus.ElementsPlus;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public record HighPressureCrystallizerDisplay(ItemStack input, ItemStack output) {

    public static final RecipeType<HighPressureCrystallizerDisplay> TYPE =
            RecipeType.create(ElementsPlus.MOD_ID, "high_pressure_crystallizer", HighPressureCrystallizerDisplay.class);

    public static HighPressureCrystallizerDisplay create(Item input, Item output) {
        return new HighPressureCrystallizerDisplay(new ItemStack(input), new ItemStack(output));
    }
}
