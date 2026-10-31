package com.elementsplus.blocks.entity;

import com.elementsplus.ModAdvancements;
import com.elementsplus.ModBlockEntityTypes;
import com.elementsplus.core.circuit.CircuitItemData;
import com.elementsplus.core.circuit.CustomComponentCompiler;
import com.elementsplus.core.circuit.diagram.CircuitDiagram;
import com.elementsplus.core.circuit.storage.CircuitStorage;
import com.elementsplus.core.circuit.storage.StoredCircuit;
import com.elementsplus.core.experiment.BaseExperiment;
import com.elementsplus.core.experiment.BuiltinExperimentChapters;
import com.elementsplus.core.experiment.BuiltinExperiments;
import com.elementsplus.core.experiment.ExperimentChapter;
import com.elementsplus.core.experiment.ExperimentFailureInfo;
import com.elementsplus.core.experiment.ExperimentHost;
import com.elementsplus.core.experiment.SequentialFailureInfo;
import com.elementsplus.menu.ExperimentTableMenu;
import com.elementsplus.network.CircuitStorageSyncPayload;
import com.elementsplus.network.CustomComponentCompilePayload;
import com.elementsplus.network.ExperimentTableScreenDataPayload;
import com.elementsplus.network.ExperimentTableStatusPayload;
import com.elementsplus.player.PlayerExperimentsAttachment;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

/**
 * 实验桌方块实体：额外槽位会持久保存在这里。
 * 实验状态只在方块加载期间有效（不持久化）。
 */
public class ExperimentTableBlockEntity extends BaseContainerBlockEntity implements ExperimentHost {
    private NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);
    private String selectedChapter = null;
    private String selectedExperiment = null;

    private ExperimentStatus status = ExperimentStatus.IDLE;
    private boolean paused = false;
    private BaseExperiment activeExperiment = null;
    private UUID initiator = null;
    private int testIndex = 0;
    private int time = 0;
    private float progress = 0f;
    private final List<Integer> testResults = new ArrayList<>();
    private final List<Component> tooltipLines = new ArrayList<>();
    private ExperimentFailureInfo failureInfo = null;
    private SequentialFailureInfo sequentialFailure = null;
    private Object experimentState = null;
    private ItemStack startedStack = null;

    public ExperimentTableBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(ModBlockEntityTypes.EXPERIMENT_TABLE, blockPos, blockState);
    }

    public String getSelectedChapter() {
        return selectedChapter;
    }

    public void setSelectedChapter(String selectedChapter) {
        if (!Objects.equals(this.selectedChapter, selectedChapter)) {
            this.selectedChapter = selectedChapter;
            this.setChanged();
        }
    }

    public String getSelectedExperiment() {
        return selectedExperiment;
    }

    public void setSelectedExperiment(String selectedExperiment) {
        if (!Objects.equals(this.selectedExperiment, selectedExperiment)) {
            this.selectedExperiment = selectedExperiment;
            this.setChanged();
        }
    }

    public ExperimentStatus getStatus() {
        return status;
    }

    public boolean isPaused() {
        return paused;
    }

    @Override
    protected void loadAdditional(CompoundTag compoundTag, HolderLookup.Provider provider) {
        super.loadAdditional(compoundTag, provider);
        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(compoundTag, this.items, provider);
        this.selectedChapter = compoundTag.contains("SelectedChapter") ? compoundTag.getString("SelectedChapter") : null;
        this.selectedExperiment = compoundTag.contains("SelectedExperiment") ? compoundTag.getString("SelectedExperiment") : null;
    }

    @Override
    protected void saveAdditional(CompoundTag compoundTag, HolderLookup.Provider provider) {
        super.saveAdditional(compoundTag, provider);
        ContainerHelper.saveAllItems(compoundTag, this.items, provider);
        if (this.selectedChapter != null) {
            compoundTag.putString("SelectedChapter", this.selectedChapter);
        }
        if (this.selectedExperiment != null) {
            compoundTag.putString("SelectedExperiment", this.selectedExperiment);
        }
    }

    @Override
    public int getContainerSize() {
        return this.items.size();
    }

    @Override
    public void setItem(int index, ItemStack itemStack) {
        this.getItems().set(index, itemStack);
        if (!itemStack.isEmpty()) {
            itemStack.setCount(Math.min(this.getMaxStackSize(), itemStack.getCount()));
        }
        this.setChanged();
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
    protected @NotNull Component getDefaultName() {
        return Component.translatable("container.elements-plus.experiment_table");
    }

    @Override
    protected @NotNull AbstractContainerMenu createMenu(int i, Inventory inventory) {
        return new ExperimentTableMenu(i, inventory, this, ContainerLevelAccess.create(this.level, this.worldPosition));
    }

    /* ============================================================
     *  实验流程
     * ============================================================ */

    public void serverTick() {
        if (this.level == null || this.level.isClientSide) {
            return;
        }
        if (status == ExperimentStatus.IDLE) {
            return;
        }
        if (startedStack == null || this.getItem(0) != startedStack) {
            resetExperimentState();
            syncStatus();
            return;
        }
        if (status == ExperimentStatus.BUSY && !paused) {
            stepExperiment();
        }
    }

    private void stepExperiment() {
        if (!(this.level instanceof ServerLevel)) {
            return;
        }
        BaseExperiment experiment = activeExperiment;
        if (experiment == null) {
            resetExperimentState();
            syncStatus();
            return;
        }
        BaseExperiment.Context context = new BaseExperiment.Context(this, this, this.getItem(0));
        context.time = time;
        context.testIndex = testIndex;
        context.playerUuid = initiator;
        context.experimentState = experimentState;
        boolean ok = experiment.tick(context);
        this.testIndex = context.testIndex;
        this.experimentState = context.experimentState;
        this.time = context.time + 1;
        this.setChanged();
        if (ok) {
            syncStatus();
        }
    }

    /**
     * 尝试开始实验。仅当玩家解锁的章节包含该实验时才有效。
     */
    public void startExperiment(String experimentName, ServerPlayer player) {
        if (status == ExperimentStatus.BUSY) {
            return;
        }
        BaseExperiment experiment = experimentName == null ? null : BuiltinExperiments.byId(experimentName);
        if (experiment == null || experiment == BuiltinExperiments.CUSTOM_COMPONENT) {
            return;
        }
        Set<ExperimentChapter> unlocked = BuiltinExperimentChapters.getUnlockedByName(PlayerExperimentsAttachment.get(player));
        boolean allowed = false;
        for (ExperimentChapter chapter : unlocked) {
            if (chapter.containsExperiment(experiment)) {
                allowed = true;
                break;
            }
        }
        if (!allowed) {
            return;
        }

        BaseExperiment.Context context = new BaseExperiment.Context(this, this, this.getItem(0));
        context.playerUuid = player.getUUID();
        List<Component> reasons = experiment.preCheckReasons(context);
        startedStack = this.getItem(0);
        if (reasons.isEmpty()) {
            status = ExperimentStatus.BUSY;
            paused = false;
            activeExperiment = experiment;
            initiator = player.getUUID();
            testIndex = 0;
            time = 0;
            progress = 0f;
            testResults.clear();
            int count = experiment.getTestCaseCount();
            for (int i = 0; i < count; i++) {
                testResults.add(-1);
            }
            tooltipLines.clear();
            failureInfo = null;
            sequentialFailure = null;
            experimentState = null;
        } else {
            status = ExperimentStatus.ERROR;
            paused = false;
            activeExperiment = null;
            initiator = null;
            testIndex = 0;
            time = 0;
            progress = 0f;
            testResults.clear();
            tooltipLines.clear();
            tooltipLines.add(Component.translatable("experiment.elements-plus.precheck.header"));
            tooltipLines.addAll(reasons);
            failureInfo = null;
            sequentialFailure = null;
            experimentState = null;
        }
        this.setChanged();
        syncStatus();
    }

    /** 自定义元件编译入口：校验表单和电路，保存到分离存储并完成实验。 */
    public void compileCustomComponent(CustomComponentCompilePayload payload, ServerPlayer player) {
        if (status == ExperimentStatus.BUSY) {
            return;
        }
        ItemStack stack = this.getItem(0);
        CircuitDiagram diagram = stack.get(com.elementsplus.ModDataComponents.CIRCUIT_DIAGRAM);
        if (diagram == null || CircuitItemData.isCompiled(stack)
                || stack.has(com.elementsplus.ModDataComponents.EQUIVALENT_COMPONENT)) {
            status = ExperimentStatus.ERROR;
            setErrorLines(List.of(Component.translatable("experiment.elements-plus.precheck.not_diagram")));
            syncStatus();
            return;
        }
        List<Component> reasons = CustomComponentCompiler.validate(diagram, payload.name(),
                payload.width(), payload.height(), payload.pins());
        if (!reasons.isEmpty()) {
            status = ExperimentStatus.ERROR;
            initiator = player.getUUID();
            setErrorLines(reasons);
            syncStatus();
            return;
        }
        StoredCircuit record = CustomComponentCompiler.compile(diagram, payload.name(), payload.description(),
                payload.iconItem(), payload.iconChar(), payload.color(), payload.width(), payload.height(),
                payload.pins(), player.getUUID(), player.getGameProfile().getName());
        CircuitItemData.bindCompiled(stack, record);
        startedStack = stack;
        activeExperiment = BuiltinExperiments.CUSTOM_COMPONENT;
        initiator = player.getUUID();
        status = ExperimentStatus.SUCCESS;
        paused = false;
        progress = 1f;
        testResults.clear();
        failureInfo = null;
        sequentialFailure = null;
        experimentState = null;
        tooltipLines.clear();
        PlayerExperimentsAttachment.get(player).add(BuiltinExperiments.CUSTOM_COMPONENT.getName());
        ModAdvancements.award(player, "self_made", "compile_custom");
        ModAdvancements.onExperimentCompleted(player, BuiltinExperiments.CUSTOM_COMPONENT.getName());
        level.playSound(null, getBlockPos(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.0F);
        this.setChanged();
        syncStatus();
        for (ServerPlayer tracking : PlayerLookup.tracking(this)) {
            ServerPlayNetworking.send(tracking, screenDataFor(tracking));
        }
        if (level.getServer() != null) {
            CircuitStorageSyncPayload storageSync = new CircuitStorageSyncPayload(CircuitStorage.get().ordered());
            for (ServerPlayer online : level.getServer().getPlayerList().getPlayers()) {
                ServerPlayNetworking.send(online, storageSync);
            }
        }
    }

    public void pauseExperiment() {
        if (status == ExperimentStatus.BUSY && !paused) {
            paused = true;
            this.setChanged();
            syncStatus();
        }
    }

    public void resumeExperiment() {
        if (status == ExperimentStatus.BUSY && paused) {
            paused = false;
            this.setChanged();
            syncStatus();
        }
    }

    public void stopExperiment() {
        if (status == ExperimentStatus.IDLE) {
            return;
        }
        resetExperimentState();
        syncStatus();
    }

    private void resetExperimentState() {
        status = ExperimentStatus.IDLE;
        paused = false;
        activeExperiment = null;
        initiator = null;
        testIndex = 0;
        time = 0;
        progress = 0f;
        testResults.clear();
        tooltipLines.clear();
        failureInfo = null;
        sequentialFailure = null;
        experimentState = null;
        startedStack = null;
        this.setChanged();
    }

    @Override
    public void finishExperiment(boolean success) {
        if (this.level == null) {
            return;
        }
        paused = false;
        if (success) {
            status = ExperimentStatus.SUCCESS;
            failureInfo = null;
            sequentialFailure = null;
            experimentState = null;
            level.playSound(null, getBlockPos(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.0F);
            progress = 1f;
            BaseExperiment experiment = activeExperiment;
            ItemStack stack = this.getItem(0);
            if (experiment != null && !stack.isEmpty()) {
                BaseExperiment.Context context = new BaseExperiment.Context(this, this, stack);
                context.playerUuid = initiator;
                if (initiator != null && this.level.getServer() != null) {
                    ServerPlayer author = this.level.getServer().getPlayerList().getPlayer(initiator);
                    context.playerName = author == null ? initiator.toString() : author.getGameProfile().getName();
                }
                experiment.onComplete(context, true);
            }
            if (activeExperiment != null && initiator != null && this.level.getServer() != null) {
                ServerPlayer init = this.level.getServer().getPlayerList().getPlayer(initiator);
                if (init != null) {
                    PlayerExperimentsAttachment.get(init).add(activeExperiment.getName());
                    ModAdvancements.onExperimentCompleted(init, activeExperiment.getName());
                }
            }
            if (this.level instanceof ServerLevel serverLevel) {
                for (ServerPlayer tracking : PlayerLookup.tracking(this)) {
                    ServerPlayNetworking.send(tracking, screenDataFor(tracking));
                }
                if (this.level.getServer() != null) {
                    CircuitStorageSyncPayload storageSync = new CircuitStorageSyncPayload(CircuitStorage.get().ordered());
                    for (ServerPlayer online : this.level.getServer().getPlayerList().getPlayers()) {
                        ServerPlayNetworking.send(online, storageSync);
                    }
                }
            }
        } else {
            status = ExperimentStatus.ERROR;
            level.playSound(null, getBlockPos(), SoundEvents.NOTE_BLOCK_HARP.value(), SoundSource.PLAYERS, 1.0F, 0.5F);
        }
        this.setChanged();
        syncStatus();
    }

    @Override
    public void setErrorLines(List<Component> lines) {
        tooltipLines.clear();
        if (lines != null) {
            tooltipLines.addAll(lines);
        }
        failureInfo = null;
        sequentialFailure = null;
        this.setChanged();
    }

    @Override
    public void setFailureInfo(ExperimentFailureInfo info) {
        this.failureInfo = info;
        this.setChanged();
    }

    @Override
    public void setSequentialFailureInfo(SequentialFailureInfo info) {
        this.sequentialFailure = info;
        this.setChanged();
    }

    @Override
    public void reportTestResult(int index, boolean pass) {
        if (index >= 0 && index < testResults.size()) {
            testResults.set(index, pass ? 1 : 0);
        }
        recomputeProgress();
        this.setChanged();
        syncStatus();
    }

    private void recomputeProgress() {
        if (testResults.isEmpty()) {
            progress = 0f;
            return;
        }
        int passed = 0;
        for (int result : testResults) {
            if (result > 0) {
                passed++;
            }
        }
        progress = (float) passed / testResults.size();
    }

    @Override
    public void transformActiveItem(Function<ItemStack, ItemStack> function) {
        ItemStack current = this.getItem(0);
        if (current.isEmpty()) {
            return;
        }
        ItemStack next = function.apply(current);
        if (next != current) {
            this.getItems().set(0, next);
            startedStack = next;
        }
        this.setChanged();
    }

    public ExperimentTableStatusPayload buildStatusPayload() {
        List<String> tooltipJson = new ArrayList<>();
        if (this.level != null) {
            for (Component line : tooltipLines) {
                tooltipJson.add(Component.Serializer.toJson(line, this.level.registryAccess()));
            }
        }
        return new ExperimentTableStatusPayload(
                worldPosition,
                status,
                paused,
                activeExperiment != null ? activeExperiment.getName() : null,
                progress,
                new ArrayList<>(testResults),
                tooltipJson,
                failureInfo,
                status == ExperimentStatus.BUSY ? testIndex : -1,
                sequentialFailure
        );
    }

    private ExperimentTableScreenDataPayload screenDataFor(ServerPlayer player) {
        Set<String> unlocked = new HashSet<>();
        for (ExperimentChapter chapter : BuiltinExperimentChapters.getUnlockedByName(PlayerExperimentsAttachment.get(player))) {
            unlocked.add(chapter.name);
        }
        return new ExperimentTableScreenDataPayload(worldPosition, selectedChapter, selectedExperiment, unlocked, PlayerExperimentsAttachment.get(player));
    }

    public void syncStatus() {
        if (this.level == null || this.level.isClientSide) {
            return;
        }
        ExperimentTableStatusPayload payload = buildStatusPayload();
        for (ServerPlayer tracking : PlayerLookup.tracking(this)) {
            ServerPlayNetworking.send(tracking, payload);
        }
    }
}