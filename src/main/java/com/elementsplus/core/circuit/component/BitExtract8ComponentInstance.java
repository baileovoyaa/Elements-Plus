package com.elementsplus.core.circuit.component;

import net.minecraft.network.chat.Component;

import java.util.List;

/** 8 位取位器：输出 8 位输入 A 中指定通道的信号强度。 */
public class BitExtract8ComponentInstance extends AbstractComponentInstance {

    public static final String KEY_BIT = "bit";
    public static final int DEFAULT_BIT = 3;
    public static final List<Config> CONFIGS = List.of(new IntConfig(
            KEY_BIT,
            Component.translatable("circuit.elements-plus.config.bit_index"),
            Component.translatable("circuit.elements-plus.config.bit_index.description"),
            0,
            7,
            1,
            DEFAULT_BIT,
            true
    ));

    public BitExtract8ComponentInstance() {
        super();
    }

    public BitExtract8ComponentInstance(BitExtract8ComponentInstance source) {
        super(source);
    }

    @Override
    protected List<Config> defaultConfigs() {
        return CONFIGS;
    }

    @Override
    protected CircuitComponentInstance createEmptyCopy() {
        return new BitExtract8ComponentInstance();
    }

    public int getBitIndex() {
        return Math.clamp(getInt(KEY_BIT), 0, 7);
    }

    public void setBitIndex(int bit) {
        setInt(KEY_BIT, Math.clamp(bit, 0, 7));
    }
}
