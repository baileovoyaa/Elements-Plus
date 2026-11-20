package com.elementsplus.core.circuit.component;

import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * 磁盘元件实例：256 字节内容作为客户端临时状态保存，不写入电路图序列化。
 * 复位不清空内容；拿掉电路图后随元件一起丢弃。
 *
 * <p>“标签”是电路图里可编辑的属性，会随电路图保存，并显示在红石接口的磁盘槽位旁。</p>
 */
public class DiskComponentInstance extends AbstractComponentInstance {
    public static final int CAPACITY = 256;
    public static final String KEY_LABEL = "label";

    public static final StringConfig LABEL = new StringConfig(
            KEY_LABEL,
            Component.translatable("circuit.elements-plus.config.label"),
            null,
            "",
            64
    );

    private static final List<Config> CONFIGS = List.of(LABEL);

    private byte[] data = new byte[CAPACITY];

    public DiskComponentInstance() {
        super();
    }

    public DiskComponentInstance(DiskComponentInstance source) {
        super(source);
        this.data = source.data.clone();
    }

    @Override
    protected List<Config> defaultConfigs() {
        return CONFIGS;
    }

    @Override
    protected CircuitComponentInstance createEmptyCopy() {
        return new DiskComponentInstance();
    }

    @Override
    public CircuitComponentInstance copy() {
        return new DiskComponentInstance(this);
    }

    /** 磁盘标签；未设置时返回空串，由界面回退为“磁盘 N”。 */
    public String getLabel() {
        String label = getString(KEY_LABEL);
        if (label == null) {
            return "";
        }
        // 标签为单行文本，避免存档中被写入换行导致界面异常。
        return label.replace('\n', ' ').replace('\r', ' ').trim();
    }

    public void setLabel(String label) {
        setString(KEY_LABEL, label == null ? "" : label);
    }

    public byte[] getData() {
        return data.clone();
    }

    public void setData(byte[] next) {
        if (next == null) {
            this.data = new byte[CAPACITY];
            return;
        }
        this.data = new byte[CAPACITY];
        System.arraycopy(next, 0, this.data, 0, Math.min(CAPACITY, next.length));
    }

    public int getByte(int address) {
        return data[address & (CAPACITY - 1)] & 0xFF;
    }

    public void setByte(int address, int value) {
        data[address & (CAPACITY - 1)] = (byte) value;
    }
}
