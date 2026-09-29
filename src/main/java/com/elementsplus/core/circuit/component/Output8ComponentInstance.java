package com.elementsplus.core.circuit.component;

import net.minecraft.network.chat.Component;

import java.util.List;

public class Output8ComponentInstance extends AbstractComponentInstance {

    public static final String KEY_LABEL = "label";

    public static final StringConfig LABEL = new StringConfig(
            KEY_LABEL,
            Component.translatable("circuit.elements-plus.config.label"),
            null,
            "",
            64
    );

    public static final List<Config> CONFIGS = List.of(LABEL);

    public Output8ComponentInstance() {
        super();
    }

    public Output8ComponentInstance(Output8ComponentInstance source) {
        super(source);
    }

    @Override
    protected List<Config> defaultConfigs() {
        return CONFIGS;
    }

    @Override
    protected CircuitComponentInstance createEmptyCopy() {
        return new Output8ComponentInstance();
    }

    public String getLabel() {
        return getString(KEY_LABEL);
    }

    public void setLabel(String value) {
        setString(KEY_LABEL, value);
    }
}