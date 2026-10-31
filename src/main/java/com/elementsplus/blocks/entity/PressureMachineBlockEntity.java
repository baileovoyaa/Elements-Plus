package com.elementsplus.blocks.entity;

import com.elementsplus.ElementsPlus;
import com.elementsplus.ModAdvancements;
import com.elementsplus.ModBlockEntityTypes;
import com.elementsplus.blocks.PressureMachineBlock;
import com.elementsplus.menu.PressureMachineMenu;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
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
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

/**
 * 加压机原型。
 * <p>
 * 当前实现为可运行的原型：
 * <ul>
 *     <li>压强按“密闭空间”共享（{@link PressureNetwork}），连接同一空间的多台机器读写同一份压强；</li>
 *     <li>空间检测为有界洪水填充：完整碰撞箱方块视为实心（玻璃也算实心），
 *     台阶/楼梯等不完整方块视为空气；任一边长超过 16 格或空气数超过 4096 视为开放空间；</li>
 *     <li>流量随压差变化：顺压差（入口更高）时更快，逆压差（出口更高）时更慢；
 *     输入或输出口被堵住、或两侧连成同一空间时不工作；</li>
 *     <li>空间压强偏离标准值会施加反胃与高压/低压伤害；</li>
 *     <li>参数均为占位值，见下方常量，后续进游戏实测调整。</li>
 * </ul>
 */
public class PressureMachineBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {

    // ---- 原型参数（后续实测调整） ----
    /** 密闭空间允许的最大边长（格）。 */
    public static final int MAX_AXIS = 16;
    /** 密闭空间允许的最大空气方块数（16×16×16）。 */
    public static final int MAX_VOLUME = MAX_AXIS * MAX_AXIS * MAX_AXIS;
    /** 压差为 0 时的基础流量，B·atm/s。 */
    public static final double TRANSFER_RATE = 2.08;
    /** 顺压差（入口压强更高）时的流量增益，每 atm。 */
    public static final double FORWARD_GAIN = 1.0;
    /** 逆压差（出口压强更高）时的流量衰减，每 atm。 */
    public static final double REVERSE_GAIN = 1.0;
    /** 顺压差时的最大流量倍率。 */
    public static final double MAX_FLOW_MULTIPLIER = 4.0;
    /** 泄漏死区。 */
    public static final double LEAK_DEADZONE = 0.1;
    /** 泄漏幂指数。 */
    public static final double LEAK_BETA = 3.0;
    /** 泄漏系数。 */
    public static final double LEAK_C = 0.304;
    /** 压强下限/上限（atm）。下限为 0 以允许真空与低压伤害。 */
    public static final double MIN_PRESSURE = 0.0;
    public static final double MAX_PRESSURE = 3.0;

    /** 反胃阈值（|p-1|）。 */
    public static final double NAUSEA_THRESHOLD = 0.7;
    /** 高压伤害阈值（p-1）。 */
    public static final double HIGH_DAMAGE_THRESHOLD = 1.25;
    /** 低压伤害阈值（1-p）。 */
    public static final double LOW_DAMAGE_THRESHOLD = 0.9;
    /** 高压伤害系数（心/秒/Δ）。 */
    public static final double HIGH_DAMAGE_K = 0.9;
    /** 低压伤害系数（心/秒/Δ）。 */
    public static final double LOW_DAMAGE_K = 5;

    /** 高压结晶器开始补充压强的阈值。 */
    public static final double CRYSTALLIZER_THRESHOLD = 1.5;
    /** 每补充 1 点结晶器压强消耗的空气量，B·atm。 */
    public static final double CRYSTALLIZER_AIR_COST = 6.0;
    /** 结晶器补充间隔（tick）。 */
    public static final int CRYSTALLIZER_INTERVAL = 10;
    /** 空间扫描间隔（tick）。 */
    public static final int SCAN_INTERVAL = 20;

    private static final ResourceKey<DamageType> PRESSURE_HIGH_DAMAGE =
            ResourceKey.create(Registries.DAMAGE_TYPE, ElementsPlus.id("pressure_high"));
    private static final ResourceKey<DamageType> PRESSURE_LOW_DAMAGE =
            ResourceKey.create(Registries.DAMAGE_TYPE, ElementsPlus.id("pressure_low"));

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
    private static final SpaceInfo EMPTY_SPACE = new SpaceInfo(0, false, Set.of(), Set.of(), null, null, null);

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
    private int flowSyncCooldown;
    private int lastSentFlow;

    private SpaceInfo inputSpace = EMPTY_SPACE;
    private SpaceInfo outputSpace = EMPTY_SPACE;

    int scanCooldown;
    int crystallizerCooldown;

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
        return litTime > 0 && flow > 0.0;
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
        this.flow = tag.getDouble("Flow");
        // 压强由共享网络维护（当前为内存实现），这里只做默认值。
        this.inputPressure = 1.0;
        this.outputPressure = 1.0;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, this.items, registries);
        tag.putShort("BurnTime", (short) this.litTime);
        tag.putShort("BurnTimeTotal", (short) this.litDuration);
        tag.putDouble("Flow", this.flow);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        // 客户端 animateTick 需要知道当前流量；只同步粒子和状态相关的小字段。
        CompoundTag tag = new CompoundTag();
        tag.putDouble("Flow", this.flow);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** 低频同步流量到附近客户端，避免每 tick 发送方块实体包。 */
    private void syncFlowToClients(Level level) {
        if (!(level instanceof ServerLevel)) {
            return;
        }
        if (flowSyncCooldown > 0) {
            flowSyncCooldown--;
            return;
        }
        int quantized = (int) Math.round(flow * 1000.0);
        if (quantized == lastSentFlow) {
            return;
        }
        lastSentFlow = quantized;
        flowSyncCooldown = 5;
        ClientboundBlockEntityDataPacket packet = ClientboundBlockEntityDataPacket.create(this);
        for (ServerPlayer player : PlayerLookup.tracking(this)) {
            player.connection.send(packet);
        }
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

        // 共享压强：先读取，再按空间施加一次泄漏，最后读回最新值。
        be.syncFromNetwork(level);
        be.applyLeaks(level);
        be.syncFromNetwork(level);

        // 端口是否被堵住每 tick 立即判断，避免扫描间隔内继续工作。
        Direction facing = state.getValue(PressureMachineBlock.FACING);
        boolean portsOpen = isAirCell(level, pos.relative(facing))
                && isAirCell(level, pos.relative(facing.getOpposite()));
        boolean canWork = portsOpen && be.canWork();

        ItemStack fuel = be.items.get(0);
        if (be.litTime <= 0 && canWork && !fuel.isEmpty() && isFuel(fuel)) {
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
        if (be.litTime > 0 && canWork) {
            be.pump(level, pos);
            changed = true;
        }
        be.writeToNetwork(level);
        be.syncFlowToClients(level);

        // 生物效果每秒结算一次（同一空间由共享网络去重）。
        be.applySpaceEffects(level);

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

    /** 把空气从入口空间搬到出口空间；流量随压差变化，并受可用空气/容量限制。 */
    private void pump(Level level, BlockPos pos) {
        final double dt = 1.0 / 20.0;
        if (!canWork()) {
            flow = 0.0;
            return;
        }

        double effectiveInput = inputSealed ? inputPressure : 1.0;
        double effectiveOutput = outputSealed ? outputPressure : 1.0;
        double delta = effectiveInput - effectiveOutput;

        double transfer = nominalFlow(delta) * dt;
        // 入口只能搬走“高于最低压强”的空气
        if (inputSealed) {
            double available = Math.max(0.0, (inputPressure - MIN_PRESSURE) * inputVolume);
            transfer = Math.min(transfer, available);
        }
        // 出口只能容纳“低于最高压强”的空气
        if (outputSealed) {
            double room = Math.max(0.0, (MAX_PRESSURE - outputPressure) * outputVolume);
            transfer = Math.min(transfer, room);
        }
        if (transfer <= 0.0) {
            flow = 0.0;
            return;
        }

        if (inputSealed) {
            inputPressure = clamp(inputPressure - transfer / inputVolume);
        }
        if (outputSealed) {
            outputPressure = clamp(outputPressure + transfer / outputVolume);
        }
        flow = transfer / dt;

        // 高压空间给结晶器补充压强（原型：直接补充结晶器的 pressure 值，并消耗空间空气）
        if (outputSealed && outputVolume > 0 && outputPressure > CRYSTALLIZER_THRESHOLD) {
            if (crystallizerCooldown > 0) {
                crystallizerCooldown--;
            } else {
                boolean supplied = false;
                for (BlockPos crystallizerPos : outputSpace.crystallizers()) {
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
                    if (air < MIN_PRESSURE * outputVolume) {
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

    /**
     * 输入/输出口都不能被堵住，且至少一侧是密闭空间时才搬运空气。
     * 只开一侧的开放空间也允许（例如从大气抽气给密闭空间加压）。
     */
    private boolean canWork() {
        return !sameSpace && inputVolume > 0 && outputVolume > 0 && (inputSealed || outputSealed);
    }

    /**
     * 名义流量（B·atm/s）：顺压差时随压差增大而变快，逆压差时随压差增大而变慢。
     */
    private static double nominalFlow(double delta) {
        if (delta >= 0.0) {
            return TRANSFER_RATE * Math.min(1.0 + FORWARD_GAIN * delta, MAX_FLOW_MULTIPLIER);
        }
        return TRANSFER_RATE / (1.0 + REVERSE_GAIN * (-delta));
    }

    private static double leakRate(double pressure) {
        double delta = pressure - 1.0;
        double over = Math.max(0.0, Math.abs(delta) - LEAK_DEADZONE);
        return LEAK_C * Math.signum(delta) * Math.pow(over, LEAK_BETA);
    }

    private static double clamp(double pressure) {
        return Math.max(MIN_PRESSURE, Math.min(MAX_PRESSURE, pressure));
    }

    // ---- 共享压力网络 ----

    /** 从共享网络同步当前压强到本地缓存。 */
    private void syncFromNetwork(Level level) {
        PressureNetwork network = PressureNetwork.get(level);

        if (inputSealed && inputSpace.key() != null) {
            inputPressure = network.get(inputSpace.key());
        } else {
            inputPressure = 1.0;
            if (inputSpace.key() != null) {
                network.remove(inputSpace.key());
            }
        }

        if (sameSpace) {
            outputPressure = inputPressure;
        } else if (outputSealed && outputSpace.key() != null) {
            outputPressure = network.get(outputSpace.key());
        } else {
            outputPressure = 1.0;
            if (outputSpace.key() != null) {
                network.remove(outputSpace.key());
            }
        }
    }

    /** 同一 tick 内同一空间只泄漏一次。 */
    private void applyLeaks(Level level) {
        final double dt = 1.0 / 20.0;
        long gameTime = level.getGameTime();
        PressureNetwork network = PressureNetwork.get(level);

        if (inputSealed && inputVolume > 0 && inputSpace.key() != null
                && network.markLeak(inputSpace.key(), gameTime)) {
            double pressure = network.get(inputSpace.key());
            network.set(inputSpace.key(), clamp(pressure - leakRate(pressure) / inputVolume * dt));
        }
        if (!sameSpace && outputSealed && outputVolume > 0 && outputSpace.key() != null
                && network.markLeak(outputSpace.key(), gameTime)) {
            double pressure = network.get(outputSpace.key());
            network.set(outputSpace.key(), clamp(pressure - leakRate(pressure) / outputVolume * dt));
        }
    }

    /** 把本 tick 更新后的压强写回共享网络。 */
    private void writeToNetwork(Level level) {
        PressureNetwork network = PressureNetwork.get(level);
        if (inputSealed && inputSpace.key() != null) {
            network.set(inputSpace.key(), inputPressure);
        }
        if (!sameSpace && outputSealed && outputSpace.key() != null) {
            network.set(outputSpace.key(), outputPressure);
        }
    }

    // ---- 空间检测 ----

    private void refreshSpaces(Level level, BlockPos pos, BlockState state) {
        Direction facing = state.getValue(PressureMachineBlock.FACING);
        BlockPos inputStart = pos.relative(facing.getOpposite());
        BlockPos outputStart = pos.relative(facing);
        inputSpace = scanSpace(level, inputStart);
        outputSpace = scanSpace(level, outputStart);

        inputVolume = inputSpace.volume();
        inputSealed = inputSpace.sealed();
        outputVolume = outputSpace.volume();
        outputSealed = outputSpace.sealed();
        // 前后连成同一片空间时，搬运没有意义；同时避免把同一空间当成两份来“造气”。
        sameSpace = inputSpace.cells().contains(outputStart) || outputSpace.cells().contains(inputStart);
    }

    /**
     * 从 {@code start} 开始做有界洪水填充。
     * 完整碰撞箱方块（包括玻璃）视为实心；不完整方块视为空气。
     */
    private static SpaceInfo scanSpace(Level level, BlockPos start) {
        if (!isAirCell(level, start)) {
            return EMPTY_SPACE;
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

        BlockPos min = new BlockPos(minX, minY, minZ);
        BlockPos max = new BlockPos(maxX, maxY, maxZ);
        return new SpaceInfo(visited.size(), sealed, crystallizers, visited, min, min, max);
    }

    private static boolean isAirCell(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return true;
        }
        // 玻璃等完整方块：碰撞箱是完整方块，视为实心；
        // 台阶、楼梯等不完整方块视为空气。
        return !state.isCollisionShapeFullBlock(level, pos);
    }

    private record SpaceInfo(int volume, boolean sealed, Set<BlockPos> crystallizers,
                             Set<BlockPos> cells, BlockPos key, BlockPos min, BlockPos max) {
    }

    // ---- 生物效果 ----

    private void applySpaceEffects(Level level) {
        long second = level.getGameTime() / 20L;
        PressureNetwork network = PressureNetwork.get(level);
        if (inputSealed) {
            applyEffectsForSpace(level, network, inputSpace, inputPressure, second);
        }
        if (!sameSpace && outputSealed) {
            applyEffectsForSpace(level, network, outputSpace, outputPressure, second);
        }
    }

    private void applyEffectsForSpace(Level level, PressureNetwork network, SpaceInfo space,
                                      double pressure, long second) {
        if (space == null || !space.sealed() || space.cells().isEmpty()
                || space.key() == null || space.min() == null || space.max() == null) {
            return;
        }
        if (!network.markEffect(space.key(), second)) {
            return;
        }

        double delta = pressure - 1.0;
        boolean nausea = Math.abs(delta) >= NAUSEA_THRESHOLD;
        boolean highDamage = delta >= HIGH_DAMAGE_THRESHOLD;
        boolean lowDamage = -delta >= LOW_DAMAGE_THRESHOLD;
        if (!nausea && !highDamage && !lowDamage) {
            return;
        }

        AABB box = new AABB(
                space.min().getX(), space.min().getY(), space.min().getZ(),
                space.max().getX() + 1, space.max().getY() + 1, space.max().getZ() + 1);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box)) {
            if (!space.cells().contains(entity.blockPosition())) {
                continue;
            }
            if (entity instanceof Player player && (player.isCreative() || player.isSpectator())) {
                continue;
            }
            if (nausea) {
                entity.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 0, false, false));
            }
            if (highDamage) {
                double dps = HIGH_DAMAGE_K * (delta - HIGH_DAMAGE_THRESHOLD);
                boolean damaged = entity.hurt(pressureDamage(level, PRESSURE_HIGH_DAMAGE), (float) (dps * 2.0));
                if (damaged && entity instanceof ServerPlayer serverPlayer) {
                    ModAdvancements.award(serverPlayer, "pressure_cooker", "pressure_high");
                }
            } else if (lowDamage) {
                double dps = LOW_DAMAGE_K * (-delta - LOW_DAMAGE_THRESHOLD);
                boolean damaged = entity.hurt(pressureDamage(level, PRESSURE_LOW_DAMAGE), (float) (dps * 2.0));
                if (damaged && pressure < 0.01 && entity instanceof ServerPlayer serverPlayer) {
                    ModAdvancements.award(serverPlayer, "boiling_point", "pressure_low");
                }
            }
        }
    }

    private static DamageSource pressureDamage(Level level, ResourceKey<DamageType> key) {
        Holder<DamageType> holder = level.registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(key);
        return new DamageSource(holder);
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
