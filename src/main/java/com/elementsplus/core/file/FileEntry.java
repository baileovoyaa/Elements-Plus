package com.elementsplus.core.file;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 文件表格中的一行。网络传输时由 {@code FileSystemDataPayload} 负责序列化。
 *
 * @param path     在所属根节点内的路径；共享空间使用共享文件 UUID 的字符串
 * @param name     显示名
 * @param folder   是否为文件夹
 * @param size     文件字节数；文件夹为 -1（异步统计）
 * @param ownerId  共享文件拥有者；其他根节点为 {@code null}
 * @param ownerName 共享文件拥有者名字；其他根节点为空串
 * @param editable 当前玩家是否可写
 * @param shared   是否处于共享状态
 * @param editing  是否有人正在编辑（协作）
 */
public record FileEntry(String path, String name, boolean folder, int size,
                        @Nullable UUID ownerId, String ownerName,
                        boolean editable, boolean shared, boolean editing) {

    public FileEntry {
        ownerName = ownerName == null ? "" : ownerName;
    }

    public static FileEntry folder(String path, String name) {
        return new FileEntry(path, name, true, -1, null, "", true, false, false);
    }

    public static FileEntry file(String path, String name, int size, boolean editable) {
        return new FileEntry(path, name, false, size, null, "", editable, false, false);
    }

    public FileEntry withEditing(boolean editing) {
        return new FileEntry(path, name, folder, size, ownerId, ownerName, editable, shared, editing);
    }

    public FileEntry withSize(int size) {
        return new FileEntry(path, name, folder, size, ownerId, ownerName, editable, shared, editing);
    }

    public FileEntry withShared(boolean shared) {
        return new FileEntry(path, name, folder, size, ownerId, ownerName, editable, shared, editing);
    }

    public String extension() {
        return folder ? "" : FilePaths.extension(name);
    }
}
