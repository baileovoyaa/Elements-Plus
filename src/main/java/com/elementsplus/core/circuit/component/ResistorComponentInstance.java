package com.elementsplus.core.circuit.component;

import net.minecraft.network.chat.Component;

import java.util.List;

public class ResistorComponentInstance extends AbstractComponentInstance {

    public static final String KEY_DECAY = "decay";

    public static final IntConfig DECAY = new IntConfig(
            KEY_DECAY,
            Component.translatable("circuit.elements-plus.config.decay"),
            null,
            0,
            15,
            1,
            1,
            true
    );

    public static final List<Config> CONFIGS = List.of(DECAY);

    public ResistorComponentInstance() {
        super();
    }

    public ResistorComponentInstance(ResistorComponentInstance source) {
        super(source);
    }

    @Override
    protected List<Config> defaultConfigs() {
        return CONFIGS;
    }

    @Override
    protected CircuitComponentInstance createEmptyCopy() {
        return new ResistorComponentInstance();
    }

    public int getDecay() {
        return getInt(KEY_DECAY);
    }

    public void setDecay(int value) {
        setInt(KEY_DECAY, value);
    }
}