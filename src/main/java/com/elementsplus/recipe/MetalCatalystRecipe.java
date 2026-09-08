package com.elementsplus.recipe;

import com.elementsplus.ElementsPlus;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public class MetalCatalystRecipe implements Recipe<MetalCatalystRecipe.Input> {

    public static final ResourceLocation TYPE_ID = ElementsPlus.id("metal_catalyst");
    public static final RecipeType<MetalCatalystRecipe> TYPE = Registry.register(BuiltInRegistries.RECIPE_TYPE, TYPE_ID, new RecipeType<MetalCatalystRecipe>() {
        @Override
        public String toString() {
            return TYPE_ID.toString();
        }
    });
    public static final RecipeSerializer<MetalCatalystRecipe> SERIALIZER = RecipeSerializer.register(TYPE_ID.toString(), new Serializer());

    public record InputEntry(Ingredient ingredient, int count) {
        public static final MapCodec<InputEntry> CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        Ingredient.CODEC.fieldOf("item").forGetter(InputEntry::ingredient),
                        com.mojang.serialization.Codec.INT.optionalFieldOf("count", 1).forGetter(InputEntry::count)
                ).apply(instance, InputEntry::new)
        );

        public boolean test(ItemStack stack) {
            return ingredient.test(stack) && stack.getCount() >= count;
        }
    }

    private final List<InputEntry> inputs;
    private final ItemStack output;

    public MetalCatalystRecipe(List<InputEntry> inputs, ItemStack output) {
        this.inputs = inputs;
        this.output = output;
    }

    @Override
    public boolean matches(Input input, Level level) {
        if (input.size() != inputs.size()) {
            return false;
        }
        boolean[] matched = new boolean[inputs.size()];
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            boolean found = false;
            for (int j = 0; j < inputs.size(); j++) {
                if (!matched[j] && inputs.get(j).test(stack)) {
                    matched[j] = true;
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack assemble(Input input, HolderLookup.Provider registries) {
        return output.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return false;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return output;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public RecipeType<?> getType() {
        return TYPE;
    }

    public List<InputEntry> getInputs() {
        return inputs;
    }

    public static List<MetalCatalystRecipe> getAll(Level level) {
        return level.getRecipeManager().getAllRecipesFor(TYPE).stream()
                .map(net.minecraft.world.item.crafting.RecipeHolder::value)
                .toList();
    }

    public static MetalCatalystRecipe findRecipe(List<ItemStack> inputStacks, Level level) {
        for (MetalCatalystRecipe recipe : getAll(level)) {
            Input input = new Input(inputStacks);
            if (recipe.matches(input, level)) {
                return recipe;
            }
        }
        return null;
    }

    public static boolean canCraft(List<ItemStack> inputStacks, Level level) {
        return findRecipe(inputStacks, level) != null;
    }

    public record Input(List<ItemStack> items) implements RecipeInput {
        @Override
        public ItemStack getItem(int index) {
            return items.get(index);
        }

        @Override
        public int size() {
            return items.size();
        }
    }

    public static final class Serializer implements RecipeSerializer<MetalCatalystRecipe> {

        public static final MapCodec<MetalCatalystRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        InputEntry.CODEC.codec().listOf().fieldOf("inputs").forGetter(r -> r.inputs),
                        ItemStack.CODEC.fieldOf("result").forGetter(r -> r.output)
                ).apply(instance, MetalCatalystRecipe::new)
        );

        public static final StreamCodec<RegistryFriendlyByteBuf, MetalCatalystRecipe> STREAM_CODEC = StreamCodec.of(
                (buf, recipe) -> {
                    buf.writeVarInt(recipe.inputs.size());
                    for (InputEntry entry : recipe.inputs) {
                        Ingredient.CONTENTS_STREAM_CODEC.encode(buf, entry.ingredient());
                        buf.writeVarInt(entry.count());
                    }
                    ItemStack.STREAM_CODEC.encode(buf, recipe.output);
                },
                (buf) -> {
                    int count = buf.readVarInt();
                    List<InputEntry> inputs = new ArrayList<>();
                    for (int i = 0; i < count; i++) {
                        Ingredient ingredient = Ingredient.CONTENTS_STREAM_CODEC.decode(buf);
                        int entryCount = buf.readVarInt();
                        inputs.add(new InputEntry(ingredient, entryCount));
                    }
                    ItemStack output = ItemStack.STREAM_CODEC.decode(buf);
                    return new MetalCatalystRecipe(inputs, output);
                }
        );

        @Override
        public MapCodec<MetalCatalystRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, MetalCatalystRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
