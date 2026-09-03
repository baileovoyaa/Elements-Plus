package com.elementsplus.compat.jei;

import com.elementsplus.ElementsPlus;
import com.elementsplus.ModBlocks;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * JEI category for recipes larger than 3x3, displayed in a 5x5 grid.
 * Only the Advanced Crafting Table is shown as catalyst.
 */
public class AdvancedCraftingCategory implements IRecipeCategory<AdvancedCraftingRecipeDisplay> {

    public static final ResourceLocation UID = ElementsPlus.id("advanced_crafting");
    public static final int GRID_SIZE = 5;
    public static final int SLOT_SPACING = 18;

    private static final int GRID_X = 1;
    private static final int GRID_Y = 1;

    private static final int ARROW_X = GRID_X + GRID_SIZE * SLOT_SPACING + 2;
    private static final int ARROW_Y = GRID_Y + (GRID_SIZE * SLOT_SPACING) / 2 - 8;

    private static final int OUTPUT_X = ARROW_X + 26;
    private static final int OUTPUT_Y = GRID_Y + (GRID_SIZE * SLOT_SPACING) / 2 - 8;

    public static final int WIDTH = OUTPUT_X + 18 + 4;
    public static final int HEIGHT = GRID_SIZE * SLOT_SPACING + 2;

    private final IDrawable background;
    private final IDrawable icon;

    public AdvancedCraftingCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.drawableBuilder(
                        ElementsPlus.id("textures/gui/advanced_crafting_jei.png"),
                        0, 0, WIDTH, HEIGHT)
                .setTextureSize(WIDTH, HEIGHT)
                .build();
        this.icon = guiHelper.createDrawableItemLike(ModBlocks.ADVANCED_CRAFTING_TABLE);
    }

    @Override
    public @NotNull RecipeType<AdvancedCraftingRecipeDisplay> getRecipeType() {
        return AdvancedCraftingRecipeDisplay.TYPE;
    }

    @Override
    public @NotNull Component getTitle() {
        return Component.translatable("gui.jei.category.elements-plus.advanced_crafting");
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void draw(AdvancedCraftingRecipeDisplay recipe,
                     IRecipeSlotsView recipeSlotsView,
                     GuiGraphics guiGraphics,
                     double mouseX,
                     double mouseY) {
        background.draw(guiGraphics, 0, 0);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, AdvancedCraftingRecipeDisplay display, IFocusGroup focuses) {        RecipeHolder<CraftingRecipe> recipeHolder = display.recipeHolder();
        CraftingRecipe recipe = recipeHolder.value();

        List<Ingredient> ingredients = recipe.getIngredients();
        int recipeWidth = GridSize.getWidth(recipe);
        int recipeHeight = GridSize.getHeight(recipe);

        // Center the recipe in the 5x5 grid
        int offsetX = (GRID_SIZE - recipeWidth) / 2;
        int offsetY = (GRID_SIZE - recipeHeight) / 2;

        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                int recipeCol = col - offsetX;
                int recipeRow = row - offsetY;

                if (recipeCol >= 0 && recipeCol < recipeWidth && recipeRow >= 0 && recipeRow < recipeHeight) {
                    int ingredientIndex = recipeRow * recipeWidth + recipeCol;
                    if (ingredientIndex < ingredients.size()) {
                        Ingredient ingredient = ingredients.get(ingredientIndex);
                        if (!ingredient.isEmpty()) {
                            int slotX = GRID_X + col * SLOT_SPACING;
                            int slotY = GRID_Y + row * SLOT_SPACING;
                            builder.addInputSlot(slotX, slotY)
                                    .addIngredients(ingredient);
                        }
                    }
                }
            }
        }

        if (net.minecraft.client.Minecraft.getInstance().level != null) {
            builder.addOutputSlot(OUTPUT_X, OUTPUT_Y).addItemStack(recipe.getResultItem(net.minecraft.client.Minecraft.getInstance().level.registryAccess()));
        }

        // Move the recipe transfer button to the bottom-right of the category
        builder.moveRecipeTransferButton(OUTPUT_X, OUTPUT_Y + 20);
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, AdvancedCraftingRecipeDisplay display, IFocusGroup focuses) {
        builder.addRecipeArrowWidget()
                .setPosition(ARROW_X, ARROW_Y);
    }

    /**
     * Utility to obtain the grid width/height of a CraftingRecipe.
     * Vanilla CraftingRecipe does not expose dimensions; only shaped recipes do.
     */
    public static final class GridSize {
        private GridSize() {}

        public static int getWidth(CraftingRecipe recipe) {
            if (recipe instanceof ShapedRecipe shaped) {
                return shaped.getWidth();
            }
            return 0;
        }

        public static int getHeight(CraftingRecipe recipe) {
            if (recipe instanceof ShapedRecipe shaped) {
                return shaped.getHeight();
            }
            return 0;
        }
    }
}
