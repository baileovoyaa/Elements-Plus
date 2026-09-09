package com.elementsplus.blocks.entity;

import com.elementsplus.ModBlockEntityTypes;
import com.elementsplus.menu.MetalCatalystMenu;
import com.elementsplus.recipe.MetalCatalystRecipe;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.StackedContentsCompatible;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MetalCatalystBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer, StackedContentsCompatible {

    private static final int[] SLOTS_FOR_UP = new int[]{0, 1};
    private static final int[] SLOTS_FOR_DOWN = new int[]{4, 2};
    private static final int[] SLOTS_FOR_SIDES = new int[]{2, 3, 0, 1};
    protected NonNullList<ItemStack> items = NonNullList.withSize(5, ItemStack.EMPTY);
    int litTime;
    int litDuration;
    int cookingProgress;
    int cookingTotalTime;
    int waste;
    @Nullable
    protected final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int i) {
            return switch (i) {
                case 0 -> litTime;
                case 1 -> litDuration;
                case 2 -> cookingProgress;
                case 3 -> cookingTotalTime;
                case 4 -> waste;
                default -> 0;
            };
        }

        @Override
        public void set(int i, int j) {
            switch (i) {
                case 0 -> litTime = j;
                case 1 -> litDuration = j;
                case 2 -> cookingProgress = j;
                case 3 -> cookingTotalTime = j;
                case 4 -> waste = j;
            }
        }

        @Override
        public int getCount() {
            return 5;
        }
    };
    private final Object2IntOpenHashMap<ResourceLocation> recipesUsed = new Object2IntOpenHashMap<>();

    protected MetalCatalystBlockEntity(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState) {
        super(blockEntityType, blockPos, blockState);
    }

    public MetalCatalystBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(ModBlockEntityTypes.METAL_CATALYST, blockPos, blockState);
    }

    public static Map<Item, Integer> getFuel() {
        return FurnaceBlockEntity.getFuel();
    }

    private boolean isLit() {
        return this.litTime > 0;
    }

    public boolean isOverfilled() {
        return this.waste >= 90;
    }

    public int getWaste() {
        return this.waste;
    }

    public boolean extractWaste(int amount) {
        if (this.waste < amount) {
            return false;
        }
        this.waste -= amount;
        setChanged(level, worldPosition, getBlockState());
        return true;
    }

    public void clearWaste() {
        this.waste = 0;
        setChanged(level, worldPosition, getBlockState());
    }

    @Override
    protected void loadAdditional(CompoundTag compoundTag, HolderLookup.Provider provider) {
        super.loadAdditional(compoundTag, provider);
        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(compoundTag, this.items, provider);
        this.litTime = compoundTag.getShort("BurnTime");
        this.cookingProgress = compoundTag.getShort("CookTime");
        this.cookingTotalTime = compoundTag.getShort("CookTimeTotal");
        this.litDuration = this.getBurnDuration(this.items.get(2));
        this.waste = compoundTag.getShort("Waste");
        CompoundTag compoundTag2 = compoundTag.getCompound("RecipesUsed");
        for (String string : compoundTag2.getAllKeys()) {
            this.recipesUsed.put(ResourceLocation.parse(string), compoundTag2.getInt(string));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag compoundTag, HolderLookup.Provider provider) {
        super.saveAdditional(compoundTag, provider);
        compoundTag.putShort("BurnTime", (short) this.litTime);
        compoundTag.putShort("CookTime", (short) this.cookingProgress);
        compoundTag.putShort("CookTimeTotal", (short) this.cookingTotalTime);
        ContainerHelper.saveAllItems(compoundTag, this.items, provider);
        compoundTag.putShort("Waste", (short) this.waste);
        CompoundTag compoundTag2 = new CompoundTag();
        this.recipesUsed.forEach((resourceLocation, integer) -> compoundTag2.putInt(resourceLocation.toString(), integer));
        compoundTag.put("RecipesUsed", compoundTag2);
    }

    public static void serverTick(Level level, BlockPos blockPos, BlockState blockState, MetalCatalystBlockEntity blockEntity) {
        boolean wasLit = blockEntity.isLit();
        boolean changed = false;
        if (blockEntity.isLit()) {
            blockEntity.litTime--;
        }

        ItemStack fuel = blockEntity.items.get(2);
        boolean hasFuel = !fuel.isEmpty();
        List<ItemStack> inputs = new ArrayList<>();
        if (!blockEntity.items.get(0).isEmpty()) inputs.add(blockEntity.items.get(0));
        if (!blockEntity.items.get(1).isEmpty()) inputs.add(blockEntity.items.get(1));

        boolean canWork = !blockEntity.isOverfilled() && hasFuel && canBurn(blockEntity, inputs, level);

        if (blockEntity.isLit() || canWork) {
            int maxStackSize = blockEntity.getMaxStackSize();
            if (!blockEntity.isLit() && canWork) {
                blockEntity.litTime = blockEntity.getBurnDuration(fuel);
                blockEntity.litDuration = blockEntity.litTime;
                if (blockEntity.isLit()) {
                    changed = true;
                    Item item = fuel.getItem();
                    fuel.shrink(1);
                    if (fuel.isEmpty()) {
                        Item item2 = item.getCraftingRemainingItem();
                        blockEntity.items.set(2, item2 == null ? ItemStack.EMPTY : new ItemStack(item2));
                    }
                }
            }

            if (blockEntity.isLit() && !blockEntity.isOverfilled() && canBurn(blockEntity, inputs, level)) {
                blockEntity.cookingProgress++;
                if (blockEntity.cookingProgress >= blockEntity.cookingTotalTime) {
                    blockEntity.cookingProgress = 0;
                    blockEntity.cookingTotalTime = getTotalCookTime();
                    burn(blockEntity, level, inputs);
                    changed = true;
                }
            } else {
                blockEntity.cookingProgress = 0;
            }
        } else if (blockEntity.cookingProgress > 0) {
            blockEntity.cookingProgress = Mth.clamp(blockEntity.cookingProgress - 2, 0, blockEntity.cookingTotalTime);
        }

        if (wasLit != blockEntity.isLit()) {
            changed = true;
            level.setBlock(blockPos, blockState, 3);
        }

        if (changed) {
            setChanged(level, blockPos, blockState);
        }
    }

    private static boolean canBurn(MetalCatalystBlockEntity blockEntity, List<ItemStack> inputs, Level level) {
        MetalCatalystRecipe recipe = MetalCatalystRecipe.findRecipe(inputs, level);
        if (recipe == null) {
            return false;
        }
        ItemStack output = blockEntity.items.get(4);
        ItemStack willOutput = recipe.getResultItem(level.registryAccess());
        if (output.isEmpty()) {
            return true;
        } else if (!ItemStack.isSameItemSameComponents(output, willOutput)) {
            return false;
        } else {
            return output.getCount() < blockEntity.getMaxStackSize() && output.getCount() < output.getMaxStackSize() && willOutput.getCount() + output.getCount() <= willOutput.getMaxStackSize();
        }
    }

    private static void burn(MetalCatalystBlockEntity blockEntity, Level level, List<ItemStack> inputs) {
        if (!canBurn(blockEntity, inputs, level)) {
            return;
        }
        MetalCatalystRecipe recipe = MetalCatalystRecipe.findRecipe(inputs, level);
        if (recipe == null) {
            return;
        }

        ItemStack output = blockEntity.items.get(4);
        ItemStack willOutput = recipe.getResultItem(level.registryAccess());
        if (output.isEmpty()) {
            blockEntity.items.set(4, willOutput.copy());
        } else if (ItemStack.isSameItemSameComponents(output, willOutput)) {
            output.grow(willOutput.getCount());
        }

        for (int i = 0; i < inputs.size(); i++) {
            if (i < recipe.getInputs().size()) {
                int shrinkAmount = recipe.getInputs().get(i).count();
                blockEntity.items.get(i).shrink(shrinkAmount);
            }
        }

        RandomSource random = level.random;
        ItemStack catalyst = blockEntity.items.get(3);
        if (!catalyst.isEmpty() && random.nextDouble() < 0.2) {
            catalyst.shrink(1);
            blockEntity.waste = Math.min(100, blockEntity.waste + 10);
        }
    }

    protected int getBurnDuration(ItemStack itemStack) {
        if (itemStack.isEmpty()) {
            return 0;
        }
        Item item = itemStack.getItem();
        return getFuel().getOrDefault(item, 0);
    }

    private static int getTotalCookTime() {
        return 200;
    }

    public static boolean isFuel(ItemStack itemStack) {
        return getFuel().containsKey(itemStack.getItem());
    }

    @Override
    public int @NotNull [] getSlotsForFace(Direction direction) {
        if (direction == Direction.DOWN) {
            return SLOTS_FOR_DOWN;
        } else {
            return direction == Direction.UP ? SLOTS_FOR_UP : SLOTS_FOR_SIDES;
        }
    }

    @Override
    public boolean canPlaceItemThroughFace(int i, ItemStack itemStack, @Nullable Direction direction) {
        if (direction == Direction.UP) {
            return i == 0 || i == 1;
        }
        if (direction == Direction.DOWN) {
            return false;
        }
        if (isFuel(itemStack)) {
            return i == 2;
        }
        if (itemStack.is(com.elementsplus.ModItems.CATALYST)) {
            return i == 3;
        }
        return i == 0 || i == 1;
    }

    @Override
    public boolean canTakeItemThroughFace(int i, ItemStack itemStack, Direction direction) {
        if (direction == Direction.DOWN) {
            return i == 4 || i == 2 && itemStack.is(Items.BUCKET);
        }
        return false;
    }

    @Override
    public int getContainerSize() {
        return this.items.size();
    }

    @Override
    protected @NotNull NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> nonNullList) {
        this.items = nonNullList;
    }

    @Override
    public boolean canPlaceItem(int i, ItemStack itemStack) {
        if (i == 4) {
            return false;
        }
        if (i == 0 || i == 1) {
            return true;
        }
        if (i == 2) {
            return isFuel(itemStack) || itemStack.is(Items.BUCKET);
        }
        if (i == 3) {
            return itemStack.is(com.elementsplus.ModItems.CATALYST);
        }
        return false;
    }

    @Override
    public void fillStackedContents(StackedContents stackedContents) {
        for (ItemStack itemStack : this.items) {
            stackedContents.accountStack(itemStack);
        }
    }

    @Override
    protected @NotNull Component getDefaultName() {
        return Component.translatable("container.elements-plus.metal_catalyst");
    }

    @Override
    protected @NotNull AbstractContainerMenu createMenu(int i, Inventory inventory) {
        return new MetalCatalystMenu(i, inventory, this, this.dataAccess);
    }
}
