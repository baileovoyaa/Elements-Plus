package com.elementsplus.core.circuit.component;

import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 8 位模拟常量：每个通道保存一个 0~15 的信号强度。
 * 默认值从高位到低位为 15 13 11 9 0 2 4 6。
 */
public class ConstAnalog8ComponentInstance extends AbstractComponentInstance {

    public static final String KEY_BIT_PREFIX = "bit";
    public static final int LANE_COUNT = 8;
    /** 下标 0~7 对应从低到高的通道。 */
    public static final int[] DEFAULT_LANES = {6, 4, 2, 0, 9, 11, 13, 15};
    public static final List<Config> CONFIGS;

    static {
        List<Config> configs = new ArrayList<>();
        for (int b = 0; b < LANE_COUNT; b++) {
            configs.add(new IntConfig(
                    KEY_BIT_PREFIX + b,
                    Component.translatable("circuit.elements-plus.config.bit", String.valueOf(b)),
                    null,
                    0,
                    15,
                    1,
                    DEFAULT_LANES[b],
                    true
            ));
        }
        CONFIGS = List.copyOf(configs);
    }

    public ConstAnalog8ComponentInstance() {
        super();
    }

    public ConstAnalog8ComponentInstance(ConstAnalog8ComponentInstance source) {
        super(source);
    }

    @Override
    protected List<Config> defaultConfigs() {
        return CONFIGS;
    }

    @Override
    protected CircuitComponentInstance createEmptyCopy() {
        return new ConstAnalog8ComponentInstance();
    }

    public int getLane(int lane) {
        return getInt(KEY_BIT_PREFIX + lane);
    }

    public void setLane(int lane, int value) {
        setInt(KEY_BIT_PREFIX + lane, Math.clamp(value, 0, 15));
    }

    /** 返回从低到高的 8 个通道强度。 */
    public int[] getLanes() {
        int[] lanes = new int[LANE_COUNT];
        for (int b = 0; b < LANE_COUNT; b++) {
            lanes[b] = getLane(b);
        }
        return lanes;
    }

    /** 写入从低到高的 8 个通道强度。 */
    public void setLanes(int[] lanes) {
        for (int b = 0; b < LANE_COUNT; b++) {
            setLane(b, lanes != null && b < lanes.length ? lanes[b] : 0);
        }
    }
}
