package com.elementsplus.client.file;

import com.elementsplus.core.file.FileRoot;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 客户端临时文件剪贴板。关闭实验桌 / 粘贴完成后清空，与系统剪贴板无关。
 */
public final class FileClipboard {
    /** 一个被复制/剪切的条目。 */
    public record Entry(FileRoot root, String path, String name, boolean folder, boolean cut) {
    }

    private static final List<Entry> ENTRIES = new ArrayList<>();
    private static boolean cut;

    private FileClipboard() {
    }

    public static void copy(List<Entry> entries) {
        ENTRIES.clear();
        cut = false;
        for (Entry entry : entries) {
            ENTRIES.add(new Entry(entry.root(), entry.path(), entry.name(), entry.folder(), false));
        }
    }

    public static void cut(List<Entry> entries) {
        ENTRIES.clear();
        cut = true;
        for (Entry entry : entries) {
            ENTRIES.add(new Entry(entry.root(), entry.path(), entry.name(), entry.folder(), true));
        }
    }

    public static void clear() {
        ENTRIES.clear();
        cut = false;
    }

    public static boolean isEmpty() {
        return ENTRIES.isEmpty();
    }

    public static boolean isCut() {
        return cut;
    }

    public static List<Entry> entries() {
        return new ArrayList<>(ENTRIES);
    }

    /** 从剪贴板移除一个条目（粘贴成功后逐个消耗）。 */
    public static void remove(Entry entry) {
        ENTRIES.remove(entry);
        if (ENTRIES.isEmpty()) {
            cut = false;
        }
    }

    public static @Nullable Entry first() {
        return ENTRIES.isEmpty() ? null : ENTRIES.get(0);
    }

    public static int size() {
        return ENTRIES.size();
    }
}
