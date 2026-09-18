package com.elementsplus.core.circuit.component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractComponentInstance implements CircuitComponentInstance {

    protected final Map<String, Object> values = new LinkedHashMap<>();
    private final List<Config> configs;

    protected AbstractComponentInstance() {
        this.configs = List.copyOf(defaultConfigs());
        for (Config config : configs) {
            if (config instanceof IntConfig ic) {
                values.put(ic.key, ic.defaultValue);
            } else if (config instanceof FloatConfig fc) {
                values.put(fc.key, fc.defaultValue);
            } else if (config instanceof StringConfig sc) {
                values.put(sc.key, sc.defaultValue);
            }
        }
    }

    protected AbstractComponentInstance(AbstractComponentInstance source) {
        this.configs = source.configs;
        this.values.putAll(source.values);
    }

    /** 子类返回静态的 Config 描述列表（仅在构造时读取一次） */
    protected abstract List<Config> defaultConfigs();

    @Override
    public List<Config> getConfigs() {
        return configs;
    }

    @Override
    public int getInt(String key) {
        Object v = values.get(key);
        return v instanceof Number n ? n.intValue() : 0;
    }

    @Override
    public float getFloat(String key) {
        Object v = values.get(key);
        return v instanceof Number n ? n.floatValue() : 0f;
    }

    @Override
    public String getString(String key) {
        Object v = values.get(key);
        return v instanceof String s ? s : "";
    }

    @Override
    public void setInt(String key, int value) {
        values.put(key, value);
    }

    @Override
    public void setFloat(String key, float value) {
        values.put(key, value);
    }

    @Override
    public void setString(String key, String value) {
        values.put(key, value);
    }

    @Override
    public boolean hasConfig(String key) {
        for (Config config : configs) {
            if (config.key.equals(key)) return true;
        }
        return false;
    }

    @Override
    public CircuitComponentInstance copy() {
        AbstractComponentInstance copy = (AbstractComponentInstance) createEmptyCopy();
        copy.values.putAll(this.values);
        return copy;
    }

    /** 创建一个仅填充了默认值的同类型实例 */
    protected abstract CircuitComponentInstance createEmptyCopy();
}