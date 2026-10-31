package com.elementsplus;

import com.elementsplus.core.experiment.BaseExperiment;
import com.elementsplus.core.experiment.BuiltinExperimentChapters;
import com.elementsplus.core.experiment.BuiltinExperiments;
import com.elementsplus.player.PlayerExperimentsAttachment;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashSet;
import java.util.Set;

/** 统一处理内置进度，避免各调用点重复查找 AdvancementHolder。 */
public final class ModAdvancements {
    private ModAdvancements() {
    }

    public static void award(ServerPlayer player, String advancement, String criterion) {
        if (player == null) {
            return;
        }
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        AdvancementHolder holder = server.getAdvancements().get(ElementsPlus.id(advancement));
        if (holder != null) {
            player.getAdvancements().award(holder, criterion);
        }
    }

    /**
     * 实验成功后的统一进度判定：任意实验、可选实验、ALU 基础版链路、全实验。
     * 调用前应已把实验写入 {@link PlayerExperimentsAttachment}。
     */
    public static void onExperimentCompleted(ServerPlayer player, String experimentName) {
        BaseExperiment experiment = BuiltinExperiments.byId(experimentName);
        if (experiment == null) {
            return;
        }
        award(player, "trial_and_error", "experiment_success");
        if (BuiltinExperimentChapters.isOptionalExperiment(experiment)) {
            award(player, "challenge_accepted", "optional_experiment_success");
        }

        Set<String> completed = PlayerExperimentsAttachment.get(player);
        if (completed.containsAll(names(BuiltinExperimentChapters.requiredExperimentsFor(
                BuiltinExperimentChapters.ALU_BASIC)))) {
            award(player, "logical_conclusion", "alu_basic_complete");
        }
        if (completed.containsAll(names(BuiltinExperimentChapters.allExperimentsIncludingOptional()))) {
            award(player, "perfect_ten", "all_experiments");
        }
    }

    private static Set<String> names(Set<BaseExperiment> experiments) {
        Set<String> names = new HashSet<>();
        for (BaseExperiment experiment : experiments) {
            names.add(experiment.getName());
        }
        return names;
    }
}
