package com.elementsplus.blocks.entity;

import com.elementsplus.ModBlockEntityTypes;
import com.elementsplus.menu.CrystallizerMenu;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class CrystallizerBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer, StackedContentsCompatible {
    private static final int[] SLOTS_FOR_UP = new int[]{0};
    private static final int[] SLOTS_FOR_DOWN = new int[]{2, 1};
    private static final int[] SLOTS_FOR_SIDES = new int[]{1};
    protected NonNullList<ItemStack> items = NonNullList.withSize(4, ItemStack.EMPTY);
    int litTime;
    int litDuration;
    int cookingProgress;
    int cookingTotalTime;
    int pressure;
    @Nullable
    protected final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int i) {
            return switch (i) {
                case 0 -> litTime;
                case 1 -> litDuration;
                case 2 -> cookingProgress;
                case 3 -> cookingTotalTime;
                default -> 0;
            };
        }

        @Override
        public void set(int i, int j) {
            switch (i) {
                case 0:
                    litTime = j;
                    break;
                case 1:
                    litDuration = j;
                    break;
                case 2:
                    cookingProgress = j;
                    break;
                case 3:
                    cookingTotalTime = j;
            }
        }

        @Override
        public int getCount() {
            return 4;
        }
    };
    private final Object2IntOpenHashMap<ResourceLocation> recipesUsed = new Object2IntOpenHashMap<>();

    protected CrystallizerBlockEntity(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState) {
        super(blockEntityType, blockPos, blockState);
    }

    public static Map<Item, Integer> getFuel() {
        return FurnaceBlockEntity.getFuel();
    }

    private boolean isLit() {
        return this.litTime > 0;
    }

    @Override
    protected void loadAdditional(CompoundTag compoundTag, HolderLookup.Provider provider) {
        super.loadAdditional(compoundTag, provider);
        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(compoundTag, this.items, provider);
        this.litTime = compoundTag.getShort("BurnTime");
        this.cookingProgress = compoundTag.getShort("CookTime");
        this.cookingTotalTime = compoundTag.getShort("CookTimeTotal");
        this.litDuration = this.getBurnDuration(this.items.get(1));
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
        CompoundTag compoundTag2 = new CompoundTag();
        this.recipesUsed.forEach((resourceLocation, integer) -> compoundTag2.putInt(resourceLocation.toString(), integer));
        compoundTag.put("RecipesUsed", compoundTag2);
    }

    public static void serverTick(Level level, BlockPos blockPos, BlockState blockState, CrystallizerBlockEntity crystallizerBlockEntity) {
        boolean bl = crystallizerBlockEntity.isLit();
        boolean bl2 = false;
        if (crystallizerBlockEntity.isLit()) {
            crystallizerBlockEntity.litTime--;
        }

        ItemStack itemStack = crystallizerBlockEntity.items.get(1);
        ItemStack itemStack2 = crystallizerBlockEntity.items.get(0);
        boolean hasIngredient = !itemStack2.isEmpty();
        boolean hasFuel = !itemStack.isEmpty();
        if (crystallizerBlockEntity.isLit() || hasFuel && hasIngredient) {

            int maxStackSize = crystallizerBlockEntity.getMaxStackSize();
            if (!crystallizerBlockEntity.isLit() && canBurn(crystallizerBlockEntity.items, maxStackSize)) {
                crystallizerBlockEntity.litTime = crystallizerBlockEntity.getBurnDuration(itemStack);
                crystallizerBlockEntity.litDuration = crystallizerBlockEntity.litTime;
                if (crystallizerBlockEntity.isLit()) {
                    bl2 = true;
                    if (hasFuel) {
                        Item item = itemStack.getItem();
                        itemStack.shrink(1);
                        if (itemStack.isEmpty()) {
                            Item item2 = item.getCraftingRemainingItem();
                            crystallizerBlockEntity.items.set(1, item2 == null ? ItemStack.EMPTY : new ItemStack(item2));
                        }
                    }
                }
            }

            if (crystallizerBlockEntity.isLit() && canBurn(crystallizerBlockEntity.items, maxStackSize)) {
                crystallizerBlockEntity.cookingProgress++;
                if (crystallizerBlockEntity.cookingProgress == crystallizerBlockEntity.cookingTotalTime) {
                    crystallizerBlockEntity.cookingProgress = 0;
                    crystallizerBlockEntity.cookingTotalTime = getTotalCookTime();
                    burn(crystallizerBlockEntity.items, maxStackSize);

                    bl2 = true;
                }
            } else {
                crystallizerBlockEntity.cookingProgress = 0;
            }
        } else if (!crystallizerBlockEntity.isLit() && crystallizerBlockEntity.cookingProgress > 0) {
            crystallizerBlockEntity.cookingProgress = Mth.clamp(crystallizerBlockEntity.cookingProgress - 2, 0, crystallizerBlockEntity.cookingTotalTime);
        }

        if (bl != crystallizerBlockEntity.isLit()) {
            bl2 = true;
            level.setBlock(blockPos, blockState, 3);
        }

        if (bl2) {
            setChanged(level, blockPos, blockState);
        }
    }

    private static boolean canBurn(NonNullList<ItemStack> nonNullList, int i) {
        if (!nonNullList.get(0).isEmpty()) {
            ItemStack itemStack = Items.AMETHYST_SHARD.getDefaultInstance();
            if (itemStack.isEmpty()) {
                return false;
            } else {
                ItemStack itemStack2 = nonNullList.get(2);
                if (itemStack2.isEmpty()) {
                    return true;
                } else if (!ItemStack.isSameItemSameComponents(itemStack2, itemStack)) {
                    return false;
                } else {
                    return itemStack2.getCount() < i && itemStack2.getCount() < itemStack2.getMaxStackSize() || itemStack2.getCount() < itemStack.getMaxStackSize();
                }
            }
        } else {
            return false;
        }
    }

    private static boolean burn(NonNullList<ItemStack> nonNullList, int i) {
        if (canBurn(nonNullList, i)) {
            ItemStack itemStack = nonNullList.get(0);
            ItemStack itemStack2 = Items.AMETHYST_SHARD.getDefaultInstance();
            ItemStack itemStack3 = nonNullList.get(2);
            if (itemStack3.isEmpty()) {
                nonNullList.set(2, itemStack2.copy());
            } else if (ItemStack.isSameItemSameComponents(itemStack3, itemStack2)) {
                itemStack3.grow(1);
            }

            if (itemStack.is(Blocks.WET_SPONGE.asItem()) && !nonNullList.get(1).isEmpty() && nonNullList.get(1).is(Items.BUCKET)) {
                nonNullList.set(1, new ItemStack(Items.WATER_BUCKET));
            }

            itemStack.shrink(1);
            return true;
        } else {
            return false;
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
        return this.canPlaceItem(i, itemStack);
    }

    @Override
    public boolean canTakeItemThroughFace(int i, ItemStack itemStack, Direction direction) {
        return direction != Direction.DOWN || i != 1 || itemStack.is(Items.WATER_BUCKET) || itemStack.is(Items.BUCKET);
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
    public void setItem(int i, ItemStack itemStack) {
        ItemStack itemStack2 = this.items.get(i);
        boolean bl = !itemStack.isEmpty() && ItemStack.isSameItemSameComponents(itemStack2, itemStack);
        this.items.set(i, itemStack);
        itemStack.limitSize(this.getMaxStackSize(itemStack));
        if (i == 0 && !bl) {
            this.cookingTotalTime = getTotalCookTime();
            this.cookingProgress = 0;
            this.setChanged();
        }
    }

    @Override
    public boolean canPlaceItem(int i, ItemStack itemStack) {
        if (i == 2) {
            return false;
        }

        if (i != 1) {
            return true;
        }

        ItemStack itemStack2 = this.items.get(1);
        return isFuel(itemStack) || itemStack.is(Items.BUCKET) && !itemStack2.is(Items.BUCKET);
    }

    @Override
    public void fillStackedContents(StackedContents stackedContents) {
        for (ItemStack itemStack : this.items) {
            stackedContents.accountStack(itemStack);
        }
    }

    public CrystallizerBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(ModBlockEntityTypes.CRYSTALLIZER, blockPos, blockState);
    }

    @Override
    protected @NotNull Component getDefaultName() {
        return Component.translatable("container.elements-plus.crystallizer");
    }

    @Override
    protected @NotNull AbstractContainerMenu createMenu(int i, Inventory inventory) {
        return new CrystallizerMenu(i, inventory, this, this.dataAccess);
    }
}
