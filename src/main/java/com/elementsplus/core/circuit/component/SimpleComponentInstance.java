package com.elementsplus.core.circuit.component;

import java.util.List;

public class SimpleComponentInstance implements CircuitComponentInstance {

    public static final SimpleComponentInstance INSTANCE = new SimpleComponentInstance();

    private static final List<Config> NONE = List.of();

    @Override
    public List<Config> getConfigs() {
        return NONE;
    }

    @Override
    public int getInt(String key) {
        return 0;
    }

    @Override
    public float getFloat(String key) {
        return 0f;
    }

    @Override
    public void setInt(String key, int value) {
    }

    @Override
    public void setFloat(String key, float value) {
    }

    @Override
    public boolean hasConfig(String key) {
        return false;
    }

    @Override
    public CircuitComponentInstance copy() {
        return this; // 无状态单例，可安全共享
    }
}