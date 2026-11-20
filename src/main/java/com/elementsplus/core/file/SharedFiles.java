package com.elementsplus.core.file;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 实验桌共享空间。共享文件只在实验桌打开期间存在，不写入存档；文件内容是
 * 文件拥有者客户端文件的副本，修改后同步回拥有者的客户端。
 */
public final class SharedFiles {

    /** 单个共享文件。 */
    public static final class SharedFile {
        private final UUID id;
        private final UUID owner;
        private String ownerName;
        private final String ownerPath;
        private String name;
        private byte[] data;

        private SharedFile(UUID owner, String ownerName, String ownerPath, String name, byte[] data) {
            this.id = UUID.randomUUID();
            this.owner = owner;
            this.ownerName = ownerName == null ? "" : ownerName;
            this.ownerPath = ownerPath;
            this.name = name;
            this.data = data == null ? new byte[0] : data;
        }

        public UUID id() {
            return id;
        }

        public UUID owner() {
            return owner;
        }

        public String ownerName() {
            return ownerName;
        }

        public void setOwnerName(String ownerName) {
            this.ownerName = ownerName == null ? "" : ownerName;
        }

        public String ownerPath() {
            return ownerPath;
        }

        public String name() {
            return name;
        }

        public byte[] data() {
            return data.clone();
        }

        public int size() {
            return data.length;
        }

        void setData(byte[] data) {
            this.data = data == null ? new byte[0] : data;
        }
    }

    /** 正在编辑某个文件的玩家。 */
    public record Editor(UUID player, String name, int start, int end) {
    }

    public enum ShareResult {
        OK,
        ALREADY_SHARED,
        TOO_LARGE,
        TOO_MANY,
        INVALID_PATH
    }

    public enum UpdateResult {
        OK,
        NOT_FOUND,
        TOO_LARGE
    }

    private final Map<UUID, SharedFile> files = new LinkedHashMap<>();
    /** fileId -> (playerId -> editor) */
    private final Map<UUID, Map<UUID, Editor>> editors = new LinkedHashMap<>();

    public List<SharedFile> all() {
        return new ArrayList<>(files.values());
    }

    public @Nullable SharedFile byId(UUID id) {
        return id == null ? null : files.get(id);
    }

    public @Nullable SharedFile byOwnerPath(UUID owner, String path) {
        if (owner == null || path == null) {
            return null;
        }
        for (SharedFile file : files.values()) {
            if (file.owner.equals(owner) && file.ownerPath.equals(path)) {
                return file;
            }
        }
        return null;
    }

    public int countFor(UUID owner) {
        int count = 0;
        for (SharedFile file : files.values()) {
            if (file.owner.equals(owner)) {
                count++;
            }
        }
        return count;
    }

    public ShareResult share(UUID owner, String ownerName, String ownerPath, String name,
                             byte[] data, int maxFileSize, int maxPerPlayer) {
        String path = FilePaths.normalize(ownerPath);
        if (owner == null || path == null || path.isEmpty() || FilePaths.validateName(name) == null) {
            return ShareResult.INVALID_PATH;
        }
        if (byOwnerPath(owner, path) != null) {
            return ShareResult.ALREADY_SHARED;
        }
        if (data != null && data.length > maxFileSize) {
            return ShareResult.TOO_LARGE;
        }
        if (countFor(owner) >= maxPerPlayer) {
            return ShareResult.TOO_MANY;
        }
        SharedFile file = new SharedFile(owner, ownerName, path, name, data);
        files.put(file.id, file);
        return ShareResult.OK;
    }

    /** 拥有者主动关闭共享。 */
    public boolean unshare(UUID fileId, UUID requester) {
        SharedFile file = files.get(fileId);
        if (file == null || !file.owner.equals(requester)) {
            return false;
        }
        remove(fileId);
        return true;
    }

    /** 断开一个玩家共享的所有文件。 */
    public List<UUID> removeOwner(UUID owner) {
        List<UUID> removed = new ArrayList<>();
        for (SharedFile file : new ArrayList<>(files.values())) {
            if (file.owner.equals(owner)) {
                removed.add(file.id);
            }
        }
        for (UUID id : removed) {
            remove(id);
        }
        return removed;
    }

    private void remove(UUID fileId) {
        files.remove(fileId);
        editors.remove(fileId);
    }

    public void clear() {
        files.clear();
        editors.clear();
    }

    /* ============================================================
     *  协作编辑
     * ============================================================ */

    public void join(UUID fileId, UUID player, String name) {
        if (!files.containsKey(fileId) || player == null) {
            return;
        }
        editors.computeIfAbsent(fileId, key -> new LinkedHashMap<>())
                .putIfAbsent(player, new Editor(player, name == null ? "" : name, 0, 0));
    }

    public void leave(UUID fileId, UUID player) {
        Map<UUID, Editor> map = editors.get(fileId);
        if (map != null) {
            map.remove(player);
            if (map.isEmpty()) {
                editors.remove(fileId);
            }
        }
    }

    /** 玩家关闭界面/退出时清理其所有编辑会话。 */
    public List<UUID> leavePlayer(UUID player) {
        List<UUID> left = new ArrayList<>();
        List<UUID> empty = new ArrayList<>();
        for (Map.Entry<UUID, Map<UUID, Editor>> entry : editors.entrySet()) {
            if (entry.getValue().remove(player) != null) {
                left.add(entry.getKey());
            }
            if (entry.getValue().isEmpty()) {
                empty.add(entry.getKey());
            }
        }
        for (UUID fileId : empty) {
            editors.remove(fileId);
        }
        return left;
    }

    public void updateCursor(UUID fileId, UUID player, int start, int end) {
        Map<UUID, Editor> map = editors.get(fileId);
        if (map == null) {
            return;
        }
        Editor editor = map.get(player);
        if (editor != null) {
            map.put(player, new Editor(player, editor.name(), start, end));
        }
    }

    public List<Editor> editors(UUID fileId) {
        Map<UUID, Editor> map = editors.get(fileId);
        return map == null ? List.of() : new ArrayList<>(map.values());
    }

    public boolean isEditing(UUID fileId) {
        Map<UUID, Editor> map = editors.get(fileId);
        return map != null && !map.isEmpty();
    }

    /** 正在被 {@code self} 以外的玩家编辑。 */
    public boolean isEditingByOthers(UUID fileId, UUID self) {
        Map<UUID, Editor> map = editors.get(fileId);
        if (map == null) {
            return false;
        }
        return map.keySet().stream().anyMatch(player -> !player.equals(self));
    }

    public UpdateResult updateData(UUID fileId, byte[] data, int maxSize, UUID editor) {
        SharedFile file = files.get(fileId);
        if (file == null) {
            return UpdateResult.NOT_FOUND;
        }
        if (data != null && data.length > maxSize) {
            return UpdateResult.TOO_LARGE;
        }
        file.setData(data);
        return UpdateResult.OK;
    }

    /** 正在编辑某个文件的所有玩家 id（用于断开后清理）。 */
    public Set<UUID> editorIds(UUID fileId) {
        Map<UUID, Editor> map = editors.get(fileId);
        return map == null ? Set.of() : new LinkedHashSet<>(map.keySet());
    }

    /** 玩家 -> 正在编辑的文件 id。 */
    public Map<UUID, Set<UUID>> editorsByPlayer() {
        Map<UUID, Set<UUID>> result = new HashMap<>();
        for (Map.Entry<UUID, Map<UUID, Editor>> entry : editors.entrySet()) {
            for (UUID player : entry.getValue().keySet()) {
                result.computeIfAbsent(player, key -> new LinkedHashSet<>()).add(entry.getKey());
            }
        }
        return result;
    }
}
