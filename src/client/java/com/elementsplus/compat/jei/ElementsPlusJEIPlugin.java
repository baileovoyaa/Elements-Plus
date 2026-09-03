package com.elementsplus.compat.jei;

import com.elementsplus.ElementsPlus;
import com.elementsplus.ModBlocks;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@JeiPlugin
public class ElementsPlusJEIPlugin implements IModPlugin {

    public static final ResourceLocation UID = ElementsPlus.id("jei_plugin_client");

    @Override
    public @NotNull ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(
                new AdvancedCraftingCategory(registration.getJeiHelpers().getGuiHelper())
        );
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        ElementsPlus.LOGGER.info("Registering recipes for JEI");
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }

        List<AdvancedCraftingRecipeDisplay> largeRecipes = new ArrayList<>();

        List<RecipeHolder<CraftingRecipe>> craftingRecipes =
                level.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING);

        for (RecipeHolder<CraftingRecipe> recipeHolder : craftingRecipes) {
            CraftingRecipe recipe = recipeHolder.value();
            int width = AdvancedCraftingCategory.GridSize.getWidth(recipe);
            int height = AdvancedCraftingCategory.GridSize.getHeight(recipe);

            // Only recipes larger than 3x3 belong in the advanced category.
            // JEI's vanilla crafting category is limited to 3x3 (max 9 inputs) and
            // automatically excludes these, so we register them here.
            if (width > 3 || height > 3) {
                largeRecipes.add(new AdvancedCraftingRecipeDisplay(recipeHolder));
            }
        }

        registration.addRecipes(AdvancedCraftingRecipeDisplay.TYPE, largeRecipes);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        // Vanilla crafting category: show both the vanilla crafting table and the advanced one.
        // (3x3-or-smaller recipes are shown here with JEI's default 3x3 grid.)
        registration.addRecipeCatalyst(ModBlocks.ADVANCED_CRAFTING_TABLE, RecipeTypes.CRAFTING);

        // Advanced crafting category: only the advanced crafting table.
        registration.addRecipeCatalyst(ModBlocks.ADVANCED_CRAFTING_TABLE, AdvancedCraftingRecipeDisplay.TYPE);
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        registration.addRecipeTransferHandler(new AdvancedCraftingTransferInfo());
    }
}
