package com.elementsplus.client.screen;

import com.elementsplus.menu.CrystallizerMenu;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.inventory.AbstractFurnaceScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.SmeltingRecipeBookComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.RecipeBookMenu;

import static com.elementsplus.ElementsPlus.MOD_ID;

@Environment(EnvType.CLIENT)
public class CrystallizerScreen extends AbstractFurnaceScreen<CrystallizerMenu> {
    private static final ResourceLocation LIT_PROGRESS_SPRITE = ResourceLocation.withDefaultNamespace("container/furnace/lit_progress");
    private static final ResourceLocation BURN_PROGRESS_SPRITE = ResourceLocation.withDefaultNamespace("container/furnace/burn_progress");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(MOD_ID, "textures/gui/crystallizer.png");

    public CrystallizerScreen(CrystallizerMenu furnaceMenu, Inventory inventory, Component component) {
        super(furnaceMenu, new SmeltingRecipeBookComponent(), inventory, component, TEXTURE, LIT_PROGRESS_SPRITE, BURN_PROGRESS_SPRITE);
    }

    @Override
    public void init() {
        this.leftPos = (this.width - this.imageWidth) / 2;
        this.topPos = (this.height - this.imageHeight) / 2;
        boolean widthTooNarrow = this.width < 379;
        this.recipeBookComponent.init(this.width, this.height, this.minecraft, widthTooNarrow, this.menu);
        this.leftPos = this.recipeBookComponent.updateScreenPosition(this.width, this.imageWidth);
        this.addRenderableWidget(new ImageButton(this.leftPos + 10, this.height / 2 - 49, 20, 18, RecipeBookComponent.RECIPE_BUTTON_SPRITES, button -> {
            this.recipeBookComponent.toggleVisibility();
            this.leftPos = this.recipeBookComponent.updateScreenPosition(this.width, this.imageWidth);
            button.setPosition(this.leftPos + 20, this.height / 2 - 49);
        }));
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
    }
}
