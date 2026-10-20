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
        return matchSlots(input.items()) != null;
    }

    /**
     * 将配方输入匹配到真实槽位。
     *
     * @param slotStacks 输入槽中的物品栈，列表下标即真实槽位下标（允许包含空栈）
     * @return {@code slotMapping[配方输入下标] = 槽位下标}；无法匹配时返回 {@code null}
     */
    public int[] matchSlots(List<ItemStack> slotStacks) {
        int nonEmpty = 0;
        for (ItemStack stack : slotStacks) {
            if (!stack.isEmpty()) {
                nonEmpty++;
            }
        }
        if (nonEmpty != inputs.size()) {
            return null;
        }
        int[] mapping = new int[inputs.size()];
        boolean[] used = new boolean[slotStacks.size()];
        return assign(0, slotStacks, used, mapping) ? mapping : null;
    }

    /**
     * 回溯匹配：为每个配方输入分配一个互不相同的非空槽位。
     * 槽位数量很少（最多两个），回溯成本可以忽略；相比逐个贪心匹配，
     * 它能正确处理“槽位顺序与配方顺序不同、且数量不同”的情况。
     */
    private boolean assign(int recipeIndex, List<ItemStack> slotStacks, boolean[] used, int[] mapping) {
        if (recipeIndex >= inputs.size()) {
            return true;
        }
        InputEntry entry = inputs.get(recipeIndex);
        for (int slot = 0; slot < slotStacks.size(); slot++) {
            if (used[slot]) {
                continue;
            }
            ItemStack stack = slotStacks.get(slot);
            if (stack.isEmpty() || !entry.test(stack)) {
                continue;
            }
            used[slot] = true;
            mapping[recipeIndex] = slot;
            if (assign(recipeIndex + 1, slotStacks, used, mapping)) {
                return true;
            }
            used[slot] = false;
        }
        return false;
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

    /**
     * 匹配结果：命中的配方，以及每个配方输入对应的真实槽位下标。
     */
    public record Match(MetalCatalystRecipe recipe, int[] slotMapping) {
    }

    /**
     * 在给定槽位物品中查找可用的配方（只返回配方本身）。
     */
    public static MetalCatalystRecipe findRecipe(List<ItemStack> slotStacks, Level level) {
        Match match = findMatch(slotStacks, level);
        return match == null ? null : match.recipe();
    }

    /**
     * 在给定槽位物品中查找可用配方，并返回“配方输入 → 真实槽位”的映射。
     * 扣料时必须使用该映射，不能按“非空槽压缩后”的顺序扣料。
     */
    public static Match findMatch(List<ItemStack> slotStacks, Level level) {
        for (MetalCatalystRecipe recipe : getAll(level)) {
            int[] mapping = recipe.matchSlots(slotStacks);
            if (mapping != null) {
                return new Match(recipe, mapping);
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
