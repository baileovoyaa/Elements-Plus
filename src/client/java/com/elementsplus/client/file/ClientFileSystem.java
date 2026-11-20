package com.elementsplus.client.file;

import com.elementsplus.core.file.FileEntry;
import com.elementsplus.core.file.FilePaths;
import com.elementsplus.core.file.MemoryFileSystem;
import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户端本地文件系统，直接映射 {@code config/elements-plus/files}。
 */
public final class ClientFileSystem {
    private static final Map<String, CompletableFuture<Long>> FOLDER_SIZES = new ConcurrentHashMap<>();
    private static long sizeVersion;

    public static long sizeVersion() {
        return sizeVersion;
    }

    private ClientFileSystem() {
    }

    public static Path root() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("elements-plus").resolve("files");
        try {
            Files.createDirectories(path);
        } catch (IOException ignored) {
            // 目录创建失败时后续操作会各自失败并显示错误。
        }
        return path;
    }

    private static @Nullable Path resolve(String relative) {
        String normalized = FilePaths.normalizeDir(relative);
        if (normalized == null) {
            return null;
        }
        Path base = root().normalize();
        Path target = normalized.isEmpty() ? base : base.resolve(normalized).normalize();
        if (!target.startsWith(base)) {
            return null;
        }
        return target;
    }

    public static boolean exists(String path) {
        Path target = resolve(path);
        return target != null && Files.exists(target);
    }

    public static boolean isFolder(String path) {
        Path target = resolve(path);
        return target != null && Files.isDirectory(target);
    }

    public static List<FileEntry> list(String dir) {
        Path target = resolve(dir);
        List<FileEntry> result = new ArrayList<>();
        if (target == null || !Files.isDirectory(target)) {
            return result;
        }
        String normalizedDir = FilePaths.normalizeDir(dir);
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(target)) {
            for (Path child : stream) {
                String name = child.getFileName().toString();
                String valid = FilePaths.validateName(name);
                if (valid == null) {
                    continue;
                }
                String path = FilePaths.join(normalizedDir, valid);
                if (Files.isDirectory(child)) {
                    result.add(FileEntry.folder(path, valid));
                } else {
                    long size = 0;
                    try {
                        size = Files.size(child);
                    } catch (IOException ignored) {
                    }
                    result.add(FileEntry.file(path, valid, (int) Math.min(Integer.MAX_VALUE, size), true));
                }
            }
        } catch (IOException ignored) {
            return result;
        }
        result.sort(Comparator.comparing((FileEntry entry) -> entry.folder()).reversed()
                .thenComparing(entry -> entry.name().toLowerCase(Locale.ROOT)));
        return result;
    }

    public static byte[] read(String path) {
        Path target = resolve(path);
        if (target == null || !Files.isRegularFile(target)) {
            return null;
        }
        try {
            return Files.readAllBytes(target);
        } catch (IOException e) {
            return null;
        }
    }

    private static void invalidateFolderSizes(String path) {
        sizeVersion++;
        String current = FilePaths.normalizeDir(path);
        while (true) {
            FOLDER_SIZES.remove(current);
            if (current.isEmpty()) {
                break;
            }
            current = FilePaths.parent(current);
        }
    }

    public static MemoryFileSystem.WriteResult write(String path, byte[] data, boolean overwrite) {
        Path target = resolve(path);
        if (target == null || target.equals(root())) {
            return MemoryFileSystem.WriteResult.invalidPath();
        }
        if (Files.exists(target)) {
            if (Files.isDirectory(target)) {
                return MemoryFileSystem.WriteResult.pathOccupied();
            }
            if (!overwrite) {
                return MemoryFileSystem.WriteResult.alreadyExists();
            }
        }
        try {
            Path parent = target.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.write(target, data == null ? new byte[0] : data);
            invalidateFolderSizes(path);
            return MemoryFileSystem.WriteResult.ok();
        } catch (IOException e) {
            return MemoryFileSystem.WriteResult.invalidPath();
        }
    }

    public static MemoryFileSystem.WriteResult createFolder(String path) {
        Path target = resolve(path);
        if (target == null || target.equals(root())) {
            return MemoryFileSystem.WriteResult.invalidPath();
        }
        if (Files.exists(target)) {
            return MemoryFileSystem.WriteResult.alreadyExists();
        }
        try {
            Path parent = target.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.createDirectory(target);
            invalidateFolderSizes(path);
            return MemoryFileSystem.WriteResult.ok();
        } catch (IOException e) {
            return MemoryFileSystem.WriteResult.invalidPath();
        }
    }

    public static boolean delete(String path) {
        Path target = resolve(path);
        if (target == null || target.equals(root()) || !Files.exists(target)) {
            return false;
        }
        try {
            if (Files.isDirectory(target)) {
                try (var stream = Files.walk(target)) {
                    for (Path child : stream.sorted(Comparator.reverseOrder()).toList()) {
                        Files.deleteIfExists(child);
                    }
                }
            } else {
                Files.deleteIfExists(target);
            }
            String normalized = FilePaths.normalizeDir(path);
            FOLDER_SIZES.keySet().removeIf(key -> key.equals(normalized)
                    || FilePaths.isUnder(key, normalized));
            invalidateFolderSizes(normalized);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static boolean rename(String path, String newName) {
        String valid = FilePaths.validateName(newName);
        if (valid == null) {
            return false;
        }
        Path source = resolve(path);
        if (source == null || !Files.exists(source)) {
            return false;
        }
        Path target = source.getParent() == null ? null : source.getParent().resolve(valid);
        if (target == null || Files.exists(target)) {
            return false;
        }
        try {
            Files.move(source, target);
            invalidateFolderSizes(path);
            invalidateFolderSizes(FilePaths.join(FilePaths.parent(path), valid));
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static boolean transfer(String sourcePath, String targetDir, boolean copy, boolean overwrite) {
        Path source = resolve(sourcePath);
        Path dir = resolve(targetDir);
        if (source == null || dir == null || !Files.exists(source) || !Files.isDirectory(dir)) {
            return false;
        }
        Path normalizedSource = source.toAbsolutePath().normalize();
        Path normalizedDir = dir.toAbsolutePath().normalize();
        if (Files.isDirectory(source)
                && (normalizedDir.equals(normalizedSource)
                || normalizedDir.startsWith(normalizedSource))) {
            // 不允许把文件夹移动/复制到自身或子目录，否则递归复制会卡死。
            return false;
        }
        Path target = dir.resolve(source.getFileName().toString());
        String targetPath = FilePaths.join(FilePaths.normalizeDir(targetDir),
                source.getFileName().toString());
        if (targetPath.equals(FilePaths.normalize(sourcePath))) {
            // 在同一目录原地粘贴/覆盖：源和目标相同，直接视为成功，避免先删除源。
            return true;
        }
        try {
            if (Files.exists(target)) {
                if (!overwrite) {
                    return false;
                }
                delete(targetPath);
            }
            if (copy) {
                copyRecursive(source, target);
            } else {
                Files.move(source, target);
            }
            invalidateFolderSizes(sourcePath);
            invalidateFolderSizes(targetPath);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /** 复制/移动到明确的完整目标路径，用于粘贴重命名。 */
    public static boolean transferTo(String sourcePath, String targetPath,
                                     boolean copy, boolean overwrite) {
        String from = FilePaths.normalize(sourcePath);
        String to = FilePaths.normalize(targetPath);
        if (from == null || from.isEmpty() || to == null || to.isEmpty()) {
            return false;
        }
        if (from.equals(to)) {
            return true;
        }
        Path source = resolve(from);
        Path target = resolve(to);
        if (source == null || target == null || !Files.exists(source)) {
            return false;
        }
        Path normalizedSource = source.toAbsolutePath().normalize();
        Path normalizedTarget = target.toAbsolutePath().normalize();
        if (Files.isDirectory(source) && normalizedTarget.startsWith(normalizedSource)) {
            return false;
        }
        try {
            if (Files.exists(target)) {
                if (!overwrite) {
                    return false;
                }
                delete(to);
            }
            Path parent = target.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            if (copy) {
                copyRecursive(source, target);
            } else {
                Files.move(source, target);
            }
            invalidateFolderSizes(from);
            invalidateFolderSizes(to);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static void copyRecursive(Path source, Path target) throws IOException {
        if (Files.isDirectory(source)) {
            Files.createDirectories(target);
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(source)) {
                for (Path child : stream) {
                    copyRecursive(child, target.resolve(child.getFileName().toString()));
                }
            }
        } else {
            Path parent = target.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.copy(source, target, StandardCopyOption.COPY_ATTRIBUTES);
        }
    }

    /** 异步统计文件夹大小；完成后缓存。 */
    public static CompletableFuture<Long> folderSize(String path) {
        String key = FilePaths.normalizeDir(path);
        return FOLDER_SIZES.computeIfAbsent(key, ignored -> CompletableFuture.supplyAsync(() -> {
            Path target = resolve(key);
            if (target == null || !Files.isDirectory(target)) {
                return 0L;
            }
            try (var stream = Files.walk(target)) {
                long total = 0;
                for (Path child : (Iterable<Path>) stream::iterator) {
                    if (Files.isRegularFile(child)) {
                        try {
                            total += Files.size(child);
                        } catch (IOException sizeError) {
                            // 文件可能在这期间被删除，忽略。
                        }
                    }
                }
                return total;
            } catch (IOException e) {
                return 0L;
            }
        }));
    }

    public static void invalidateFolderSize(String path) {
        String key = FilePaths.normalizeDir(path);
        for (String cached : new ArrayList<>(FOLDER_SIZES.keySet())) {
            if (cached.equals(key) || FilePaths.isUnder(cached, key) || FilePaths.isUnder(key, cached)) {
                FOLDER_SIZES.remove(cached);
            }
        }
    }
}
