package com.elementsplus.core.circuit.component;

import net.minecraft.network.chat.Component;

import java.util.List;

public class ResonatorComponentInstance extends AbstractComponentInstance {

    public static final String KEY_HIGH_DURATION = "high_duration";
    public static final String KEY_LOW_DURATION = "low_duration";
    public static final String KEY_INITIAL_PHASE = "initial_phase";

    public static final IntConfig HIGH_DURATION = new IntConfig(KEY_HIGH_DURATION, Component.translatable("circuit.elements-plus.config.high_duration"), null, 1, 20, 1, 1, true);
    public static final IntConfig LOW_DURATION = new IntConfig(KEY_LOW_DURATION, Component.translatable("circuit.elements-plus.config.low_duration"), null, 1, 20, 1, 1, true);
    public static final IntConfig INITIAL_PHASE = new IntConfig(KEY_INITIAL_PHASE, Component.translatable("circuit.elements-plus.config.initial_phase"), null, 0, 40, 1, 0, true);

    public static final List<Config> CONFIGS = List.of(HIGH_DURATION, LOW_DURATION, INITIAL_PHASE);

    public ResonatorComponentInstance() {
        super();
    }

    public ResonatorComponentInstance(ResonatorComponentInstance source) {
        super(source);
    }

    @Override
    protected List<Config> defaultConfigs() {
        return CONFIGS;
    }

    @Override
    protected CircuitComponentInstance createEmptyCopy() {
        return new ResonatorComponentInstance();
    }

    public int getHighDuration() {
        return getInt(KEY_HIGH_DURATION);
    }

    public void setHighDuration(int value) {
        setInt(KEY_HIGH_DURATION, value);
    }

    public int getLowDuration() {
        return getInt(KEY_LOW_DURATION);
    }

    public void setLowDuration(int value) {
        setInt(KEY_LOW_DURATION, value);
    }

    public int getInitialPhase() {
        return getInt(KEY_INITIAL_PHASE);
    }

    public void setInitialPhase(int value) {
        setInt(KEY_INITIAL_PHASE, value);
    }
}