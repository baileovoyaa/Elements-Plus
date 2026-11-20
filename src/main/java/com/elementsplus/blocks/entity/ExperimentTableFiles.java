package com.elementsplus.blocks.entity;

import com.elementsplus.ModDataComponents;
import com.elementsplus.ModItems;
import com.elementsplus.core.circuit.CircuitComponent;
import com.elementsplus.core.circuit.CircuitComponentRegistry;
import com.elementsplus.core.circuit.CircuitItemData;
import com.elementsplus.core.circuit.diagram.CircuitDiagram;
import com.elementsplus.core.circuit.storage.CircuitStorage;
import com.elementsplus.core.circuit.storage.StoredCircuit;
import com.elementsplus.core.config.ElementsPlusServerConfig;
import com.elementsplus.core.file.CircuitDiagramFile;
import com.elementsplus.core.file.DiskData;
import com.elementsplus.core.file.ExternalDevice;
import com.elementsplus.core.file.FileEntry;
import com.elementsplus.core.file.FileHeader;
import com.elementsplus.core.file.FilePaths;
import com.elementsplus.core.file.FileRoot;
import com.elementsplus.core.file.FileSessions;
import com.elementsplus.core.file.FileText;
import com.elementsplus.core.file.MemoryFileSystem;
import com.elementsplus.core.file.SharedFiles;
import com.elementsplus.item.PunchedFileItem;
import com.elementsplus.item.TextFileItem;
import com.elementsplus.menu.ExperimentTableMenu;
import com.elementsplus.network.FileActionPayload;
import com.elementsplus.network.FileActionResultPayload;
import com.elementsplus.network.FileContentPayload;
import com.elementsplus.network.FileEditorActionPayload;
import com.elementsplus.network.FileEditorStatePayload;
import com.elementsplus.network.FileFolderArchivePayload;
import com.elementsplus.network.FileFolderPastePayload;
import com.elementsplus.network.FileSystemDataPayload;
import com.elementsplus.network.FileSystemRequestPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * 实验桌文件系统的服务端逻辑：内存、共享空间、外接设备，以及多人编辑会话。
 */
public final class ExperimentTableFiles {
    private final ExperimentTableBlockEntity table;
    private final MemoryFileSystem memory = new MemoryFileSystem(ElementsPlusServerConfig.memoryMaxBytes());
    private final SharedFiles shared = new SharedFiles();
    private final FileSessions sessions = new FileSessions();
    /** 每个玩家在各根节点上最后浏览的目录，用于共享/编辑状态变化时刷新列表。 */
    private final Map<UUID, Map<FileRoot, String>> viewedPaths = new HashMap<>();

    public ExperimentTableFiles(ExperimentTableBlockEntity table) {
        this.table = table;
    }

    public MemoryFileSystem memory() {
        return memory;
    }

    public SharedFiles shared() {
        return shared;
    }

    public FileSessions sessions() {
        return sessions;
    }

    /* ============================================================
     *  生命周期
     * ============================================================ */

    public void load(CompoundTag tag) {
        memory.setCapacity(ElementsPlusServerConfig.memoryMaxBytes());
        memory.load(tag);
    }

    public void save(CompoundTag tag) {
        memory.save(tag);
    }

    public void onRemoved() {
        sessions.clear();
        shared.clear();
        viewedPaths.clear();
    }

    /** 关闭实验桌界面：结束编辑会话并断开该玩家的所有共享。 */
    public void onViewerClosed(ServerPlayer player) {
        UUID playerId = player.getUUID();
        closeSession(playerId, true);
        List<UUID> removedShares = shared.removeOwner(playerId);
        for (UUID fileId : removedShares) {
            String key = "shared:" + fileId;
            for (FileSessions.Session session : sessions.forKey(key)) {
                closeSession(session.player(), false);
            }
            sessions.reset(key);
        }
        viewedPaths.remove(playerId);
        if (!removedShares.isEmpty()) {
            refreshAll(FileRoot.SHARED, playerId);
            refreshAll(FileRoot.CLIENT, playerId);
        }
    }

    private List<ServerPlayer> viewers() {
        List<ServerPlayer> result = new ArrayList<>();
        if (!(table.getLevel() instanceof ServerLevel level)) {
            return result;
        }
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            if (player.serverLevel() != level) {
                continue;
            }
            BlockPos pos = ExperimentTableMenu.getOpenTable(player.getUUID());
            if (table.getBlockPos().equals(pos)) {
                result.add(player);
            }
        }
        return result;
    }

    /* ============================================================
     *  列表 / 读取
     * ============================================================ */

    public List<FileEntry> list(FileRoot root, String path, ServerPlayer player) {
        return switch (root) {
            case TABLE_MEMORY -> {
                List<FileEntry> entries = new ArrayList<>();
                for (FileEntry entry : memory.list(path)) {
                    // 文件夹大小只在悬停提示中使用，表格不显示。
                    if (entry.folder()) {
                        entries.add(entry.withSize(memory.folderSize(entry.path()))
                                .withEditing(!sessions.forKey("memory:" + entry.path()).isEmpty()));
                    } else {
                        entries.add(entry.withEditing(!sessions.forKey("memory:" + entry.path()).isEmpty()));
                    }
                }
                yield entries;
            }
            case SHARED -> {
                List<FileEntry> entries = new ArrayList<>();
                for (SharedFiles.SharedFile file : shared.all()) {
                    entries.add(new FileEntry(file.id().toString(), file.name(), false, file.size(),
                            file.owner(), file.ownerName(), true, true, shared.isEditing(file.id())));
                }
                entries.sort((a, b) -> a.name().toLowerCase(Locale.ROOT)
                        .compareTo(b.name().toLowerCase(Locale.ROOT)));
                yield entries;
            }
            case EXTERNAL_DEVICE -> ExternalDevice.list(table.getItem(0), path);
            case CLIENT -> List.of();
        };
    }

    /** 读取请求的响应；文件不存在或不可读时返回 {@code null}。 */
    public byte @Nullable [] read(FileRoot root, String path, ServerPlayer player) {
        return switch (root) {
            case TABLE_MEMORY -> memory.read(path);
            case SHARED -> {
                UUID id = parseId(path);
                SharedFiles.SharedFile file = shared.byId(id);
                yield file == null ? null : file.data();
            }
            case EXTERNAL_DEVICE -> ExternalDevice.read(table.getItem(0), path);
            case CLIENT -> {
                SharedFiles.SharedFile file = shared.byOwnerPath(player.getUUID(), path);
                yield file == null ? null : file.data();
            }
        };
    }

    public void handleRequest(FileSystemRequestPayload payload, ServerPlayer player) {
        if (payload == null || !table.getBlockPos().equals(payload.pos())) {
            return;
        }
        FileRoot root = payload.root();
        String path = root.isFlat() ? FilePaths.normalizeDir(payload.path()) : FilePaths.normalizeDir(payload.path());
        if (payload.request() == FileSystemRequestPayload.Request.FOLDER) {
            if (root != FileRoot.TABLE_MEMORY) {
                send(player, FileActionResultPayload.fail(table.getBlockPos(), path,
                        "gui.elements-plus.file.error.not_allowed"));
                return;
            }
            if (!memory.isFolder(path)) {
                send(player, FileActionResultPayload.fail(table.getBlockPos(), path,
                        "gui.elements-plus.file.error.not_found"));
                return;
            }
            send(player, new FileFolderArchivePayload(table.getBlockPos(), root, path,
                    collectMemoryFolder(path)));
            return;
        }
        if (payload.request() == FileSystemRequestPayload.Request.READ) {
            byte[] data = read(root, payload.path(), player);
            if (data == null) {
                send(player, FileActionResultPayload.fail(table.getBlockPos(), payload.path(),
                        "gui.elements-plus.file.error.not_found"));
                return;
            }
            send(player, new FileContentPayload(table.getBlockPos(), root, payload.path(), data,
                    FileContentPayload.Purpose.COPY, ""));
            return;
        }
        viewedPaths.computeIfAbsent(player.getUUID(), key -> new EnumMap<>(FileRoot.class)).put(root, path);
        sendListing(player, root, path);
    }

    public void sendListing(ServerPlayer player, FileRoot root, String path) {
        memory.setCapacity(ElementsPlusServerConfig.memoryMaxBytes());
        List<FileEntry> entries = list(root, path, player);
        List<String> sharedPaths = new ArrayList<>();
        for (SharedFiles.SharedFile file : shared.all()) {
            if (file.owner().equals(player.getUUID())) {
                sharedPaths.add(file.ownerPath());
            }
        }
        List<FileSystemDataPayload.PathEditors> pathEditors = new ArrayList<>();
        // (编辑器 key, 界面中的路径)
        List<String[]> targets = new ArrayList<>();
        switch (root) {
            case TABLE_MEMORY -> {
                for (FileEntry entry : entries) {
                    targets.add(new String[]{"memory:" + entry.path(), entry.path()});
                }
            }
            case SHARED -> {
                for (FileEntry entry : entries) {
                    targets.add(new String[]{"shared:" + entry.path(), entry.path()});
                }
            }
            case EXTERNAL_DEVICE -> {
                ItemStack device = table.getItem(0);
                if (ExternalDevice.isFolderDevice(device)) {
                    for (FileEntry entry : entries) {
                        if (!entry.folder()) {
                            targets.add(new String[]{"external:" + table.getBlockPos().asLong()
                                    + ":" + entry.path(), entry.path()});
                        }
                    }
                } else {
                    ExternalDevice.Entry entry = deviceEntry();
                    targets.add(new String[]{"external:" + table.getBlockPos().asLong(),
                            entry == null ? "" : entry.name()});
                }
            }
            case CLIENT -> {
                for (String sharedPath : sharedPaths) {
                    SharedFiles.SharedFile file = shared.byOwnerPath(player.getUUID(), sharedPath);
                    if (file != null) {
                        targets.add(new String[]{"shared:" + file.id(), sharedPath});
                    }
                }
            }
        }
        for (String[] target : targets) {
            List<String> editorNames = new ArrayList<>();
            for (FileSessions.Session session : sessions.forKey(target[0])) {
                editorNames.add(session.playerName());
            }
            if (!editorNames.isEmpty()) {
                pathEditors.add(new FileSystemDataPayload.PathEditors(target[1],
                        String.join(", ", editorNames)));
            }
        }
        send(player, new FileSystemDataPayload(table.getBlockPos(), root, path, entries,
                memory.usedBytes(), memory.getCapacity(), shared.countFor(player.getUUID()),
                ElementsPlusServerConfig.maxSharedPerPlayer(), sharedPaths, pathEditors));
    }

    /** 刷新所有打开该实验桌的玩家的某个根节点列表。 */
    public void refreshAll(FileRoot root) {
        refreshAll(root, null);
    }

    /** 刷新列表，可跳过正在关闭界面的玩家（其连接可能已经断开）。 */
    public void refreshAll(FileRoot root, @Nullable UUID skip) {
        for (ServerPlayer player : viewers()) {
            if (skip != null && skip.equals(player.getUUID())) {
                continue;
            }
            Map<FileRoot, String> paths = viewedPaths.get(player.getUUID());
            String path = paths == null ? "" : paths.getOrDefault(root, "");
            sendListing(player, root, path);
        }
    }

    private void send(ServerPlayer player, net.minecraft.network.protocol.common.custom.CustomPacketPayload payload) {
        if (player != null && player.connection != null) {
            ServerPlayNetworking.send(player, payload);
        }
    }

    /* ============================================================
     *  文件操作
     * ============================================================ */

    public void handleAction(FileActionPayload payload, ServerPlayer player) {
        if (payload == null || !table.getBlockPos().equals(payload.pos())) {
            return;
        }
        FileRoot root = payload.root();
        String first = payload.paths().isEmpty() ? "" : payload.paths().get(0);
        String error = switch (payload.action()) {
            case CREATE_FILE -> createFile(root, payload, player);
            case CREATE_FOLDER -> createFolder(root, payload, player);
            case RENAME -> rename(root, payload, player);
            case DELETE -> delete(root, payload, player);
            case TRANSFER -> transfer(root, payload, player);
            case TRANSFER_TO -> transferTo(root, payload);
            case WRITE_FILE -> writeFile(root, payload, player);
            case SHARE -> share(payload, player);
            case UNSHARE -> unshare(root, payload, player);
            case PRINT -> printToSlot(root, payload, player);
            case UPLOAD_CIRCUIT -> uploadCircuit(payload, player);
        };
        if (error != null) {
            send(player, FileActionResultPayload.fail(table.getBlockPos(), first, error));
        } else {
            send(player, FileActionResultPayload.ok(table.getBlockPos(), first));
        }
        if (root == FileRoot.TABLE_MEMORY) {
            refreshAll(FileRoot.TABLE_MEMORY);
        } else if (root == FileRoot.SHARED) {
            refreshAll(FileRoot.SHARED);
        }
        if (payload.action() == FileActionPayload.Action.PRINT
                || payload.action() == FileActionPayload.Action.UPLOAD_CIRCUIT) {
            table.markDeviceStackSynced(table.getItem(0));
            refreshAll(FileRoot.EXTERNAL_DEVICE);
        }
    }

    private @Nullable String createFile(FileRoot root, FileActionPayload payload, ServerPlayer player) {
        if (root != FileRoot.TABLE_MEMORY) {
            return "gui.elements-plus.file.error.not_allowed";
        }
        String name = FilePaths.validateName(payload.name());
        if (name == null) {
            return "gui.elements-plus.file.error.invalid_name";
        }
        String path = FilePaths.join(FilePaths.normalizeDir(payload.target()), name);
        MemoryFileSystem.WriteResult result = memory.createFile(path);
        return result.success() ? null : result.failureKey();
    }

    private @Nullable String createFolder(FileRoot root, FileActionPayload payload, ServerPlayer player) {
        if (root != FileRoot.TABLE_MEMORY) {
            return "gui.elements-plus.file.error.not_allowed";
        }
        String name = FilePaths.validateName(payload.name());
        if (name == null) {
            return "gui.elements-plus.file.error.invalid_name";
        }
        String path = FilePaths.join(FilePaths.normalizeDir(payload.target()), name);
        MemoryFileSystem.WriteResult result = memory.createFolder(path);
        return result.success() ? null : result.failureKey();
    }

    private @Nullable String rename(FileRoot root, FileActionPayload payload, ServerPlayer player) {
        if (root != FileRoot.TABLE_MEMORY) {
            return "gui.elements-plus.file.error.not_allowed";
        }
        String name = FilePaths.validateName(payload.name());
        if (name == null) {
            return "gui.elements-plus.file.error.invalid_name";
        }
        String path = FilePaths.normalize(payload.paths().isEmpty() ? "" : payload.paths().get(0));
        if (path == null || path.isEmpty() || !memory.exists(path)) {
            return "gui.elements-plus.file.error.not_found";
        }
        if (!memory.rename(path, name)) {
            return "gui.elements-plus.file.error.already_exists";
        }
        String target = FilePaths.join(FilePaths.parent(path), name);
        remapOpenSessions(path, target);
        return null;
    }

    private @Nullable String delete(FileRoot root, FileActionPayload payload, ServerPlayer player) {
        if (root != FileRoot.TABLE_MEMORY) {
            return "gui.elements-plus.file.error.not_allowed";
        }
        boolean any = false;
        for (String raw : payload.paths()) {
            String path = FilePaths.normalize(raw);
            if (path == null || path.isEmpty()) {
                continue;
            }
            if (memory.delete(path)) {
                any = true;
                closeSessionsUnder("memory:" + path, path);
            }
        }
        return any ? null : "gui.elements-plus.file.error.not_found";
    }

    private @Nullable String transferTo(FileRoot root, FileActionPayload payload) {
        if (root != FileRoot.TABLE_MEMORY) {
            return "gui.elements-plus.file.error.not_allowed";
        }
        String source = payload.paths().isEmpty() ? "" : payload.paths().get(0);
        String target = FilePaths.normalize(payload.target());
        if (target == null || target.isEmpty()) {
            return "gui.elements-plus.file.error.invalid_path";
        }
        if (!memory.transferTo(source, target, payload.copy(), payload.overwrite())) {
            return "gui.elements-plus.file.error.already_exists";
        }
        return null;
    }

    private @Nullable String transfer(FileRoot root, FileActionPayload payload, ServerPlayer player) {
        if (root != FileRoot.TABLE_MEMORY) {
            return "gui.elements-plus.file.error.not_allowed";
        }
        String targetDir = FilePaths.normalizeDir(payload.target());
        boolean any = false;
        for (String raw : payload.paths()) {
            String path = FilePaths.normalize(raw);
            if (path == null || path.isEmpty()) {
                continue;
            }
            String target = FilePaths.join(targetDir, FilePaths.name(path));
            if (path.equals(target)) {
                any = true;
                continue;
            }
            if (memory.isFolder(path)
                    && (targetDir.equals(path) || FilePaths.isUnder(targetDir, path))) {
                return "gui.elements-plus.file.error.into_self";
            }
            if (memory.exists(target)) {
                if (!payload.overwrite()) {
                    return "gui.elements-plus.file.error.already_exists";
                }
                memory.delete(target);
            }
            if (memory.transfer(path, targetDir, payload.copy())) {
                any = true;
                if (!payload.copy()) {
                    remapOpenSessions(path, target);
                }
            } else {
                return memory.exists(target) ? "gui.elements-plus.file.error.already_exists"
                        : "gui.elements-plus.file.error.no_space";
            }
        }
        return any ? null : "gui.elements-plus.file.error.not_found";
    }

    /** 递归收集内存文件夹，供客户端复制到本地。 */
    private List<FileFolderArchivePayload.Entry> collectMemoryFolder(String sourcePath) {
        List<FileFolderArchivePayload.Entry> entries = new ArrayList<>();
        collectMemoryFolder(sourcePath, sourcePath, entries);
        return entries;
    }

    private void collectMemoryFolder(String basePath, String dir,
                                     List<FileFolderArchivePayload.Entry> entries) {
        for (FileEntry entry : memory.list(dir)) {
            String relative = basePath.isEmpty() ? entry.path()
                    : entry.path().substring(basePath.length() + 1);
            if (entry.folder()) {
                entries.add(new FileFolderArchivePayload.Entry(relative, true, new byte[0]));
                collectMemoryFolder(basePath, entry.path(), entries);
            } else {
                byte[] data = memory.read(entry.path());
                entries.add(new FileFolderArchivePayload.Entry(relative, false,
                        data == null ? new byte[0] : data));
            }
        }
    }

    /** 目标已存在时，覆盖前可回收的内存字节数（文件头 + 内容）。 */
    private int targetMemoryBytes(String path) {
        if (memory.isFolder(path)) {
            return memory.folderSize(path);
        }
        byte[] data = memory.read(path);
        return data == null ? 0
                : FileHeader.sizeFor(FilePaths.name(path)) + data.length;
    }

    /** 客户端把整个文件夹写入实验桌内存；先校验容量，失败时不保留半成品。 */
    public @Nullable String pasteFolder(FileFolderPastePayload payload, ServerPlayer player) {
        if (payload == null || !table.getBlockPos().equals(payload.pos())) {
            return "gui.elements-plus.file.error.not_found";
        }
        String targetDir = FilePaths.normalizeDir(payload.targetDir());
        String folderName = FilePaths.validateName(payload.folderName());
        if (folderName == null) {
            return "gui.elements-plus.file.error.invalid_name";
        }
        String targetPath = FilePaths.join(targetDir, folderName);
        if (targetPath.isEmpty()) {
            return "gui.elements-plus.file.error.invalid_path";
        }
        boolean targetExists = memory.exists(targetPath);
        if (targetExists && !payload.overwrite()) {
            return "gui.elements-plus.file.error.already_exists";
        }
        int required = 0;
        List<FileFolderArchivePayload.Entry> valid = new ArrayList<>();
        for (FileFolderArchivePayload.Entry entry : payload.entries()) {
            String relative = FilePaths.normalize(entry.path());
            if (relative == null || relative.isEmpty()) {
                return "gui.elements-plus.file.error.invalid_path";
            }
            String path = FilePaths.join(targetPath, relative);
            byte[] data = entry.data() == null ? new byte[0] : entry.data();
            valid.add(new FileFolderArchivePayload.Entry(relative, entry.folder(), data));
            if (!entry.folder()) {
                required += FileHeader.sizeFor(FilePaths.name(path)) + data.length;
            }
        }
        int available = memory.freeBytes();
        if (targetExists && payload.overwrite()) {
            available += targetMemoryBytes(targetPath);
        }
        if (required > available) {
            return "gui.elements-plus.file.error.not_enough_space";
        }
        if (targetExists) {
            memory.delete(targetPath);
        }
        boolean created = memory.createFolder(targetPath).success();
        if (!created && !memory.isFolder(targetPath)) {
            return "gui.elements-plus.file.error.invalid_path";
        }
        for (FileFolderArchivePayload.Entry entry : valid) {
            if (!entry.folder()) {
                continue;
            }
            String path = FilePaths.join(targetPath, entry.path());
            MemoryFileSystem.WriteResult result = memory.createFolder(path);
            if (!result.success() && !memory.isFolder(path)) {
                memory.delete(targetPath);
                return result.failureKey();
            }
        }
        for (FileFolderArchivePayload.Entry entry : valid) {
            if (entry.folder()) {
                continue;
            }
            String path = FilePaths.join(targetPath, entry.path());
            MemoryFileSystem.WriteResult result = memory.write(path, entry.data());
            if (!result.success()) {
                memory.delete(targetPath);
                return result.failureKey();
            }
        }
        refreshAll(FileRoot.TABLE_MEMORY);
        return null;
    }

    private @Nullable String writeFile(FileRoot root, FileActionPayload payload, ServerPlayer player) {
        String path = payload.paths().isEmpty() ? "" : payload.paths().get(0);
        byte[] data = payload.data() == null ? new byte[0] : payload.data();
        switch (root) {
            case TABLE_MEMORY -> {
                String normalized = FilePaths.normalize(path);
                if (normalized == null || normalized.isEmpty()) {
                    return "gui.elements-plus.file.error.invalid_name";
                }
                if (memory.exists(normalized) && !payload.overwrite()) {
                    return "gui.elements-plus.file.error.already_exists";
                }
                MemoryFileSystem.WriteResult result = memory.write(normalized, data);
                if (!result.success()) {
                    return result.failureKey();
                }
                bumpEditorRevision("memory:" + normalized);
                return null;
            }
            case EXTERNAL_DEVICE -> {
                if (!ModItems.DISK.equals(table.getItem(0).getItem())) {
                    return "gui.elements-plus.file.error.not_writable";
                }
                return writeDisk(data);
            }
            case SHARED -> {
                UUID id = parseId(path);
                SharedFiles.UpdateResult result = shared.updateData(id, data,
                        ElementsPlusServerConfig.sharedFileMaxBytes(), player.getUUID());
                if (result == SharedFiles.UpdateResult.NOT_FOUND) {
                    return "gui.elements-plus.file.error.not_found";
                }
                if (result == SharedFiles.UpdateResult.TOO_LARGE) {
                    return "gui.elements-plus.file.error.too_large";
                }
                bumpEditorRevision("shared:" + id);
                return null;
            }
            case CLIENT -> {
                SharedFiles.SharedFile file = shared.byOwnerPath(player.getUUID(), path);
                if (file == null) {
                    return "gui.elements-plus.file.error.not_shared";
                }
                SharedFiles.UpdateResult result = shared.updateData(file.id(), data,
                        ElementsPlusServerConfig.sharedFileMaxBytes(), player.getUUID());
                if (result == SharedFiles.UpdateResult.TOO_LARGE) {
                    return "gui.elements-plus.file.error.too_large";
                }
                bumpEditorRevision("shared:" + file.id());
                return null;
            }
        }
        return null;
    }

    private @Nullable String share(FileActionPayload payload, ServerPlayer player) {
        if (payload.root() != FileRoot.CLIENT) {
            return "gui.elements-plus.file.error.not_allowed";
        }
        String path = FilePaths.normalize(payload.paths().isEmpty() ? "" : payload.paths().get(0));
        if (path == null || path.isEmpty()) {
            return "gui.elements-plus.file.error.invalid_name";
        }
        byte[] data = payload.data() == null ? new byte[0] : payload.data();
        String name = FilePaths.name(path);
        if (FilePaths.validateName(name) == null) {
            return "gui.elements-plus.file.error.invalid_name";
        }
        SharedFiles.ShareResult result = shared.share(player.getUUID(),
                player.getGameProfile().getName(), path, name, data,
                ElementsPlusServerConfig.sharedFileMaxBytes(),
                ElementsPlusServerConfig.maxSharedPerPlayer());
        String error = switch (result) {
            case OK -> null;
            case ALREADY_SHARED -> "gui.elements-plus.file.error.already_shared";
            case TOO_LARGE -> "gui.elements-plus.file.error.too_large";
            case TOO_MANY -> "gui.elements-plus.file.error.too_many_shared";
            case INVALID_PATH -> "gui.elements-plus.file.error.invalid_name";
        };
        if (error == null) {
            refreshAll(FileRoot.SHARED);
            refreshAll(FileRoot.CLIENT);
        }
        return error;
    }

    private @Nullable String unshare(FileRoot root, FileActionPayload payload, ServerPlayer player) {
        String path = payload.paths().isEmpty() ? "" : payload.paths().get(0);
        SharedFiles.SharedFile file;
        if (root == FileRoot.CLIENT) {
            file = shared.byOwnerPath(player.getUUID(), path);
        } else if (root == FileRoot.SHARED) {
            file = shared.byId(parseId(path));
        } else {
            return "gui.elements-plus.file.error.not_allowed";
        }
        if (file == null) {
            return "gui.elements-plus.file.error.not_found";
        }
        if (!shared.unshare(file.id(), player.getUUID())) {
            return "gui.elements-plus.file.error.not_owner";
        }
        String key = "shared:" + file.id();
        for (FileSessions.Session session : sessions.forKey(key)) {
            notifyOffline(session);
            closeSession(session.player(), false);
        }
        sessions.reset(key);
        refreshAll(FileRoot.SHARED);
        refreshAll(FileRoot.CLIENT);
        return null;
    }

    /* ============================================================
     *  外接设备与打印
     * ============================================================ */

    /** 外接设备当前物品对应的文件。 */
    public @Nullable ExternalDevice.Entry deviceEntry() {
        return ExternalDevice.describe(table.getItem(0));
    }

    private @Nullable String writeDisk(byte[] data) {
        ItemStack stack = table.getItem(0);
        if (!DiskData.write(stack, data)) {
            return "gui.elements-plus.file.error.too_large";
        }
        table.setItem(0, stack);
        table.markDeviceStackSynced(stack);
        return null;
    }

    private @Nullable String printToSlot(FileRoot root, FileActionPayload payload, ServerPlayer player) {
        if (root == FileRoot.EXTERNAL_DEVICE) {
            return "gui.elements-plus.file.error.not_printable";
        }
        byte[] data = payload.data() == null ? new byte[0] : payload.data();
        String kind = payload.name() == null ? "" : payload.name();
        String sourceName = payload.paths().isEmpty() ? "" : payload.paths().get(0);
        return printToSlot(data, kind, sourceName);
    }

    /** 把数据打印到外接设备槽位；成功返回 {@code null}。 */
    public @Nullable String printToSlot(byte[] data, String kind) {
        return printToSlot(data, kind, "");
    }

    public @Nullable String printToSlot(byte[] data, String kind, String sourceName) {
        ItemStack slot = table.getItem(0);
        if (slot.isEmpty()) {
            return "gui.elements-plus.file.error.no_device";
        }
        if (slot.is(ModItems.DISK)) {
            if (!DiskData.canWrite(data)) {
                return "gui.elements-plus.file.error.too_large";
            }
            if (!DiskData.write(slot, data)) {
                return "gui.elements-plus.file.error.too_large";
            }
            table.setItem(0, slot);
            return null;
        }
        if (slot.is(Items.PAPER)) {
            if ("text".equals(kind)) {
                if (!FileText.canOpenAsText(data, 0)) {
                    return "gui.elements-plus.file.error.not_text";
                }
                ItemStack result = new ItemStack(ModItems.TEXT_FILE);
                if (!TextFileItem.setData(result, data)) {
                    return "gui.elements-plus.file.error.too_large";
                }
                applyPrintedName(result, sourceName, false);
                table.setItem(0, result);
                return null;
            }
            ItemStack result = new ItemStack(ModItems.PUNCHED_FILE);
            if (!PunchedFileItem.setData(result, data)) {
                return "gui.elements-plus.file.error.too_large";
            }
            applyPrintedName(result, sourceName, false);
            table.setItem(0, result);
            return null;
        }
        if (slot.is(ModItems.CIRCUIT_DIAGRAM)) {
            return uploadToSlot(data, sourceName);
        }
        return "gui.elements-plus.file.error.not_printable";
    }

    /** 打印到纸/电路图时按打印源文件名重命名；磁盘保持原名。 */
    private static void applyPrintedName(ItemStack stack, String sourceName, boolean stripExtension) {
        if (sourceName == null || sourceName.isBlank()) {
            return;
        }
        String name = sourceName;
        if (stripExtension) {
            int dot = name.lastIndexOf('.');
            if (dot > 0) {
                name = name.substring(0, dot);
            }
        }
        // 原版物品自定义名上限（铁砧界面）为 50 个字符。
        if (name.length() > 50) {
            name = name.substring(0, 50);
        }
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,
                Component.literal(name));
    }

    private @Nullable String uploadCircuit(FileActionPayload payload, ServerPlayer player) {
        String sourceName = payload.paths().isEmpty() ? "" : payload.paths().get(0);
        return uploadToSlot(payload.data() == null ? new byte[0] : payload.data(), sourceName);
    }

    private @Nullable String uploadToSlot(byte[] data) {
        return uploadToSlot(data, "");
    }

    private @Nullable String uploadToSlot(byte[] data, String sourceName) {
        ItemStack slot = table.getItem(0);
        if (!slot.is(ModItems.CIRCUIT_DIAGRAM)) {
            return "gui.elements-plus.file.error.no_diagram";
        }
        if (!ElementsPlusServerConfig.allowCircuitUpload()) {
            return "gui.elements-plus.file.error.upload_disabled";
        }
        if (CircuitItemData.isCompiled(slot)
                || slot.get(ModDataComponents.EQUIVALENT_COMPONENT) != null) {
            return "gui.elements-plus.file.error.diagram_compiled";
        }
        CircuitDiagram diagram = CircuitDiagramFile.decode(data);
        List<String> errors = CircuitDiagramFile.validate(data, diagram);
        if (!errors.isEmpty()) {
            return errors.get(0);
        }
        slot.set(ModDataComponents.CIRCUIT_DIAGRAM, diagram);
        applyPrintedName(slot, sourceName, true);
        table.setItem(0, slot);
        return null;
    }

    /* ============================================================
     *  编辑器会话
     * ============================================================ */

    public void handleEditor(FileEditorActionPayload payload, ServerPlayer player) {
        if (payload == null || !table.getBlockPos().equals(payload.pos())) {
            return;
        }
        UUID playerId = player.getUUID();
        FileSessions.Session session = sessions.get(playerId);
        if (payload.mode() == FileEditorActionPayload.Mode.OPEN) {
            openEditor(payload, player);
            return;
        }
        if (session == null || !session.root().equals(payload.root())
                || !session.path().equals(payload.path())) {
            // 会话已经失效（例如文件被删除），通知客户端关闭。
            send(player, new FileEditorStatePayload(table.getBlockPos(), payload.root(), payload.path(),
                    new byte[0], -1, List.of(), true, 0,
                    "gui.elements-plus.file.error.not_found", false));
            return;
        }
        switch (payload.mode()) {
            case CLOSE -> closeSession(playerId, true);
            case CURSOR -> {
                sessions.put(session.withCursor(payload.cursorStart(), payload.cursorEnd()));
                broadcastState(session.key(), sessions.revision(session.key()), null);
            }
            case MODE -> {
                // 一个编辑者切换模式时，同一文件的所有编辑者一起切换，避免两端模式不一致。
                boolean textMode = payload.textMode();
                for (FileSessions.Session other : new ArrayList<>(sessions.forKey(session.key()))) {
                    sessions.put(other.withTextMode(textMode));
                }
                sessions.put(sessions.get(playerId).withCursor(payload.cursorStart(), payload.cursorEnd()));
                broadcastState(session.key(), sessions.revision(session.key()), null);
            }
            case UPDATE -> updateEditor(payload, player, session);
            default -> {
            }
        }
    }

    private void openEditor(FileEditorActionPayload payload, ServerPlayer player) {
        UUID playerId = player.getUUID();
        FileRoot root = payload.root();
        String path = payload.path();
        if (root == FileRoot.CLIENT) {
            String normalized = FilePaths.normalize(path);
            if (normalized == null || normalized.isEmpty()) {
                return;
            }
            path = normalized;
        }
        String key = keyFor(root, path, playerId);
        if (key == null) {
            send(player, new FileEditorStatePayload(table.getBlockPos(), root, path, new byte[0], -1,
                    List.of(), true, 0, "gui.elements-plus.file.error.not_found", false));
            return;
        }
        byte[] data = dataForKey(key);
        if (data == null) {
            send(player, new FileEditorStatePayload(table.getBlockPos(), root, path, new byte[0], -1,
                    List.of(), true, 0, "gui.elements-plus.file.error.not_found", false));
            return;
        }
        FileSessions.Session previous = sessions.get(playerId);
        if (previous != null) {
            closeSession(playerId, true);
        }
        FileSessions.Session session = new FileSessions.Session(playerId,
                player.getGameProfile().getName(), root, path, key, payload.textMode(),
                payload.cursorStart(), payload.cursorEnd());
        sessions.put(session);
        if (key.startsWith("shared:")) {
            UUID fileId = parseId(key.substring("shared:".length()));
            shared.join(fileId, playerId, player.getGameProfile().getName());
        }
        sendState(session, sessions.revision(key), null);
        broadcastState(key, sessions.revision(key), playerId);
        refreshAll(FileRoot.TABLE_MEMORY);
        refreshAll(FileRoot.SHARED);
        refreshAll(FileRoot.CLIENT);
    }

    private void updateEditor(FileEditorActionPayload payload, ServerPlayer player,
                              FileSessions.Session session) {
        byte[] data = payload.data() == null ? new byte[0] : payload.data();
        String key = session.key();
        byte[] previous = dataForKey(key);
        String error = null;
        if (key.startsWith("memory:")) {
            String path = key.substring("memory:".length());
            MemoryFileSystem.WriteResult result = memory.write(path, data);
            if (!result.success()) {
                error = result.failureKey();
            }
        } else if (key.startsWith("shared:")) {
            UUID fileId = parseId(key.substring("shared:".length()));
            SharedFiles.SharedFile file = shared.byId(fileId);
            if (file == null) {
                error = "gui.elements-plus.file.error.not_found";
            } else {
                SharedFiles.UpdateResult result = shared.updateData(fileId, data,
                        ElementsPlusServerConfig.sharedFileMaxBytes(), player.getUUID());
                if (result == SharedFiles.UpdateResult.TOO_LARGE) {
                    error = "gui.elements-plus.file.error.too_large";
                } else if (result == SharedFiles.UpdateResult.NOT_FOUND) {
                    error = "gui.elements-plus.file.error.not_found";
                } else {
                    saveSharedToOwner(file, data, player.getUUID());
                }
            }
        } else if (key.startsWith("external:")) {
            if (!ModItems.DISK.equals(table.getItem(0).getItem())) {
                error = "gui.elements-plus.file.error.not_writable";
            } else {
                error = writeDisk(data);
            }
        }
        if (error != null) {
            send(player, FileActionResultPayload.fail(table.getBlockPos(), session.path(), error));
            return;
        }
        if (previous != null) {
            adjustEditorCursors(key, previous, data, player.getUUID());
        }
        sessions.put(session.withCursor(payload.cursorStart(), payload.cursorEnd())
                .withTextMode(payload.textMode()));
        long revision = sessions.bumpRevision(key);
        broadcastState(key, revision, null);
        if (key.startsWith("shared:")) {
            refreshAll(FileRoot.SHARED);
        }
        if (key.startsWith("external:")) {
            refreshAll(FileRoot.EXTERNAL_DEVICE);
        }
    }

    /**
     * 内容被其他人修改后，把其他编辑者的光标/选区按文本流平移，
     * 避免在前方插入/删除时光标漂移。
     */
    private void adjustEditorCursors(String key, byte[] previous, byte[] current, UUID editor) {
        for (FileSessions.Session session : sessions.forKey(key)) {
            if (session.player().equals(editor)) {
                continue;
            }
            int start = session.start();
            int end = session.end();
            if (session.textMode()) {
                String before = FileText.decodeLossless(previous);
                String after = FileText.decodeLossless(current);
                if (before != null && after != null) {
                    int[] range = diffRange(before, after);
                    start = adjustIndex(start, range[0], range[1], range[2], after.length());
                    end = adjustIndex(end, range[0], range[1], range[2], after.length());
                } else {
                    int[] range = diffRange(previous, current);
                    start = adjustIndex(start, range[0], range[1], range[2], current.length);
                    end = adjustIndex(end, range[0], range[1], range[2], current.length);
                }
            } else {
                int[] range = diffRange(previous, current);
                start = adjustIndex(start, range[0], range[1], range[2], current.length);
                end = adjustIndex(end, range[0], range[1], range[2], current.length);
            }
            sessions.put(session.withCursor(start, end));
        }
    }

    /** 返回 {公共前缀, 旧内容结束位置, 新内容结束位置}。 */
    private static int[] diffRange(byte[] before, byte[] after) {
        int prefix = 0;
        int max = Math.min(before.length, after.length);
        while (prefix < max && before[prefix] == after[prefix]) {
            prefix++;
        }
        int suffix = 0;
        while (suffix < max - prefix
                && before[before.length - 1 - suffix] == after[after.length - 1 - suffix]) {
            suffix++;
        }
        return new int[]{prefix, before.length - suffix, after.length - suffix};
    }

    private static int[] diffRange(String before, String after) {
        int prefix = 0;
        int max = Math.min(before.length(), after.length());
        while (prefix < max && before.charAt(prefix) == after.charAt(prefix)) {
            prefix++;
        }
        int suffix = 0;
        while (suffix < max - prefix
                && before.charAt(before.length() - 1 - suffix)
                == after.charAt(after.length() - 1 - suffix)) {
            suffix++;
        }
        return new int[]{prefix, before.length() - suffix, after.length() - suffix};
    }

    private static int adjustIndex(int index, int prefix, int oldEnd, int newEnd, int newLength) {
        if (index < prefix) {
            return index;
        }
        if (index >= oldEnd) {
            return Math.max(0, Math.min(newLength, index + (newEnd - oldEnd)));
        }
        return Math.max(0, Math.min(newLength, newEnd));
    }

    /** 共享文件变化后写回拥有者的客户端文件。 */
    private void saveSharedToOwner(SharedFiles.SharedFile file, byte[] data, UUID editor) {
        if (file.owner().equals(editor)) {
            return;
        }
        if (!(table.getLevel() instanceof ServerLevel level)) {
            return;
        }
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(file.owner());
        if (owner == null) {
            return;
        }
        send(owner, new FileContentPayload(table.getBlockPos(), FileRoot.CLIENT,
                file.ownerPath(), data, FileContentPayload.Purpose.COPY, ""));
    }

    private int dataMaxBytes(FileSessions.Session session) {
        String key = session.key();
        if (key.startsWith("memory:")) {
            String path = key.substring("memory:".length());
            int current = Math.max(0, memory.fileSize(path));
            return Math.max(0, memory.freeBytes() + current);
        }
        if (key.startsWith("shared:") || key.startsWith("client:")) {
            return ElementsPlusServerConfig.sharedFileMaxBytes();
        }
        if (key.startsWith("external:")) {
            return DiskData.CAPACITY;
        }
        return ElementsPlusServerConfig.textFileMaxBytes();
    }

    private boolean readOnly(FileSessions.Session session) {
        if (session.key().startsWith("external:")) {
            return !ModItems.DISK.equals(table.getItem(0).getItem());
        }
        return false;
    }

    private @Nullable String keyFor(FileRoot root, String path, UUID playerId) {
        switch (root) {
            case TABLE_MEMORY -> {
                String normalized = FilePaths.normalize(path);
                if (normalized == null || !memory.isFile(normalized)) {
                    return null;
                }
                return "memory:" + normalized;
            }
            case SHARED -> {
                SharedFiles.SharedFile file = shared.byId(parseId(path));
                return file == null ? null : "shared:" + file.id();
            }
            case CLIENT -> {
                SharedFiles.SharedFile file = shared.byOwnerPath(playerId, path);
                return file == null ? "client:" + path : "shared:" + file.id();
            }
            case EXTERNAL_DEVICE -> {
                ItemStack device = table.getItem(0);
                if (ExternalDevice.isFolderDevice(device)) {
                    if (ExternalDevice.read(device, path) == null) {
                        return null;
                    }
                    return "external:" + table.getBlockPos().asLong() + ":" + path;
                }
                ExternalDevice.Entry entry = deviceEntry();
                if (entry == null || !entry.listed() || !entry.name().equals(path)) {
                    return null;
                }
                return "external:" + table.getBlockPos().asLong();
            }
        }
        return null;
    }

    private byte @Nullable [] dataForKey(String key) {
        if (key.startsWith("memory:")) {
            return memory.read(key.substring("memory:".length()));
        }
        if (key.startsWith("shared:")) {
            SharedFiles.SharedFile file = shared.byId(parseId(key.substring("shared:".length())));
            return file == null ? null : file.data();
        }
        if (key.startsWith("external:")) {
            String rest = key.substring("external:".length());
            int split = rest.indexOf(':');
            String path = split < 0 ? "" : rest.substring(split + 1);
            ItemStack device = table.getItem(0);
            if (ExternalDevice.isFolderDevice(device)) {
                return ExternalDevice.read(device, path);
            }
            ExternalDevice.Entry entry = deviceEntry();
            return entry == null ? null : entry.data();
        }
        return null;
    }

    private void bumpEditorRevision(String key) {
        long revision = sessions.bumpRevision(key);
        broadcastState(key, revision, null);
    }

    private void sendState(FileSessions.Session session, long revision, @Nullable String errorKey) {
        sendState(session, revision, errorKey, false);
    }

    private void sendState(FileSessions.Session session, long revision, @Nullable String errorKey,
                           boolean reopen) {
        List<FileEditorStatePayload.EditorInfo> editors = new ArrayList<>();
        for (FileSessions.Session other : sessions.forKey(session.key())) {
            editors.add(new FileEditorStatePayload.EditorInfo(other.player(), other.playerName(),
                    other.start(), other.end(), other.textMode()));
        }
        byte[] data = dataForKey(session.key());
        boolean exists = data != null;
        send(viewer(session.player()), new FileEditorStatePayload(table.getBlockPos(), session.root(),
                session.path(), data == null ? new byte[0] : data, revision, editors,
                readOnly(session), dataMaxBytes(session),
                errorKey != null ? errorKey : (exists ? "" : "gui.elements-plus.file.error.not_found"),
                reopen));
    }

    private void broadcastState(String key, long revision) {
        broadcastState(key, revision, null);
    }

    /** 把同一文件的状态广播给所有编辑者，{@code skip} 为 null 时包括所有人。 */
    private void broadcastState(String key, long revision, @Nullable UUID skip) {
        for (FileSessions.Session session : sessions.forKey(key)) {
            if (skip != null && session.player().equals(skip)) {
                continue;
            }
            sendState(session, revision, null);
        }
    }

    private @Nullable ServerPlayer viewer(UUID playerId) {
        if (!(table.getLevel() instanceof ServerLevel level)) {
            return null;
        }
        return level.getServer().getPlayerList().getPlayer(playerId);
    }

    private void closeSession(UUID playerId, boolean notify) {
        FileSessions.Session session = sessions.remove(playerId);
        if (session == null) {
            return;
        }
        if (session.key().startsWith("shared:")) {
            UUID fileId = parseId(session.key().substring("shared:".length()));
            shared.leave(fileId, playerId);
        }
        if (notify) {
            broadcastState(session.key(), sessions.revision(session.key()));
        }
        if (session.key().startsWith("shared:")) {
            refreshAll(FileRoot.CLIENT, playerId);
            refreshAll(FileRoot.SHARED, playerId);
        }
    }

    private void closeSessionsUnder(String keyPrefix, String path) {
        for (FileSessions.Session session : sessions.all()) {
            if (session.root() == FileRoot.TABLE_MEMORY
                    && (session.path().equals(path) || FilePaths.isUnder(session.path(), path))) {
                notifyOffline(session);
                closeSession(session.player(), true);
                sessions.reset(session.key());
            }
        }
    }

    /** 外接设备变化时通知正在编辑该设备的玩家转为离线只读，或切换到新文件。 */
    public void onExternalDeviceChanged() {
        ItemStack device = table.getItem(0);
        List<FileSessions.Session> editing = new ArrayList<>();
        for (FileSessions.Session session : sessions.all()) {
            if (session.root() == FileRoot.EXTERNAL_DEVICE) {
                editing.add(session);
            }
        }
        if (ExternalDevice.isFolderDevice(device)) {
            // 文件夹整树只读：不重新打开编辑器；若旧路径仍存在，只刷新内容。
            for (FileSessions.Session session : editing) {
                byte[] data = ExternalDevice.read(device, session.path());
                if (data == null) {
                    notifyOffline(session);
                    continue;
                }
                String key = "external:" + table.getBlockPos().asLong() + ":" + session.path();
                long revision = sessions.bumpRevision(key);
                FileSessions.Session updated = new FileSessions.Session(session.player(),
                        session.playerName(), session.root(), session.path(), key,
                        session.textMode(), session.start(), session.end());
                sessions.put(updated);
                sendState(updated, revision, null, false);
            }
            refreshAll(FileRoot.EXTERNAL_DEVICE);
            return;
        }
        String key = "external:" + table.getBlockPos().asLong();
        ExternalDevice.Entry entry = deviceEntry();
        if (entry == null || !entry.listed()) {
            for (FileSessions.Session session : editing) {
                notifyOffline(session);
            }
            refreshAll(FileRoot.EXTERNAL_DEVICE);
            return;
        }
        long revision = sessions.bumpRevision(key);
        for (FileSessions.Session session : editing) {
            // 更换介质后编辑器应相当于关闭旧文件、重新打开新文件。
            FileSessions.Session updated = new FileSessions.Session(session.player(),
                    session.playerName(), session.root(), entry.name(), key,
                    session.textMode(), 0, 0);
            sessions.put(updated);
            sendState(updated, revision, null, true);
        }
        refreshAll(FileRoot.EXTERNAL_DEVICE);
    }

    /** 文件/设备消失时，让客户端保留内容并显示 [离线]。 */
    private void notifyOffline(FileSessions.Session session) {
        ServerPlayer player = viewer(session.player());
        if (player == null) {
            return;
        }
        send(player, new FileEditorStatePayload(table.getBlockPos(), session.root(), session.path(),
                new byte[0], -1, List.of(), true, 0,
                "gui.elements-plus.file.error.not_found", false));
    }

    private void remapOpenSessions(String from, String to) {
        for (FileSessions.Session session : sessions.all()) {
            if (session.root() != FileRoot.TABLE_MEMORY) {
                continue;
            }
            if (session.path().equals(from) || FilePaths.isUnder(session.path(), from)) {
                String nextPath = to + session.path().substring(from.length());
                sessions.put(new FileSessions.Session(session.player(), session.playerName(),
                        session.root(), nextPath, "memory:" + nextPath,
                        session.textMode(), session.start(), session.end()));
            }
        }
    }

    private static @Nullable UUID parseId(String text) {
        try {
            return UUID.fromString(text);
        } catch (IllegalArgumentException | NullPointerException e) {
            return null;
        }
    }

    /** 用于告警文本的路径显示。 */
    public static Component displayPath(String path) {
        return Component.literal(path).withStyle(ChatFormatting.GRAY);
    }

    /** 文件头大小（供客户端预览容量）。 */
    public static int headerSize(String name) {
        return FileHeader.sizeFor(name);
    }
}
