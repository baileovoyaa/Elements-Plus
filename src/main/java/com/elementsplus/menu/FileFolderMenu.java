package com.elementsplus.menu;

import com.elementsplus.ModItems;
import com.elementsplus.core.config.ElementsPlusServerConfig;
import com.elementsplus.core.file.FileFolderData;
import com.elementsplus.network.FileFolderDataPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** 文件夹界面：只显示文件夹内容列表，下方是玩家物品栏。 */
public class FileFolderMenu extends AbstractContainerMenu {
    public static final int IMAGE_WIDTH = 320;
    public static final int IMAGE_HEIGHT = 280;
    public static final int INVENTORY_X = 79;
    public static final int INVENTORY_Y = 196;
    public static final int HOTBAR_Y = 254;

    private final Inventory playerInventory;
    private final @Nullable InteractionHand hand;
    private final boolean serverSide;
    private int lockedInventoryIndex = -1;

    public FileFolderMenu(int id, Inventory inventory) {
        this(id, inventory, null);
    }

    public FileFolderMenu(int id, Inventory inventory, @Nullable InteractionHand hand) {
        super(com.elementsplus.ModMenuTypes.FILE_FOLDER, id);
        this.playerInventory = inventory;
        this.hand = hand;
        this.serverSide = !inventory.player.level().isClientSide;
        if (hand == InteractionHand.MAIN_HAND) {
            this.lockedInventoryIndex = inventory.selected;
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(createPlayerSlot(9 + row * 9 + col,
                        INVENTORY_X + col * 18, INVENTORY_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(createPlayerSlot(col, INVENTORY_X + col * 18, HOTBAR_Y));
        }
    }

    private Slot createPlayerSlot(int inventoryIndex, int x, int y) {
        return new Slot(playerInventory, inventoryIndex, x, y) {
            @Override
            public boolean mayPickup(Player player) {
                return inventoryIndex != lockedInventoryIndex;
            }

            @Override
            public boolean mayPlace(ItemStack stack) {
                return inventoryIndex != lockedInventoryIndex;
            }
        };
    }

    public void setLockedInventoryIndex(int index) {
        this.lockedInventoryIndex = index;
    }

    public Inventory getPlayerInventory() {
        return playerInventory;
    }

    public int capacityItems() {
        return ElementsPlusServerConfig.fileFolderCapacity();
    }

    public int capacityBytes() {
        return ElementsPlusServerConfig.fileFolderMaxBytes();
    }

    private ItemStack folderStack() {
        if (hand == null) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = playerInventory.player.getItemInHand(hand);
        return stack.is(ModItems.FILE_FOLDER) ? stack : ItemStack.EMPTY;
    }

    public void sendContents(ServerPlayer player) {
        ItemStack folder = folderStack();
        List<ItemStack> contents = folder.isEmpty()
                ? List.of() : new ArrayList<>(FileFolderData.contents(folder));
        ServerPlayNetworking.send(player, new FileFolderDataPayload(containerId, contents,
                capacityItems(), capacityBytes(), this.getCarried().copy(), lockedInventoryIndex));
    }

    public void insertCarried(ServerPlayer player) {
        if (!(player.containerMenu instanceof FileFolderMenu menu) || menu != this) {
            return;
        }
        ItemStack carried = this.getCarried();
        if (!FileFolderData.isStorable(carried)) {
            return;
        }
        ItemStack folder = folderStack();
        if (folder.isEmpty()) {
            return;
        }
        List<ItemStack> contents = new ArrayList<>(FileFolderData.contents(folder));
        int size = FileFolderData.fileBytes(carried);
        int addedItems = FileFolderData.itemCount(carried);
        if (FileFolderData.itemCount(contents) + addedItems > capacityItems()
                || FileFolderData.totalBytes(contents) + size > capacityBytes()) {
            sendContents(player);
            return;
        }
        ItemStack moved = carried.copy();
        moved.setCount(1);
        contents.add(moved);
        FileFolderData.setContents(folder, contents);
        carried.shrink(1);
        this.setCarried(carried.isEmpty() ? ItemStack.EMPTY : carried);
        sendContents(player);
    }

    public void takeToCursor(ServerPlayer player, int index) {
        if (!(player.containerMenu instanceof FileFolderMenu menu) || menu != this
                || !this.getCarried().isEmpty()) {
            return;
        }
        ItemStack folder = folderStack();
        if (folder.isEmpty()) {
            return;
        }
        List<ItemStack> contents = new ArrayList<>(FileFolderData.contents(folder));
        if (index < 0 || index >= contents.size()) {
            return;
        }
        ItemStack taken = contents.remove(index);
        FileFolderData.setContents(folder, contents);
        this.setCarried(taken.copy());
        sendContents(player);
    }

    public void takeToInventory(ServerPlayer player, int index) {
        if (!(player.containerMenu instanceof FileFolderMenu menu) || menu != this) {
            return;
        }
        ItemStack folder = folderStack();
        if (folder.isEmpty()) {
            return;
        }
        List<ItemStack> contents = new ArrayList<>(FileFolderData.contents(folder));
        if (index < 0 || index >= contents.size()) {
            return;
        }
        ItemStack taken = contents.get(index).copy();
        if (!player.getInventory().add(taken)) {
            return;
        }
        contents.remove(index);
        FileFolderData.setContents(folder, contents);
        player.containerMenu.broadcastChanges();
        sendContents(player);
    }

    @Override
    public @NotNull ItemStack quickMoveStack(Player player, int index) {
        if (!(player instanceof ServerPlayer serverPlayer) || !serverSide
                || index < 0 || index >= this.slots.size()) {
            return ItemStack.EMPTY;
        }
        int inventoryIndex = index < 27 ? index + 9 : index - 27;
        if (inventoryIndex == lockedInventoryIndex) {
            return ItemStack.EMPTY;
        }
        Slot slot = this.slots.get(index);
        ItemStack stack = slot.getItem();
        if (!FileFolderData.isStorable(stack)) {
            return ItemStack.EMPTY;
        }
        ItemStack folder = folderStack();
        if (folder.isEmpty()) {
            return ItemStack.EMPTY;
        }
        List<ItemStack> contents = new ArrayList<>(FileFolderData.contents(folder));
        int size = FileFolderData.fileBytes(stack);
        int addedItems = FileFolderData.itemCount(stack);
        if (FileFolderData.itemCount(contents) + addedItems > capacityItems()
                || FileFolderData.totalBytes(contents) + size > capacityBytes()) {
            return ItemStack.EMPTY;
        }
        ItemStack moved = stack.copy();
        moved.setCount(1);
        contents.add(moved);
        FileFolderData.setContents(folder, contents);
        stack.shrink(1);
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        sendContents(serverPlayer);
        return moved;
    }

    @Override
    public boolean stillValid(Player player) {
        if (hand == null) {
            return true;
        }
        return player.getItemInHand(hand).is(ModItems.FILE_FOLDER);
    }

    @Override
    public void broadcastChanges() {
        if (serverSide && hand != null
                && playerInventory.player instanceof ServerPlayer serverPlayer
                && serverPlayer.containerMenu == this
                && !serverPlayer.getItemInHand(hand).is(ModItems.FILE_FOLDER)) {
            serverPlayer.closeContainer();
            return;
        }
        super.broadcastChanges();
    }
}
