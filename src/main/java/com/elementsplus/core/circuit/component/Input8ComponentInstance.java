package com.elementsplus.core.circuit.component;

import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class Input8ComponentInstance extends AbstractComponentInstance {

    public static final String KEY_LABEL = "label";
    public static final String KEY_BIT_PREFIX = "bit";

    public static final StringConfig LABEL = new StringConfig(
            KEY_LABEL,
            Component.translatable("circuit.elements-plus.config.label"),
            null,
            "",
            64
    );

    public static final int LANE_COUNT = 8;

    public static final List<Config> CONFIGS;

    static {
        List<Config> configs = new ArrayList<>();
        configs.add(LABEL);
        for (int b = 0; b < LANE_COUNT; b++) {
            configs.add(new IntConfig(
                    KEY_BIT_PREFIX + b,
                    Component.translatable("circuit.elements-plus.config.bit", String.valueOf(b)),
                    null,
                    0,
                    15,
                    1,
                    0,
                    true
            ));
        }
        CONFIGS = List.copyOf(configs);
    }

    public Input8ComponentInstance() {
        super();
    }

    public Input8ComponentInstance(Input8ComponentInstance source) {
        super(source);
    }

    @Override
    protected List<Config> defaultConfigs() {
        return CONFIGS;
    }

    @Override
    protected CircuitComponentInstance createEmptyCopy() {
        return new Input8ComponentInstance();
    }

    public static boolean isBitKey(String key) {
        return key.startsWith(KEY_BIT_PREFIX);
    }

    public int getBit(int bit) {
        return getInt(KEY_BIT_PREFIX + bit);
    }

    public void setBit(int bit, int value) {
        setInt(KEY_BIT_PREFIX + bit, Math.clamp(value, 0, 15));
    }

    /** 8 位二进制值：0~255。一位为"非 0"即计为该位为 1。 */
    public int getValue() {
        int v = 0;
        for (int b = 0; b < LANE_COUNT; b++) {
            if (getBit(b) != 0) v |= (1 << b);
        }
        return v;
    }

    /** 将 0~255 写入各位滑块：对应位为 1 时置 15，否则置 0。 */
    public void setValue(int value) {
        for (int b = 0; b < LANE_COUNT; b++) {
            setBit(b, ((value >> b) & 1) != 0 ? 15 : 0);
        }
    }

    public String getLabel() {
        return getString(KEY_LABEL);
    }

    public void setLabel(String value) {
        setString(KEY_LABEL, value);
    }
}