package com.elementsplus.menu;

import com.elementsplus.ModBlocks;
import com.elementsplus.ModDataComponents;
import com.elementsplus.ModItems;
import com.elementsplus.ModMenuTypes;
import com.elementsplus.core.circuit.ChipManufacture;
import com.elementsplus.core.circuit.diagram.CircuitDiagram;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class LithographyMachineMenu extends AbstractContainerMenu {
    private final ContainerLevelAccess access;
    private final Inventory playerInventory;
    private final boolean serverSide;
    public final Slot diagramSlot;
    public final Slot manufactureInputSlot;
    public final Slot manufactureOutputSlot;
    Runnable slotUpdateListener = () -> {
    };
    /**
     * 制造页复制电路图时，是否保留 diagram 槽电路图的“已编译”状态（EQUIVALENT_COMPONENT）。
     * 默认勾选，与客户端 CheckBox 默认状态保持一致。
     */
    private boolean copyCompiled = true;
    private boolean updatingResult = false;
    private boolean outputGenerated = false;
    public final Container container = new SimpleContainer(3) {
        @Override
        public void setChanged() {
            super.setChanged();
            slotsChanged(this);
            slotUpdateListener.run();
        }
    };

    public LithographyMachineMenu(int i, Inventory inventory) {
        this(i, inventory, ContainerLevelAccess.NULL);
    }

    public LithographyMachineMenu(int i, Inventory inventory, ContainerLevelAccess containerLevelAccess) {
        super(ModMenuTypes.LITHOGRAPHY_MACHINE, i);
        this.access = containerLevelAccess;
        this.playerInventory = inventory;
        this.serverSide = !inventory.player.level().isClientSide();

        for (int j = 0; j < 3; j++) {
            for (int k = 0; k < 9; k++) {
                this.addSlot(new Slot(inventory, k + j * 9 + 9, 6 + j * 18, 8 + k * 18));
            }
        }

        for (int j = 0; j < 9; j++) {
            this.addSlot(new Slot(inventory, j, 6 + 3 * 18 + 3, 8 + j * 18));
        }

        diagramSlot = this.addSlot(new Slot(this.container, 0, 90, 26) {
            @Override
            public void onTake(Player player, ItemStack itemStack) {
                super.onTake(player, itemStack);
            }
        });

        manufactureInputSlot = this.addSlot(new Slot(this.container, 1, 108, 26));
        manufactureOutputSlot = this.addSlot(new Slot(this.container, 2, 150, 26) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(Player player, ItemStack itemStack) {
                if (isManufactureEligible(container.getItem(0), container.getItem(1))) {
                    // 制造模式：取出前先扣空芯片与原料，再重算输出
                    consumeManufactureForTake();
                } else {
                    // 复制模式：默认 onTake 触发 setChanged → updateManufactureResult 消耗模板
                    super.onTake(player, itemStack);
                }
            }
        });

        updateManufactureResult();
    }

    /**
     * 复制电路图：副本继承模板槽物品的其他组件，CIRCUIT_DIAGRAM 覆写为 diagram 槽的电路图；
     * diagram 槽电路图带有 EQUIVALENT_COMPONENT 且勾选“复制编译状态”时，一并复制该组件。
     */
    private boolean isCopyEligible(ItemStack diagram, ItemStack input) {
        return !diagram.isEmpty() && diagram.is(ModItems.CIRCUIT_DIAGRAM)
                && diagram.has(ModDataComponents.CIRCUIT_DIAGRAM)
                && !input.isEmpty() && input.is(ModItems.CIRCUIT_DIAGRAM)
                && !input.has(ModDataComponents.EQUIVALENT_COMPONENT);
    }

    private ItemStack manufactureCopy(ItemStack diagram, ItemStack input) {
        if (!isCopyEligible(diagram, input)) {
            return ItemStack.EMPTY;
        }
        ItemStack output = input.copy();
        output.setCount(1);
        output.set(ModDataComponents.CIRCUIT_DIAGRAM, diagram.get(ModDataComponents.CIRCUIT_DIAGRAM).copy());
        if (copyCompiled && diagram.has(ModDataComponents.EQUIVALENT_COMPONENT)) {
            output.set(ModDataComponents.EQUIVALENT_COMPONENT, diagram.get(ModDataComponents.EQUIVALENT_COMPONENT));
        } else {
            output.remove(ModDataComponents.EQUIVALENT_COMPONENT);
        }
        return output;
    }

    /**
     * 制造分支前提：diagram 槽的电路图带 EQUIVALENT_COMPONENT，输入槽放空芯片，且空芯片规模能容纳电路图规模。
     */
    private boolean isManufactureEligible(ItemStack diagram, ItemStack input) {
        if (diagram.isEmpty() || !diagram.is(ModItems.CIRCUIT_DIAGRAM)
                || !diagram.has(ModDataComponents.CIRCUIT_DIAGRAM)
                || !diagram.has(ModDataComponents.EQUIVALENT_COMPONENT)) {
            return false;
        }
        CircuitDiagram.Scale chipScale = ChipManufacture.emptyChipScale(input);
        if (chipScale == null) {
            return false;
        }
        CircuitDiagram circuit = diagram.get(ModDataComponents.CIRCUIT_DIAGRAM);
        return circuit != null && ChipManufacture.canHoldScale(chipScale, circuit.scale);
    }

    /**
     * 根据 diagram/模板槽内容维护输出槽。
     * 输出槽被取走后（内容变空而仍在产出条件内），消耗一份模板并基于剩余模板重新生成。
     */
    private void updateManufactureResult() {
        if (updatingResult || !serverSide) {
            return;
        }
        updatingResult = true;
        try {
            ItemStack diagram = this.container.getItem(0);
            ItemStack input = this.container.getItem(1);
            if (isCopyEligible(diagram, input)) {
                updateCopyResult(diagram, input);
                return;
            }
            if (isManufactureEligible(diagram, input)) {
                updateChipResult(diagram, input);
                return;
            }
            outputGenerated = false;
            if (!this.container.getItem(2).isEmpty()) {
                this.container.setItem(2, ItemStack.EMPTY);
            }
        } finally {
            updatingResult = false;
        }
    }

    /** 复制分支（原逻辑）：复制电路图并维护模板消耗。 */
    private void updateCopyResult(ItemStack diagram, ItemStack input) {
        ItemStack expected = manufactureCopy(diagram, input);
        ItemStack current = this.container.getItem(2);
        if (ItemStack.isSameItemSameComponents(current, expected)) {
            return;
        }
        if (!current.isEmpty()) {
            // 输出槽内容与预期不符（如源电路被编辑）→ 直接覆写，不消耗模板
            this.container.setItem(2, expected);
            return;
        }
        // 输出槽为空：首次生成，或副本被取走
        if (outputGenerated) {
            this.container.removeItem(1, 1);
            input = this.container.getItem(1);
            expected = manufactureCopy(diagram, input);
        }
        outputGenerated = !expected.isEmpty();
        this.container.setItem(2, expected);
    }

    /**
     * 制造分支：原料足够则输出对应规模 chip；否则清空输出。
     * 取出 chip 的扣料由 {@link #consumeManufactureForTake()} 负责，此处只做幂等预期维护。
     */
    private void updateChipResult(ItemStack diagram, ItemStack input) {
        CircuitDiagram circuit = diagram.get(ModDataComponents.CIRCUIT_DIAGRAM);
        CircuitDiagram.Scale chipScale = ChipManufacture.emptyChipScale(input);
        ResourceLocation equiv = diagram.get(ModDataComponents.EQUIVALENT_COMPONENT);
        if (circuit == null || chipScale == null || equiv == null
                || !ChipManufacture.canHoldScale(chipScale, circuit.scale)) {
            outputGenerated = false;
            if (!this.container.getItem(2).isEmpty()) {
                this.container.setItem(2, ItemStack.EMPTY);
            }
            return;
        }
        boolean sufficient = ChipManufacture.allSufficient(
                this.playerInventory.items,
                ChipManufacture.computeRequirements(circuit));
        ItemStack expected = sufficient ? ChipManufacture.makeChip(chipScale, equiv) : ItemStack.EMPTY;
        ItemStack current = this.container.getItem(2);
        if (ItemStack.isSameItemSameComponents(current, expected)) {
            return;
        }
        if (!current.isEmpty()) {
            // 输出槽与预期不符（如源电路被编辑）→ 直接覆写，不消耗空芯片
            this.container.setItem(2, expected);
            return;
        }
        this.container.setItem(2, expected);
        outputGenerated = !expected.isEmpty();
    }

    /** 取出制造产物：扣 1 个空芯片 + 物品栏内所需原料，然后重算输出。 */
    private void consumeManufactureForTake() {
        if (!this.container.getItem(1).isEmpty()) {
            this.container.removeItem(1, 1);
        }
        ItemStack diagramStack = this.container.getItem(0);
        CircuitDiagram circuit = diagramStack.get(ModDataComponents.CIRCUIT_DIAGRAM);
        if (circuit != null) {
            for (ChipManufacture.MaterialRequirement requirement : ChipManufacture.computeRequirements(circuit)) {
                ChipManufacture.consume(this.playerInventory, requirement);
            }
        }
        markPlayerInventoryChanged();
        updateManufactureResult();
    }

    /** 强制物品栏槽位标记脏，让服务端下一次 broadcastChanges 把数量变化同步给客户端。 */
    private void markPlayerInventoryChanged() {
        for (int i = 0; i < 36; i++) {
            this.slots.get(i).setChanged();
        }
    }

    /**
     * 客户端 CheckBox 切换“复制编译状态”时的服务端回写。
     */
    public void setCopyCompiled(boolean value) {
        this.copyCompiled = value;
        updateManufactureResult();
    }

    @Override
    public @NotNull ItemStack quickMoveStack(Player player, int i) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(i);
        if (slot != null && slot.hasItem()) {
            if (i == 38) {
                return quickMoveManufactureOutput(player, slot);
            }
            ItemStack itemStack2 = slot.getItem();
            itemStack = itemStack2.copy();
            if (i < 36) {
                if (!this.moveItemStackTo(itemStack2, 36, 38, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(itemStack2, 0, 36, false)) {
                return ItemStack.EMPTY;
            }
            if (itemStack2.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            if (itemStack2.getCount() == itemStack.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, itemStack2);
        }
        return itemStack;
    }

    /**
     * Shift 点击制造输出槽：批量制造（类似原版工作台），
     * 直到输入槽为空、原料不足或背包放不下为止。
     */
    private ItemStack quickMoveManufactureOutput(Player player, Slot slot) {
        if (!isManufactureEligible(this.container.getItem(0), this.container.getItem(1))) {
            // 复制模式：单次移出，交给默认 onTake → updateManufactureResult 消耗模板
            ItemStack copy = slot.getItem().copy();
            ItemStack stack2 = slot.getItem();
            if (!this.moveItemStackTo(stack2, 0, 36, false)) {
                return ItemStack.EMPTY;
            }
            if (stack2.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            if (stack2.getCount() == copy.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, stack2);
            return copy;
        }
        ItemStack moved = ItemStack.EMPTY;
        int safety = 0;
        while (safety++ < 64) {
            if (!isManufactureEligible(this.container.getItem(0), this.container.getItem(1))) {
                break;
            }
            ItemStack out = slot.getItem();
            if (out.isEmpty()) {
                break;
            }
            ItemStack before = out.copy();
            if (!this.moveItemStackTo(out, 0, 36, false)) {
                break;
            }
            if (out.getCount() == before.getCount()) {
                break;
            }
            if (moved.isEmpty()) {
                moved = before;
            }
            if (slot.getItem().isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            }
            consumeManufactureForTake();
        }
        return moved;
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        // 物品栏可能在任意时机变化（外部拾取/丢弃），每 tick 重算输出
        updateManufactureResult();
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, ModBlocks.LITHOGRAPHY_MACHINE);
    }

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        updateManufactureResult();
    }

    public void onDiagramChanged() {
        this.container.setChanged();
    }

    public Inventory getPlayerInventory() {
        return playerInventory;
    }

    public void returnCarriedToInventory() {
        ItemStack carried = this.getCarried();
        if (carried.isEmpty()) {
            return;
        }
        this.moveItemStackTo(carried, 0, 36, false);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);

        // 如果有结果/输出槽，通常不返还，先清空，防止复制物品
        // this.resultContainer.removeItemNoUpdate(1);

        // 返还输入容器中的物品
        this.access.execute((level, blockPos) -> this.clearContainer(player, this.container));
    }

}