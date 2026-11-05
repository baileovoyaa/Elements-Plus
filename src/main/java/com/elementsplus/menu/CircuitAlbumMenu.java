package com.elementsplus.menu;

import com.elementsplus.ModItems;
import com.elementsplus.core.circuit.CircuitAlbumData;
import com.elementsplus.core.config.ElementsPlusServerConfig;
import com.elementsplus.network.CircuitAlbumDataPayload;
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

/** 电路图册界面：下方玩家物品栏，上方是可滚动电路图列表（列表本身由客户端绘制）。 */
public class CircuitAlbumMenu extends AbstractContainerMenu {
    public static final int IMAGE_WIDTH = 320;
    public static final int IMAGE_HEIGHT = 280;
    public static final int INVENTORY_X = 79;
    public static final int INVENTORY_Y = 196;
    public static final int HOTBAR_Y = 254;

    private final Inventory playerInventory;
    private final @Nullable InteractionHand hand;
    private final boolean serverSide;
    /** 物品栏中需要锁定的格子（主手电路图册所在快捷栏格）；-1 表示没有可锁定的可视格。 */
    private int lockedInventoryIndex = -1;

    public CircuitAlbumMenu(int id, Inventory inventory) {
        this(id, inventory, null);
    }

    public CircuitAlbumMenu(int id, Inventory inventory, @Nullable InteractionHand hand) {
        super(com.elementsplus.ModMenuTypes.CIRCUIT_ALBUM, id);
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

    public int capacity() {
        return ElementsPlusServerConfig.circuitAlbumCapacity();
    }

    private ItemStack albumStack() {
        if (hand == null) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = playerInventory.player.getItemInHand(hand);
        return stack.is(ModItems.CIRCUIT_ALBUM) ? stack : ItemStack.EMPTY;
    }

    public void sendContents(ServerPlayer player) {
        ItemStack album = albumStack();
        List<ItemStack> contents = album.isEmpty()
                ? List.of()
                : new ArrayList<>(CircuitAlbumData.contents(album));
        ServerPlayNetworking.send(player, new CircuitAlbumDataPayload(
                containerId, capacity(), contents, this.getCarried().copy(), lockedInventoryIndex));
    }

    public void insertCarried(ServerPlayer player) {
        if (!(player.containerMenu instanceof CircuitAlbumMenu menu) || menu != this) {
            return;
        }
        ItemStack carried = this.getCarried();
        if (carried.isEmpty() || !carried.is(ModItems.CIRCUIT_DIAGRAM)) {
            return;
        }
        ItemStack album = albumStack();
        if (album.isEmpty()) {
            return;
        }
        List<ItemStack> contents = new ArrayList<>(CircuitAlbumData.contents(album));
        if (contents.size() >= capacity()) {
            sendContents(player);
            return;
        }
        ItemStack moved = carried.copy();
        moved.setCount(1);
        contents.add(moved);
        CircuitAlbumData.setContents(album, contents);

        carried.shrink(1);
        this.setCarried(carried.isEmpty() ? ItemStack.EMPTY : carried);
        sendContents(player);
    }

    public void takeToCursor(ServerPlayer player, int index) {
        if (!(player.containerMenu instanceof CircuitAlbumMenu menu) || menu != this) {
            return;
        }
        if (!this.getCarried().isEmpty()) {
            return;
        }
        ItemStack album = albumStack();
        if (album.isEmpty()) {
            return;
        }
        List<ItemStack> contents = new ArrayList<>(CircuitAlbumData.contents(album));
        if (index < 0 || index >= contents.size()) {
            return;
        }
        ItemStack taken = contents.remove(index);
        CircuitAlbumData.setContents(album, contents);
        this.setCarried(taken.copy());
        sendContents(player);
    }

    public void takeToInventory(ServerPlayer player, int index) {
        if (!(player.containerMenu instanceof CircuitAlbumMenu menu) || menu != this) {
            return;
        }
        ItemStack album = albumStack();
        if (album.isEmpty()) {
            return;
        }
        List<ItemStack> contents = new ArrayList<>(CircuitAlbumData.contents(album));
        if (index < 0 || index >= contents.size()) {
            return;
        }
        ItemStack taken = contents.get(index).copy();
        if (!player.getInventory().add(taken)) {
            return;
        }
        contents.remove(index);
        CircuitAlbumData.setContents(album, contents);
        player.containerMenu.broadcastChanges();
        sendContents(player);
    }

    @Override
    public @NotNull ItemStack quickMoveStack(Player player, int index) {
        if (!(player instanceof ServerPlayer serverPlayer) || !serverSide) {
            return ItemStack.EMPTY;
        }
        if (index < 0 || index >= this.slots.size()) {
            return ItemStack.EMPTY;
        }
        int inventoryIndex = index < 27 ? index + 9 : index - 27;
        if (inventoryIndex == lockedInventoryIndex) {
            return ItemStack.EMPTY;
        }
        Slot slot = this.slots.get(index);
        ItemStack stack = slot.getItem();
        if (stack.isEmpty() || !stack.is(ModItems.CIRCUIT_DIAGRAM)) {
            return ItemStack.EMPTY;
        }
        ItemStack album = albumStack();
        if (album.isEmpty()) {
            return ItemStack.EMPTY;
        }
        List<ItemStack> contents = new ArrayList<>(CircuitAlbumData.contents(album));
        if (contents.size() >= capacity()) {
            return ItemStack.EMPTY;
        }
        ItemStack result = stack.copy();
        ItemStack moved = stack.copy();
        moved.setCount(1);
        contents.add(moved);
        CircuitAlbumData.setContents(album, contents);

        stack.shrink(1);
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        sendContents(serverPlayer);
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        if (hand == null) {
            return true;
        }
        return player.getItemInHand(hand).is(ModItems.CIRCUIT_ALBUM);
    }

    @Override
    public void broadcastChanges() {
        // 兜底：例如按 F 交换副手时仍可能绕过容器槽位限制，离开手部后关闭界面避免幽灵界面。
        if (serverSide && hand != null
                && playerInventory.player instanceof ServerPlayer serverPlayer
                && serverPlayer.containerMenu == this
                && !serverPlayer.getItemInHand(hand).is(ModItems.CIRCUIT_ALBUM)) {
            serverPlayer.closeContainer();
            return;
        }
        super.broadcastChanges();
    }
}
