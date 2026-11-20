package com.elementsplus.core.file;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 实验桌内存文件系统。
 *
 * <p>内存不显示文件头，但容量统计包含每个文件的 {@link FileHeader}；多个文件共用
 * 同一块容量。文件夹本身不占空间，但会持久化（允许空文件夹）。</p>
 */
public final class MemoryFileSystem {
    private int capacity;
    private final Map<String, byte[]> files = new LinkedHashMap<>();
    private final Set<String> folders = new LinkedHashSet<>();

    public MemoryFileSystem(int capacity) {
        this.capacity = Math.max(0, Math.min(FileHeader.MAX_DATA_LENGTH, capacity));
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = Math.max(0, Math.min(FileHeader.MAX_DATA_LENGTH, capacity));
    }

    public Map<String, byte[]> getFiles() {
        return files;
    }

    public Set<String> getFolders() {
        return folders;
    }

    public void clear() {
        files.clear();
        folders.clear();
    }

    /** 已占用字节数（文件内容 + 文件头）。 */
    public int usedBytes() {
        int used = 0;
        for (Map.Entry<String, byte[]> entry : files.entrySet()) {
            used += FileHeader.sizeFor(FilePaths.name(entry.getKey())) + entry.getValue().length;
        }
        return used;
    }

    public int freeBytes() {
        return Math.max(0, capacity - usedBytes());
    }

    public boolean isEmpty() {
        return files.isEmpty() && folders.isEmpty();
    }

    public boolean exists(String path) {
        String normalized = FilePaths.normalize(path);
        if (normalized == null) {
            return false;
        }
        if (normalized.isEmpty()) {
            return true;
        }
        return files.containsKey(normalized) || folders.contains(normalized);
    }

    public boolean isFolder(String path) {
        String normalized = FilePaths.normalize(path);
        return normalized != null && (normalized.isEmpty() || folders.contains(normalized));
    }

    public boolean isFile(String path) {
        String normalized = FilePaths.normalize(path);
        return normalized != null && files.containsKey(normalized);
    }

    @Nullable
    public byte[] read(String path) {
        String normalized = FilePaths.normalize(path);
        if (normalized == null) {
            return null;
        }
        byte[] data = files.get(normalized);
        return data == null ? null : data.clone();
    }

    public int fileSize(String path) {
        String normalized = FilePaths.normalize(path);
        byte[] data = normalized == null ? null : files.get(normalized);
        return data == null ? -1 : data.length;
    }

    /** 文件夹递归总大小（内容 + 文件头）。 */
    public int folderSize(String path) {
        String normalized = FilePaths.normalizeDir(path);
        int total = 0;
        for (Map.Entry<String, byte[]> entry : files.entrySet()) {
            if (FilePaths.isDirectChild(entry.getKey(), normalized) || FilePaths.isUnder(entry.getKey(), normalized)) {
                total += FileHeader.sizeFor(FilePaths.name(entry.getKey())) + entry.getValue().length;
            }
        }
        return total;
    }

    /**
     * 列出目录下的直接子项：文件夹在前，再按名称排序。
     */
    public List<FileEntry> list(String dir) {
        String normalized = FilePaths.normalizeDir(dir);
        List<FileEntry> result = new ArrayList<>();
        for (String folder : folders) {
            if (folder.isEmpty() || !FilePaths.isDirectChild(folder, normalized)) {
                continue;
            }
            result.add(FileEntry.folder(folder, FilePaths.name(folder)));
        }
        for (Map.Entry<String, byte[]> entry : files.entrySet()) {
            if (!FilePaths.isDirectChild(entry.getKey(), normalized)) {
                continue;
            }
            result.add(FileEntry.file(entry.getKey(), FilePaths.name(entry.getKey()),
                    entry.getValue().length, true));
        }
        result.sort(Comparator.comparing((FileEntry entry) -> entry.folder()).reversed()
                .thenComparing(entry -> entry.name().toLowerCase(java.util.Locale.ROOT)));
        return result;
    }

    /** 所有文件路径（用于同步/保存）。 */
    public List<String> filePaths() {
        return new ArrayList<>(files.keySet());
    }

    /** 写入文件，必要时自动创建父文件夹。容量不足或路径非法时返回失败原因。 */
    public WriteResult write(String path, byte[] data) {
        String normalized = FilePaths.normalize(path);
        if (normalized == null || normalized.isEmpty()) {
            return WriteResult.invalidPath();
        }
        if (folders.contains(normalized)) {
            return WriteResult.pathOccupied();
        }
        byte[] content = data == null ? new byte[0] : data;
        int old = files.containsKey(normalized) ? files.get(normalized).length : -1;
        int nextUsed = usedBytes() - (old < 0 ? 0 : old + FileHeader.sizeFor(FilePaths.name(normalized)))
                + content.length + FileHeader.sizeFor(FilePaths.name(normalized));
        if (nextUsed > capacity) {
            return WriteResult.noSpace();
        }
        String parent = FilePaths.parent(normalized);
        if (!parent.isEmpty() && !ensureFolders(parent)) {
            return WriteResult.invalidPath();
        }
        files.put(normalized, content);
        return WriteResult.ok();
    }

    /** 新建空文件；同名文件已存在时失败。 */
    public WriteResult createFile(String path) {
        String normalized = FilePaths.normalize(path);
        if (normalized == null || normalized.isEmpty()) {
            return WriteResult.invalidPath();
        }
        if (exists(normalized)) {
            return WriteResult.alreadyExists();
        }
        return write(normalized, new byte[0]);
    }

    public WriteResult createFolder(String path) {
        String normalized = FilePaths.normalize(path);
        if (normalized == null || normalized.isEmpty()) {
            return WriteResult.invalidPath();
        }
        if (files.containsKey(normalized) || folders.contains(normalized)) {
            return WriteResult.alreadyExists();
        }
        if (!ensureFolders(normalized)) {
            return WriteResult.invalidPath();
        }
        return WriteResult.ok();
    }

    private boolean ensureFolders(String path) {
        if (path == null) {
            return false;
        }
        StringBuilder current = new StringBuilder();
        for (String part : FilePaths.components(path)) {
            if (!current.isEmpty()) {
                current.append('/');
            }
            current.append(part);
            String folder = current.toString();
            if (files.containsKey(folder)) {
                return false;
            }
            folders.add(folder);
        }
        return true;
    }

    public boolean delete(String path) {
        String normalized = FilePaths.normalize(path);
        if (normalized == null || normalized.isEmpty()) {
            return false;
        }
        boolean removed = files.remove(normalized) != null;
        if (folders.remove(normalized)) {
            removed = true;
        }
        files.keySet().removeIf(file -> FilePaths.isUnder(file, normalized));
        folders.removeIf(folder -> FilePaths.isUnder(folder, normalized));
        return removed;
    }

    /** 重命名文件或文件夹；目标已存在时失败。 */
    public boolean rename(String path, String newName) {
        String normalized = FilePaths.normalize(path);
        String name = FilePaths.validateName(newName);
        if (normalized == null || normalized.isEmpty() || name == null) {
            return false;
        }
        String target = FilePaths.join(FilePaths.parent(normalized), name);
        if (target.equals(normalized)) {
            return true;
        }
        if (exists(target)) {
            return false;
        }
        if (files.containsKey(normalized)) {
            byte[] data = files.remove(normalized);
            files.put(target, data);
            return true;
        }
        if (folders.contains(normalized)) {
            remap(normalized, target, true);
            return true;
        }
        return false;
    }

    /**
     * 把文件/文件夹移动到目标目录。
     *
     * @param source   源路径
     * @param targetDir 目标目录
     * @param copy     {@code true} 复制，{@code false} 移动
     */
    public boolean transfer(String source, String targetDir, boolean copy) {
        String from = FilePaths.normalize(source);
        String dir = FilePaths.normalizeDir(targetDir);
        if (from == null || from.isEmpty() || dir.equals(from) || FilePaths.isUnder(dir, from)) {
            return false;
        }
        String to = FilePaths.join(dir, FilePaths.name(from));
        if (to.equals(from)) {
            return true;
        }
        if (exists(to)) {
            return false;
        }
        if (files.containsKey(from)) {
            byte[] data = files.get(from);
            WriteResult result = write(to, data);
            if (!result.success()) {
                return false;
            }
            if (!copy) {
                files.remove(from);
            }
            return true;
        }
        if (!folders.contains(from)) {
            return false;
        }
        if (copy) {
            for (Map.Entry<String, byte[]> entry : new ArrayList<>(files.entrySet())) {
                String path = entry.getKey();
                if (!FilePaths.isUnder(path, from)) {
                    continue;
                }
                String targetPath = to + path.substring(from.length());
                WriteResult result = write(targetPath, entry.getValue());
                if (!result.success()) {
                    delete(to);
                    return false;
                }
            }
            for (String folder : new ArrayList<>(folders)) {
                if (FilePaths.isUnder(folder, from)) {
                    folders.add(to + folder.substring(from.length()));
                }
            }
            return true;
        }
        remap(from, to, true);
        return true;
    }

    /** 复制/移动到明确的完整目标路径，用于粘贴重命名。 */
    public boolean transferTo(String sourcePath, String targetPath, boolean copy, boolean overwrite) {
        String from = FilePaths.normalize(sourcePath);
        String to = FilePaths.normalize(targetPath);
        if (from == null || from.isEmpty() || to == null || to.isEmpty() || from.equals(to)) {
            return from != null && from.equals(to);
        }
        if (FilePaths.isUnder(to, from)) {
            return false;
        }
        boolean targetExists = exists(to);
        if (targetExists && !overwrite) {
            return false;
        }
        if (targetExists) {
            delete(to);
        }
        if (files.containsKey(from)) {
            byte[] data = files.get(from);
            WriteResult result = write(to, data);
            if (!result.success()) {
                return false;
            }
            if (!copy) {
                files.remove(from);
            }
            return true;
        }
        if (!folders.contains(from)) {
            return false;
        }
        if (copy) {
            folders.add(to);
            for (Map.Entry<String, byte[]> entry : new ArrayList<>(files.entrySet())) {
                String path = entry.getKey();
                if (!FilePaths.isUnder(path, from)) {
                    continue;
                }
                String target = to + path.substring(from.length());
                WriteResult result = write(target, entry.getValue());
                if (!result.success()) {
                    delete(to);
                    return false;
                }
            }
            for (String folder : new ArrayList<>(folders)) {
                if (FilePaths.isUnder(folder, from)) {
                    folders.add(to + folder.substring(from.length()));
                }
            }
            return true;
        }
        remap(from, to, true);
        return true;
    }

    /** 把以 {@code from} 为前缀的所有路径替换为 {@code to} 前缀。 */
    private void remap(String from, String to, boolean removeOld) {
        Map<String, byte[]> movedFiles = new LinkedHashMap<>();
        for (Map.Entry<String, byte[]> entry : new ArrayList<>(files.entrySet())) {
            String path = entry.getKey();
            if (!FilePaths.isUnder(path, from)) {
                continue;
            }
            movedFiles.put(to + path.substring(from.length()), entry.getValue());
            if (removeOld) {
                files.remove(path);
            }
        }
        Set<String> movedFolders = new LinkedHashSet<>();
        for (String folder : new ArrayList<>(folders)) {
            if (!FilePaths.isUnder(folder, from)) {
                continue;
            }
            movedFolders.add(to + folder.substring(from.length()));
            if (removeOld) {
                folders.remove(folder);
            }
        }
        files.putAll(movedFiles);
        folders.addAll(movedFolders);
    }

    /** 复制/移动目标路径上是否已存在同名项。 */
    public boolean targetExists(String sourcePath, String targetDir) {
        return exists(FilePaths.join(FilePaths.normalizeDir(targetDir), FilePaths.name(sourcePath)));
    }

    /* ============================================================
     *  持久化 / 序列化
     * ============================================================ */

    public void save(CompoundTag tag) {
        tag.putInt("Capacity", capacity);
        ListTag fileList = new ListTag();
        for (Map.Entry<String, byte[]> entry : files.entrySet()) {
            CompoundTag fileTag = new CompoundTag();
            fileTag.putString("Path", entry.getKey());
            fileTag.putByteArray("Data", entry.getValue());
            // 文件头也写入存档，方便以后扩展/校验；加载时以路径为准重建。
            byte[] header = FileHeader.encode(FilePaths.name(entry.getKey()), entry.getValue().length);
            if (header != null) {
                fileTag.putByteArray("Header", header);
            }
            fileList.add(fileTag);
        }
        tag.put("Files", fileList);
        ListTag folderList = new ListTag();
        for (String folder : folders) {
            folderList.add(StringTag.valueOf(folder));
        }
        tag.put("Folders", folderList);
    }

    public void load(CompoundTag tag) {
        clear();
        ListTag folderList = tag.getList("Folders", Tag.TAG_STRING);
        for (int i = 0; i < folderList.size(); i++) {
            String folder = FilePaths.normalize(folderList.getString(i));
            if (folder != null && !folder.isEmpty()) {
                ensureFolders(folder);
            }
        }
        ListTag fileList = tag.getList("Files", Tag.TAG_COMPOUND);
        for (int i = 0; i < fileList.size(); i++) {
            CompoundTag fileTag = fileList.getCompound(i);
            String path = FilePaths.normalize(fileTag.getString("Path"));
            if (path == null || path.isEmpty()) {
                continue;
            }
            byte[] data = fileTag.getByteArray("Data");
            String parent = FilePaths.parent(path);
            if (!parent.isEmpty()) {
                ensureFolders(parent);
            }
            files.put(path, data);
        }
    }

    /** 操作结果：成功或具体失败原因，便于界面显示。 */
    public record WriteResult(boolean success, Failure failure) {
        public enum Failure {
            NONE,
            INVALID_PATH,
            ALREADY_EXISTS,
            PATH_OCCUPIED,
            NO_SPACE
        }

        public static WriteResult ok() {
            return new WriteResult(true, Failure.NONE);
        }

        public static WriteResult invalidPath() {
            return new WriteResult(false, Failure.INVALID_PATH);
        }

        public static WriteResult alreadyExists() {
            return new WriteResult(false, Failure.ALREADY_EXISTS);
        }

        public static WriteResult pathOccupied() {
            return new WriteResult(false, Failure.PATH_OCCUPIED);
        }

        public static WriteResult noSpace() {
            return new WriteResult(false, Failure.NO_SPACE);
        }

        public String failureKey() {
            return "gui.elements-plus.file.error." + failure.name().toLowerCase(java.util.Locale.ROOT);
        }
    }
}
