package com.elementsplus.core.circuit.component;

import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 8 位常量/掩码的位配置。每一位是布尔值，输出时 1 对应 15，0 对应 0。
 */
public class Const8ComponentInstance extends AbstractComponentInstance {

    public static final String KEY_BIT_PREFIX = "bit";
    public static final int LANE_COUNT = 8;
    /** 默认值 123：低 8 位从低到高为 1 1 0 1 1 1 1 0。 */
    public static final int DEFAULT_VALUE = 123;
    public static final List<Config> CONFIGS;

    static {
        List<Config> configs = new ArrayList<>();
        for (int b = 0; b < LANE_COUNT; b++) {
            configs.add(new BooleanConfig(
                    KEY_BIT_PREFIX + b,
                    Component.translatable("circuit.elements-plus.config.bit", String.valueOf(b)),
                    null,
                    ((DEFAULT_VALUE >> b) & 1) != 0
            ));
        }
        CONFIGS = List.copyOf(configs);
    }

    public Const8ComponentInstance() {
        super();
    }

    public Const8ComponentInstance(Const8ComponentInstance source) {
        super(source);
    }

    @Override
    protected List<Config> defaultConfigs() {
        return CONFIGS;
    }

    @Override
    protected CircuitComponentInstance createEmptyCopy() {
        return new Const8ComponentInstance();
    }

    public boolean getBit(int bit) {
        return getBoolean(KEY_BIT_PREFIX + bit);
    }

    public void setBit(int bit, boolean value) {
        setBoolean(KEY_BIT_PREFIX + bit, value);
    }

    /** 8 位掩码值：0~255。 */
    public int getValue() {
        int value = 0;
        for (int b = 0; b < LANE_COUNT; b++) {
            if (getBit(b)) {
                value |= 1 << b;
            }
        }
        return value;
    }

    /** 将 0~255 写入各位。 */
    public void setValue(int value) {
        for (int b = 0; b < LANE_COUNT; b++) {
            setBit(b, ((value >> b) & 1) != 0);
        }
    }
}
