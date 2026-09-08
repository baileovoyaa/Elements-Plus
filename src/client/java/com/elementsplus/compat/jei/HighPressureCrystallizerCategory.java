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
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.NotNull;

public class HighPressureCrystallizerCategory implements IRecipeCategory<HighPressureCrystallizerDisplay> {

    public static final ResourceLocation UID = ElementsPlus.id("high_pressure_crystallizer");

    private static final int INPUT_X = 1;
    private static final int INPUT_Y = 1;
    private static final int ARROW_X = INPUT_X + 18 + 4;
    private static final int ARROW_Y = INPUT_Y + 4;
    private static final int OUTPUT_X = ARROW_X + 26;
    private static final int OUTPUT_Y = INPUT_Y;

    public static final int WIDTH = OUTPUT_X + 18 + 4;
    public static final int HEIGHT = 18 + 2;

    private final IDrawable background;
    private final IDrawable icon;

    public HighPressureCrystallizerCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createBlankDrawable(WIDTH, HEIGHT);
        this.icon = guiHelper.createDrawableItemLike(ModBlocks.CRYSTALLIZER);
    }

    @Override
    public @NotNull RecipeType<HighPressureCrystallizerDisplay> getRecipeType() {
        return HighPressureCrystallizerDisplay.TYPE;
    }

    @Override
    public @NotNull Component getTitle() {
        return Component.translatable("gui.jei.category.elements-plus.high_pressure_crystallizer");
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
    public void draw(HighPressureCrystallizerDisplay recipe,
                     IRecipeSlotsView recipeSlotsView,
                     GuiGraphics guiGraphics,
                     double mouseX,
                     double mouseY) {
        background.draw(guiGraphics, 0, 0);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, HighPressureCrystallizerDisplay display, IFocusGroup focuses) {
        builder.addInputSlot(INPUT_X, INPUT_Y)
                .addIngredients(Ingredient.of(display.input()));

        builder.addOutputSlot(OUTPUT_X, OUTPUT_Y)
                .addItemStack(display.output());
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, HighPressureCrystallizerDisplay display, IFocusGroup focuses) {
        builder.addRecipeArrowWidget()
                .setPosition(ARROW_X, ARROW_Y);
    }
}
