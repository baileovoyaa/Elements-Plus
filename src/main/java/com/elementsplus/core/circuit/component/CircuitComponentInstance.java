package com.elementsplus.core.circuit.component;

import net.minecraft.network.chat.Component;

import java.util.List;

public interface CircuitComponentInstance {

    /**
     * 该实例支持的配置描述（元数据，不包含当前值）。
     * 每个实例必须返回稳定（复用）的列表。
     */
    List<Config> getConfigs();

    int getInt(String key);

    float getFloat(String key);

    void setInt(String key, int value);

    void setFloat(String key, float value);

    boolean hasConfig(String key);

    /** 当前值（Integer / Float） */
    default Object getValue(String key) {
        for (Config config : getConfigs()) {
            if (config.key.equals(key)) {
                return config instanceof IntConfig ? getInt(key) : getFloat(key);
            }
        }
        return null;
    }

    /** 深拷贝（无状态实例可返回自身） */
    CircuitComponentInstance copy();

    /** 配置描述：只有元数据，序列化时只保存值 */
    class Config {
        public final String key;
        public final Component name;
        public final Component description;

        public Config(String key, Component name, Component description) {
            this.key = key;
            this.name = name;
            this.description = description != null ? description : Component.empty();
        }
    }

    class IntConfig extends Config {
        public final int min;
        public final int max;
        public final int step;
        public final int defaultValue;
        public final boolean showSlider;

        public IntConfig(String key, Component name, Component description, int min, int max, int step, int defaultValue, boolean showSlider) {
            super(key, name, description);
            this.min = min;
            this.max = max;
            this.step = Math.max(1, step);
            this.defaultValue = defaultValue;
            this.showSlider = showSlider;
        }
    }

    class FloatConfig extends Config {
        public final float min;
        public final float max;
        public final float defaultValue;
        public final boolean showSlider;

        public FloatConfig(String key, Component name, Component description, float min, float max, float defaultValue, boolean showSlider) {
            super(key, name, description);
            this.min = min;
            this.max = max;
            this.defaultValue = defaultValue;
            this.showSlider = showSlider;
        }
    }
}