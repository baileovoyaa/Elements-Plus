package com.elementsplus.core.file;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 实验桌上的文件编辑会话。文件本身可以是实验桌内存、共享文件或外接设备，
 * 因此用规范化 key 把不同根节点下的同一个文件归并到一起。
 */
public final class FileSessions {

    /**
     * 一个玩家当前打开的编辑器。
     *
     * @param key 规范化文件标识，例如 {@code memory:a/b.txt}、{@code shared:<uuid>}
     */
    public record Session(UUID player, String playerName, FileRoot root, String path, String key,
                          boolean textMode, int start, int end) {
        public Session withCursor(int start, int end) {
            return new Session(player, playerName, root, path, key, textMode, start, end);
        }

        public Session withTextMode(boolean textMode) {
            return new Session(player, playerName, root, path, key, textMode, start, end);
        }
    }

    private final Map<UUID, Session> sessions = new LinkedHashMap<>();
    private final Map<String, Long> revisions = new HashMap<>();
    private long nextRevision = 1;

    public @Nullable Session get(UUID player) {
        return player == null ? null : sessions.get(player);
    }

    public void put(Session session) {
        sessions.put(session.player(), session);
    }

    public @Nullable Session remove(UUID player) {
        return player == null ? null : sessions.remove(player);
    }

    public List<Session> all() {
        return new ArrayList<>(sessions.values());
    }

    public List<Session> forKey(String key) {
        List<Session> result = new ArrayList<>();
        for (Session session : sessions.values()) {
            if (session.key().equals(key)) {
                result.add(session);
            }
        }
        return result;
    }

    public long revision(String key) {
        return revisions.getOrDefault(key, 0L);
    }

    public long bumpRevision(String key) {
        long revision = nextRevision++;
        revisions.put(key, revision);
        return revision;
    }

    public void reset(String key) {
        revisions.remove(key);
    }

    public void clear() {
        sessions.clear();
        revisions.clear();
        nextRevision = 1;
    }

    /**
     * 编辑器模式下所有玩家的光标信息，不含自己以外 root 的差异。
     */
    public List<FileEditorView> views(String key, UUID self) {
        List<FileEditorView> result = new ArrayList<>();
        for (Session session : forKey(key)) {
            if (session.player().equals(self)) {
                continue;
            }
            result.add(new FileEditorView(session.player(), session.playerName(),
                    session.start(), session.end(), session.textMode()));
        }
        return result;
    }

    public record FileEditorView(UUID player, String name, int start, int end, boolean textMode) {
    }
}
