package com.elementsplus.core.experiment;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Function;

/**
 * 实验宿主，通常由实验桌方块实体实现。
 * 实验通过 Context 与本接口交互，从而将结果反馈给界面。
 */
public interface ExperimentHost {
    /**
     * 实验结束（成功或失败）。
     */
    void finishExperiment(boolean success);

    /**
     * 设置实验失败原因（statusButton 悬停提示）。
     */
    void setErrorLines(List<Component> lines);

    /**
     * 报告某个测例的结果。
     *
     * @param index 测例索引
     * @param pass  是否通过
     */
    void reportTestResult(int index, boolean pass);

    /**
     * 转化当前正在实验的物品。
     */
    void transformActiveItem(Function<ItemStack, ItemStack> function);
}