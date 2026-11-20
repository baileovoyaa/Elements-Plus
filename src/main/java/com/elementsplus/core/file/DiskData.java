package com.elementsplus.core.file;

import com.elementsplus.ModDataComponents;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * 磁盘物品的数据访问。磁盘固定 256 字节容量，实际内容长度可以更短；
 * 未写入时为 0 字节。
 */
public final class DiskData {
    public static final int CAPACITY = 256;

    private DiskData() {
    }

    public static byte[] read(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return new byte[0];
        }
        byte[] data = stack.get(ModDataComponents.DISK_DATA);
        return data == null ? new byte[0] : data.clone();
    }

    /** 写入磁盘；超过 256 字节时失败并返回 {@code false}。 */
    public static boolean write(ItemStack stack, @Nullable byte[] data) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        byte[] content = data == null ? new byte[0] : data;
        if (content.length > CAPACITY) {
            return false;
        }
        stack.set(ModDataComponents.DISK_DATA, content.clone());
        return true;
    }

    public static boolean canWrite(byte[] data) {
        return data == null || data.length <= CAPACITY;
    }
}
