package com.elementsplus.core.circuit.component;

import net.minecraft.network.chat.Component;

import java.util.List;

public class CapacitorComponentInstance extends AbstractComponentInstance {

    public static final String KEY_CHARGE_SPEED = "charge_speed";
    public static final String KEY_DISCHARGE_SPEED = "discharge_speed";

    public static final IntConfig CHARGE_SPEED = new IntConfig(
            KEY_CHARGE_SPEED,
            Component.translatable("circuit.elements-plus.config.charge_speed"),
            null,
            0,
            15,
            1,
            15,
            true
    );
    public static final IntConfig DISCHARGE_SPEED = new IntConfig(
            KEY_DISCHARGE_SPEED,
            Component.translatable("circuit.elements-plus.config.discharge_speed"),
            null,
            0,
            15,
            1,
            15,
            true
    );

    public static final List<Config> CONFIGS = List.of(CHARGE_SPEED, DISCHARGE_SPEED);

    public CapacitorComponentInstance() {
        super();
    }

    public CapacitorComponentInstance(CapacitorComponentInstance source) {
        super(source);
    }

    @Override
    protected List<Config> defaultConfigs() {
        return CONFIGS;
    }

    @Override
    protected CircuitComponentInstance createEmptyCopy() {
        return new CapacitorComponentInstance();
    }

    public int getChargeSpeed() {
        return getInt(KEY_CHARGE_SPEED);
    }

    public int getDischargeSpeed() {
        return getInt(KEY_DISCHARGE_SPEED);
    }
}