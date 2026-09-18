package com.elementsplus.core.circuit.component;

import net.minecraft.network.chat.Component;

import java.util.List;

public class InputComponentInstance extends AbstractComponentInstance {

    public static final String KEY_SIGNAL = "signal";
    public static final String KEY_LABEL = "label";

    public static final IntConfig SIGNAL = new IntConfig(
            KEY_SIGNAL,
            Component.translatable("circuit.elements-plus.config.signal"),
            null,
            0,
            15,
            1,
            0,
            true
    );

    public static final StringConfig LABEL = new StringConfig(
            KEY_LABEL,
            Component.translatable("circuit.elements-plus.config.label"),
            null,
            "",
            64
    );

    public static final List<Config> CONFIGS = List.of(LABEL, SIGNAL);

    public InputComponentInstance() {
        super();
    }

    public InputComponentInstance(InputComponentInstance source) {
        super(source);
    }

    @Override
    protected List<Config> defaultConfigs() {
        return CONFIGS;
    }

    @Override
    protected CircuitComponentInstance createEmptyCopy() {
        return new InputComponentInstance();
    }

    public int getSignal() {
        return getInt(KEY_SIGNAL);
    }

    public void setSignal(int value) {
        setInt(KEY_SIGNAL, value);
    }

    public String getLabel() {
        return getString(KEY_LABEL);
    }

    public void setLabel(String value) {
        setString(KEY_LABEL, value);
    }
}