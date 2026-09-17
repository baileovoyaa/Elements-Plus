package com.elementsplus.recipe;

import com.elementsplus.ElementsPlus;
import com.elementsplus.ModDataComponents;
import com.elementsplus.ModItems;
import com.elementsplus.core.circuit.diagram.CircuitDiagram;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.level.Level;

public class ScaleUpgradeRecipe extends CustomRecipe {

    public static final ResourceLocation TYPE_ID = ElementsPlus.id("scale_upgrade_circuit_diagram");

    public static final RecipeSerializer<ScaleUpgradeRecipe> SERIALIZER =
            RecipeSerializer.register(TYPE_ID.toString(),
                    new SimpleCraftingRecipeSerializer<>(ScaleUpgradeRecipe::new));

    public ScaleUpgradeRecipe(CraftingBookCategory category) {
        super(category);
    }

    /**
     * 中间放电路图、外圈一圈纸（类似原版地图扩展），且 3x3 区域外为空时匹配。
     * 只允许升级到"超大"为止；无限规模不参与升级。
     */
    @Override
    public boolean matches(CraftingInput input, Level level) {
        int w = input.width();
        int h = input.height();
        if (w < 3 || h < 3) {
            return false;
        }
        for (int ox = 0; ox <= w - 3; ox++) {
            for (int oy = 0; oy <= h - 3; oy++) {
                if (isUpgradeAt(input, ox, oy)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isUpgradeAt(CraftingInput input, int ox, int oy) {
        boolean diagram = false;
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                ItemStack stack = input.getItem(ox + x, oy + y);
                if (x == 1 && y == 1) {
                    if (!isUpgradeableDiagram(stack)) {
                        return false;
                    }
                    diagram = true;
                } else if (!stack.is(Items.PAPER)) {
                    return false;
                }
            }
        }
        if (!diagram) {
            return false;
        }
        int w = input.width();
        int h = input.height();
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                if (x >= ox && x < ox + 3 && y >= oy && y < oy + 3) {
                    continue;
                }
                if (!input.getItem(x, y).isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean isUpgradeableDiagram(ItemStack stack) {
        if (!stack.is(ModItems.CIRCUIT_DIAGRAM)) {
            return false;
        }
        CircuitDiagram diagram = stack.get(ModDataComponents.CIRCUIT_DIAGRAM);
        return diagram != null && diagram.scale.canUpgrade();
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack source = findDiagram(input);
        if (source == null) {
            return ItemStack.EMPTY;
        }
        CircuitDiagram diagram = source.get(ModDataComponents.CIRCUIT_DIAGRAM);
        if (diagram == null) {
            return ItemStack.EMPTY;
        }
        CircuitDiagram upgraded = diagram.copy();
        upgraded.scale = diagram.scale.upgrade();
        ItemStack output = new ItemStack(ModItems.CIRCUIT_DIAGRAM);
        output.set(ModDataComponents.CIRCUIT_DIAGRAM, upgraded);
        return output;
    }

    private static ItemStack findDiagram(CraftingInput input) {
        for (ItemStack stack : input.items()) {
            if (isUpgradeableDiagram(stack)) {
                return stack;
            }
        }
        return null;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 3 && height >= 3;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public RecipeType<?> getType() {
        return RecipeType.CRAFTING;
    }
}