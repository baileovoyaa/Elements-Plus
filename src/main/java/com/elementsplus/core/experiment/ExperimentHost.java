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
     * 设置失败测例的结构化数值，供客户端按本地数字格式渲染。
     */
    default void setFailureInfo(ExperimentFailureInfo info) {
    }

    /** 设置时序测例的完整失败轨迹（逐 tick 数据）。 */
    default void setSequentialFailureInfo(SequentialFailureInfo info) {
    }

    /**
     * 报告某个测例的结果。
     *
     * @param index 测例索引
     * @param pass  是否通过
     */
    void reportTestResult(int index, boolean pass);

    /**
     * 报告测例状态：1 通过、0 失败、2 TLE（仍属于失败）。
     */
    default void reportTestResultState(int index, int result) {
        reportTestResult(index, result == 1);
    }

    /** 记录交互式测例成功时实际执行的 tick 数。 */
    default void setTestTicks(int index, int ticks) {
    }

    /**
     * 转化当前正在实验的物品。
     */
    void transformActiveItem(Function<ItemStack, ItemStack> function);
}