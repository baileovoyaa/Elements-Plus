package com.elementsplus.client.screen;

import com.elementsplus.menu.CrystallizerMenu;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screens.inventory.AbstractFurnaceScreen;
import net.minecraft.client.gui.screens.recipebook.SmeltingRecipeBookComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import static com.elementsplus.ElementsPlus.MOD_ID;

@Environment(EnvType.CLIENT)
public class CrystallizerScreen extends AbstractFurnaceScreen<CrystallizerMenu> {
    private static final ResourceLocation LIT_PROGRESS_SPRITE = ResourceLocation.withDefaultNamespace("container/furnace/lit_progress");
    private static final ResourceLocation BURN_PROGRESS_SPRITE = ResourceLocation.withDefaultNamespace("container/furnace/burn_progress");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(MOD_ID, "textures/gui/crystallizer.png");

    public CrystallizerScreen(CrystallizerMenu furnaceMenu, Inventory inventory, Component component) {
        super(furnaceMenu, new SmeltingRecipeBookComponent(), inventory, component, TEXTURE, LIT_PROGRESS_SPRITE, BURN_PROGRESS_SPRITE);
    }
}
