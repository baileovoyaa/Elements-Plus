package com.elementsplus.blocks.entity;

import com.elementsplus.ModBlockEntityTypes;
import com.elementsplus.blocks.PressureMachineBlock;
import com.elementsplus.menu.PressureMachineMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

/**
 * 加压机原型。
 * <p>
 * 当前实现为了先“跑起来”，做了如下简化，后续实测时再调整：
 * <ul>
 *     <li>输入/输出空间的压强按“每台加压机各存一份”处理，尚未做多方块共享空间网络；</li>
 *     <li>空间检测为有界洪水填充：不完整方块（{@code canOcclude() == false} 且非方块实体）视为空气，
 *     任一边长超过 16 格或空气数超过 4096 视为开放空间；</li>
 *     <li>参数均为占位值，见下方常量；</li>
 *     <li>暂未实现生物反胃/伤害效果。</li>
 * </ul>
 */
public class PressureMachineBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {

    // ---- 原型参数（后续实测调整） ----
    /** 密闭空间允许的最大边长（格）。 */
    public static final int MAX_AXIS = 16;
    /** 密闭空间允许的最大空气方块数（16×16×16）。 */
    public static final int MAX_VOLUME = MAX_AXIS * MAX_AXIS * MAX_AXIS;
    /** 加压机流量，B·atm/s。 */
    public static final double TRANSFER_RATE = 2.08;
    /** 泄漏死区。 */
    public static final double LEAK_DEADZONE = 0.1;
    /** 泄漏幂指数。 */
    public static final double LEAK_BETA = 3.0;
    /** 泄漏系数。 */
    public static final double LEAK_C = 0.304;
    /** 压强下限/上限（atm）。 */
    public static final double MIN_PRESSURE = 0.1;
    public static final double MAX_PRESSURE = 3.0;
    /** 高压结晶器开始补充压强的阈值。 */
    public static final double CRYSTALLIZER_THRESHOLD = 1.5;
    /** 每补充 1 点结晶器压强消耗的空气量，B·atm。 */
    public static final double CRYSTALLIZER_AIR_COST = 6.0;
    /** 结晶器补充间隔（tick）。 */
    public static final int CRYSTALLIZER_INTERVAL = 10;
    /** 空间扫描间隔（tick）。 */
    public static final int SCAN_INTERVAL = 20;

    // ---- ContainerData 索引 ----
    public static final int DATA_LIT_TIME = 0;
    public static final int DATA_LIT_DURATION = 1;
    public static final int DATA_INPUT_PRESSURE = 2;
    public static final int DATA_OUTPUT_PRESSURE = 3;
    public static final int DATA_INPUT_VOLUME = 4;
    public static final int DATA_OUTPUT_VOLUME = 5;
    public static final int DATA_INPUT_SEALED = 6;
    public static final int DATA_OUTPUT_SEALED = 7;
    public static final int DATA_FLOW = 8;
    public static final int DATA_WORKING = 9;
    public static final int DATA_SAME_SPACE = 10;
    public static final int DATA_COUNT = 11;

    private static final int[] SLOTS = new int[]{0};

    protected NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);

    int litTime;
    int litDuration;

    double inputPressure = 1.0;
    double outputPressure = 1.0;
    int inputVolume;
    int outputVolume;
    boolean inputSealed;
    boolean outputSealed;
    /** 入口与出口是否连成同一个空间（此时搬运没有意义）。 */
    boolean sameSpace;
    double flow;

    int scanCooldown;
    int crystallizerCooldown;
    private final Set<BlockPos> outputCrystallizers = new HashSet<>();

    protected final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_LIT_TIME -> litTime;
                case DATA_LIT_DURATION -> litDuration;
                case DATA_INPUT_PRESSURE -> (int) Math.round(inputPressure * 1000.0);
                case DATA_OUTPUT_PRESSURE -> (int) Math.round(outputPressure * 1000.0);
                case DATA_INPUT_VOLUME -> inputVolume;
                case DATA_OUTPUT_VOLUME -> outputVolume;
                case DATA_INPUT_SEALED -> inputSealed ? 1 : 0;
                case DATA_OUTPUT_SEALED -> outputSealed ? 1 : 0;
                case DATA_FLOW -> (int) Math.round(flow * 1000.0);
                case DATA_WORKING -> isWorking() ? 1 : 0;
                case DATA_SAME_SPACE -> sameSpace ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case DATA_LIT_TIME -> litTime = value;
                case DATA_LIT_DURATION -> litDuration = value;
                case DATA_INPUT_PRESSURE -> inputPressure = value / 1000.0;
                case DATA_OUTPUT_PRESSURE -> outputPressure = value / 1000.0;
                case DATA_INPUT_VOLUME -> inputVolume = value;
                case DATA_OUTPUT_VOLUME -> outputVolume = value;
                case DATA_INPUT_SEALED -> inputSealed = value != 0;
                case DATA_OUTPUT_SEALED -> outputSealed = value != 0;
                case DATA_FLOW -> flow = value / 1000.0;
                case DATA_SAME_SPACE -> sameSpace = value != 0;
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public PressureMachineBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(ModBlockEntityTypes.PRESSURE_MACHINE, blockPos, blockState);
    }

    protected PressureMachineBlockEntity(BlockEntityType<?> type, BlockPos blockPos, BlockState blockState) {
        super(type, blockPos, blockState);
    }

    // ---- 对外只读数据（GUI 用） ----

    public double getFlow() {
        return flow;
    }

    public double getInputPressure() {
        return inputPressure;
    }

    public double getOutputPressure() {
        return outputPressure;
    }

    public int getInputVolume() {
        return inputVolume;
    }

    public int getOutputVolume() {
        return outputVolume;
    }

    public boolean isInputSealed() {
        return inputSealed;
    }

    public boolean isOutputSealed() {
        return outputSealed;
    }

    public boolean isWorking() {
        return litTime > 0 && !sameSpace
                && ((inputSealed && inputVolume > 0) || (outputSealed && outputVolume > 0));
    }

    public boolean isSameSpace() {
        return sameSpace;
    }

    public int getLitTime() {
        return litTime;
    }

    public int getLitDuration() {
        return litDuration;
    }

    // ---- 保存/加载 ----

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, this.items, registries);
        this.litTime = tag.getShort("BurnTime");
        this.litDuration = tag.getShort("BurnTimeTotal");
        this.inputPressure = tag.contains("InputPressure") ? tag.getDouble("InputPressure") : 1.0;
        this.outputPressure = tag.contains("OutputPressure") ? tag.getDouble("OutputPressure") : 1.0;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, this.items, registries);
        tag.putShort("BurnTime", (short) this.litTime);
        tag.putShort("BurnTimeTotal", (short) this.litDuration);
        tag.putDouble("InputPressure", this.inputPressure);
        tag.putDouble("OutputPressure", this.outputPressure);
    }

    // ---- tick ----

    public static void serverTick(Level level, BlockPos pos, BlockState state, PressureMachineBlockEntity be) {
        boolean changed = false;

        if (be.litTime > 0) {
            be.litTime--;
        }
        if (be.scanCooldown <= 0) {
            be.refreshSpaces(level, pos, state);
            be.scanCooldown = SCAN_INTERVAL;
        } else {
            be.scanCooldown--;
        }

        // 泄漏始终存在（仅对密闭空间生效）
        changed |= be.maintainSpaces();

        boolean hasSpace = !be.sameSpace
                && ((be.inputSealed && be.inputVolume > 0) || (be.outputSealed && be.outputVolume > 0));
        ItemStack fuel = be.items.get(0);
        if (be.litTime <= 0 && hasSpace && !fuel.isEmpty() && isFuel(fuel)) {
            int burn = be.getBurnDuration(fuel);
            if (burn > 0) {
                be.litTime = burn;
                be.litDuration = burn;
                Item item = fuel.getItem();
                fuel.shrink(1);
                if (fuel.isEmpty()) {
                    Item remainder = item.getCraftingRemainingItem();
                    be.items.set(0, remainder == null ? ItemStack.EMPTY : new ItemStack(remainder));
                }
                changed = true;
            }
        }

        be.flow = 0.0;
        if (be.litTime > 0 && hasSpace) {
            be.pump(level, pos);
            changed = true;
        }

        boolean lit = be.litTime > 0;
        if (state.getValue(PressureMachineBlock.LIT) != lit) {
            changed = true;
            state = state.setValue(PressureMachineBlock.LIT, lit);
            level.setBlock(pos, state, 3);
        }

        if (changed) {
            setChanged(level, pos, state);
        }
    }

    /** 把空气从入口空间搬到出口空间；参数均为原型值。 */
    private void pump(Level level, BlockPos pos) {
        final double dt = 1.0 / 20.0;
        if (sameSpace) {
            flow = 0.0;
            return;
        }

        if (inputSealed && inputVolume > 0) {
            double air = inputPressure * inputVolume - TRANSFER_RATE * dt;
            inputPressure = clamp(air / inputVolume);
        }
        if (outputSealed && outputVolume > 0) {
            double air = outputPressure * outputVolume + TRANSFER_RATE * dt;
            outputPressure = clamp(air / outputVolume);
        }
        flow = TRANSFER_RATE;

        // 高压空间给结晶器补充压强（原型：直接补充结晶器的 pressure 值，并消耗空间空气）
        if (outputSealed && outputVolume > 0 && outputPressure > CRYSTALLIZER_THRESHOLD) {
            if (crystallizerCooldown > 0) {
                crystallizerCooldown--;
            } else {
                boolean supplied = false;
                for (BlockPos crystallizerPos : outputCrystallizers) {
                    if (outputPressure <= CRYSTALLIZER_THRESHOLD) {
                        break;
                    }
                    if (!(level.getBlockEntity(crystallizerPos) instanceof CrystallizerBlockEntity crystallizer)) {
                        continue;
                    }
                    if (crystallizer.getPressure() >= 100) {
                        continue;
                    }
                    double air = outputPressure * outputVolume - CRYSTALLIZER_AIR_COST;
                    if (air <= 0.0) {
                        break;
                    }
                    crystallizer.addPressure(1);
                    crystallizer.setChanged();
                    outputPressure = clamp(air / outputVolume);
                    supplied = true;
                }
                if (supplied) {
                    crystallizerCooldown = CRYSTALLIZER_INTERVAL;
                }
            }
        }
    }

    /** 对密闭空间施加泄漏，返回压强是否发生变化。 */
    private boolean maintainSpaces() {
        final double dt = 1.0 / 20.0;
        boolean changed = false;
        if (inputSealed && inputVolume > 0) {
            double before = inputPressure;
            inputPressure = clamp(inputPressure - leakRate(inputPressure) / inputVolume * dt);
            changed |= Math.abs(inputPressure - before) > 1.0e-12;
        }
        if (sameSpace) {
            if (outputPressure != inputPressure) {
                outputPressure = inputPressure;
                changed = true;
            }
        } else if (outputSealed && outputVolume > 0) {
            double before = outputPressure;
            outputPressure = clamp(outputPressure - leakRate(outputPressure) / outputVolume * dt);
            changed |= Math.abs(outputPressure - before) > 1.0e-12;
        }
        return changed;
    }

    private static double leakRate(double pressure) {
        double delta = pressure - 1.0;
        double over = Math.max(0.0, Math.abs(delta) - LEAK_DEADZONE);
        return LEAK_C * Math.signum(delta) * Math.pow(over, LEAK_BETA);
    }

    private static double clamp(double pressure) {
        return Math.max(MIN_PRESSURE, Math.min(MAX_PRESSURE, pressure));
    }

    // ---- 空间检测 ----

    private void refreshSpaces(Level level, BlockPos pos, BlockState state) {
        Direction facing = state.getValue(PressureMachineBlock.FACING);
        BlockPos inputStart = pos.relative(facing.getOpposite());
        BlockPos outputStart = pos.relative(facing);
        SpaceInfo input = scanSpace(level, inputStart);
        SpaceInfo output = scanSpace(level, outputStart);

        inputVolume = input.volume();
        inputSealed = input.sealed();
        outputVolume = output.volume();
        outputSealed = output.sealed();
        // 前后连成同一片空间时，搬运没有意义；同时避免把同一空间当成两份来“造气”。
        sameSpace = input.cells().contains(outputStart) || output.cells().contains(inputStart);

        if (!inputSealed) {
            inputPressure = 1.0;
        }
        if (sameSpace) {
            outputPressure = inputPressure;
        } else if (!outputSealed) {
            outputPressure = 1.0;
        }

        outputCrystallizers.clear();
        outputCrystallizers.addAll(output.crystallizers());
    }

    /**
     * 从 {@code start} 开始做有界洪水填充。
     * 不完整方块（不遮挡且不是方块实体）视为空气。
     */
    private static SpaceInfo scanSpace(Level level, BlockPos start) {
        if (!isAirCell(level, start)) {
            return new SpaceInfo(0, false, Set.of(), Set.of());
        }

        Set<BlockPos> visited = new HashSet<>();
        Set<BlockPos> crystallizers = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        visited.add(start);
        queue.add(start);

        int minX = start.getX(), maxX = minX;
        int minY = start.getY(), maxY = minY;
        int minZ = start.getZ(), maxZ = minZ;
        boolean sealed = true;

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            for (Direction direction : Direction.values()) {
                BlockPos next = current.relative(direction);
                if (visited.contains(next)) {
                    continue;
                }
                if (isAirCell(level, next)) {
                    visited.add(next);
                    queue.add(next);
                    minX = Math.min(minX, next.getX());
                    maxX = Math.max(maxX, next.getX());
                    minY = Math.min(minY, next.getY());
                    maxY = Math.max(maxY, next.getY());
                    minZ = Math.min(minZ, next.getZ());
                    maxZ = Math.max(maxZ, next.getZ());

                    if (visited.size() > MAX_VOLUME
                            || maxX - minX + 1 > MAX_AXIS
                            || maxY - minY + 1 > MAX_AXIS
                            || maxZ - minZ + 1 > MAX_AXIS) {
                        sealed = false;
                        break;
                    }
                } else if (level.getBlockEntity(next) instanceof CrystallizerBlockEntity) {
                    crystallizers.add(next.immutable());
                }
            }
            if (!sealed) {
                break;
            }
        }
        return new SpaceInfo(visited.size(), sealed, crystallizers, visited);
    }

    private static boolean isAirCell(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return true;
        }
        // 不完整方块视为空气；带方块实体的机器仍作为边界，避免把前后空间连通。
        return !state.canOcclude() && !(state.getBlock() instanceof BaseEntityBlock);
    }

    private record SpaceInfo(int volume, boolean sealed, Set<BlockPos> crystallizers, Set<BlockPos> cells) {
    }

    // ---- 容器 / 燃料 ----

    public static boolean isFuel(ItemStack stack) {
        return FurnaceBlockEntity.isFuel(stack);
    }

    protected int getBurnDuration(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        return FurnaceBlockEntity.getFuel().getOrDefault(stack.getItem(), 0);
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
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == 0 && isFuel(stack);
    }

    @Override
    public int @NotNull [] getSlotsForFace(Direction direction) {
        return SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction direction) {
        return this.canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return slot == 0;
    }

    @Override
    protected @NotNull Component getDefaultName() {
        return Component.translatable("container.elements-plus.pressure_machine");
    }

    @Override
    protected @NotNull AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new PressureMachineMenu(containerId, inventory, this, this.dataAccess);
    }
}
