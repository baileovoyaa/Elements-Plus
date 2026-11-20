package com.elementsplus.core.file;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 文件系统路径工具。路径统一使用 {@code /} 分隔，{@code ""} 表示根目录。
 */
public final class FilePaths {
    public static final int MAX_NAME_LENGTH = 48;

    private FilePaths() {
    }

    /**
     * 校验并规范化单个文件/文件夹名；不合法时返回 {@code null}。
     */
    public static @Nullable String validateName(@Nullable String raw) {
        if (raw == null) {
            return null;
        }
        String name = raw.trim();
        // Windows 下末尾的点和空格会被系统吞掉，统一拒绝。
        while (!name.isEmpty() && (name.endsWith(".") || name.endsWith(" "))) {
            name = name.substring(0, name.length() - 1);
        }
        if (name.isEmpty() || name.length() > MAX_NAME_LENGTH) {
            return null;
        }
        if (name.equals(".") || name.equals("..")) {
            return null;
        }
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c < 0x20 || c == 0x7F) {
                return null;
            }
            switch (c) {
                case '/', '\\', ':', '*', '?', '"', '<', '>', '|' -> {
                    return null;
                }
                default -> {
                }
            }
        }
        return name;
    }

    /**
     * 规范化相对路径：去掉首尾和重复的 {@code /}；任何一段不合法时返回 {@code null}。
     */
    public static @Nullable String normalize(@Nullable String raw) {
        if (raw == null) {
            return null;
        }
        String path = raw.replace('\\', '/').trim();
        while (path.startsWith("/")) {
            path = path.substring(1);
        }
        while (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        if (path.isEmpty()) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        for (String part : path.split("/")) {
            if (part.isEmpty()) {
                continue;
            }
            String valid = validateName(part);
            if (valid == null) {
                return null;
            }
            if (!result.isEmpty()) {
                result.append('/');
            }
            result.append(valid);
        }
        return result.toString();
    }

    /** 规范化目录路径；不合法时回退到根目录。 */
    public static String normalizeDir(@Nullable String raw) {
        String path = normalize(raw);
        return path == null ? "" : path;
    }

    public static String join(String dir, String name) {
        if (dir == null || dir.isEmpty()) {
            return name;
        }
        return dir + "/" + name;
    }

    public static String parent(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        int index = path.lastIndexOf('/');
        return index < 0 ? "" : path.substring(0, index);
    }

    public static String name(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        int index = path.lastIndexOf('/');
        return index < 0 ? path : path.substring(index + 1);
    }

    /** 扩展名（不含点）；没有扩展名时返回空串。 */
    public static String extension(String name) {
        if (name == null) {
            return "";
        }
        int index = name.lastIndexOf('.');
        if (index <= 0 || index == name.length() - 1) {
            return "";
        }
        return name.substring(index + 1);
    }

    /** 文件名去掉最后一个扩展名。 */
    public static String baseName(String name) {
        if (name == null) {
            return "";
        }
        int index = name.lastIndexOf('.');
        return index <= 0 ? name : name.substring(0, index);
    }

    /** {@code path} 是否等于 {@code ancestor} 或位于其下。 */
    public static boolean isUnder(String path, String ancestor) {
        if (ancestor == null || ancestor.isEmpty()) {
            return true;
        }
        return path.equals(ancestor) || path.startsWith(ancestor + "/");
    }

    /** {@code path} 是否为 {@code dir} 的直接子项。 */
    public static boolean isDirectChild(String path, String dir) {
        return parent(path).equals(dir == null ? "" : dir);
    }

    public static List<String> components(String path) {
        List<String> result = new ArrayList<>();
        if (path == null || path.isEmpty()) {
            return result;
        }
        for (String part : path.split("/")) {
            if (!part.isEmpty()) {
                result.add(part);
            }
        }
        return result;
    }
}
