package com.elementsplus.menu;

import com.elementsplus.ModMenuTypes;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.RecipeBookType;
import net.minecraft.world.item.crafting.RecipeType;

public class CrystallizerMenu extends AbstractFurnaceMenu {
    public CrystallizerMenu(int i, Inventory inventory) {
        super(ModMenuTypes.CRYSTALLIZER, RecipeType.SMELTING, RecipeBookType.FURNACE, i, inventory);
    }

    public CrystallizerMenu(int i, Inventory inventory, Container container, ContainerData containerData) {
        super(ModMenuTypes.CRYSTALLIZER, RecipeType.SMELTING, RecipeBookType.FURNACE, i, inventory, container, containerData);
    }
}
