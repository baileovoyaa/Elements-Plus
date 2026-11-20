package com.elementsplus.core.file;

import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

/**
 * 实验桌编程页的四个文件系统根节点。
 *
 * <p>顺序与界面左侧导航一致，网络传输只写序号。</p>
 */
public enum FileRoot implements StringRepresentable {
    /** 外接设备槽位中的物品。 */
    EXTERNAL_DEVICE("external_device"),
    /** 实验桌内存，与方块实体一起持久化。 */
    TABLE_MEMORY("table_memory"),
    /** 其他玩家共享到当前实验桌的文件。 */
    SHARED("shared"),
    /** 客户端本地文件，映射 {@code config/elements-plus/files}。 */
    CLIENT("client");

    private final String name;

    FileRoot(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public String getTranslationKey() {
        return "gui.elements-plus.file.root." + name;
    }

    public Component getDisplayName() {
        return Component.translatable(getTranslationKey());
    }

    /** 该根节点是否允许新建文件/文件夹。 */
    public boolean canCreate() {
        return this == TABLE_MEMORY || this == CLIENT;
    }

    /** 该根节点是否由服务端维护（客户端文件直接读写本地目录）。 */
    public boolean isServerSide() {
        return this != CLIENT;
    }

    /** 共享空间不做目录树；外接设备文件夹现在会展开成只读目录树。 */
    public boolean isFlat() {
        return this == SHARED;
    }

    public static FileRoot byOrdinal(int ordinal) {
        FileRoot[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : CLIENT;
    }
}
