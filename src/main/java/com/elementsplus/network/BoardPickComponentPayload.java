package com.elementsplus.network;

import com.elementsplus.ElementsPlus;
import com.elementsplus.ModItems;
import com.elementsplus.core.circuit.ChipManufacture;
import com.elementsplus.core.circuit.CircuitComponent;
import com.elementsplus.core.circuit.CircuitComponentRegistry;
import com.elementsplus.menu.LithographyMachineMenu;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** 生存模式从电路板元件列表/中键拾取一个真实元件物品到鼠标上。 */
public record BoardPickComponentPayload(ResourceLocation componentId) implements CustomPacketPayload {
    public static final Type<BoardPickComponentPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(ElementsPlus.MOD_ID, "board_pick_component"));
    public static final StreamCodec<FriendlyByteBuf, BoardPickComponentPayload> STREAM_CODEC =
            StreamCodec.of(
                    (buf, payload) -> buf.writeResourceLocation(payload.componentId()),
                    buf -> new BoardPickComponentPayload(buf.readResourceLocation())
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(BoardPickComponentPayload payload, ServerPlayNetworking.Context context) {
        ServerPlayer player = context.player();
        LithographyMachineMenu menu = ServerPayloadGate.menu(player, LithographyMachineMenu.class);
        if (menu == null
                || player.isCreative()
                || !menu.diagramSlot.getItem().is(ModItems.CIRCUIT_BOARD)
                || !menu.getCarried().isEmpty()) {
            return;
        }
        CircuitComponent component = CircuitComponentRegistry.byId(payload.componentId());
        if (component == null) return;
        ItemStack selected = ItemStack.EMPTY;
        if (component.getIngredientSupplier() instanceof CircuitComponent.ItemIngredientSupplier items) {
            for (ItemStack wanted : items.itemStacks()) {
                if (wanted.isEmpty()) continue;
                ItemStack found = findItem(player, wanted.getItem());
                if (found != null) {
                    selected = found.copyWithCount(1);
                    break;
                }
            }
        } else if (component.getIngredientSupplier() instanceof CircuitComponent.ComponentIngredientSupplier components) {
            ChipManufacture.ComponentRequirement requirement =
                    new ChipManufacture.ComponentRequirement(components.component(), 1);
            for (ItemStack stack : player.getInventory().items) {
                if (stack.isEmpty() || !ChipManufacture.isChipItem(stack)) continue;
                if (ChipManufacture.countOwned(java.util.List.of(stack), requirement) > 0) {
                    selected = stack.copyWithCount(1);
                    break;
                }
            }
        }
        if (selected.isEmpty()) return;
        // 从物品栏扣除一个；先扣普通栏位。
        if (!takeOne(player, selected)) return;
        menu.setCarried(selected);
        menu.markPlayerInventoryChangedPublic();
    }

    private static ItemStack findItem(ServerPlayer player, net.minecraft.world.item.Item item) {
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty() && stack.is(item)) return stack;
        }
        return null;
    }

    private static boolean takeOne(ServerPlayer player, ItemStack selected) {
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, selected)) {
                stack.shrink(1);
                return true;
            }
        }
        return false;
    }
}
