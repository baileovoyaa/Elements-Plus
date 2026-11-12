package com.elementsplus.recipe;

import com.elementsplus.ElementsPlus;
import com.elementsplus.ModDataComponents;
import com.elementsplus.core.circuit.ChipManufacture;
import com.elementsplus.core.circuit.diagram.CircuitDiagram;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * 8 位元件芯片配方：3x3 中间放任意空晶片，周围放 8 个对应材料。
 * 输出使用与空晶片规模相同的成品芯片，并带上等效元件组件。
 */
public class ChipComponentRecipe extends CustomRecipe {

    public static final ResourceLocation TYPE_ID = ElementsPlus.id("chip_component");
    public static final RecipeSerializer<ChipComponentRecipe> SERIALIZER =
            RecipeSerializer.register(TYPE_ID.toString(), new Serializer());

    private final Ingredient ingredient;
    private final ResourceLocation component;

    public ChipComponentRecipe(Ingredient ingredient, ResourceLocation component) {
        super(CraftingBookCategory.MISC);
        this.ingredient = ingredient;
        this.component = component;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        int width = input.width();
        int height = input.height();
        if (width < 3 || height < 3) {
            return false;
        }
        for (int ox = 0; ox <= width - 3; ox++) {
            for (int oy = 0; oy <= height - 3; oy++) {
                if (matchesAt(input, ox, oy) && outsideIsEmpty(input, ox, oy)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean matchesAt(CraftingInput input, int ox, int oy) {
        if (ChipManufacture.emptyChipScale(input.getItem(ox + 1, oy + 1)) == null) {
            return false;
        }
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 3; y++) {
                if (x == 1 && y == 1) {
                    continue;
                }
                if (!ingredient.test(input.getItem(ox + x, oy + y))) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean outsideIsEmpty(CraftingInput input, int ox, int oy) {
        for (int x = 0; x < input.width(); x++) {
            for (int y = 0; y < input.height(); y++) {
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

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        for (int ox = 0; ox <= input.width() - 3; ox++) {
            for (int oy = 0; oy <= input.height() - 3; oy++) {
                if (!matchesAt(input, ox, oy) || !outsideIsEmpty(input, ox, oy)) {
                    continue;
                }
                CircuitDiagram.Scale scale = ChipManufacture.emptyChipScale(input.getItem(ox + 1, oy + 1));
                if (scale == null) {
                    continue;
                }
                ItemStack output = new ItemStack(ChipManufacture.chipItemForScale(scale));
                output.set(ModDataComponents.EQUIVALENT_COMPONENT, component);
                return output;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 3 && height >= 3;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }

    public static final class Serializer implements RecipeSerializer<ChipComponentRecipe> {

        public static final MapCodec<ChipComponentRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        Ingredient.CODEC.fieldOf("ingredient").forGetter(recipe -> recipe.ingredient),
                        ResourceLocation.CODEC.fieldOf("component").forGetter(recipe -> recipe.component)
                ).apply(instance, ChipComponentRecipe::new)
        );

        public static final StreamCodec<RegistryFriendlyByteBuf, ChipComponentRecipe> STREAM_CODEC = StreamCodec.of(
                (buf, recipe) -> {
                    Ingredient.CONTENTS_STREAM_CODEC.encode(buf, recipe.ingredient);
                    ResourceLocation.STREAM_CODEC.encode(buf, recipe.component);
                },
                buf -> new ChipComponentRecipe(
                        Ingredient.CONTENTS_STREAM_CODEC.decode(buf),
                        ResourceLocation.STREAM_CODEC.decode(buf))
        );

        @Override
        public MapCodec<ChipComponentRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ChipComponentRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
