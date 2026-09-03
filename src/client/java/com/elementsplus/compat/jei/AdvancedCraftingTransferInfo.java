package com.elementsplus.compat.jei;

import com.elementsplus.menu.AdvancedCraftingTableMenu;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferInfo;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Transfer info so JEI can move ingredients into the 5x5 crafting grid of the
 * Advanced Crafting Table when clicking the "+" button.
 * <p>
 * AdvancedCraftingTableMenu slots:
 *   0       = result slot
 *   1 - 25  = crafting grid (5x5, row-major)
 *   26 - 52 = player inventory (27 slots)
 *   53 - 61 = player hotbar (9 slots)
 * <p>
 * {@code getRecipeSlots} returns only the grid slots that correspond to non-empty
 * recipe ingredients, in the same order that {@link AdvancedCraftingCategory#setRecipe}
 * creates JEI input slots, so that JEI's default transfer logic lines them up correctly.
 */
public class AdvancedCraftingTransferInfo
        implements IRecipeTransferInfo<AdvancedCraftingTableMenu, AdvancedCraftingRecipeDisplay> {

    private static final int GRID_SIZE = AdvancedCraftingCategory.GRID_SIZE;
    private static final int GRID_SLOT_START = 1; // result slot is index 0
    private static final int INVENTORY_START = 26;
    private static final int HOTBAR_END = 62;

    @Override
    public @NotNull Class<? extends AdvancedCraftingTableMenu> getContainerClass() {
        return AdvancedCraftingTableMenu.class;
    }

    @Override
    public @NotNull Optional<MenuType<AdvancedCraftingTableMenu>> getMenuType() {
        return Optional.of(com.elementsplus.ModMenuTypes.ADVANCED_CRAFTING_TABLE);
    }

    @Override
    public @NotNull RecipeType<AdvancedCraftingRecipeDisplay> getRecipeType() {
        return AdvancedCraftingRecipeDisplay.TYPE;
    }

    @Override
    public boolean canHandle(AdvancedCraftingTableMenu container, AdvancedCraftingRecipeDisplay recipe) {
        return true;
    }

    @Nullable
    @Override
    public mezz.jei.api.recipe.transfer.IRecipeTransferError getHandlingError(AdvancedCraftingTableMenu container, AdvancedCraftingRecipeDisplay recipe) {
        return IRecipeTransferInfo.super.getHandlingError(container, recipe);
    }

    @Override
    public @NotNull List<Slot> getRecipeSlots(AdvancedCraftingTableMenu container, AdvancedCraftingRecipeDisplay display) {
        RecipeHolder<net.minecraft.world.item.crafting.CraftingRecipe> recipeHolder = display.recipeHolder();
        net.minecraft.world.item.crafting.CraftingRecipe recipe = recipeHolder.value();

        List<Ingredient> ingredients = recipe.getIngredients();
        int recipeWidth = AdvancedCraftingCategory.GridSize.getWidth(recipe);
        int recipeHeight = AdvancedCraftingCategory.GridSize.getHeight(recipe);

        int offsetX = (GRID_SIZE - recipeWidth) / 2;
        int offsetY = (GRID_SIZE - recipeHeight) / 2;

        List<Slot> result = new ArrayList<>();
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                int recipeCol = col - offsetX;
                int recipeRow = row - offsetY;
                if (recipeCol >= 0 && recipeCol < recipeWidth && recipeRow >= 0 && recipeRow < recipeHeight) {
                    int ingredientIndex = recipeRow * recipeWidth + recipeCol;
                    if (ingredientIndex < ingredients.size()) {
                        Ingredient ingredient = ingredients.get(ingredientIndex);
                        if (!ingredient.isEmpty()) {
                            int menuSlotIndex = GRID_SLOT_START + row * GRID_SIZE + col;
                            result.add(container.getSlot(menuSlotIndex));
                        }
                    }
                }
            }
        }
        return result;
    }

    @Override
    public @NotNull List<Slot> getInventorySlots(AdvancedCraftingTableMenu container, AdvancedCraftingRecipeDisplay recipe) {
        List<Slot> slots = new ArrayList<>();
        for (int i = INVENTORY_START; i < HOTBAR_END; i++) {
            slots.add(container.getSlot(i));
        }
        return slots;
    }

    @Override
    public boolean requireCompleteSets(AdvancedCraftingTableMenu container, AdvancedCraftingRecipeDisplay recipe) {
        return IRecipeTransferInfo.super.requireCompleteSets(container, recipe);
    }
}
