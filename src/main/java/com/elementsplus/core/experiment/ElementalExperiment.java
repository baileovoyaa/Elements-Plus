package com.elementsplus.core.experiment;

import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * FUTURE
 */
public class ElementalExperiment extends BaseExperiment {
    @Override
    public boolean tick(Context context) {
        return false;
    }

    @Override
    public boolean preCheck(Context context) {
        return false;
    }

    @Override
    public List<Component> preCheckReasons(Context context) {
        return List.of(Component.translatable("experiment.elements-plus.precheck.generic"));
    }
}