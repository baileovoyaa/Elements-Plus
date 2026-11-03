package com.elementsplus.core.circuit.component;

import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;

/**
 * 256 字节 ROM 的实例数据。
 * <p>
 * 数据以 512 个十六进制字符保存在配置字符串中，这样可以直接沿用
 * {@link CircuitDiagram} 现有的配置序列化路径（SNBT、网络包、复制/粘贴）。
 */
public class RomComponentInstance extends AbstractComponentInstance {
    public static final String KEY_DATA = "data";
    private static final int BYTE_COUNT = 256;

    public static final StringConfig DATA = new StringConfig(
            KEY_DATA,
            Component.translatable("circuit.elements-plus.config.rom_data"),
            null,
            "00".repeat(BYTE_COUNT),
            BYTE_COUNT * 2
    );

    private static final List<Config> CONFIGS = List.of(DATA);

    public RomComponentInstance() {
        super();
    }

    public RomComponentInstance(RomComponentInstance source) {
        super(source);
    }

    @Override
    protected List<Config> defaultConfigs() {
        return CONFIGS;
    }

    @Override
    protected CircuitComponentInstance createEmptyCopy() {
        return new RomComponentInstance();
    }

    /** 读取整个 ROM 的副本。 */
    public byte[] getData() {
        byte[] result = new byte[BYTE_COUNT];
        String hex = getString(KEY_DATA);
        for (int i = 0; i < BYTE_COUNT; i++) {
            int pos = i * 2;
            if (pos + 2 <= hex.length()) {
                try {
                    result[i] = (byte) Integer.parseInt(hex.substring(pos, pos + 2), 16);
                } catch (NumberFormatException ignored) {
                    result[i] = 0;
                }
            }
        }
        return result;
    }

    public void setData(byte[] data) {
        StringBuilder sb = new StringBuilder(BYTE_COUNT * 2);
        for (int i = 0; i < BYTE_COUNT; i++) {
            int value = data != null && i < data.length ? data[i] & 0xFF : 0;
            sb.append(String.format(Locale.ROOT, "%02X", value));
        }
        setString(KEY_DATA, sb.toString());
    }

    public int getByte(int address) {
        String hex = getString(KEY_DATA);
        int pos = (address & 0xFF) * 2;
        if (pos + 2 > hex.length()) {
            return 0;
        }
        try {
            return Integer.parseInt(hex.substring(pos, pos + 2), 16) & 0xFF;
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    public void setByte(int address, int value) {
        byte[] data = getData();
        data[address & 0xFF] = (byte) value;
        setData(data);
    }
}
