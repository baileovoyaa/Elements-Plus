package com.elementsplus.core.circuit.component;

import net.minecraft.network.chat.Component;

import java.util.List;

public class ResistorComponentInstance extends AbstractComponentInstance {

    public static final String KEY_MULTIPLIER = "multiplier";

    public static final FloatConfig MULTIPLIER = new FloatConfig(
            KEY_MULTIPLIER,
            Component.translatable("circuit.elements-plus.config.multiplier"),
            null,
            0f,
            1f,
            0.5f,
            true
    );

    public static final List<Config> CONFIGS = List.of(MULTIPLIER);

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

    public float getMultiplier() {
        return getFloat(KEY_MULTIPLIER);
    }

    public void setMultiplier(float value) {
        setFloat(KEY_MULTIPLIER, value);
    }
}