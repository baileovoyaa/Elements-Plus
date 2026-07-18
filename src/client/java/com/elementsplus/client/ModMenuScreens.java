package com.elementsplus.client;

import com.elementsplus.ModMenuTypes;
import com.elementsplus.client.screen.AdvancedCraftingTableScreen;
import com.elementsplus.client.screen.CrystallizerScreen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screens.MenuScreens;

@Environment(EnvType.CLIENT)
public class ModMenuScreens {
    public static void initialize() {
        MenuScreens.register(ModMenuTypes.CRYSTALLIZER, CrystallizerScreen::new);
        MenuScreens.register(ModMenuTypes.ADVANCED_CRAFTING_TABLE, AdvancedCraftingTableScreen::new);
    }
}
