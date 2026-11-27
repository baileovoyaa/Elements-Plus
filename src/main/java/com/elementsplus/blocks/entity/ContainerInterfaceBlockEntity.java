package com.elementsplus.blocks.entity;

import com.elementsplus.ModBlockEntityTypes;
import com.elementsplus.blocks.RedstoneInterfaceBlock;
import com.elementsplus.menu.ContainerInterfaceMenu;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.arguments.item.ItemPredicateArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 容器接口方块实体：在红石接口的电路/电池/磁盘体系上增加 IS 槽位和 6 个内部架构引脚，
 * 解析 8 位容器命令流并通过结果缓冲区回传。
 */
public class ContainerInterfaceBlockEntity extends RedstoneInterfaceBlockEntity {
    public static final int SLOT_IS = 8;
    public static final int SLOT_COUNT = 9;

    private static final String[] INTERNAL_PINS = {"VI", "DI", "RO", "RI", "VO", "DO"};
    private static final int RESULT_CAPACITY = 1024;
    private static final int ARGS_CAPACITY = 1024;

    private ItemStack interfaceSlot = ItemStack.EMPTY;
    private int cp;
    private int sp;
    private int argi;
    private long timeTicks;
    private final StringBuilder args = new StringBuilder();
    private final ByteArrayOutputStream result = new ByteArrayOutputStream();
    private final byte[] stash = new byte[RESULT_CAPACITY];
    private int stashSize;
    private int pendingOpcode = -1;
    private int pendingRemaining;
    private boolean pendingLength;
    private byte[] pendingData = new byte[0];
    private int pendingPos;
    private int lastVo;
    private int lastRi;
    private int lastDo = -1;
    private final List<String> iteratorKeys = new ArrayList<>();
    private int iteratorIndex = -1;
    private String iteratorPath = "";
    private boolean iterating;
    private boolean stateDirty;

    public ContainerInterfaceBlockEntity(BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        this(ModBlockEntityTypes.CONTAINER_INTERFACE, pos, state);
    }

    public ContainerInterfaceBlockEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type,
                                         BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        super(type, pos, state);
    }

    @Override
    protected boolean isInternalPin(String label) {
        for (String pin : INTERNAL_PINS) {
            if (pin.equals(label)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.elements-plus.container_interface");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        refreshCircuitForMenu();
        syncForMenu();
        return new ContainerInterfaceMenu(id, inventory, this, getData());
    }

    private void refreshCircuitForMenu() {
        // 父类 createMenu 会调用 refreshCircuit()，但它是私有的；触发一次 setChanged 即可。
        setChanged();
    }

    private void syncForMenu() {
        // 让客户端在打开界面前拿到最新的表面状态。
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    public ItemStack getItem(int slot) {
        if (slot == SLOT_IS) {
            return interfaceSlot;
        }
        return slot >= 0 && slot < SLOT_IS ? super.getItem(slot) : ItemStack.EMPTY;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot == SLOT_IS) {
            interfaceSlot = stack == null ? ItemStack.EMPTY : stack;
            setChanged();
        } else if (slot >= 0 && slot < SLOT_IS) {
            super.setItem(slot, stack);
        }
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (slot == SLOT_IS) {
            if (interfaceSlot.isEmpty() || amount <= 0) {
                return ItemStack.EMPTY;
            }
            ItemStack result = interfaceSlot.split(amount);
            setChanged();
            return result;
        }
        return super.removeItem(slot, amount);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (slot == SLOT_IS) {
            ItemStack stack = interfaceSlot;
            interfaceSlot = ItemStack.EMPTY;
            return stack;
        }
        return super.removeItemNoUpdate(slot);
    }

    @Override
    public void clearContent() {
        super.clearContent();
        interfaceSlot = ItemStack.EMPTY;
        setChanged();
    }

    @Override
    public boolean isEmpty() {
        return super.isEmpty() && interfaceSlot.isEmpty();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        NonNullList<ItemStack> one = NonNullList.withSize(1, ItemStack.EMPTY);
        one.set(0, interfaceSlot);
        CompoundTag is = new CompoundTag();
        ContainerHelper.saveAllItems(is, one, provider);
        tag.put("InterfaceSlot", is);
        tag.putInt("CP", cp);
        tag.putInt("SP", sp);
        tag.putInt("ARGI", argi);
        tag.putLong("TimeTicks", timeTicks);
        tag.putString("ARGS", args.toString());
        tag.putByteArray("Result", result.toByteArray());
        tag.putByteArray("Stash", Arrays.copyOf(stash, stashSize));
        tag.putInt("PendingOpcode", pendingOpcode);
        tag.putBoolean("PendingLength", pendingLength);
        tag.putInt("PendingPos", pendingPos);
        tag.putByteArray("PendingData", pendingData);
        tag.putString("IteratorPath", iteratorPath);
        tag.putBoolean("Iterating", iterating);
        tag.putInt("IteratorIndex", iteratorIndex);
        ListTag iteratorList = new ListTag();
        for (String key : iteratorKeys) {
            iteratorList.add(StringTag.valueOf(key));
        }
        tag.put("IteratorKeys", iteratorList);
    }

    @Override
    public void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        if (tag.contains("InterfaceSlot", Tag.TAG_COMPOUND)) {
            NonNullList<ItemStack> one = NonNullList.withSize(1, ItemStack.EMPTY);
            ContainerHelper.loadAllItems(tag.getCompound("InterfaceSlot"), one, provider);
            interfaceSlot = one.get(0);
        }
        cp = tag.getInt("CP");
        sp = tag.getInt("SP");
        argi = tag.getInt("ARGI");
        timeTicks = tag.getLong("TimeTicks");
        args.setLength(0);
        args.append(tag.getString("ARGS"));
        result.reset();
        byte[] savedResult = tag.getByteArray("Result");
        result.write(savedResult, 0, Math.min(savedResult.length, RESULT_CAPACITY));
        byte[] savedStash = tag.getByteArray("Stash");
        stashSize = Math.min(savedStash.length, stash.length);
        System.arraycopy(savedStash, 0, stash, 0, stashSize);
        pendingOpcode = tag.getInt("PendingOpcode");
        pendingLength = tag.getBoolean("PendingLength");
        pendingPos = tag.getInt("PendingPos");
        pendingData = tag.getByteArray("PendingData");
        iteratorPath = tag.getString("IteratorPath");
        iterating = tag.getBoolean("Iterating");
        iteratorIndex = tag.getInt("IteratorIndex");
        iteratorKeys.clear();
        ListTag keys = tag.getList("IteratorKeys", Tag.TAG_STRING);
        for (int i = 0; i < keys.size(); i++) {
            iteratorKeys.add(keys.getString(i));
        }
    }

    @Override
    public void serverTick() {
        if (level == null || level.isClientSide) {
            return;
        }
        if (isRunning()) {
            timeTicks++;
        }
        updateArchitectureInputs();
        super.serverTick();
        readArchitectureOutputs();
        updateArchitectureInputs();
        if (stateDirty) {
            stateDirty = false;
            setChanged();
        }
    }

    private void updateArchitectureInputs() {
        setInputPinValue("VI", result.size() > 0 ? 1 : 0);
        setInputPinValue("DI", result.size() > 0 ? result.toByteArray()[0] & 0xFF : 0);
        setInputPinValue("RO", 1);
    }

    private void readArchitectureOutputs() {
        int ri = getOutputPinValue("RI") != 0 ? 1 : 0;
        if (ri != 0 && lastRi == 0 && result.size() > 0) {
            byte[] bytes = result.toByteArray();
            result.reset();
            result.write(bytes, 1, bytes.length - 1);
            stateDirty = true;
        }
        lastRi = ri;
        int vo = getOutputPinValue("VO") != 0 ? 1 : 0;
        int doValue = getOutputPinValue("DO") & 0xFF;
        if (vo != 0 && (lastVo == 0 || doValue != lastDo)) {
            acceptCommandByte(doValue);
        }
        lastVo = vo;
        lastDo = doValue;
    }

    /* ============================================================
     * 命令解析
     * ============================================================ */

    private void acceptCommandByte(int value) {
        if (pendingOpcode >= 0) {
            if (pendingLength) {
                pendingLength = false;
                pendingData = new byte[value];
                pendingPos = 0;
                if (value == 0) {
                    executePending();
                }
                return;
            }
            pendingData[pendingPos++] = (byte) value;
            if (pendingPos >= pendingData.length) {
                executePending();
            }
            return;
        }
        int opcode = value & 0xFF;
        if (opcode == 0x10) {
            pendingOpcode = opcode;
            pendingData = new byte[1];
            pendingPos = 0;
            pendingRemaining = 0;
            return;
        }
        if (opcode == 0x14 || opcode == 0x15) {
            pendingOpcode = opcode;
            pendingLength = true;
            return;
        }
        execute(opcode, new byte[0]);
    }

    private void executePending() {
        int opcode = pendingOpcode;
        byte[] data = pendingData;
        pendingOpcode = -1;
        pendingRemaining = 0;
        pendingLength = false;
        pendingData = new byte[0];
        pendingPos = 0;
        execute(opcode, data);
    }

    private void execute(int opcode, byte[] data) {
        try {
            switch (opcode) {
                case 0x00 -> {
                }
                case 0x01 -> appendValue(0x02, "CONTAINER_INTERFACE v1.0.0".getBytes(StandardCharsets.UTF_8));
                case 0x02 -> appendValue(0x00, intBytes((int) timeTicks & 0xFFFFFFFFL, 4));
                case 0x03 -> resetRuntime();
                case 0x04 -> setRunning(false);
                case 0x05 -> {
                    resetRuntime();
                    setRunning(false);
                }
                case 0x10 -> argi = data.length > 0 ? data[0] & 0xFF : argi;
                case 0x11 -> argi = (argi + 1) & 0xFF;
                case 0x12 -> argi = (argi - 1) & 0xFF;
                case 0x13 -> appendValue(0x00, intBytes(argi, 1));
                case 0x14, 0x15 -> executeArgs(opcode, data);
                case 0x16 -> appendValue(0x02, args.toString().getBytes(StandardCharsets.UTF_8));
                case 0x20 -> sp = argi;
                case 0x21 -> appendValue(0x00, intBytes(sp, 1));
                case 0x22 -> appendStatus(slotExists() ? 0x00 : (container() == null ? 0x41 : 0x01));
                case 0x23 -> sp = (sp + 1) & 0xFF;
                case 0x24 -> sp = (sp - 1) & 0xFF;
                case 0x25 -> slotSwap();
                case 0x26 -> slotMerge(false);
                case 0x27 -> slotMerge(true);
                case 0x28 -> slotTake();
                case 0x29 -> slotPut();
                case 0x2A -> slotDrop();
                case 0x2B -> appendStatus(slotMatchStatus());
                case 0x2C -> appendStatus(slotSameStatus());
                case 0x2D -> slotReady();
                case 0x2E -> dropInterfaceSlot();
                case 0x40, 0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0x47 -> cp = opcode & 7;
                case 0x48 -> appendValue(0x00, intBytes(cp, 1));
                case 0x49 -> contInsert();
                case 0x4A -> {
                    if (container() == null) {
                        appendStatus(0x41);
                    } else {
                        appendStatus(contReady() ? 0x00 : 0x01);
                    }
                }
                case 0x4B -> contCollect();
                case 0x4C -> appendStatus(container() == null ? 0x01 : 0x00);
                case 0x4D -> {
                    Container container = container();
                    if (container == null) {
                        appendStatus(0x41);
                    } else {
                        appendStatusValue(0x00, intBytes(Math.min(255, container.getContainerSize()), 1));
                    }
                }
                case 0x4E -> contFind();
                case 0x50 -> readId();
                case 0x51 -> {
                    if (!slotExists()) {
                        appendStatus(slotErrorStatus());
                    } else {
                        appendStatusValue(0x00, 0x02, nameBytes(false));
                    }
                }
                case 0x52 -> {
                    if (!slotExists()) {
                        appendStatus(slotErrorStatus());
                    } else {
                        appendStatusValue(0x00, 0x02, localizedNameBytes(false));
                    }
                }
                case 0x53 -> {
                    if (!slotExists()) {
                        appendStatus(slotErrorStatus());
                    } else {
                        appendStatusValue(0x00, 0x02, localizedNameBytes(true));
                    }
                }
                case 0x54 -> {
                    if (!slotExists()) {
                        appendStatus(slotErrorStatus());
                    } else {
                        appendStatusValue(0x00, 0x00, intBytes(slotStack().getCount(), 1));
                    }
                }
                case 0x55 -> {
                    if (!slotExists()) {
                        appendStatus(slotErrorStatus());
                    } else {
                        appendStatusValue(0x00, 0x00, intBytes(slotStack().getMaxStackSize(), 1));
                    }
                }
                case 0x56 -> damage();
                case 0x57 -> color();
                case 0x60 -> compSize();
                case 0x61 -> compType();
                case 0x62 -> compIter();
                case 0x63 -> compNext();
                case 0x64 -> compRead();
                case 0x65 -> compDump(false);
                case 0x66 -> compDump(true);
                case 0x70 -> {
                    stashSize = Math.min(stash.length, result.size());
                    byte[] bytes = result.toByteArray();
                    System.arraycopy(bytes, 0, stash, 0, stashSize);
                    result.reset();
                }
                case 0x71 -> {
                    result.reset();
                    result.write(stash, 0, stashSize);
                    stashSize = 0;
                }
                case 0x72 -> appendValue(0x00, intBytes(stashSize, 1));
                case 0x73 -> result.reset();
                default -> appendStatus(0x50);
            }
        } catch (RuntimeException e) {
            appendStatus(0x50);
        }
        stateDirty = true;
    }

    private void executeArgs(int opcode, byte[] data) {
        String text = new String(data, StandardCharsets.UTF_8);
        if (opcode == 0x14) {
            args.setLength(0);
        }
        if (args.toString().getBytes(StandardCharsets.UTF_8).length + data.length > ARGS_CAPACITY) {
            appendStatus(0x10);
            return;
        }
        args.append(text);
        appendStatus(0x00);
    }

    /* ============================================================
     * 槽位/容器操作
     * ============================================================ */

    private @Nullable Container container() {
        if (level == null) {
            return null;
        }
        if (cp == 6) {
            return this;
        }
        if (cp < 0 || cp >= FACES.length) {
            return null;
        }
        BlockEntity entity = level.getBlockEntity(worldPosition.relative(FACES[cp]));
        return entity instanceof Container container ? container : null;
    }

    private boolean slotExists() {
        Container container = container();
        return container != null && sp != 255 && sp >= 0 && sp < container.getContainerSize();
    }

    private int slotErrorStatus() {
        return container() == null ? 0x41 : 0x40;
    }

    private ItemStack slotStack() {
        Container container = container();
        if (container == null || sp < 0 || sp >= container.getContainerSize()) {
            return ItemStack.EMPTY;
        }
        return container.getItem(sp);
    }

    private void setSlotStack(ItemStack stack) {
        Container container = container();
        if (container == null || sp < 0 || sp >= container.getContainerSize()) {
            return;
        }
        container.setItem(sp, stack);
        container.setChanged();
    }

    private ItemStack interfaceStack() {
        return interfaceSlot;
    }

    private void setInterfaceStack(ItemStack stack) {
        interfaceSlot = stack == null ? ItemStack.EMPTY : stack;
        setChanged();
    }

    private void slotSwap() {
        if (!slotExists()) {
            appendStatus(container() == null ? 0x41 : 0x40);
            return;
        }
        ItemStack target = slotStack().copy();
        ItemStack current = interfaceStack().copy();
        if (!current.isEmpty() && !containerCanPlace(current)) {
            appendStatus(0x30);
            return;
        }
        setSlotStack(current);
        setInterfaceStack(target);
        appendStatus(0x00);
    }

    private void slotMerge(boolean interact) {
        if (!slotExists()) {
            appendStatus(container() == null ? 0x41 : 0x40);
            return;
        }
        ItemStack target = slotStack();
        ItemStack current = interfaceStack();
        if (interact && !ItemStack.isSameItemSameComponents(target, current)) {
            slotSwap();
            return;
        }
        if (!ItemStack.isSameItemSameComponents(target, current)) {
            appendStatus(0x02);
            return;
        }
        int max = Math.min(target.getMaxStackSize(), containerMaxStack());
        int move = Math.min(current.getCount(), max - target.getCount());
        if (move <= 0) {
            appendStatus(0x02);
            return;
        }
        target.grow(move);
        current.shrink(move);
        setSlotStack(target);
        setInterfaceStack(current);
        appendStatus(0x00);
    }

    private void slotTake() {
        if (!slotExists()) {
            appendStatus(container() == null ? 0x41 : 0x40);
            return;
        }
        if (argi == 0) {
            appendStatus(0x02);
            return;
        }
        ItemStack target = slotStack();
        if (target.isEmpty()) {
            appendStatus(0x02);
            return;
        }
        ItemStack current = interfaceStack();
        if (!current.isEmpty() && !ItemStack.isSameItemSameComponents(target, current)) {
            appendStatus(0x02);
            return;
        }
        int space = current.isEmpty() ? target.getMaxStackSize()
                : Math.min(target.getMaxStackSize(), containerMaxStack()) - current.getCount();
        int move = Math.min(argi, Math.min(target.getCount(), space));
        if (move <= 0) {
            appendStatus(0x02);
            return;
        }
        ItemStack taken = target.split(move);
        setSlotStack(target);
        setInterfaceStack(current.isEmpty() ? taken : current.copyWithCount(current.getCount() + move));
        appendStatusValue(0x00, intBytes(move, 1));
    }

    private void slotPut() {
        if (!slotExists()) {
            appendStatus(container() == null ? 0x41 : 0x40);
            return;
        }
        if (argi == 0 || interfaceStack().isEmpty()) {
            appendStatus(0x02);
            return;
        }
        ItemStack current = interfaceStack();
        if (!containerCanPlace(current)) {
            appendStatus(0x30);
            return;
        }
        ItemStack target = slotStack();
        int max = Math.min(target.isEmpty() ? current.getMaxStackSize() : target.getMaxStackSize(),
                containerMaxStack());
        int space = target.isEmpty() ? max : Math.min(max, target.getMaxStackSize()) - target.getCount();
        if (space <= 0 || (!target.isEmpty() && !ItemStack.isSameItemSameComponents(target, current))) {
            appendStatus(0x02);
            return;
        }
        int move = Math.min(argi, Math.min(current.getCount(), space));
        if (move <= 0) {
            appendStatus(0x02);
            return;
        }
        if (target.isEmpty()) {
            setSlotStack(current.copyWithCount(move));
        } else {
            target.grow(move);
            setSlotStack(target);
        }
        current.shrink(move);
        setInterfaceStack(current);
        appendStatusValue(0x00, intBytes(move, 1));
    }

    private void slotDrop() {
        if (!slotExists()) {
            appendStatus(container() == null ? 0x41 : 0x40);
            return;
        }
        ItemStack target = slotStack();
        if (target.isEmpty()) {
            appendStatus(0x02);
            return;
        }
        ItemStack drop = target.copy();
        setSlotStack(ItemStack.EMPTY);
        if (level instanceof ServerLevel serverLevel) {
            Containers.dropItemStack(serverLevel, worldPosition.getX() + 0.5,
                    worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, drop);
        }
        appendStatus(0x00);
    }

    private void dropInterfaceSlot() {
        if (interfaceSlot.isEmpty()) {
            appendStatus(0x02);
            return;
        }
        ItemStack drop = interfaceSlot.copy();
        setInterfaceStack(ItemStack.EMPTY);
        if (level instanceof ServerLevel serverLevel) {
            Containers.dropItemStack(serverLevel, worldPosition.getX() + 0.5,
                    worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, drop);
        }
        appendStatus(0x00);
    }

    private void slotReady() {
        if (!slotExists()) {
            appendStatus(container() == null ? 0x41 : 0x40);
            return;
        }
        ItemStack current = interfaceStack();
        if (current.isEmpty() || !containerCanPlace(current)) {
            appendStatus(0x01);
            return;
        }
        ItemStack target = slotStack();
        if (!target.isEmpty()) {
            int max = Math.min(target.getMaxStackSize(), containerMaxStack());
            if (!ItemStack.isSameItemSameComponents(target, current)
                    || target.getCount() >= max) {
                appendStatus(0x01);
                return;
            }
        }
        appendStatus(0x00);
    }

    private int slotSameStatus() {
        if (!slotExists()) {
            return container() == null ? 0x41 : 0x40;
        }
        return ItemStack.isSameItemSameComponents(slotStack(), interfaceStack()) ? 0x00 : 0x01;
    }

    private int slotMatchStatus() {
        if (!slotExists()) {
            return container() == null ? 0x41 : 0x40;
        }
        try {
            return matchesPredicate(slotStack(), args.toString()) ? 0x00 : 0x01;
        } catch (CommandSyntaxException e) {
            return 0x33;
        }
    }

    private void contInsert() {
        Container container = container();
        if (container == null) {
            appendStatus(0x41);
            return;
        }
        ItemStack current = interfaceStack();
        if (current.isEmpty()) {
            appendStatus(0x02);
            return;
        }
        int total = 0;
        for (int i = 0; i < container.getContainerSize() && !current.isEmpty(); i++) {
            if (!container.canPlaceItem(i, current)) {
                continue;
            }
            ItemStack target = container.getItem(i);
            if (!target.isEmpty() && !ItemStack.isSameItemSameComponents(target, current)) {
                continue;
            }
            int max = Math.min(current.getMaxStackSize(), container.getMaxStackSize());
            int space = target.isEmpty() ? max : Math.min(max, target.getMaxStackSize()) - target.getCount();
            if (space <= 0) {
                continue;
            }
            int move = Math.min(current.getCount(), space);
            if (target.isEmpty()) {
                container.setItem(i, current.copyWithCount(move));
            } else {
                target.grow(move);
                container.setItem(i, target);
            }
            current.shrink(move);
            total += move;
        }
        setInterfaceStack(current);
        if (total > 0) {
            appendStatusValue(0x00, intBytes(total, 1));
        } else {
            appendStatus(0x02);
        }
    }

    private boolean contReady() {
        Container container = container();
        if (container == null) {
            return false;
        }
        ItemStack current = interfaceStack();
        if (current.isEmpty()) {
            return true;
        }
        for (int i = 0; i < container.getContainerSize(); i++) {
            if (!container.canPlaceItem(i, current)) {
                continue;
            }
            ItemStack target = container.getItem(i);
            if (target.isEmpty()
                    || (ItemStack.isSameItemSameComponents(target, current)
                    && target.getCount() < target.getMaxStackSize())) {
                return true;
            }
        }
        return false;
    }

    private void contCollect() {
        Container container = container();
        if (container == null) {
            appendStatus(0x41);
            return;
        }
        ItemStack current = interfaceStack().copy();
        if (current.isEmpty()) {
            for (int i = 0; i < container.getContainerSize(); i++) {
                if (!container.getItem(i).isEmpty()) {
                    current = container.getItem(i).copyWithCount(1);
                    break;
                }
            }
        }
        if (current.isEmpty()) {
            appendStatus(0x02);
            return;
        }
        int total = 0;
        int max = current.getMaxStackSize();
        for (int i = 0; i < container.getContainerSize() && current.getCount() < max; i++) {
            ItemStack target = container.getItem(i);
            if (target.isEmpty() || !ItemStack.isSameItemSameComponents(target, current)) {
                continue;
            }
            int move = Math.min(target.getCount(), max - current.getCount());
            target.shrink(move);
            container.setItem(i, target);
            current.grow(move);
            total += move;
        }
        setInterfaceStack(current);
        if (total > 0) {
            appendStatusValue(0x00, intBytes(total, 1));
        } else {
            appendStatus(0x02);
        }
    }

    private void contFind() {
        Container container = container();
        if (container == null) {
            appendStatus(0x41);
            return;
        }
        int start = sp == 255 ? 0 : sp + 1;
        if (start > container.getContainerSize()) {
            appendStatus(0x40);
            return;
        }
        for (int i = start; i < container.getContainerSize(); i++) {
            try {
                if (matchesPredicate(container.getItem(i), args.toString())) {
                    sp = i;
                    appendStatus(0x00);
                    return;
                }
            } catch (CommandSyntaxException e) {
                appendStatus(0x33);
                return;
            }
        }
        appendStatus(0x32);
    }

    private boolean containerCanPlace(ItemStack stack) {
        Container container = container();
        if (container == null) {
            return false;
        }
        if (cp == 6 && sp == SLOT_IS) {
            return true;
        }
        return container.canPlaceItem(sp, stack);
    }

    private int containerMaxStack() {
        Container container = container();
        return container == null ? 64 : Math.max(1, container.getMaxStackSize());
    }

    /* ============================================================
     * 读取
     * ============================================================ */

    private void readId() {
        if (!slotExists()) {
            appendStatus(container() == null ? 0x41 : 0x40);
            return;
        }
        ItemStack stack = slotStack();
        String id = stack.isEmpty() ? "minecraft:air"
                : BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        appendStatusValue(0x00, 0x02, id.getBytes(StandardCharsets.UTF_8));
    }

    private byte[] nameBytes(boolean localized) {
        ItemStack stack = slotStack();
        if (stack.isEmpty()) {
            return localized ? Component.translatable("block.minecraft.air").getString()
                    .getBytes(StandardCharsets.UTF_8) : new byte[0];
        }
        Component component = localized ? stack.getHoverName() : stack.get(DataComponents.CUSTOM_NAME);
        return component == null ? new byte[0] : component.getString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] localizedNameBytes(boolean display) {
        ItemStack stack = slotStack();
        if (stack.isEmpty()) {
            return Component.translatable("block.minecraft.air").getString().getBytes(StandardCharsets.UTF_8);
        }
        return (display ? stack.getHoverName() : Component.translatable(stack.getDescriptionId()))
                .getString().getBytes(StandardCharsets.UTF_8);
    }

    private void damage() {
        if (!slotExists()) {
            appendStatus(container() == null ? 0x41 : 0x40);
            return;
        }
        ItemStack stack = slotStack();
        Integer damage = stack.get(DataComponents.DAMAGE);
        Integer max = stack.get(DataComponents.MAX_DAMAGE);
        int value = damage == null || max == null ? 0xFFFF : damage;
        appendStatusValue(0x00, 0x00, intBytes(value, 2));
    }

    private void color() {
        if (!slotExists()) {
            appendStatus(container() == null ? 0x41 : 0x40);
            return;
        }
        ItemStack stack = slotStack();
        int color = 0xFF;
        var dyed = stack.get(DataComponents.DYED_COLOR);
        if (dyed != null) {
            int rgb = dyed.rgb();
            int index = nearestDyeColor(rgb);
            if (index >= 0) {
                color = index;
            }
        }
        appendStatusValue(0x00, 0x00, intBytes(color, 1));
    }

    private static int nearestDyeColor(int rgb) {
        int best = -1;
        int bestDistance = Integer.MAX_VALUE;
        for (int i = 0; i < 16; i++) {
            int color = net.minecraft.world.item.DyeColor.byId(i).getTextColor();
            int dr = ((rgb >> 16) & 0xFF) - ((color >> 16) & 0xFF);
            int dg = ((rgb >> 8) & 0xFF) - ((color >> 8) & 0xFF);
            int db = (rgb & 0xFF) - (color & 0xFF);
            int distance = dr * dr + dg * dg + db * db;
            if (distance < bestDistance) {
                bestDistance = distance;
                best = i;
            }
        }
        return best;
    }

    /* ============================================================
     * 组件读取（简化实现：路径按 SNBT 根/键/下标解析）
     * ============================================================ */

    private @Nullable Tag componentRoot() {
        if (!slotExists()) {
            return null;
        }
        var encoded = ItemStack.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, slotStack());
        Tag tag = encoded.result().orElse(null);
        if (tag instanceof CompoundTag compound && compound.contains("components", Tag.TAG_COMPOUND)) {
            return compound.getCompound("components");
        }
        return tag;
    }

    private @Nullable Tag componentAt() {
        Tag root = componentRoot();
        if (root == null) {
            return null;
        }
        String path = args.toString().trim();
        if (iterating && iteratorIndex >= 0 && iteratorIndex < iteratorKeys.size()) {
            path = iteratorPath.isEmpty() ? iteratorKeys.get(iteratorIndex)
                    : iteratorPath + "." + iteratorKeys.get(iteratorIndex);
            if (iteratorPath.isEmpty()) {
                // 根级列表下标也使用同一种相对路径。
                path = iteratorKeys.get(iteratorIndex);
            }
        }
        if (path.isEmpty()) {
            return root;
        }
        Tag current = root;
        for (String part : path.split("\\.")) {
            if (current instanceof CompoundTag compound) {
                current = compound.get(part);
            } else if (current instanceof ListTag list && part.matches("\\d+")) {
                int index = Integer.parseInt(part);
                current = index >= 0 && index < list.size() ? list.get(index) : null;
            } else {
                return null;
            }
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    private void compSize() {
        if (!slotExists()) {
            appendStatus(slotErrorStatus());
            return;
        }
        Tag tag = componentAt();
        if (tag == null) {
            appendStatus(0x21);
            return;
        }
        appendStatusValue(0x00, 0x00, intBytes(tagSize(tag), 2));
    }

    private void compType() {
        if (!slotExists()) {
            appendStatus(slotErrorStatus());
            return;
        }
        Tag tag = componentAt();
        if (tag == null) {
            appendStatus(0x21);
            return;
        }
        appendStatusValue(0x00, 0x00, intBytes(tagType(tag), 1));
    }

    private void compIter() {
        if (!slotExists()) {
            appendStatus(slotErrorStatus());
            return;
        }
        Tag tag = componentAt();
        iteratorKeys.clear();
        iteratorIndex = -1;
        iterating = false;
        if (tag instanceof CompoundTag compound) {
            iteratorKeys.addAll(compound.getAllKeys());
        } else if (tag instanceof ListTag list) {
            for (int i = 0; i < list.size(); i++) {
                iteratorKeys.add(Integer.toString(i));
            }
        } else {
            appendStatus(0x22);
            return;
        }
        iterating = true;
        iteratorPath = args.toString();
        appendStatus(0x00);
    }

    private void compNext() {
        if (!iterating) {
            appendStatus(0x22);
            return;
        }
        iteratorIndex++;
        if (iteratorIndex >= iteratorKeys.size()) {
            appendStatus(0x23);
            return;
        }
        String key = iteratorKeys.get(iteratorIndex);
        if (key.matches("\\d+")) {
            appendStatusValue(0x00, 0x00, intBytes(Integer.parseInt(key), 2));
        } else {
            appendStatusValue(0x00, 0x02, key.getBytes(StandardCharsets.UTF_8));
        }
    }

    private void compRead() {
        if (!slotExists()) {
            appendStatus(slotErrorStatus());
            return;
        }
        Tag tag = componentAt();
        if (tag == null) {
            appendStatus(0x21);
            return;
        }
        if (tag instanceof NumericTag numeric) {
            appendStatusValue(0x00, 0x00, intBytes(numeric.getAsLong(), tagSize(tag)));
        } else if (tag instanceof StringTag string) {
            appendStatusValue(0x00, 0x02, string.getAsString().getBytes(StandardCharsets.UTF_8));
        } else {
            appendStatus(0x22);
        }
    }

    private void compDump(boolean snbt) {
        if (!slotExists()) {
            appendStatus(slotErrorStatus());
            return;
        }
        Tag tag = componentAt();
        if (tag == null) {
            appendStatus(0x21);
            return;
        }
        try {
            byte[] data = snbt ? new StringTagVisitor().visit(tag).getBytes(StandardCharsets.UTF_8) : tagToBytes(tag);
            appendStatusValue(0x00, snbt ? 0x02 : 0x05, data);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 将 Tag 序列化为 byte[]
     */
    public static byte[] tagToBytes(Tag tag) throws IOException {
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
             DataOutputStream dataOutput = new DataOutputStream(byteStream)) {

            // 使用 writeUnnamedTag 或 writeAnyTag 均可
            NbtIo.writeUnnamedTag(tag, dataOutput);
            // 或 NbtIo.writeAnyTag(tag, dataOutput);

            dataOutput.flush();
            return byteStream.toByteArray();
        }
    }

    private static int tagSize(Tag tag) {
        if (tag instanceof CompoundTag compound) {
            return compound.size();
        }
        if (tag instanceof ListTag list) {
            return list.size();
        }
        if (tag instanceof StringTag string) {
            return string.getAsString().getBytes(StandardCharsets.UTF_8).length;
        }
        if (tag instanceof NumericTag numeric) {
            return switch (tag.getId()) {
                case 1, 3 -> 1;
                case 2, 5 -> 2;
                case 4, 6 -> 4;
                default -> 8;
            };
        }
        return 0;
    }

    private static int tagType(Tag tag) {
        if (tag instanceof CompoundTag) {
            return 0x04;
        }
        if (tag instanceof ListTag) {
            return 0x03;
        }
        if (tag instanceof StringTag) {
            return 0x02;
        }
        return tag.getId() == 5 || tag.getId() == 6 ? 0x01 : 0x00;
    }

    /* ============================================================
     * 结果缓冲区
     * ============================================================ */

    private void appendStatus(int status) {
        appendResult(new byte[]{(byte) 0xFF, (byte) status});
    }

    private void appendStatusValue(int status, byte[] value) {
        appendStatusValue(status, 0x00, value);
    }

    private void appendStatusValue(int status, int type, byte[] value) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(0xFF);
        out.write(status);
        out.write(type);
        out.write((value.length >> 8) & 0xFF);
        out.write(value.length & 0xFF);
        out.writeBytes(value);
        appendResult(out.toByteArray());
    }

    private void appendValue(int type, byte[] value) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(type);
        out.write((value.length >> 8) & 0xFF);
        out.write(value.length & 0xFF);
        out.writeBytes(value);
        appendResult(out.toByteArray());
    }

    private void appendResult(byte[] data) {
        if (result.size() + data.length > RESULT_CAPACITY) {
            if (RESULT_CAPACITY - result.size() >= 2) {
                result.write(0xFF);
                result.write(0x11);
            }
            return;
        }
        result.writeBytes(data);
    }

    private static byte[] intBytes(long value, int count) {
        byte[] result = new byte[count];
        for (int i = count - 1; i >= 0; i--) {
            result[i] = (byte) (value & 0xFF);
            value >>>= 8;
        }
        return result;
    }

    private void resetRuntime() {
        cp = 0;
        sp = 0;
        argi = 0;
        timeTicks = 0;
        args.setLength(0);
        result.reset();
        stashSize = 0;
        pendingOpcode = -1;
        pendingLength = false;
        pendingData = new byte[0];
        pendingPos = 0;
        iterating = false;
        iteratorKeys.clear();
        iteratorIndex = -1;
    }

    private boolean matchesPredicate(ItemStack stack, String predicate) throws CommandSyntaxException {
        String value = predicate == null ? "" : predicate.trim();
        if (level != null) {
            return value.isEmpty() || matches(parsePredicate(value, level.registryAccess()), stack);
        }
        return false;
    }

    /**
     * 解析物品谓词字符串（SNBT 语法）并返回可用于匹配的谓词对象
     *
     * @param predicateString 例如 "minecraft:diamond_sword" 或 "#minecraft:swords[damage=0]"
     * @param registries      注册表访问，通常来自 ServerLevel.registryAccess() 或客户端连接
     */
    private static ItemPredicateArgument.Result parsePredicate(String predicateString, HolderLookup.Provider registries) throws CommandSyntaxException {

        // 1. 构建 CommandBuildContext（解析物品组件需要注册表信息）
        CommandBuildContext buildContext = CommandBuildContext.simple(registries, FeatureFlags.DEFAULT_FLAGS);

        // 2. 创建 ItemPredicateArgument 实例
        ItemPredicateArgument argumentType = new ItemPredicateArgument(buildContext);

        // 3. 直接解析字符串，无需 CommandContext
        StringReader reader = new StringReader(predicateString);
        ItemPredicateArgument.Result predicate = argumentType.parse(reader);

        // 4. 确保整个字符串都被消费（可选，用于检测尾部多余字符）
        if (reader.canRead()) {
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherExpectedArgumentSeparator().createWithContext(reader);
        }

        return predicate;
    }

    /**
     * 测试物品是否匹配谓词
     */
    private static boolean matches(ItemPredicateArgument.Result predicate, ItemStack stack) {
        return predicate.test(stack);
    }

    private void addTestDataForDump() {
        // 保留方法避免未来序列化扩展破坏结构。
    }
}
