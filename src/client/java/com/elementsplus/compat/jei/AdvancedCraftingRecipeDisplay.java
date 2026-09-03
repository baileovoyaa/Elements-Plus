package com.elementsplus.compat.jei;

import com.elementsplus.ElementsPlus;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;

/**
 * Wrapper around a vanilla CraftingRecipe for display in the AdvancedCraftingCategory.
 * Used for recipes that exceed 3x3 and need the 5x5 grid.
 */
public record AdvancedCraftingRecipeDisplay(RecipeHolder<CraftingRecipe> recipeHolder) {

    public static final RecipeType<AdvancedCraftingRecipeDisplay> TYPE = RecipeType.create(ElementsPlus.MOD_ID, "advanced_crafting", AdvancedCraftingRecipeDisplay.class);

    public CraftingRecipe getRecipe() {
        return recipeHolder.value();
    }

    public ResourceLocation getId() {
        return recipeHolder.id();
    }
}
