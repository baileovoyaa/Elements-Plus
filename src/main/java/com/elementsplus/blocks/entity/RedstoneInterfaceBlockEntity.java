package com.elementsplus.blocks.entity;

import com.elementsplus.ModBlockEntityTypes;
import com.elementsplus.ModItems;
import com.elementsplus.blocks.RedstoneInterfaceBlock;
import com.elementsplus.core.circuit.BuiltinCircuitComponents;
import com.elementsplus.core.circuit.ChipManufacture;
import com.elementsplus.core.circuit.CircuitComponent;
import com.elementsplus.core.circuit.CircuitComponentRegistry;
import com.elementsplus.core.circuit.CircuitItemData;
import com.elementsplus.core.circuit.CircuitSimulator;
import com.elementsplus.core.circuit.CreativeChipCircuit;
import com.elementsplus.core.circuit.component.Input8ComponentInstance;
import com.elementsplus.core.circuit.component.InputComponentInstance;
import com.elementsplus.core.circuit.component.Output8ComponentInstance;
import com.elementsplus.core.circuit.component.OutputComponentInstance;
import com.elementsplus.core.circuit.diagram.CircuitDiagram;
import com.elementsplus.core.config.ElementsPlusServerConfig;
import com.elementsplus.menu.RedstoneInterfaceMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 红石接口方块实体。负责保存槽位、把芯片电路的输入/输出引脚分配到六个面，
 * 以及在世界 tick 中驱动电路模拟。
 */
public class RedstoneInterfaceBlockEntity extends BaseContainerBlockEntity implements MenuProvider {
    public static final int SLOT_CIRCUIT = 0;
    public static final int SLOT_BATTERY = 1;
    public static final int SLOT_DISK_START = 2;
    public static final int SLOT_COUNT = 8;

    public static final Direction[] FACES = {
            Direction.DOWN, Direction.UP, Direction.NORTH,
            Direction.SOUTH, Direction.WEST, Direction.EAST
    };

    public record PinInfo(int index, String label, CircuitComponent.PinType type,
                          int bitWidth, int x, int y) {
    }

    private NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final List<PinInfo> pins = new ArrayList<>();
    private final Map<Direction, Integer> assignments = new EnumMap<>(Direction.class);
    private final Map<Direction, Integer> faceValues = new EnumMap<>(Direction.class);

    private ItemStack lastCircuit = ItemStack.EMPTY;
    private CircuitDiagram diagram;
    private CircuitSimulator simulator;
    @Nullable
    private CompoundTag pendingSimulatorState;
    private boolean running;
    private boolean hibernating;
    private boolean singleStepRequested;
    private boolean skipNextPerformanceSample;
    private boolean loading;
    private boolean refreshing;
    private long lastTickNanos;
    private double averageMs;
    private double lastStepMs;
    private final java.util.ArrayDeque<Double> samples = new java.util.ArrayDeque<>();

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> running ? 1 : 0;
                case 1 -> hibernating ? 1 : 0;
                case 2 -> (int) Math.round(averageMs * 1000.0);
                case 3 -> (int) Math.round(lastStepMs * 1000.0);
                case 4 -> lastCircuit.isEmpty() ? 0 : 1;
                case 5 -> hasBattery() ? 1 : 0;
                case 6 -> ElementsPlusServerConfig.interfaceAvgMsptLimit();
                case 7 -> ElementsPlusServerConfig.interfaceMaxMsptLimit();
                case 8 -> ElementsPlusServerConfig.interfaceSampleTicks();
                case 9 -> worldPosition.getX();
                case 10 -> worldPosition.getY();
                case 11 -> worldPosition.getZ();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            // 只读数据。
        }

        @Override
        public int getCount() {
            return 12;
        }
    };

    public RedstoneInterfaceBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntityTypes.REDSTONE_INTERFACE, pos, state);
    }

    public RedstoneInterfaceBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null && !level.isClientSide && !loading && !refreshing) {
            refreshing = true;
            try {
                refreshCircuit();
            } finally {
                refreshing = false;
            }
        }
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.elements-plus.redstone_interface");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        refreshCircuit();
        sync();
        return new RedstoneInterfaceMenu(id, inventory, this, data);
    }

    public ContainerData getData() {
        return data;
    }

    public List<PinInfo> getPins() {
        return List.copyOf(pins);
    }

    public Map<Direction, Integer> getAssignments() {
        return Map.copyOf(assignments);
    }

    @Nullable
    public PinInfo getFacePin(Direction face) {
        Integer index = assignments.get(face);
        return index == null || index < 0 || index >= pins.size() ? null : pins.get(index);
    }

    public int getFaceValue(Direction face) {
        return faceValues.getOrDefault(face, 0);
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isHibernating() {
        return hibernating;
    }

    public double getAverageMs() {
        return averageMs;
    }

    public double getLastStepMs() {
        return lastStepMs;
    }

    public boolean hasBattery() {
        return items.get(SLOT_BATTERY).is(ModItems.AMETHYST_BATTERY);
    }

    public boolean hasCircuit() {
        return !lastCircuit.isEmpty() && simulator != null;
    }

    public void assignPin(int pinIndex, Direction face) {
        if (pinIndex < 0 || pinIndex >= pins.size()) {
            return;
        }
        assignments.entrySet().removeIf(entry -> entry.getValue() == pinIndex);
        assignments.put(face, pinIndex);
        faceValues.remove(face);
        setChangedAndSync();
    }

    public void unassignPin(Direction face) {
        if (assignments.remove(face) != null) {
            faceValues.remove(face);
            setChangedAndSync();
        }
    }

    public void setRunning(boolean value) {
        if (value) {
            if (hibernating) {
                resetPerformance();
            }
            hibernating = false;
        }
        running = value;
        singleStepRequested = false;
        sync();
        setChanged();
    }

    public void singleStep() {
        if (hibernating) {
            resetPerformance();
        }
        singleStepRequested = true;
        running = false;
        hibernating = false;
        sync();
        setChanged();
    }

    public void resetCircuit() {
        if (simulator != null) {
            simulator.reset();
        }
        running = false;
        singleStepRequested = false;
        hibernating = false;
        faceValues.clear();
        resetPerformance();
        sync();
        setChanged();
    }

    public void serverTick() {
        if (level == null || level.isClientSide) {
            return;
        }
        long now = System.nanoTime();
        long wallGap = lastTickNanos == 0 ? 0 : now - lastTickNanos;
        lastTickNanos = now;
        // 游戏暂停或操作系统休眠后，丢弃旧的性能采样窗口。
        if (wallGap > 1_000_000_000L) {
            resetPerformance();
            skipNextPerformanceSample = true;
        }

        refreshCircuit();
        updateRedstoneConnections();
        if (!hasBattery() || !hasCircuit() || hibernating) {
            if (hibernating) {
                emitSmoke();
            } else if (!hasBattery() || !hasCircuit()) {
                if (!faceValues.isEmpty()) {
                    faceValues.clear();
                    sync();
                }
            }
            return;
        }

        applyInputs();
        if (singleStepRequested) {
            singleStepRequested = false;
            runStep();
        } else if (running) {
            runStep();
            if (hibernating) {
                running = false;
                sync();
            }
        }
    }

    private void runStep() {
        if (simulator == null) {
            return;
        }
        long start = System.nanoTime();
        simulator.step();
        double elapsed = (System.nanoTime() - start) / 1_000_000.0;
        lastStepMs = Math.max(0, elapsed);
        collectOutputs();
        if (skipNextPerformanceSample) {
            skipNextPerformanceSample = false;
        } else {
            recordPerformance(lastStepMs);
        }
    }

    private void refreshCircuit() {
        ItemStack stack = items.get(SLOT_CIRCUIT);
        if (!isCircuitCarrier(stack)) {
            if (!lastCircuit.isEmpty() || simulator != null || !pins.isEmpty()) {
                lastCircuit = ItemStack.EMPTY;
                diagram = null;
                simulator = null;
                pendingSimulatorState = null;
                pins.clear();
                assignments.clear();
                faceValues.clear();
                running = false;
                hibernating = false;
                resetPerformance();
                sync();
            }
            return;
        }
        boolean sameChip = !lastCircuit.isEmpty()
                && ItemStack.isSameItemSameComponents(lastCircuit, stack);
        if (sameChip && simulator != null) {
            return;
        }

        lastCircuit = stack.copy();
        diagram = resolveDiagram(stack);
        if (!sameChip) {
            pins.clear();
            assignments.clear();
            faceValues.clear();
            running = false;
            hibernating = false;
            singleStepRequested = false;
            resetPerformance();
            if (!loading) {
                pendingSimulatorState = null;
            }
        }
        if (diagram == null) {
            // 电路存储可能在区块加载之后才可用；不要在此处丢弃已保存的引脚分配。
            simulator = null;
            if (!sameChip) {
                sync();
            }
            return;
        }
        diagram = diagram.copy();
        buildPins();
        // 保留已经保存的引脚分配。重新构建模拟器时，分配索引仍然有效。
        assignments.entrySet().removeIf(entry -> entry.getValue() < 0 || entry.getValue() >= pins.size());
        simulator = new CircuitSimulator();
        simulator.setDiagram(diagram);
        if (pendingSimulatorState != null) {
            simulator.loadState(pendingSimulatorState);
            pendingSimulatorState = null;
        }
        sync();
        setChanged();
    }

    private static boolean isCircuitCarrier(ItemStack stack) {
        if (stack.isEmpty() || stack.is(ModItems.CIRCUIT_DIAGRAM)) {
            // 电路图不是实际电路载体，不能放进电路槽。
            return false;
        }
        if (ChipManufacture.isCreativeComponent(stack)) {
            return true;
        }
        if (stack.getItem() instanceof com.elementsplus.item.ChipItem) {
            return CircuitItemData.diagram(stack) != null
                    || CircuitItemData.compiledId(stack) != null
                    || CircuitItemData.componentId(stack) != null;
        }
        // 内置元件物品、六种紫水晶元件等带有等效元件 id，
        // 可以作为单组件芯片使用。
        return CircuitItemData.componentId(stack) != null;
    }

    @Nullable
    private static CircuitDiagram resolveDiagram(ItemStack stack) {
        CircuitDiagram diagram = CircuitItemData.diagram(stack);
        if (diagram != null) {
            return diagram;
        }
        // 元件物品通常只带等效元件 id，没有编译电路。
        // 如果没有编译电路，就把等效元件包装成一张默认配置的最小电路图。
        if (CircuitItemData.compiledId(stack) == null) {
            CircuitComponent component = CircuitComponentRegistry.byId(CircuitItemData.componentId(stack));
            return CreativeChipCircuit.wrap(component);
        }
        return null;
    }

    private void buildPins() {
        List<PinInfo> found = new ArrayList<>();
        for (CircuitDiagram.Chunk chunk : diagram.chunks.values()) {
            for (CircuitDiagram.Component component : chunk.components) {
                CircuitComponent.PinType type;
                int width;
                String label;
                if (component.component == BuiltinCircuitComponents.INPUT
                        && component.instance instanceof InputComponentInstance input) {
                    type = CircuitComponent.PinType.INPUT;
                    width = 1;
                    label = input.getLabel();
                } else if (component.component == BuiltinCircuitComponents.INPUT_8
                        && component.instance instanceof Input8ComponentInstance input8) {
                    type = CircuitComponent.PinType.INPUT;
                    width = 8;
                    label = input8.getLabel();
                } else if (component.component == BuiltinCircuitComponents.OUTPUT
                        && component.instance instanceof OutputComponentInstance output) {
                    type = CircuitComponent.PinType.OUTPUT;
                    width = 1;
                    label = output.getLabel();
                } else if (component.component == BuiltinCircuitComponents.OUTPUT_8
                        && component.instance instanceof Output8ComponentInstance output8) {
                    type = CircuitComponent.PinType.OUTPUT;
                    width = 8;
                    label = output8.getLabel();
                } else {
                    continue;
                }
                if (label == null) {
                    label = "";
                }
                found.add(new PinInfo(0, label, type, width, component.x, component.y));
            }
        }
        found.sort(Comparator.comparing(PinInfo::label)
                .thenComparingInt(PinInfo::x)
                .thenComparingInt(PinInfo::y)
                .thenComparing(pin -> pin.type().name()));
        for (int i = 0; i < found.size(); i++) {
            PinInfo pin = found.get(i);
            pins.add(new PinInfo(i, pin.label(), pin.type(), pin.bitWidth(), pin.x(), pin.y()));
        }
    }

    private void applyInputs() {
        if (simulator == null || diagram == null) {
            return;
        }
        boolean changed = false;
        for (Map.Entry<Direction, Integer> entry : assignments.entrySet()) {
            PinInfo pin = pins.get(entry.getValue());
            if (pin.type() != CircuitComponent.PinType.INPUT) {
                continue;
            }
            int value = pin.bitWidth() == 1 ? readRedstone(entry.getKey()) : readAdjacentBus(entry.getKey());
            if (faceValues.getOrDefault(entry.getKey(), -1) != value) {
                faceValues.put(entry.getKey(), value);
                changed = true;
            }
            CircuitDiagram.Block block = diagram.getBlock(pin.x(), pin.y());
            if (block instanceof CircuitDiagram.Component component) {
                if (pin.bitWidth() == 1 && component.instance instanceof InputComponentInstance input) {
                    input.setSignal(value);
                } else if (pin.bitWidth() == 8 && component.instance instanceof Input8ComponentInstance input8) {
                    input8.setValue(value);
                }
            }
        }
        if (changed) {
            sync();
        }
    }

    private int readRedstone(Direction face) {
        if (level == null) {
            return 0;
        }
        BlockPos neighbor = worldPosition.relative(face);
        int signal = level.getSignal(neighbor, face);
        signal = Math.max(signal, level.getDirectSignal(neighbor, face));
        return Math.max(0, Math.min(15, signal));
    }

    private int readAdjacentBus(Direction face) {
        if (level == null) {
            return 0;
        }
        BlockPos neighborPos = worldPosition.relative(face);
        if (level.getBlockEntity(neighborPos) instanceof RedstoneInterfaceBlockEntity neighbor) {
            PinInfo pin = neighbor.getFacePin(face.getOpposite());
            if (pin != null && pin.type() == CircuitComponent.PinType.OUTPUT && pin.bitWidth() == 8) {
                return neighbor.getFaceValue(face.getOpposite()) & 0xFF;
            }
        }
        return 0;
    }

    private void collectOutputs() {
        if (simulator == null) {
            return;
        }
        boolean changed = false;
        for (Map.Entry<Direction, Integer> entry : assignments.entrySet()) {
            PinInfo pin = pins.get(entry.getValue());
            if (pin.type() != CircuitComponent.PinType.OUTPUT) {
                continue;
            }
            int value = pin.bitWidth() == 1
                    ? simulator.getComponentValue(pin.x(), pin.y()) & 0xF
                    : simulator.getComponentBusValue(pin.x(), pin.y()) & 0xFF;
            if (faceValues.getOrDefault(entry.getKey(), -1) != value) {
                faceValues.put(entry.getKey(), value);
                changed = true;
            }
        }
        if (changed) {
            sync();
        }
    }

    private void recordPerformance(double ms) {
        int sampleTicks = ElementsPlusServerConfig.interfaceSampleTicks();
        int avgLimit = ElementsPlusServerConfig.interfaceAvgMsptLimit();
        int maxLimit = ElementsPlusServerConfig.interfaceMaxMsptLimit();
        int window = sampleTicks == 0 ? 100 : Math.max(1, sampleTicks);
        samples.addLast(ms);
        while (samples.size() > window) {
            samples.removeFirst();
        }
        double sum = 0;
        double max = 0;
        for (double sample : samples) {
            sum += sample;
            max = Math.max(max, sample);
        }
        averageMs = sum / samples.size();
        if (sampleTicks == 0) {
            return;
        }
        if (maxLimit == 0) {
            hibernate();
            return;
        }
        if (avgLimit == 0 && samples.size() >= sampleTicks) {
            hibernate();
            return;
        }
        if (samples.size() >= sampleTicks
                && (averageMs > avgLimit || max > maxLimit)) {
            hibernate();
        }
    }

    private void hibernate() {
        if (hibernating) {
            return;
        }
        hibernating = true;
        running = false;
        if (level != null) {
            level.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        sync();
        setChanged();
    }

    private void emitSmoke() {
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE,
                    worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5,
                    1, 0.12, 0.12, 0.12, 0.0);
        }
    }

    private void resetPerformance() {
        samples.clear();
        averageMs = 0;
        lastStepMs = 0;
    }

    private void setChangedAndSync() {
        setChanged();
        sync();
    }

    private void updateRedstoneConnections() {
        if (level == null || level.isClientSide) {
            return;
        }
        int mask = 0;
        for (Map.Entry<Direction, Integer> entry : assignments.entrySet()) {
            if (entry.getValue() >= 0 && entry.getValue() < pins.size()
                    && pins.get(entry.getValue()).bitWidth() == 1) {
                mask |= 1 << entry.getKey().ordinal();
            }
        }
        BlockState state = getBlockState();
        if (state.hasProperty(RedstoneInterfaceBlock.REDSTONE_CONNECTIONS)
                && state.getValue(RedstoneInterfaceBlock.REDSTONE_CONNECTIONS) != mask) {
            level.setBlock(worldPosition,
                    state.setValue(RedstoneInterfaceBlock.REDSTONE_CONNECTIONS, mask), 3);
        }
    }

    private void sync() {
        setChanged();
        if (level != null && !loading) {
            updateRedstoneConnections();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            if (!level.isClientSide) {
                level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
            }
        }
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        loading = true;
        try {
            items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
            ContainerHelper.loadAllItems(tag, items, provider);
            boolean fullSave = tag.contains("Items", CompoundTag.TAG_LIST);
            lastCircuit = ItemStack.EMPTY;
            pins.clear();
            assignments.clear();
            faceValues.clear();
            pendingSimulatorState = fullSave && tag.contains("Simulator", CompoundTag.TAG_COMPOUND)
                    ? tag.getCompound("Simulator").copy() : null;
            if (fullSave) {
                refreshCircuit();
            }
            if (tag.contains("Pins", CompoundTag.TAG_LIST)) {
                loadPins(tag.getList("Pins", CompoundTag.TAG_COMPOUND));
            }
            if (tag.contains("Assignments", CompoundTag.TAG_LIST)) {
                loadAssignments(tag.getList("Assignments", CompoundTag.TAG_COMPOUND));
            }
            if (tag.contains("FaceValues", CompoundTag.TAG_LIST)) {
                loadFaceValues(tag.getList("FaceValues", CompoundTag.TAG_COMPOUND));
            }
            running = tag.getBoolean("Running");
            hibernating = tag.getBoolean("Hibernating");
            if (simulator != null && pendingSimulatorState != null) {
                simulator.loadState(pendingSimulatorState);
                pendingSimulatorState = null;
            }
        } finally {
            loading = false;
        }
        updateRedstoneConnections();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        ContainerHelper.saveAllItems(tag, items, provider);
        tag.putBoolean("Running", running);
        tag.putBoolean("Hibernating", hibernating);
        tag.put("Pins", savePins());
        tag.put("Assignments", saveAssignments());
        tag.put("FaceValues", saveFaceValues());
        if (simulator != null) {
            tag.put("Simulator", simulator.saveState());
        } else if (pendingSimulatorState != null) {
            tag.put("Simulator", pendingSimulatorState.copy());
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.put("Pins", savePins());
        tag.put("Assignments", saveAssignments());
        tag.put("FaceValues", saveFaceValues());
        tag.putBoolean("Running", running);
        tag.putBoolean("Hibernating", hibernating);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private ListTag savePins() {
        ListTag list = new ListTag();
        for (PinInfo pin : pins) {
            CompoundTag item = new CompoundTag();
            item.putInt("Index", pin.index());
            item.putString("Label", pin.label());
            item.putString("Type", pin.type().name());
            item.putInt("Width", pin.bitWidth());
            item.putInt("X", pin.x());
            item.putInt("Y", pin.y());
            list.add(item);
        }
        return list;
    }

    private ListTag saveAssignments() {
        ListTag list = new ListTag();
        for (Map.Entry<Direction, Integer> entry : assignments.entrySet()) {
            CompoundTag item = new CompoundTag();
            item.putString("Face", entry.getKey().getSerializedName());
            item.putInt("Pin", entry.getValue());
            list.add(item);
        }
        return list;
    }

    private ListTag saveFaceValues() {
        ListTag list = new ListTag();
        for (Map.Entry<Direction, Integer> entry : faceValues.entrySet()) {
            CompoundTag item = new CompoundTag();
            item.putString("Face", entry.getKey().getSerializedName());
            item.putInt("Value", entry.getValue());
            list.add(item);
        }
        return list;
    }

    private void loadPins(ListTag list) {
        pins.clear();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag item = list.getCompound(i);
            try {
                pins.add(new PinInfo(item.getInt("Index"), item.getString("Label"),
                        CircuitComponent.PinType.valueOf(item.getString("Type")),
                        item.getInt("Width"), item.getInt("X"), item.getInt("Y")));
            } catch (IllegalArgumentException ignored) {
                // Ignore malformed pin data.
            }
        }
    }

    private void loadAssignments(ListTag list) {
        assignments.clear();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag item = list.getCompound(i);
            Direction face = Direction.byName(item.getString("Face"));
            int index = item.getInt("Pin");
            if (face != null && index >= 0 && index < pins.size()) {
                assignments.put(face, index);
            }
        }
    }

    private void loadFaceValues(ListTag list) {
        faceValues.clear();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag item = list.getCompound(i);
            Direction face = Direction.byName(item.getString("Face"));
            if (face != null) {
                faceValues.put(face, item.getInt("Value"));
            }
        }
    }

    public int getOutputSignal(Direction direction) {
        // getSignal 的方向是“从查询方块指向本方块”的相反方向；
        // 例如本方块西侧有红石线时，查询会传入 EAST。
        Direction face = direction.getOpposite();
        PinInfo pin = getFacePin(face);
        return pin != null && pin.type() == CircuitComponent.PinType.OUTPUT && pin.bitWidth() == 1
                ? getFaceValue(face) & 0xF : 0;
    }

    public int getAssignedPinIndex(Direction face) {
        return assignments.getOrDefault(face, -1);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
    }

    public CircuitDiagram getDiagram() {
        return diagram;
    }
}
