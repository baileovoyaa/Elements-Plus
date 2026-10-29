package com.elementsplus.core.experiment;

import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * 自定义元件不是普通测例实验：实际编译由实验桌通过配置表单直接调用
 * {@code compileCustomComponent} 完成。这个实验对象只负责章节列表展示。
 */
public class CustomComponentExperiment extends BaseExperiment {
    @Override
    public boolean tick(Context context) {
        return false;
    }

    @Override
    public List<Component> preCheckReasons(Context context) {
        return List.of();
    }

    @Override
    public int getTestCaseCount() {
        return 0;
    }
}
