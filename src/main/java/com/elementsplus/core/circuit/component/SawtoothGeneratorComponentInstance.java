package com.elementsplus.core.circuit.component;

import net.minecraft.network.chat.Component;

import java.util.List;

public class SawtoothGeneratorComponentInstance extends AbstractComponentInstance {

    public static final String KEY_INITIAL_PHASE = "initial_phase";

    public static final IntConfig INITIAL_PHASE = new IntConfig(
            KEY_INITIAL_PHASE,
            Component.translatable("circuit.elements-plus.config.initial_phase"),
            null, 0, 15, 1, 0, true);

    public static final List<Config> CONFIGS = List.of(INITIAL_PHASE);

    public SawtoothGeneratorComponentInstance() {
        super();
    }

    public SawtoothGeneratorComponentInstance(SawtoothGeneratorComponentInstance source) {
        super(source);
    }

    @Override
    protected List<Config> defaultConfigs() {
        return CONFIGS;
    }

    @Override
    protected CircuitComponentInstance createEmptyCopy() {
        return new SawtoothGeneratorComponentInstance();
    }

    public int getInitialPhase() {
        return getInt(KEY_INITIAL_PHASE);
    }

    public void setInitialPhase(int value) {
        setInt(KEY_INITIAL_PHASE, value);
    }
}
