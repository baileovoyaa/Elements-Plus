package com.elementsplus;

import com.elementsplus.menu.AdvancedCraftingTableMenu;
import com.elementsplus.menu.CrystallizerMenu;
import com.elementsplus.menu.LithographyMachineMenu;
import com.elementsplus.menu.MetalCatalystMenu;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.flag.FeatureFlag;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

import static com.elementsplus.ElementsPlus.MOD_ID;

public class ModMenuTypes {
    public static final MenuType<CrystallizerMenu> CRYSTALLIZER = register("crystallizer", CrystallizerMenu::new);
    public static final MenuType<AdvancedCraftingTableMenu> ADVANCED_CRAFTING_TABLE = register("advanced_crafting_table", AdvancedCraftingTableMenu::new);
    public static final MenuType<MetalCatalystMenu> METAL_CATALYST = register("metal_catalyst", MetalCatalystMenu::new);
    public static final MenuType<LithographyMachineMenu> LITHOGRAPHY_MACHINE = register("lithography_machine", LithographyMachineMenu::new);

    private static <T extends AbstractContainerMenu> MenuType<T> register(String string, MenuType.MenuSupplier<T> menuSupplier) {
        return Registry.register(BuiltInRegistries.MENU, ResourceLocation.fromNamespaceAndPath(MOD_ID, string), new MenuType<>(menuSupplier, FeatureFlags.VANILLA_SET));
    }

    private static <T extends AbstractContainerMenu> MenuType<T> register(String string, MenuType.MenuSupplier<T> menuSupplier, FeatureFlag... featureFlags) {
        return Registry.register(BuiltInRegistries.MENU, ResourceLocation.fromNamespaceAndPath(MOD_ID, string), new MenuType<>(menuSupplier, FeatureFlags.REGISTRY.subset(featureFlags)));
    }

    public static void initialize() {
        // 物品已在静态块中注册
    }
}
