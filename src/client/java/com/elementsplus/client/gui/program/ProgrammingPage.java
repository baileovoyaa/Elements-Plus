package com.elementsplus.client.gui.program;

import com.elementsplus.ElementsPlus;
import com.elementsplus.ModItems;
import com.elementsplus.client.config.ClientConfig;
import com.elementsplus.client.file.ClientFileSystem;
import com.elementsplus.client.file.FileClipboard;
import com.elementsplus.client.gui.GuiUtil;
import com.elementsplus.client.screen.ExperimentTableScreen;
import com.elementsplus.core.config.ElementsPlusServerConfig;
import com.elementsplus.core.file.CircuitDiagramFile;
import com.elementsplus.core.file.ExternalDevice;
import com.elementsplus.core.file.FileEntry;
import com.elementsplus.core.file.FileHeader;
import com.elementsplus.core.file.FilePaths;
import com.elementsplus.core.file.FileRoot;
import com.elementsplus.core.file.FileText;
import com.elementsplus.core.file.ProgramAssembler;
import com.elementsplus.core.file.ProgramDisassembler;
import com.elementsplus.network.FileActionPayload;
import com.elementsplus.network.FileActionResultPayload;
import com.elementsplus.network.FileContentPayload;
import com.elementsplus.network.FileEditorActionPayload;
import com.elementsplus.network.FileEditorStatePayload;
import com.elementsplus.network.FileFolderArchivePayload;
import com.elementsplus.network.FileFolderPastePayload;
import com.elementsplus.network.FileSystemDataPayload;
import com.elementsplus.network.FileSystemRequestPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 实验桌“编程”标签页：左侧文件系统导航，右侧文件管理器 / 文件编辑器。
 */
public final class ProgrammingPage extends AbstractWidget {
    private static final int NAV_WIDTH = 79;
    private static final int NAV_ROW = 18;
    private static final int TOOLBAR_HEIGHT = 18;
    private static final int SEARCH_HEIGHT = 14;
    private static final int HEADER_HEIGHT = 11;
    private static final int TABLE_ROW = 11;
    private static final int STATUS_HEIGHT = 12;
    private static final int CONSOLE_HEADER = 11;
    private static final int CONSOLE_HEIGHT = 64;
    private static final int ICON = 16;
    /** 单个文件在客户端/服务端之间传输的最大字节数，避免超大网络包。 */
    private static final int MAX_TRANSFER_BYTES = 1024 * 1024;

    private enum Mode {MANAGER, EDITOR}

    private enum SortColumn {NAME, EXTENSION, SIZE, OWNER}

    private record EditorTarget(FileRoot root, String path, String name, boolean readOnly,
                                boolean forceHex, boolean shared) {
    }

    private record MenuEntry(Component label, Runnable action, boolean enabled) {
    }

    /** 一个待粘贴的文件。 */
    private record PasteItem(FileClipboard.Entry entry) {
    }

    private final ExperimentTableScreen screen;
    private int leftPos;
    private int topPos;
    private int imageWidth;
    private int imageHeight;
    private int pageX;
    private int pageY;
    private int pageWidth;
    private int pageHeight;

    private Mode mode = Mode.MANAGER;
    private FileRoot root = FileRoot.EXTERNAL_DEVICE;
    private String dir = "";
    private final Map<FileRoot, String> lastDirs = new EnumMap<>(FileRoot.class);
    private final Map<FileRoot, Deque<String>> backStacks = new EnumMap<>(FileRoot.class);
    private final Map<FileRoot, Deque<String>> forwardStacks = new EnumMap<>(FileRoot.class);
    private List<FileEntry> entries = new ArrayList<>();
    private final Set<String> selected = new LinkedHashSet<>();
    private String anchorPath;
    private String search = "";
    private final SingleLineInput searchInput = new SingleLineInput();
    private SortColumn sortColumn = SortColumn.NAME;
    private boolean ascending = true;
    private int tableScroll;
    /** 左侧根列表在高度不足时的滚动偏移（像素）。 */
    private int navScroll;
    private boolean managerScrollbarDrag;
    private int managerScrollbarGrabOffset = -1;
    private int managerThumbY;
    private int managerThumbHeight;
    private final Map<String, Long> folderSizes = new HashMap<>();
    private long localSizeVersion = -1;
    private final Set<String> pendingFolderSizes = new HashSet<>();

    private int memoryUsed;
    private int memoryCapacity = 32 * 1024;
    private int sharedBySelf;
    private int sharedLimit = 4;
    private final Set<String> sharedPaths = new HashSet<>();
    private final Map<String, String> editorNamesByPath = new HashMap<>();

    private boolean dragSelecting;
    private boolean dragMoved;
    private String dragPath;
    private double dragStartX;
    private double dragStartY;
    private double dragEndX;
    private double dragEndY;
    private boolean draggingFiles;
    private final List<String> dragPaths = new ArrayList<>();
    /** 本次按下是否落在容器槽位/物品栏等由 Screen 处理的区域。 */
    private boolean containerSlotPress;
    /** 本次按下是否从编辑器文本区域开始，决定拖动是否应传递给编辑器。 */
    private boolean editorMouseDown;
    private String dropTargetFolder;
    private String lastClickPath;
    private long lastClickTime;

    private final List<MenuEntry> menu = new ArrayList<>();
    private int menuX;
    private int menuY;
    private int menuWidth;
    private int pathX;
    private int pathY;
    private int pathWidth;
    private String pathText = "";
    private final Deque<ProgramDialog> dialogs = new ArrayDeque<>();
    private final List<Component> consoleLines = new ArrayList<>();
    private boolean consoleExpanded = true;
    private final List<Component> statusTooltip = new ArrayList<>();
    private boolean statusOk = true;
    private @Nullable Component transientError;
    private long errorFlashStart = -1L;
    private boolean pendingPrint;
    private boolean pendingCircuitUpload;
    private byte[] pendingPrintData;
    private boolean pendingPrintToOwnExternalDisk;
    private final List<Integer> errorLines = new ArrayList<>();
    private final Map<UUID, RemoteActivity> remoteActivity = new HashMap<>();

    private record RemoteActivity(int start, int end, long lastActive) {
    }
    private List<Component> hoverTooltip = new ArrayList<>();

    // 编辑器
    private EditorTarget editorTarget;
    private boolean editorReady;
    private final TextEditor textEditor = new TextEditor();
    private final HexEditor hexEditor = new HexEditor();
    private boolean textMode;
    private long revision = -1;
    private boolean dirty;
    private long lastEditAt;
    private long lastSentAt;
    private long lastCursorAt;
    private int lastSentCaret = -1;
    private List<FileEditorStatePayload.EditorInfo> editors = List.of();
    private int maxBytes = Integer.MAX_VALUE;
    private boolean readOnly;
    /** 文件已不存在（外接设备移除、协作关闭、外部删除）时保留内容并转为只读。 */
    private boolean offline;
    private boolean assembleOnOpen;
    private boolean disassembleOnOpen;
    private boolean printOnOpen;

    // 粘贴队列
    private final Deque<PasteItem> pasteQueue = new ArrayDeque<>();
    private String pasteTargetDir;
    private boolean pasteOverwriteAll;
    private boolean pasteSkipAll;
    private boolean pendingReadForPaste;
    private boolean pendingReadForEditor;
    private String pendingShareJoinPath;
    private @Nullable FolderPastePending pendingFolderPaste;
    private @Nullable FolderReadPending pendingFolderRead;

    private record FolderPastePending(FileClipboard.Entry source, String targetPath) {
    }

    private record FolderReadPending(FileClipboard.Entry source, String targetPath,
                                     boolean overwrite) {
    }

    public ProgrammingPage(ExperimentTableScreen screen) {
        super(0, 0, 1, 1, Component.empty());
        this.screen = screen;
        searchInput.setMaxLength(48);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }

    /* ============================================================
     *  布局
     * ============================================================ */

    public void layout(int leftPos, int topPos, int imageWidth, int imageHeight) {
        this.leftPos = leftPos;
        this.topPos = topPos;
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        this.pageX = leftPos + 5;
        this.pageY = topPos + 22;
        this.pageWidth = Math.max(120, imageWidth - 10);
        this.pageHeight = Math.max(1, imageHeight - 27);
        this.setX(pageX);
        this.setY(pageY);
        this.setWidth(pageWidth);
        this.setHeight(pageHeight);
    }

    public int deviceSlotX() {
        return leftPos + 90;
    }

    public int deviceSlotY() {
        return topPos + 26;
    }

    public int statusIconX() {
        return pageX + pageWidth - 20;
    }

    public int statusIconY() {
        return pageY + 4;
    }

    private int navX() {
        return pageX;
    }

    private int rightX() {
        return pageX + NAV_WIDTH + 5;
    }

    private int rightW() {
        return Math.max(40, pageX + pageWidth - rightX());
    }

    private int rightRight() {
        return rightX() + rightW();
    }

    private int toolbarY() {
        return pageY + 3;
    }

    private int contentTop() {
        return toolbarY() + TOOLBAR_HEIGHT + 6;
    }

    private int statusY() {
        return pageY + pageHeight - STATUS_HEIGHT;
    }

    /* ============================================================
     *  数据入口
     * ============================================================ */

    public void onFileSystemData(FileSystemDataPayload payload) {
        if (!payload.pos().equals(screen.getTablePos())) {
            return;
        }
        this.memoryUsed = payload.memoryUsed();
        this.memoryCapacity = Math.max(1, payload.memoryCapacity());
        this.sharedBySelf = payload.sharedBySelf();
        this.sharedLimit = payload.sharedLimit();
        this.sharedPaths.clear();
        this.sharedPaths.addAll(payload.sharedPaths());
        if (payload.root() == root) {
            this.editorNamesByPath.clear();
            for (FileSystemDataPayload.PathEditors editorsAt : payload.pathEditors()) {
                this.editorNamesByPath.put(editorsAt.path(), editorsAt.names());
            }
        }
        if (payload.root() == root && root == FileRoot.EXTERNAL_DEVICE
                && !dir.isEmpty()
                && !ExternalDevice.isFolderDevice(screen.experimentMenu().extraSlot.getItem())) {
            // 非文件夹外接设备没有子目录；若刚从文件夹换过来，拉回根目录避免停留在失效路径。
            navigateTo("", false);
            stack(backStacks, root).clear();
            stack(forwardStacks, root).clear();
            return;
        }
        if (payload.root() == root && payload.path().equals(dir)) {
            this.entries = payload.entries();
            applySelectionFilter();
        }
        if (root == FileRoot.CLIENT) {
            refreshLocal();
        }
    }

    public void onFileContent(FileContentPayload payload) {
        if (!payload.pos().equals(screen.getTablePos())) {
            return;
        }
        if (pendingReadForEditor && editorTarget != null && !editorReady
                && payload.root() == editorTarget.root()
                && payload.path().equals(editorTarget.path())) {
            pendingReadForEditor = false;
            initializeEditor(payload.data(), null);
            return;
        }
        if (pendingReadForPaste && pendingPasteSource != null
                && payload.root() == pendingPasteSource.root()
                && payload.path().equals(pendingPasteSource.path())) {
            pendingReadForPaste = false;
            finishPasteFromServer(payload.data());
            return;
        }
        if (payload.root() == FileRoot.CLIENT
                && payload.purpose() == FileContentPayload.Purpose.COPY) {
            // 共享文件由其他玩家修改后，拥有者客户端写回本地文件。
            ClientFileSystem.write(payload.path(), payload.data(), true);
            if (root == FileRoot.CLIENT && FilePaths.parent(payload.path()).equals(dir)) {
                refreshLocal();
            }
            if (editorTarget != null && editorTarget.root() == FileRoot.CLIENT
                    && editorTarget.path().equals(payload.path()) && editorReady && !dirty) {
                applyEditorContent(payload.data());
            }
        }
    }

    public void onEditorState(FileEditorStatePayload payload) {
        if (!payload.pos().equals(screen.getTablePos())) {
            return;
        }
        if (editorTarget == null) {
            return;
        }
        if (payload.reopen() && payload.root() == FileRoot.EXTERNAL_DEVICE
                && editorTarget.root() == FileRoot.EXTERNAL_DEVICE) {
            reopenExternalEditor(payload);
            return;
        }
        if (payload.root() != editorTarget.root()
                || !payload.path().equals(editorTarget.path())) {
            return;
        }
        if (!payload.errorKey().isEmpty()) {
            if (payload.errorKey().endsWith("not_found")) {
                if (!editorReady && !pendingReadForEditor) {
                    initializeEditor(payload.data(), null);
                }
                if (editorReady) {
                    markOffline(payload.errorKey());
                }
                this.revision = payload.revision();
                updateRemoteCursors();
                return;
            }
            if (payload.revision() < 0) {
                closeEditor(false);
                setStatus(false, Component.translatable(payload.errorKey()), true);
                return;
            }
        }
        this.editors = payload.editors();
        this.readOnly = payload.readOnly() || editorTarget.readOnly();
        this.offline = false;
        this.maxBytes = payload.maxBytes() > 0 ? payload.maxBytes() : Integer.MAX_VALUE;
        applyLimits();
        applyEditability();
        if (!editorReady) {
            if (!pendingReadForEditor) {
                initializeEditor(payload.data(), payload.errorKey().isEmpty() ? null : payload.errorKey());
            }
            // 加入已有协作时不强行改变现有模式，跟随其他编辑者。
            UUID selfId = Minecraft.getInstance().player == null ? null
                    : Minecraft.getInstance().player.getUUID();
            Boolean existingMode = null;
            for (FileEditorStatePayload.EditorInfo info : payload.editors()) {
                if (selfId != null && info.player().equals(selfId)) {
                    continue;
                }
                existingMode = info.textMode();
                break;
            }
            if (editorReady && existingMode != null && existingMode != textMode) {
                applySyncedMode(existingMode, payload.data());
            }
            this.revision = payload.revision();
            updateRemoteCursors();
            if (editorReady && syncsEditorWithServer()) {
                // 立即广播当前模式/光标，其他人无需等到第一次输入才看到。
                sendEditorMode();
                sendEditorCursor();
            }
            return;
        }
        // 其他人的模式切换同步到本地。
        UUID self = Minecraft.getInstance().player == null ? null
                : Minecraft.getInstance().player.getUUID();
        for (FileEditorStatePayload.EditorInfo info : editors) {
            if (self != null && info.player().equals(self) && info.textMode() != textMode) {
                applySyncedMode(info.textMode(), payload.data());
                this.revision = payload.revision();
                updateRemoteCursors();
                return;
            }
        }
        if (payload.revision() > revision && !dirty) {
            applyEditorContent(payload.data());
        }
        if (payload.revision() > revision) {
            this.revision = payload.revision();
        }
        updateRemoteCursors();
    }

    /** 把汇编结果写入当前打开的磁盘后，刷新编辑器内容并切到十六进制。 */
    private void refreshOwnExternalDisk(byte[] data) {
        EditorTarget target = new EditorTarget(FileRoot.EXTERNAL_DEVICE, editorTarget.path(),
                editorTarget.name(), false, true, false);
        this.offline = false;
        this.editorTarget = target;
        beginEditor(target, data);
        this.revision = -1;
        if (syncsEditorWithServer()) {
            sendEditorMode();
            sendEditorCursor();
        }
    }

    /** 外接设备被替换：相当于关闭旧文件并重新打开新文件。 */
    private void reopenExternalEditor(FileEditorStatePayload payload) {
        EditorTarget target = new EditorTarget(FileRoot.EXTERNAL_DEVICE, payload.path(),
                payload.path(), payload.readOnly(), false, false);
        this.dirty = false;
        this.offline = false;
        this.editorTarget = target;
        beginEditor(target, payload.data());
        this.editors = payload.editors();
        this.revision = payload.revision();
        updateRemoteCursors();
        if (syncsEditorWithServer()) {
            sendEditorMode();
            sendEditorCursor();
        }
    }

    public void onActionResult(FileActionResultPayload payload) {
        if (!payload.pos().equals(screen.getTablePos())) {
            return;
        }
        if (pendingFolderPaste != null) {
            FolderPastePending pending = pendingFolderPaste;
            pendingFolderPaste = null;
            if (!payload.success()) {
                setStatus(false, Component.translatable(payload.messageKey(),
                        payload.args().toArray(new Object[0])), true);
                return;
            }
            FileClipboard.Entry source = pending.source();
            if (source.root() == FileRoot.CLIENT && source.cut()) {
                ClientFileSystem.delete(source.path());
                FileClipboard.remove(source);
                if (root == FileRoot.CLIENT) {
                    refreshLocal();
                }
            }
            continuePaste();
            return;
        }
        if (pendingFolderRead != null && !payload.success()
                && payload.path().equals(pendingFolderRead.source().path())) {
            pendingFolderRead = null;
            setStatus(false, Component.translatable(payload.messageKey(),
                    payload.args().toArray(new Object[0])), true);
            continuePaste();
            return;
        }
        if (payload.success()) {
            if (pendingCircuitUpload) {
                pendingCircuitUpload = false;
                setStatus(true, Component.translatable("gui.elements-plus.file.print.success"), false);
                if (Minecraft.getInstance().player != null) {
                    Minecraft.getInstance().player.playSound(
                            SoundEvents.NOTE_BLOCK_PLING.value(), 0.8F, 1.6F);
                }
            }
            if (pendingPrint) {
                pendingPrint = false;
                setStatus(true, Component.translatable("gui.elements-plus.file.print.success"), false);
                if (Minecraft.getInstance().player != null) {
                    Minecraft.getInstance().player.playSound(
                            SoundEvents.NOTE_BLOCK_PLING.value(), 0.8F, 1.6F);
                }
                if (pendingPrintToOwnExternalDisk && pendingPrintData != null
                        && editorTarget != null && editorTarget.root() == FileRoot.EXTERNAL_DEVICE) {
                    refreshOwnExternalDisk(pendingPrintData);
                }
                pendingPrintData = null;
                pendingPrintToOwnExternalDisk = false;
            } else {
                statusOk = true;
                statusTooltip.clear();
                screen.setProgrammingStatus(true, List.of());
                transientError = null;
                errorFlashStart = -1L;
                screen.setProgrammingStatusBlinkVisible(true);
            }
            if (pendingShareJoinPath != null && pendingShareJoinPath.equals(payload.path())
                    && editorTarget != null && editorTarget.root() == FileRoot.CLIENT
                    && editorTarget.path().equals(pendingShareJoinPath)) {
                String path = pendingShareJoinPath;
                pendingShareJoinPath = null;
                this.editorTarget = new EditorTarget(FileRoot.CLIENT, editorTarget.path(),
                        editorTarget.name(), false, false, true);
                sendEditorOpen(this.editorTarget);
            }
            if (payload.path() != null && !payload.path().isEmpty()) {
                refreshListing();
            }
            return;
        }
        this.pendingShareJoinPath = null;
        this.pendingPrint = false;
        this.pendingCircuitUpload = false;
        this.pendingPrintData = null;
        this.pendingPrintToOwnExternalDisk = false;
        Component message = Component.translatable(payload.messageKey(),
                payload.args().toArray(new Object[0]));
        setStatus(false, message, true);
    }

    /** 服务端列表根节点请求。 */
    private void requestListing() {
        BlockPos pos = screen.getTablePos();
        if (pos == null) {
            return;
        }
        if (root == FileRoot.CLIENT) {
            refreshLocal();
            ClientPlayNetworking.send(new FileSystemRequestPayload(pos, FileRoot.CLIENT, dir,
                    FileSystemRequestPayload.Request.LIST));
        } else {
            ClientPlayNetworking.send(new FileSystemRequestPayload(pos, root, dir,
                    FileSystemRequestPayload.Request.LIST));
        }
    }

    private void refreshListing() {
        requestListing();
    }

    private void refreshLocal() {
        if (root != FileRoot.CLIENT) {
            return;
        }
        List<FileEntry> local = new ArrayList<>();
        for (FileEntry entry : ClientFileSystem.list(dir)) {
            local.add(new FileEntry(entry.path(), entry.name(), entry.folder(), entry.size(),
                    null, "", true, sharedPaths.contains(entry.path()),
                    editorNamesByPath.containsKey(entry.path())));
        }
        this.entries = local;
        applySelectionFilter();
        // 本地文件发生任何变化后，文件夹大小缓存必须失效并重新统计。
        localSizeVersion = ClientFileSystem.sizeVersion();
        folderSizes.clear();
        pendingFolderSizes.clear();
    }

    private void syncLocalSizeVersion() {
        if (root != FileRoot.CLIENT) {
            return;
        }
        long version = ClientFileSystem.sizeVersion();
        if (version != localSizeVersion) {
            localSizeVersion = version;
            folderSizes.clear();
            pendingFolderSizes.clear();
        }
    }

    /* ============================================================
     *  公共：状态、声音
     * ============================================================ */

    private void setStatus(boolean ok, Component message, boolean sound) {
        setStatus(ok, message, sound, true);
    }

    /**
     * @param flashError 是否让状态按钮与状态栏错误闪烁；编译错误只进控制台/状态按钮。
     */
    private void setStatus(boolean ok, Component message, boolean sound, boolean flashError) {
        this.statusOk = ok;
        this.statusTooltip.clear();
        if (message != null) {
            this.statusTooltip.add(message);
        }
        ProgramDialog activeDialog = currentDialog();
        if (activeDialog != null) {
            activeDialog.setInlineError(ok ? null : message);
        }
        screen.setProgrammingStatus(ok, statusTooltip);
        if (!ok && message != null) {
            // 只有编辑器编译错误不进入状态栏红字。
            if (flashError) {
                this.transientError = message.copy().withStyle(net.minecraft.ChatFormatting.RED);
                this.errorFlashStart = Util.getMillis();
                screen.setProgrammingStatusBlinkVisible(true);
            } else {
                this.transientError = null;
                this.errorFlashStart = -1L;
                screen.setProgrammingStatusBlinkVisible(true);
            }
            if (sound && Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.playSound(SoundEvents.NOTE_BLOCK_BASS.value(), 0.7F, 0.6F);
            }
        } else if (!ok) {
            this.transientError = null;
            this.errorFlashStart = -1L;
            screen.setProgrammingStatusBlinkVisible(true);
        } else if (ok) {
            this.transientError = null;
            this.errorFlashStart = -1L;
            screen.setProgrammingStatusBlinkVisible(true);
        }
    }

    /** 点击右上角状态按钮：清除状态与红字提示。 */
    public void clearStatus() {
        this.statusOk = true;
        this.statusTooltip.clear();
        this.transientError = null;
        this.errorFlashStart = -1L;
        screen.setProgrammingStatus(true, List.of());
        screen.setProgrammingStatusBlinkVisible(true);
    }

    private static final long ERROR_FLASH_STEP_MS = 200L;
    private static final long ERROR_FLASH_BLINK_MS = 1000L;
    private static final long ERROR_FLASH_TOTAL_MS = 2000L;

    private boolean errorFlashActive() {
        return transientError != null && errorFlashStart >= 0
                && Util.getMillis() - errorFlashStart < ERROR_FLASH_TOTAL_MS;
    }

    private boolean errorFlashVisible() {
        if (!errorFlashActive()) {
            return false;
        }
        long elapsed = Util.getMillis() - errorFlashStart;
        if (elapsed < ERROR_FLASH_BLINK_MS) {
            return (elapsed / ERROR_FLASH_STEP_MS) % 2 == 0;
        }
        // 闪烁结束后保留 1 秒再消失。
        return true;
    }

    private @Nullable Component activeTransientError() {
        return errorFlashVisible() ? transientError : null;
    }

    private void addConsole(Component line) {
        consoleLines.add(line);
        while (consoleLines.size() > 100) {
            consoleLines.remove(0);
        }
    }

    private void addErrorLines(List<ProgramAssembler.Error> errors) {
        errorLines.clear();
        for (ProgramAssembler.Error error : errors) {
            errorLines.add(error.line());
            addConsole(Component.translatable("gui.elements-plus.file.console.line",
                    error.line() + 1, error.message()));
        }
        textEditor.setHighlightLine(errors.isEmpty() ? -1 : errors.get(0).line());
        consoleExpanded = true;
    }

    private void clearErrors() {
        errorLines.clear();
        textEditor.setHighlightLine(-1);
    }

    public boolean hasErrors() {
        return !errorLines.isEmpty();
    }

    public List<Integer> errorLines() {
        return errorLines;
    }

    public boolean statusOk() {
        return statusOk;
    }

    public List<Component> statusTooltip() {
        return statusTooltip;
    }

    /* ============================================================
     *  导航
     * ============================================================ */

    private void selectRoot(FileRoot next, boolean autoOpen) {
        if (mode == Mode.EDITOR && next != root) {
            closeEditor(true);
        }
        if (next != root) {
            lastDirs.put(root, dir);
            this.root = next;
            this.dir = lastDirs.getOrDefault(next, "");
            this.search = "";
            searchInput.clear();
            this.tableScroll = 0;
            this.selected.clear();
            this.anchorPath = null;
            this.entries.clear();
        }
        requestListing();
        if (autoOpen && next == FileRoot.EXTERNAL_DEVICE) {
            openExternalDeviceIfPossible();
        }
    }

    private void navigateTo(String path, boolean record) {
        if (record) {
            stack(backStacks, root).push(dir);
            stack(forwardStacks, root).clear();
        }
        this.dir = FilePaths.normalizeDir(path);
        this.search = "";
        searchInput.clear();
        this.tableScroll = 0;
        this.selected.clear();
        this.anchorPath = null;
        requestListing();
    }

    private void goBack() {
        Deque<String> stack = stack(backStacks, root);
        if (stack.isEmpty()) {
            return;
        }
        stack(forwardStacks, root).push(dir);
        this.dir = stack.pop();
        requestListing();
    }

    private void goForward() {
        Deque<String> stack = stack(forwardStacks, root);
        if (stack.isEmpty()) {
            return;
        }
        stack(backStacks, root).push(dir);
        this.dir = stack.pop();
        requestListing();
    }

    private void goUp() {
        if (!dir.isEmpty()) {
            navigateTo(FilePaths.parent(dir), true);
        }
    }

    private static Deque<String> stack(Map<FileRoot, Deque<String>> map, FileRoot root) {
        return map.computeIfAbsent(root, key -> new ArrayDeque<>());
    }

    private void openExternalDeviceIfPossible() {
        var stack = screen.experimentMenu().extraSlot.getItem();
        ExternalDevice.Entry entry = ExternalDevice.describe(stack);
        if (entry == null || !entry.listed()) {
            return;
        }
        if (stack.is(ModItems.CIRCUIT_DIAGRAM)) {
            return;
        }
        boolean hex = stack.is(ModItems.DISK) || stack.is(ModItems.PUNCHED_FILE);
        openTarget(new EditorTarget(FileRoot.EXTERNAL_DEVICE, entry.name(), entry.name(),
                !entry.writable(), hex, false));
    }

    /* ============================================================
     *  排序 / 过滤
     * ============================================================ */

    private void applySelectionFilter() {
        Set<String> available = new HashSet<>();
        for (FileEntry entry : entries) {
            available.add(entry.path());
        }
        selected.removeIf(path -> !available.contains(path));
        if (editorTarget != null) {
            textEditor.setMaxBytes(maxBytes);
        }
    }

    private List<FileEntry> visibleEntries() {
        String query = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        List<FileEntry> result = new ArrayList<>();
        for (FileEntry entry : entries) {
            if (query.isEmpty() || entry.name().toLowerCase(Locale.ROOT).contains(query)) {
                result.add(entry);
            }
        }
        Comparator<FileEntry> comparator = switch (sortColumn) {
            case NAME -> Comparator.comparing(entry -> entry.name().toLowerCase(Locale.ROOT));
            case EXTENSION -> Comparator.comparing(entry -> entry.extension().toLowerCase(Locale.ROOT));
            case SIZE -> Comparator.comparingInt(FileEntry::size);
            case OWNER -> Comparator.comparing(entry -> entry.ownerName().toLowerCase(Locale.ROOT));
        };
        if (!ascending) {
            comparator = comparator.reversed();
        }
        comparator = comparator.thenComparing(entry -> entry.name().toLowerCase(Locale.ROOT));
        result.sort(Comparator.comparing((FileEntry entry) -> entry.folder()).reversed().thenComparing(comparator));
        return result;
    }

    /* ============================================================
     *  文件操作入口
     * ============================================================ */

    private @Nullable BlockPos tablePos() {
        return screen.getTablePos();
    }

    private void sendAction(FileActionPayload.Action action, List<String> paths, String target,
                            String name, byte[] data, boolean copy, boolean overwrite) {
        BlockPos pos = tablePos();
        if (pos == null) {
            return;
        }
        if (data != null && data.length > MAX_TRANSFER_BYTES) {
            setStatus(false, Component.translatable("gui.elements-plus.file.error.too_large"), true);
            return;
        }
        ClientPlayNetworking.send(new FileActionPayload(pos, root, action, paths, target,
                name, data, copy, overwrite));
    }

    private void createFile() {
        if (!root.canCreate()) {
            setStatus(false, Component.translatable("gui.elements-plus.file.error.not_allowed"), true);
            return;
        }
        SingleLineInput input = new SingleLineInput();
        input.setMaxLength(FilePaths.MAX_NAME_LENGTH);
        showDialog(new ProgramDialog(Component.translatable("gui.elements-plus.file.dialog.new_file"),
                Component.translatable("gui.elements-plus.file.dialog.name"), false, input,
                Component.translatable("gui.elements-plus.file.dialog.name_hint"))
                .button(Component.translatable("gui.elements-plus.toolbox.dialog.cancel"), this::closeDialog)
                .button(Component.translatable("gui.elements-plus.toolbox.dialog.ok"), () -> {
                    String name = FilePaths.validateName(input.getValue());
                    if (name == null) {
                        setStatus(false, Component.translatable("gui.elements-plus.file.error.invalid_name"), true);
                        return;
                    }
                    closeDialog();
                    if (root == FileRoot.CLIENT) {
                        var result = ClientFileSystem.write(FilePaths.join(dir, name), new byte[0], false);
                        if (!result.success()) {
                            setStatus(false, Component.translatable(result.failureKey()), true);
                        }
                        refreshLocal();
                    } else {
                        sendAction(FileActionPayload.Action.CREATE_FILE, List.of(),
                                dir, name, new byte[0], false, false);
                    }
                }));
    }

    private void createFolder() {
        if (!root.canCreate()) {
            setStatus(false, Component.translatable("gui.elements-plus.file.error.not_allowed"), true);
            return;
        }
        SingleLineInput input = new SingleLineInput();
        input.setMaxLength(FilePaths.MAX_NAME_LENGTH);
        showDialog(new ProgramDialog(Component.translatable("gui.elements-plus.file.dialog.new_folder"),
                Component.translatable("gui.elements-plus.file.dialog.name"), false, input,
                Component.translatable("gui.elements-plus.file.dialog.name_hint"))
                .button(Component.translatable("gui.elements-plus.toolbox.dialog.cancel"), this::closeDialog)
                .button(Component.translatable("gui.elements-plus.toolbox.dialog.ok"), () -> {
                    String name = FilePaths.validateName(input.getValue());
                    if (name == null) {
                        setStatus(false, Component.translatable("gui.elements-plus.file.error.invalid_name"), true);
                        return;
                    }
                    closeDialog();
                    if (root == FileRoot.CLIENT) {
                        var result = ClientFileSystem.createFolder(FilePaths.join(dir, name));
                        if (!result.success()) {
                            setStatus(false, Component.translatable(result.failureKey()), true);
                        }
                        refreshLocal();
                    } else {
                        sendAction(FileActionPayload.Action.CREATE_FOLDER, List.of(),
                                dir, name, new byte[0], false, false);
                    }
                }));
    }

    private void renameSelected() {
        FileEntry entry = singleSelected();
        if (entry == null || !root.canCreate()) {
            return;
        }
        SingleLineInput input = new SingleLineInput();
        input.setMaxLength(FilePaths.MAX_NAME_LENGTH);
        input.setValue(entry.name());
        showDialog(new ProgramDialog(Component.translatable("gui.elements-plus.file.dialog.rename"),
                Component.translatable("gui.elements-plus.file.dialog.new_name"), false, input,
                Component.translatable("gui.elements-plus.file.dialog.name_hint"))
                .button(Component.translatable("gui.elements-plus.toolbox.dialog.cancel"), this::closeDialog)
                .button(Component.translatable("gui.elements-plus.toolbox.dialog.ok"), () -> {
                    String name = FilePaths.validateName(input.getValue());
                    if (name == null) {
                        setStatus(false, Component.translatable("gui.elements-plus.file.error.invalid_name"), true);
                        return;
                    }
                    closeDialog();
                    if (root == FileRoot.CLIENT) {
                        if (!ClientFileSystem.rename(entry.path(), name)) {
                            setStatus(false, Component.translatable("gui.elements-plus.file.error.already_exists"), true);
                        }
                        refreshLocal();
                    } else {
                        sendAction(FileActionPayload.Action.RENAME, List.of(entry.path()),
                                "", name, new byte[0], false, false);
                    }
                }));
    }

    private void deleteSelected() {
        if (!root.canCreate()) {
            return;
        }
        List<FileEntry> targets = selectedEntries();
        if (targets.isEmpty()) {
            return;
        }
        Component message = targets.size() == 1
                ? Component.translatable("gui.elements-plus.file.dialog.delete_one", targets.get(0).name())
                : Component.translatable("gui.elements-plus.file.dialog.delete_many", targets.size());
        showDialog(new ProgramDialog(Component.translatable("gui.elements-plus.file.dialog.delete"),
                message, true, null)
                .button(Component.translatable("gui.elements-plus.toolbox.dialog.cancel"), this::closeDialog)
                .button(Component.translatable("gui.elements-plus.file.dialog.delete_confirm"), () -> {
                    closeDialog();
                    List<String> paths = new ArrayList<>();
                    for (FileEntry entry : targets) {
                        paths.add(entry.path());
                    }
                    if (root == FileRoot.CLIENT) {
                        for (String path : paths) {
                            ClientFileSystem.delete(path);
                        }
                        refreshLocal();
                    } else {
                        sendAction(FileActionPayload.Action.DELETE, paths, "", "",
                                new byte[0], false, false);
                    }
                    selected.clear();
                }).confirmOnEnter());
    }

    private void copySelected(boolean cut) {
        if (cut && !root.canCreate()) {
            return;
        }
        List<FileEntry> targets = selectedEntries();
        if (targets.isEmpty()) {
            return;
        }
        List<FileClipboard.Entry> clip = new ArrayList<>();
        for (FileEntry entry : targets) {
            clip.add(new FileClipboard.Entry(root, entry.path(), entry.name(), entry.folder(), cut));
        }
        if (cut) {
            FileClipboard.cut(clip);
        } else {
            FileClipboard.copy(clip);
        }
    }

    private void startPaste() {
        startPaste(dir);
    }

    private void startPaste(String targetDir) {
        if (!root.canCreate() || FileClipboard.isEmpty()) {
            return;
        }
        this.pasteTargetDir = FilePaths.normalizeDir(targetDir);
        this.pasteQueue.clear();
        for (FileClipboard.Entry entry : FileClipboard.entries()) {
            pasteQueue.add(new PasteItem(entry));
        }
        this.pasteOverwriteAll = false;
        this.pasteSkipAll = false;
        continuePaste();
    }

    private void continuePaste() {
        while (!pasteQueue.isEmpty()) {
            PasteItem item = pasteQueue.peek();
            String targetPath = FilePaths.join(pasteTargetDir, item.entry().name());
            if (item.entry().folder() && item.entry().root() != root
                    && !canCrossRootFolder(item.entry())) {
                pasteQueue.poll();
                setStatus(false, Component.translatable(
                        "gui.elements-plus.file.error.folder_cross_root"), true);
                continue;
            }
            boolean exists = targetExists(targetPath);
            if (exists && !pasteOverwriteAll && !pasteSkipAll) {
                boolean multiple = pasteQueue.size() > 1;
                ProgramDialog dialog = new ProgramDialog(
                        Component.translatable("gui.elements-plus.file.dialog.overwrite_title"),
                        Component.translatable("gui.elements-plus.file.dialog.overwrite", targetPath),
                        true, null)
                        .button(Component.translatable("gui.elements-plus.file.dialog.cancel"), () -> {
                            closeDialog();
                            pasteQueue.clear();
                        });
                if (multiple) {
                    dialog.button(Component.translatable("gui.elements-plus.file.dialog.overwrite_all"), () -> {
                        closeDialog();
                        pasteOverwriteAll = true;
                        continuePaste();
                    }).button(Component.translatable("gui.elements-plus.file.dialog.skip_all"), () -> {
                        closeDialog();
                        pasteSkipAll = true;
                        continuePaste();
                    });
                }
                if (pasteQueue.size() == 1) {
                    dialog.button(Component.translatable("gui.elements-plus.file.dialog.rename"), () -> {
                        closeDialog();
                        promptPasteRename(item);
                    });
                }
                dialog.button(Component.translatable("gui.elements-plus.file.dialog.overwrite_confirm"), () -> {
                    closeDialog();
                    pasteQueue.poll();
                    performPaste(item, targetPath, true);
                });
                dialog.button(Component.translatable("gui.elements-plus.file.dialog.skip"), () -> {
                    closeDialog();
                    pasteQueue.poll();
                    continuePaste();
                });
                showDialog(dialog);
                return;
            }
            pasteQueue.poll();
            if (exists && pasteSkipAll) {
                continue;
            }
            performPaste(item, targetPath, exists);
            return;
        }
        refreshLocal();
    }

    private boolean canCrossRootFolder(FileClipboard.Entry source) {
        if (!source.folder() || source.root() == root) {
            return false;
        }
        if (root == FileRoot.EXTERNAL_DEVICE) {
            return false;
        }
        if (source.root() == FileRoot.EXTERNAL_DEVICE) {
            return root == FileRoot.TABLE_MEMORY || root == FileRoot.CLIENT;
        }
        if (source.root() == FileRoot.CLIENT) {
            return root == FileRoot.TABLE_MEMORY;
        }
        if (source.root() == FileRoot.TABLE_MEMORY) {
            return root == FileRoot.CLIENT;
        }
        return false;
    }

    private boolean targetExists(String path) {        if (path == null) {
            return false;
        }
        if (root == FileRoot.CLIENT) {
            return ClientFileSystem.exists(path);
        }
        for (FileEntry entry : entries) {
            if (entry.path().equals(path)) {
                return true;
            }
        }
        return false;
    }

    private void promptPasteRename(PasteItem item) {
        SingleLineInput input = new SingleLineInput();
        input.setMaxLength(FilePaths.MAX_NAME_LENGTH);
        input.setValue(suggestPasteName(item.entry().name()));
        showDialog(new ProgramDialog(Component.translatable("gui.elements-plus.file.dialog.rename"),
                Component.translatable("gui.elements-plus.file.dialog.new_name"), false, input,
                Component.translatable("gui.elements-plus.file.dialog.name_hint"))
                .button(Component.translatable("gui.elements-plus.toolbox.dialog.cancel"), this::closeDialog)
                .button(Component.translatable("gui.elements-plus.toolbox.dialog.ok"), () -> {
                    String name = FilePaths.validateName(input.getValue());
                    if (name == null) {
                        setStatus(false, Component.translatable("gui.elements-plus.file.error.invalid_name"), true);
                        return;
                    }
                    String targetPath = FilePaths.join(pasteTargetDir, name);
                    if (targetExists(targetPath)) {
                        setStatus(false, Component.translatable(
                                "gui.elements-plus.file.error.already_exists"), true);
                        return;
                    }
                    closeDialog();
                    pasteQueue.clear();
                    performPaste(item, targetPath, false);
                }));
    }

    /** 为重名文件生成一个默认的“name (1).ext”名称。 */
    private String suggestPasteName(String name) {
        String extension = FilePaths.extension(name);
        String base = extension.isEmpty() ? name : name.substring(0, name.length() - extension.length() - 1);
        for (int index = 1; index < 1000; index++) {
            String candidate = base + " (" + index + ")" + (extension.isEmpty() ? "" : "." + extension);
            if (!targetExists(FilePaths.join(pasteTargetDir, candidate))) {
                return candidate;
            }
        }
        return name;
    }

    private void performPaste(PasteItem item, String targetPath, boolean overwrite) {
        FileClipboard.Entry source = item.entry();
        if (source.folder()) {
            if (source.root() == root) {
                if (root == FileRoot.CLIENT) {
                    if (!ClientFileSystem.transferTo(source.path(), targetPath,
                            !source.cut(), overwrite)) {
                        setStatus(false, Component.translatable(
                                overwrite && targetPath.equals(source.path())
                                        ? "gui.elements-plus.file.error.into_self"
                                        : "gui.elements-plus.file.error.into_self"), true);
                    }
                    refreshLocal();
                    if (source.cut()) {
                        FileClipboard.remove(source);
                    }
                } else if (root == FileRoot.TABLE_MEMORY) {
                    sendAction(FileActionPayload.Action.TRANSFER_TO, List.of(source.path()),
                            targetPath, "", new byte[0], !source.cut(), overwrite);
                    if (source.cut()) {
                        FileClipboard.remove(source);
                    }
                }
                continuePaste();
                return;
            }
            performCrossRootFolderPaste(source, targetPath, overwrite);
            return;
        }
        byte[] data = readSource(source);
        if (data == null && (source.root() == FileRoot.CLIENT
                || source.root() == FileRoot.EXTERNAL_DEVICE)) {
            pasteQueue.poll();
            setStatus(false, Component.translatable("gui.elements-plus.file.error.not_found"), true);
            continuePaste();
            return;
        }
        if (data != null && root != FileRoot.CLIENT && data.length > MAX_TRANSFER_BYTES) {
            pasteQueue.poll();
            setStatus(false, Component.translatable("gui.elements-plus.file.error.too_large"), true);
            continuePaste();
            return;
        }
        if (data == null) {
            // 需要从服务端读取（实验桌内存 / 共享空间）。
            pendingPasteSource = source;
            pendingReadForPaste = true;
            BlockPos pos = tablePos();
            if (pos != null) {
                ClientPlayNetworking.send(new FileSystemRequestPayload(pos, source.root(),
                        source.path(), FileSystemRequestPayload.Request.READ));
            } else {
                pendingReadForPaste = false;
            }
            return;
        }
        writePasted(source, targetPath, data, overwrite);
    }

    private FileClipboard.Entry pendingPasteSource;

    private void performCrossRootFolderPaste(FileClipboard.Entry source, String targetPath,
                                             boolean overwrite) {
        if (!canCrossRootFolder(source)) {
            setStatus(false, Component.translatable(
                    "gui.elements-plus.file.error.folder_cross_root"), true);
            continuePaste();
            return;
        }
        if (source.root() == FileRoot.TABLE_MEMORY) {
            BlockPos pos = tablePos();
            if (pos == null) {
                continuePaste();
                return;
            }
            pendingFolderRead = new FolderReadPending(source, targetPath, overwrite);
            ClientPlayNetworking.send(new FileSystemRequestPayload(pos, source.root(),
                    source.path(), FileSystemRequestPayload.Request.FOLDER));
            return;
        }
        List<FileFolderArchivePayload.Entry> archive = source.root() == FileRoot.EXTERNAL_DEVICE
                ? collectExternalFolder(source.path()) : collectClientFolder(source.path());
        if (archive == null) {
            setStatus(false, Component.translatable(
                    "gui.elements-plus.file.error.not_found"), true);
            continuePaste();
            return;
        }
        if (root == FileRoot.CLIENT) {
            if (!writeFolderArchiveToClient(targetPath, archive, overwrite)) {
                setStatus(false, Component.translatable(
                        "gui.elements-plus.file.error.invalid_path"), true);
            }
            continuePaste();
            return;
        }
        if (root == FileRoot.TABLE_MEMORY) {
            int required = requiredMemoryBytes(targetPath, archive);
            boolean replacing = overwrite && targetExists(targetPath);
            if (!replacing && required > Math.max(0, memoryCapacity - memoryUsed)) {
                setStatus(false, Component.translatable(
                        "gui.elements-plus.file.error.not_enough_space"), true);
                continuePaste();
                return;
            }
            BlockPos pos = tablePos();
            if (pos == null) {
                continuePaste();
                return;
            }
            pendingFolderPaste = new FolderPastePending(source, targetPath);
            ClientPlayNetworking.send(new FileFolderPastePayload(pos, FilePaths.parent(targetPath),
                    FilePaths.name(targetPath), archive, overwrite));
            return;
        }
        setStatus(false, Component.translatable(
                "gui.elements-plus.file.error.not_allowed"), true);
        continuePaste();
    }

    private void finishPasteFromServer(byte[] data) {
        FileClipboard.Entry source = pendingPasteSource;
        pendingPasteSource = null;
        if (source == null) {
            continuePaste();
            return;
        }
        String targetPath = FilePaths.join(pasteTargetDir, source.name());
        writePasted(source, targetPath, data, targetExists(targetPath) || pasteOverwriteAll);
    }

    private void writePasted(FileClipboard.Entry source, String targetPath, byte[] data,
                             boolean overwrite) {
        if (root == FileRoot.CLIENT) {
            var result = ClientFileSystem.write(targetPath, data, overwrite);
            if (!result.success()) {
                setStatus(false, Component.translatable(result.failureKey()), true);
            } else {
                afterSinglePaste(source);
            }
            refreshLocal();
            continuePaste();
            return;
        }
        if (root == FileRoot.TABLE_MEMORY) {
            sendAction(FileActionPayload.Action.WRITE_FILE, List.of(targetPath), "",
                    "", data, false, overwrite);
            afterSinglePaste(source);
            continuePaste();
            return;
        }
        setStatus(false, Component.translatable("gui.elements-plus.file.error.not_allowed"), true);
        continuePaste();
    }

    private void afterSinglePaste(FileClipboard.Entry source) {
        if (!source.cut()) {
            return;
        }
        FileClipboard.remove(source);
        if (source.root() == FileRoot.CLIENT) {
            ClientFileSystem.delete(source.path());
            if (root == FileRoot.CLIENT) {
                refreshLocal();
            }
        } else if (source.root() == FileRoot.TABLE_MEMORY) {
            BlockPos pos = tablePos();
            if (pos != null) {
                ClientPlayNetworking.send(new FileActionPayload(pos, FileRoot.TABLE_MEMORY,
                        FileActionPayload.Action.DELETE, List.of(source.path()), "", "",
                        new byte[0], false, false));
            }
        }
    }

    private byte @Nullable [] readSource(FileClipboard.Entry source) {
        if (source.root() == FileRoot.CLIENT) {
            return ClientFileSystem.read(source.path());
        }
        if (source.root() == FileRoot.EXTERNAL_DEVICE) {
            return ExternalDevice.read(screen.experimentMenu().extraSlot.getItem(), source.path());
        }
        return null;
    }

    /** 递归读取客户端本地文件夹，返回相对源文件夹的条目。 */
    private @Nullable List<FileFolderArchivePayload.Entry> collectClientFolder(String sourcePath) {
        if (!ClientFileSystem.isFolder(sourcePath)) {
            return null;
        }
        List<FileFolderArchivePayload.Entry> entries = new ArrayList<>();
        return collectClientFolder(sourcePath, sourcePath, entries) ? entries : null;
    }

    private boolean collectClientFolder(String basePath, String dir,
                                        List<FileFolderArchivePayload.Entry> entries) {
        for (FileEntry entry : ClientFileSystem.list(dir)) {
            String relative = relativePath(basePath, entry.path());
            if (entry.folder()) {
                entries.add(new FileFolderArchivePayload.Entry(relative, true, new byte[0]));
                if (!collectClientFolder(basePath, entry.path(), entries)) {
                    return false;
                }
            } else {
                byte[] data = ClientFileSystem.read(entry.path());
                if (data == null) {
                    return false;
                }
                entries.add(new FileFolderArchivePayload.Entry(relative, false, data));
            }
        }
        return true;
    }

    /** 递归读取外接设备文件夹物品。 */
    private @Nullable List<FileFolderArchivePayload.Entry> collectExternalFolder(String sourcePath) {
        ItemStack device = screen.experimentMenu().extraSlot.getItem();
        if (!ExternalDevice.isFolderDevice(device)) {
            return null;
        }
        List<FileFolderArchivePayload.Entry> entries = new ArrayList<>();
        return collectExternalFolder(device, sourcePath, sourcePath, entries) ? entries : null;
    }

    private boolean collectExternalFolder(ItemStack device, String basePath, String dir,
                                          List<FileFolderArchivePayload.Entry> entries) {
        for (FileEntry entry : ExternalDevice.list(device, dir)) {
            String relative = relativePath(basePath, entry.path());
            if (entry.folder()) {
                entries.add(new FileFolderArchivePayload.Entry(relative, true, new byte[0]));
                if (!collectExternalFolder(device, basePath, entry.path(), entries)) {
                    return false;
                }
            } else {
                byte[] data = ExternalDevice.read(device, entry.path());
                if (data == null) {
                    return false;
                }
                entries.add(new FileFolderArchivePayload.Entry(relative, false, data));
            }
        }
        return true;
    }

    private static String relativePath(String base, String path) {
        if (base.isEmpty()) {
            return path;
        }
        return path.startsWith(base + "/") ? path.substring(base.length() + 1) : path;
    }

    private int requiredMemoryBytes(String targetPath, List<FileFolderArchivePayload.Entry> entries) {
        int total = 0;
        for (FileFolderArchivePayload.Entry entry : entries) {
            if (entry.folder()) {
                continue;
            }
            String path = FilePaths.join(targetPath, entry.path());
            byte[] data = entry.data() == null ? new byte[0] : entry.data();
            total += FileHeader.sizeFor(FilePaths.name(path)) + data.length;
        }
        return total;
    }

    private boolean writeFolderArchiveToClient(String targetPath,
                                               List<FileFolderArchivePayload.Entry> entries,
                                               boolean overwrite) {
        if (ClientFileSystem.exists(targetPath) && !overwrite) {
            return false;
        }
        if (overwrite) {
            ClientFileSystem.delete(targetPath);
        }
        if (!ClientFileSystem.isFolder(targetPath)) {
            var result = ClientFileSystem.createFolder(targetPath);
            if (!result.success() && !ClientFileSystem.isFolder(targetPath)) {
                return false;
            }
        }
        for (FileFolderArchivePayload.Entry entry : entries) {
            if (!entry.folder()) {
                continue;
            }
            String path = FilePaths.join(targetPath, entry.path());
            if (!ClientFileSystem.isFolder(path) && !ClientFileSystem.createFolder(path).success()) {
                return false;
            }
        }
        for (FileFolderArchivePayload.Entry entry : entries) {
            if (entry.folder()) {
                continue;
            }
            String path = FilePaths.join(targetPath, entry.path());
            if (!ClientFileSystem.write(path, entry.data(), false).success()) {
                return false;
            }
        }
        refreshLocal();
        return true;
    }

    public void onFolderArchive(FileFolderArchivePayload payload) {
        if (!payload.pos().equals(screen.getTablePos())
                || pendingFolderRead == null
                || payload.root() != pendingFolderRead.source().root()
                || !payload.path().equals(pendingFolderRead.source().path())) {
            return;
        }
        FolderReadPending pending = pendingFolderRead;
        pendingFolderRead = null;
        if (!writeFolderArchiveToClient(pending.targetPath(), payload.entries(),
                pending.overwrite())) {
            setStatus(false, Component.translatable(
                    "gui.elements-plus.file.error.invalid_path"), true);
            continuePaste();
            return;
        }
        FileClipboard.Entry source = pending.source();
        if (source.cut()) {
            FileClipboard.remove(source);
            BlockPos pos = tablePos();
            if (pos != null) {
                ClientPlayNetworking.send(new FileActionPayload(pos, FileRoot.TABLE_MEMORY,
                        FileActionPayload.Action.DELETE, List.of(source.path()), "", "",
                        new byte[0], false, false));
            }
        }
        continuePaste();
    }

    private void transferByDrag(String sourcePath, String targetDir, boolean copy) {
        if (root == FileRoot.CLIENT) {
            if (!ClientFileSystem.transfer(sourcePath, targetDir, copy, false)) {
                setStatus(false, Component.translatable("gui.elements-plus.file.error.already_exists"), true);
            }
            refreshLocal();
        } else if (root == FileRoot.TABLE_MEMORY) {
            sendAction(FileActionPayload.Action.TRANSFER, List.of(sourcePath), targetDir,
                    "", new byte[0], copy, false);
        }
    }

    /* ============================================================
     *  选择
     * ============================================================ */

    private List<FileEntry> selectedEntries() {
        List<FileEntry> result = new ArrayList<>();
        for (FileEntry entry : visibleEntries()) {
            if (selected.contains(entry.path())) {
                result.add(entry);
            }
        }
        return result;
    }

    private @Nullable FileEntry singleSelected() {
        List<FileEntry> selectedEntries = selectedEntries();
        return selectedEntries.size() == 1 ? selectedEntries.get(0) : null;
    }

    private void clickRow(FileEntry entry, int button, boolean ctrl, boolean shift) {
        if (button == 0) {
            if (ctrl) {
                if (!selected.remove(entry.path())) {
                    selected.add(entry.path());
                }
                anchorPath = entry.path();
            } else if (shift && anchorPath != null) {
                selectRange(anchorPath, entry.path());
            } else {
                selected.clear();
                selected.add(entry.path());
                anchorPath = entry.path();
            }
            long now = Util.getMillis();
            if (now - lastClickTime < 350 && entry.path().equals(lastClickPath)) {
                lastClickPath = null;
                openEntry(entry);
                return;
            }
            lastClickPath = entry.path();
            lastClickTime = now;
        }
    }

    private void selectRange(String from, String to) {
        List<FileEntry> visible = visibleEntries();
        int start = -1;
        int end = -1;
        for (int i = 0; i < visible.size(); i++) {
            if (visible.get(i).path().equals(from)) {
                start = i;
            }
            if (visible.get(i).path().equals(to)) {
                end = i;
            }
        }
        if (start < 0 || end < 0) {
            return;
        }
        selected.clear();
        for (int i = Math.min(start, end); i <= Math.max(start, end); i++) {
            selected.add(visible.get(i).path());
        }
    }

    private void selectAll() {
        for (FileEntry entry : visibleEntries()) {
            selected.add(entry.path());
        }
        if (anchorPath == null && !entries.isEmpty()) {
            anchorPath = entries.get(0).path();
        }
    }

    private void openEntry(FileEntry entry) {
        if (entry.folder()) {
            navigateTo(entry.path(), true);
            return;
        }
        if (root == FileRoot.EXTERNAL_DEVICE) {
            ItemStack device = screen.experimentMenu().extraSlot.getItem();
            boolean forceHex = device.is(ModItems.DISK)
                    || device.is(ModItems.PUNCHED_FILE)
                    || (ExternalDevice.isFolderDevice(device)
                    && "bin".equalsIgnoreCase(entry.extension()));
            openTarget(new EditorTarget(root, entry.path(), entry.name(), !entry.editable(),
                    forceHex, false));
            return;
        }
        if (root == FileRoot.SHARED) {
            openTarget(new EditorTarget(root, entry.path(), entry.name(), false, false, true));
            return;
        }
        if (root == FileRoot.CLIENT && "dat".equalsIgnoreCase(entry.extension())
                && warnDatBeforeOpen(entry)) {
            return;
        }
        openTarget(new EditorTarget(root, entry.path(), entry.name(), false, false,
                root == FileRoot.CLIENT && sharedPaths.contains(entry.path())));
    }

    /** 本地 .dat 文件打开前提示 NBT 损坏风险。 */
    private boolean warnDatBeforeOpen(FileEntry entry) {
        showDialog(new ProgramDialog(
                Component.translatable("gui.elements-plus.file.dialog.dat_title"),
                Component.translatable("gui.elements-plus.file.dialog.dat_warning"), true, null)
                .button(Component.translatable("gui.elements-plus.toolbox.dialog.cancel"), this::closeDialog)
                .button(Component.translatable("gui.elements-plus.file.dialog.continue"), () -> {
                    closeDialog();
                    openTarget(new EditorTarget(root, entry.path(), entry.name(), false,
                            false, sharedPaths.contains(entry.path())));
                }));
        return true;
    }

    /* ============================================================
     *  右键菜单
     * ============================================================ */

    private void openContextMenu(double mouseX, double mouseY) {
        menu.clear();
        FileEntry row = entryAt(mouseX, mouseY);
        if (row != null && !selected.contains(row.path())) {
            selected.clear();
            selected.add(row.path());
            anchorPath = row.path();
        }
        List<FileEntry> targets = selectedEntries();
        if (row != null) {
            boolean folders = targets.stream().anyMatch(FileEntry::folder);
            boolean files = targets.stream().anyMatch(entry -> !entry.folder());
            if (targets.size() == 1) {
                FileEntry entry = targets.get(0);
                menu.add(new MenuEntry(Component.translatable("gui.elements-plus.file.menu.open"),
                        () -> openEntry(entry), true));
                if (!entry.folder() && root.canCreate()) {
                    menu.add(new MenuEntry(Component.translatable("gui.elements-plus.file.menu.assemble"),
                            () -> openAndAssemble(entry), true));
                    menu.add(new MenuEntry(Component.translatable("gui.elements-plus.file.menu.disassemble"),
                            () -> openAndDisassemble(entry), entry.size() <= 256));
                    menu.add(new MenuEntry(Component.translatable("gui.elements-plus.file.menu.print"),
                            () -> openAndPrint(entry), true));
                }
                if (!entry.folder() && root == FileRoot.CLIENT) {
                    boolean shared = sharedPaths.contains(entry.path());
                    menu.add(new MenuEntry(Component.translatable(shared
                                    ? "gui.elements-plus.file.menu.unshare"
                                    : "gui.elements-plus.file.menu.share"),
                            () -> toggleShare(entry), true));
                }
                if (entry.folder() && root.canCreate()) {
                    menu.add(new MenuEntry(Component.translatable("gui.elements-plus.file.menu.paste_into"),
                            () -> startPaste(entry.path()), !FileClipboard.isEmpty()));
                }
                if (root.canCreate()) {
                    menu.add(new MenuEntry(Component.translatable("gui.elements-plus.file.menu.rename"),
                            this::renameSelected, true));
                }
            } else {
                if (files && root == FileRoot.CLIENT) {
                    int fileCount = 0;
                    for (FileEntry target : targets) {
                        if (!target.folder()) {
                            fileCount++;
                        }
                    }
                    boolean canShare = sharedBySelf + fileCount <= sharedLimit;
                    menu.add(new MenuEntry(Component.translatable("gui.elements-plus.file.menu.share"),
                            this::shareSelected, canShare));
                }
            }
            if (root != FileRoot.EXTERNAL_DEVICE || targets.size() > 0) {
                menu.add(new MenuEntry(Component.translatable("gui.elements-plus.file.menu.copy"),
                        () -> copySelected(false), true));
                if (root.canCreate()) {
                    menu.add(new MenuEntry(Component.translatable("gui.elements-plus.file.menu.cut"),
                            () -> copySelected(true), true));
                    // 单个文件夹的“粘贴到文件夹内”已经单独提供，不再显示粘贴到同级目录。
                    if (!(targets.size() == 1 && targets.get(0).folder())) {
                        menu.add(new MenuEntry(Component.translatable("gui.elements-plus.file.menu.paste"),
                                this::startPaste, !FileClipboard.isEmpty()));
                    }
                    menu.add(new MenuEntry(Component.translatable("gui.elements-plus.file.menu.delete"),
                            this::deleteSelected, true));
                }
            }
            if (root == FileRoot.SHARED && targets.size() == 1) {
                UUID self = Minecraft.getInstance().player == null ? null
                        : Minecraft.getInstance().player.getUUID();
                boolean owner = self != null && targets.get(0).ownerId() != null
                        && targets.get(0).ownerId().equals(self);
                if (owner) {
                    menu.add(new MenuEntry(Component.translatable("gui.elements-plus.file.menu.unshare"),
                            () -> unshareShared(targets.get(0)), true));
                }
            }
        } else {
            if (root.canCreate()) {
                menu.add(new MenuEntry(Component.translatable("gui.elements-plus.file.menu.new_file"),
                        this::createFile, true));
                menu.add(new MenuEntry(Component.translatable("gui.elements-plus.file.menu.new_folder"),
                        this::createFolder, true));
            }
            if (root.canCreate()) {
                menu.add(new MenuEntry(Component.translatable("gui.elements-plus.file.menu.paste"),
                        this::startPaste, !FileClipboard.isEmpty()));
            }
        }
        if (menu.isEmpty()) {
            return;
        }
        updateMenuWidth();
        this.menuX = (int) Math.max(pageX + 2, Math.min(mouseX, pageX + pageWidth - menuWidth - 4));
        this.menuY = (int) Math.max(pageY + 2, Math.min(mouseY, pageY + pageHeight - menu.size() * 12 - 6));
    }

    private static String fitPathStart(Font font, String text, int width) {
        if (font.width(text) <= width) {
            return text;
        }
        String ellipsis = "...";
        int available = width - font.width(ellipsis);
        if (available <= 0) {
            // 空间连省略号都放不下时至少显示末尾字符，避免整段路径只剩“...”。
            int index = text.length();
            while (index > 0 && font.width(text.substring(index - 1)) > width) {
                index--;
            }
            return text.substring(index);
        }
        int index = text.length();
        while (index > 0 && font.width(text.substring(index - 1)) <= available) {
            index--;
        }
        if (index <= 0) {
            // 省略号之后放不下任何字符时，退回只显示末尾字符。
            int tail = text.length();
            while (tail > 0 && font.width(text.substring(tail - 1)) > width) {
                tail--;
            }
            return text.substring(tail);
        }
        return ellipsis + text.substring(index);
    }

    /** 点击工具栏路径：列出当前目录的各级父目录，点击直接跳转。 */
    private void openPathMenu() {
        menu.clear();
        menu.add(new MenuEntry(Component.literal("/"), () -> navigateTo("", true), true));
        String current = "";
        for (String part : dir.split("/")) {
            if (part.isEmpty()) {
                continue;
            }
            current = current.isEmpty() ? part : current + "/" + part;
            String target = current;
            menu.add(new MenuEntry(Component.literal("/" + target),
                    () -> navigateTo(target, true), true));
        }
        if (menu.isEmpty()) {
            return;
        }
        updateMenuWidth();
        this.menuX = Math.max(pageX + 2,
                Math.min(pathX, pageX + pageWidth - menuWidth - 4));
        this.menuY = Math.max(pageY + 2,
                Math.min(pathY + TOOLBAR_HEIGHT,
                        pageY + pageHeight - menu.size() * 12 - 6));
    }

    private void updateMenuWidth() {
        Font font = Minecraft.getInstance().font;
        int width = 60;
        for (MenuEntry entry : menu) {
            width = Math.max(width, font.width(entry.label()) + 12);
        }
        this.menuWidth = width;
    }

    private void toggleShare(FileEntry entry) {
        if (sharedPaths.contains(entry.path())) {
            unshare(entry.path());
            return;
        }
        byte[] data = ClientFileSystem.read(entry.path());
        if (data == null) {
            setStatus(false, Component.translatable("gui.elements-plus.file.error.not_found"), true);
            return;
        }
        if (data.length > ElementsPlusServerConfig.sharedFileMaxBytes()) {
            setStatus(false, Component.translatable("gui.elements-plus.file.error.too_large"), true);
            return;
        }
        if (sharedBySelf >= sharedLimit) {
            setStatus(false, Component.translatable(
                    "gui.elements-plus.file.error.too_many_shared"), true);
            return;
        }
        sendAction(FileActionPayload.Action.SHARE, List.of(entry.path()), "",
                "", data, false, false);
    }

    private void shareSelected() {
        for (FileEntry entry : selectedEntries()) {
            if (!entry.folder()) {
                toggleShare(entry);
            }
        }
    }

    private void unshare(String path) {
        BlockPos pos = tablePos();
        if (pos == null) {
            return;
        }
        ClientPlayNetworking.send(new FileActionPayload(pos, FileRoot.CLIENT,
                FileActionPayload.Action.UNSHARE, List.of(path), "", "", new byte[0], false, false));
    }

    private void unshareShared(FileEntry entry) {
        BlockPos pos = tablePos();
        if (pos == null) {
            return;
        }
        ClientPlayNetworking.send(new FileActionPayload(pos, FileRoot.SHARED,
                FileActionPayload.Action.UNSHARE, List.of(entry.path()), "", "", new byte[0],
                false, false));
    }

    private void openAndAssemble(FileEntry entry) {
        openTarget(new EditorTarget(root, entry.path(), entry.name(), !entry.editable(),
                false, false), true, false, false);
        this.assembleOnOpen = true;
    }

    private void openAndDisassemble(FileEntry entry) {
        openTarget(new EditorTarget(root, entry.path(), entry.name(), !entry.editable(),
                false, false), false, true, false);
        this.disassembleOnOpen = true;
    }

    private void openAndPrint(FileEntry entry) {
        openTarget(new EditorTarget(root, entry.path(), entry.name(), !entry.editable(),
                false, false), false, false, true);
        this.printOnOpen = true;
    }

    /* ============================================================
     *  对话框
     * ============================================================ */

    private void showDialog(ProgramDialog dialog) {
        dialog.layout(Minecraft.getInstance().font,
                Minecraft.getInstance().getWindow().getGuiScaledWidth(),
                Minecraft.getInstance().getWindow().getGuiScaledHeight());
        dialogs.addLast(dialog);
        if (dialog.input() != null) {
            dialog.input().setFocused(true);
        }
    }

    private void closeDialog() {
        ProgramDialog dialog = dialogs.pollLast();
        if (dialog != null) {
            dialog.close();
        }
    }

    private @Nullable ProgramDialog currentDialog() {
        return dialogs.peekLast();
    }

    /* ============================================================
     *  渲染
     * ============================================================ */

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Font font = Minecraft.getInstance().font;
        hoverTooltip = new ArrayList<>();
        // 页面保持全高；展开的物品栏只覆盖左下角，需在页面背景之后重绘。
        screen.drawProgrammingInventoryBackground(graphics);
        // 小窗口下内容可能超出页面区域，统一裁剪，避免压到下方物品栏。
        graphics.enableScissor(pageX, pageY, pageX + pageWidth, pageY + pageHeight);
        renderNav(graphics, font, mouseX, mouseY);
        if (mode == Mode.EDITOR) {
            renderEditor(graphics, font, mouseX, mouseY);
        } else {
            renderManager(graphics, font, mouseX, mouseY);
        }
        graphics.disableScissor();
        // 槽位背景画在工具栏之后，避免被工具栏面板覆盖；物品由容器界面绘制在其上。
        GuiUtil.drawSlot(graphics, deviceSlotX() - 1, deviceSlotY() - 1);
        if (draggingFiles && dragPath != null) {
            renderDragGhost(graphics, font, mouseX, mouseY);
        }
        renderMenu(graphics, font, mouseX, mouseY);
        if (!hoverTooltip.isEmpty() && dialogs.isEmpty() && menu.isEmpty()) {
            renderTooltipLines(graphics, mouseX, mouseY, hoverTooltip);
        }
    }

    /** 由 ExperimentTableScreen 在所有物品/按钮之后绘制，保证对话框位于最上层。 */
    public void renderDialogs(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Font font = Minecraft.getInstance().font;
        for (ProgramDialog dialog : dialogs) {
            dialog.render(graphics, font, mouseX, mouseY, partialTick);
        }
    }

    /** 左侧根列表底边：展开物品栏时让出左下角，右侧管理/编辑器不受影响。 */
    private int navBottom() {
        if (screen != null && screen.isProgrammingInventoryExpanded()) {
            return Math.max(pageY + 1, Math.min(pageY + pageHeight, screen.programmingInventoryTop() - 5));
        }
        return pageY + pageHeight - 23;
    }

    private void renderNav(GuiGraphics graphics, Font font, int mouseX, int mouseY) {
        int x = navX();
        int y = pageY + 3;
        int navBottom = navBottom();
        int offset = Mth.clamp(navScroll, 0, maxNavScroll());
        navScroll = offset;
        graphics.enableScissor(x, y, x + NAV_WIDTH, navBottom);
        GuiUtil.drawSubPanel(graphics, x, y, x + NAV_WIDTH, navBottom,
                0xFFA0A0A0, GuiUtil.SubPanelType.BORDERED);
        FileRoot[] roots = FileRoot.values();
        for (int i = 0; i < roots.length; i++) {
            FileRoot candidate = roots[i];
            int rowY = y + 2 + i * NAV_ROW - offset;
            boolean active = candidate == root;
            boolean hovered = mouseY < navBottom
                    && mouseX >= x + 1 && mouseX < x + NAV_WIDTH - 1
                    && mouseY >= rowY && mouseY < rowY + NAV_ROW - 1;
            graphics.fill(x + 1, rowY, x + NAV_WIDTH - 1, rowY + NAV_ROW - 1,
                    active ? ProgramStyle.SELECTION : (hovered ? 0x30FFFFFF : 0x00000000));
            Component label = candidate.getDisplayName();
            graphics.drawString(font, ProgramStyle.fit(font, label.getString(), NAV_WIDTH - 8),
                    x + 4, rowY + (NAV_ROW - font.lineHeight) / 2,
                    active ? ProgramStyle.SELECTED_TEXT : ProgramStyle.TEXT, false);
            if (hovered) {
                hoverTooltip.add(candidate.getDisplayName());
                hoverTooltip.add(Component.translatable(candidate.getTranslationKey() + ".hint").withStyle(ChatFormatting.GRAY));
            }
        }
        int extraY = y + 2 + roots.length * NAV_ROW + 4 - offset;
        graphics.drawString(font, ProgramStyle.fit(font,
                Component.translatable("gui.elements-plus.file.root_hint").getString(),
                NAV_WIDTH - 6), x + 3, extraY, ProgramStyle.MUTED, false);
        int maxScroll = maxNavScroll();
        if (maxScroll > 0) {
            int view = navBottom - y;
            int content = navContentHeight();
            int barHeight = Math.max(10, view * view / content);
            int barY = y + (view - barHeight) * offset / maxScroll;
            graphics.fill(x + NAV_WIDTH - 2, barY, x + NAV_WIDTH - 1, barY + barHeight, 0xFF808080);
        }
        graphics.disableScissor();
    }

    private int navContentHeight() {
        return 2 + FileRoot.values().length * NAV_ROW + 4
                + Minecraft.getInstance().font.lineHeight + 2;
    }

    private int maxNavScroll() {
        return Math.max(0, navContentHeight() - (navBottom() - pageY));
    }

    private boolean isOverNav(double mouseX, double mouseY) {
        return mouseX >= navX() && mouseX < navX() + NAV_WIDTH
                && mouseY >= pageY && mouseY < navBottom();
    }

    private boolean isOnManagerScrollbar(double mouseX, double mouseY) {
        int total = visibleEntries().size();
        int rows = managerRows();
        if (total <= rows) {
            return false;
        }
        int barX = rightRight() - 5;
        int tableY = managerTableY();
        return mouseX >= barX && mouseX < barX + 3
                && mouseY >= tableY + 1 && mouseY < tableY + managerTableHeight() - 1;
    }

    private void scrollManagerToMouse(double mouseY) {
        int total = visibleEntries().size();
        int rows = managerRows();
        if (total <= rows) {
            tableScroll = 0;
            return;
        }
        int tableY = managerTableY();
        int tableH = managerTableHeight();
        int thumbH = Math.max(8, tableH * rows / total);
        int track = Math.max(1, tableH - 2 - thumbH);
        double position = managerScrollbarGrabOffset >= 0
                ? mouseY - (tableY + 1) - managerScrollbarGrabOffset
                : mouseY - (tableY + 1) - thumbH / 2.0;
        double t = Math.max(0.0, Math.min(1.0, position / track));
        tableScroll = (int) Math.round(t * (total - rows));
    }

    private int managerTableY() {
        return contentTop() + SEARCH_HEIGHT + 2 + HEADER_HEIGHT;
    }

    private int managerTableHeight() {
        return Math.max(12, statusY() - managerTableY() - 1);
    }

    private int managerRows() {
        return Math.max(1, managerTableHeight() / TABLE_ROW);
    }

    private void renderManager(GuiGraphics graphics, Font font, int mouseX, int mouseY) {
        int x = rightX();
        int width = rightW();
        int toolbarY = toolbarY();
        int toolbarRight = pageX + pageWidth - 20;

        // 工具栏
        GuiUtil.drawSubPanel(graphics, x + 22, toolbarY, rightRight() - 2, toolbarY + TOOLBAR_HEIGHT,
                0xFFB0B0B0, GuiUtil.SubPanelType.BORDERED);
        for (ManagerButton button : ManagerButton.values()) {
            int buttonX = managerButtonX(button);
            boolean enabled = managerButtonEnabled(button);
            boolean hovered = ProgramStyle.drawIconButton(graphics, buttonX, toolbarY + 1, ICON,
                    ProgramStyle.icon(button.icon), mouseX, mouseY, enabled, false);
            if (hovered) {
                hoverTooltip.add(Component.translatable("gui.elements-plus.file." + button.tooltipKey));
            }
        }
        int bx = managerButtonX(ManagerButton.NEW_FOLDER) + ICON + 2;
        String path = "/" + dir;
        int pathWidth = Math.max(10, toolbarRight - bx - 2);
        this.pathX = bx;
        this.pathY = toolbarY;
        this.pathWidth = pathWidth;
        this.pathText = path;
        graphics.drawString(font, fitPathStart(font, path, pathWidth), bx,
                toolbarY + (TOOLBAR_HEIGHT - font.lineHeight) / 2 + 1, ProgramStyle.TEXT, false);
        if (mouseX >= bx && mouseX < bx + pathWidth
                && mouseY >= toolbarY && mouseY < toolbarY + TOOLBAR_HEIGHT) {
            hoverTooltip.add(Component.literal(path));
            hoverTooltip.add(Component.translatable("gui.elements-plus.file.path.hint").withStyle(ChatFormatting.GRAY));
        }

        // 搜索框
        int searchY = contentTop();
        searchInput.render(graphics, font, x, searchY, width - 2, SEARCH_HEIGHT,
                mouseX >= x && mouseX < x + width - 2 && mouseY >= searchY && mouseY < searchY + SEARCH_HEIGHT,
                Component.translatable("gui.elements-plus.file.search_hint"));
        int headerY = searchY + SEARCH_HEIGHT + 2;
        int tableY = headerY + HEADER_HEIGHT;
        int tableH = Math.max(12, statusY() - tableY - 1);
        GuiUtil.drawSubPanel(graphics, x, tableY, x + width - 2, tableY + tableH + STATUS_HEIGHT + 1,
                0xFFE0E0E0, GuiUtil.SubPanelType.BORDERED);

        int ownerWidth = root == FileRoot.SHARED ? 64 : 0;
        int sizeWidth = 52;
        int extWidth = 44;
        int rightEdge = x + width - 4;
        int ownerX = rightEdge - ownerWidth;
        int sizeX = ownerX - sizeWidth;
        int extX = sizeX - extWidth;
        int nameX = x + 4;

        drawHeaderCell(graphics, font, Component.translatable("gui.elements-plus.file.column.name"),
                nameX + 14, headerY, SortColumn.NAME, mouseX, mouseY);
        drawHeaderCell(graphics, font, Component.translatable("gui.elements-plus.file.column.extension"),
                extX, headerY, SortColumn.EXTENSION, mouseX, mouseY);
        drawHeaderCell(graphics, font, Component.translatable("gui.elements-plus.file.column.size"),
                sizeX, headerY, SortColumn.SIZE, mouseX, mouseY);
        if (ownerWidth > 0) {
            drawHeaderCell(graphics, font, Component.translatable("gui.elements-plus.file.column.owner"),
                    ownerX, headerY, SortColumn.OWNER, mouseX, mouseY);
        }

        List<FileEntry> visible = visibleEntries();
        int rows = Math.max(1, tableH / TABLE_ROW);
        tableScroll = Mth.clamp(tableScroll, 0, Math.max(0, visible.size() - rows));
        graphics.enableScissor(x + 1, tableY + 1, x + width - 3, tableY + tableH - 1);
        for (int row = 0; row < rows; row++) {
            int index = tableScroll + row;
            if (index >= visible.size()) {
                break;
            }
            FileEntry entry = visible.get(index);
            int rowY = tableY + 1 + row * TABLE_ROW;
            boolean isSelected = selected.contains(entry.path());
            boolean hovered = mouseX >= x + 1 && mouseX < x + width - 3
                    && mouseY >= rowY && mouseY < rowY + TABLE_ROW;
            boolean cut = FileClipboard.isCut() && FileClipboard.entries().stream()
                    .anyMatch(clip -> clip.root() == root && clip.path().equals(entry.path()));
            if (entry.editing()) {
                graphics.fill(x + 1, rowY, x + width - 3, rowY + TABLE_ROW, 0x4040C060);
            }
            if (isSelected) {
                graphics.fill(x + 1, rowY, x + width - 3, rowY + TABLE_ROW, ProgramStyle.SELECTION);
            } else if (hovered) {
                graphics.fill(x + 1, rowY, x + width - 3, rowY + TABLE_ROW, ProgramStyle.HOVER);
            }
            if (cut) {
                graphics.fill(x + 1, rowY, x + width - 3, rowY + TABLE_ROW, 0x80FFFFFF);
            }
            if (dropTargetFolder != null && entry.folder() && entry.path().equals(dropTargetFolder)) {
                graphics.fill(x + 1, rowY, x + width - 3, rowY + TABLE_ROW, 0x8000FF00);
            }
            int textColor = isSelected ? ProgramStyle.SELECTED_TEXT : ProgramStyle.TEXT;
            ProgramStyle.drawEntryIcon(graphics, nameX, rowY + 1, entry.folder());
            graphics.drawString(font, ProgramStyle.fit(font, entry.name(), extX - nameX - 18),
                    nameX + 14, rowY + 1, textColor, false);
            if (!entry.folder()) {
                graphics.drawString(font, ProgramStyle.fit(font, entry.extension(), extWidth - 4),
                        extX, rowY + 1, isSelected ? ProgramStyle.SELECTED_TEXT : ProgramStyle.MUTED, false);
                String size = formatSize(entry.size());
                graphics.drawString(font, size, sizeX + sizeWidth - 4 - font.width(size),
                        rowY + 1, isSelected ? ProgramStyle.SELECTED_TEXT : ProgramStyle.MUTED, false);
            }
            if (ownerWidth > 0) {
                graphics.drawString(font, ProgramStyle.fit(font, entry.ownerName(), ownerWidth - 4),
                        ownerX, rowY + 1, isSelected ? ProgramStyle.SELECTED_TEXT : ProgramStyle.MUTED, false);
            }
            if (hovered) {
                buildRowTooltip(entry);
            }
        }
        graphics.disableScissor();

        if (visible.isEmpty()) {
            Component empty = root == FileRoot.EXTERNAL_DEVICE
                    ? Component.translatable("gui.elements-plus.file.empty_device")
                    : Component.translatable("gui.elements-plus.file.empty_folder");
            graphics.drawString(font, empty, x + width / 2 - font.width(empty) / 2, tableY + tableH / 2 - 4, ProgramStyle.MUTED, false);
        }
        if (visible.size() > rows) {
            int barX = rightRight() - 5;
            int barHeight = Math.max(8, tableH * rows / visible.size());
            int barY = tableY + 1 + (tableH - 2 - barHeight) * tableScroll
                    / Math.max(1, visible.size() - rows);
            this.managerThumbY = barY;
            this.managerThumbHeight = barHeight;
            graphics.fill(barX, tableY + 1, barX + 3, tableY + tableH - 1, 0x40000000);
            graphics.fill(barX, barY, barX + 3, barY + barHeight, 0xFF808080);
        }

        renderStatusBar(graphics, font, mouseX, mouseY);
    }

    private void drawHeaderCell(GuiGraphics graphics, Font font, Component label, int x, int y,
                                SortColumn column, int mouseX, int mouseY) {
        int width = 42;
        boolean hovered = mouseY >= y && mouseY < y + HEADER_HEIGHT;
        String text = label.getString();
        if (sortColumn == column) {
            text += ascending ? " ^" : " v";
        }
        graphics.drawString(font, text, x, y + 1, hovered ? 0xFF000000 : ProgramStyle.MUTED, false);
    }

    private void renderStatusBar(GuiGraphics graphics, Font font, int mouseX, int mouseY) {
        int x = rightX();
        int width = rightW();
        int files = 0;
        int folders = 0;
        for (FileEntry entry : entries) {
            if (entry.folder()) {
                folders++;
            } else {
                files++;
            }
        }
        MutableComponent left = Component.translatable("gui.elements-plus.file.status.count",
                files, folders);
        List<FileEntry> selectedEntries = selectedEntries();
        if (!selectedEntries.isEmpty()) {
            long bytes = 0;
            int count = 0;
            for (FileEntry entry : selectedEntries) {
                count++;
                if (!entry.folder()) {
                    bytes += Math.max(0, entry.size());
                }
            }
            left.append(Component.literal("  "))
                    .append(Component.translatable("gui.elements-plus.file.status.selected",
                            count, formatSize((int) Math.min(Integer.MAX_VALUE, bytes))));
        }
        MutableComponent right = Component.empty();
        int fill = 0;
        int fillColor = 0xFF00A000;
        if (root == FileRoot.TABLE_MEMORY) {
            right = Component.translatable("gui.elements-plus.file.status.memory",
                    formatSize(memoryUsed), formatSize(memoryCapacity));
            fill = memoryCapacity <= 0 ? 0 : Math.min(100, memoryUsed * 100 / memoryCapacity);
            fillColor = fill > 85 ? 0xFFC00000 : 0xFF00A000;
        } else if (root == FileRoot.SHARED) {
            right = Component.translatable("gui.elements-plus.file.status.shared",
                    sharedBySelf, sharedLimit);
        }
        boolean flashHidden = errorFlashActive() && !errorFlashVisible();
        Component error = flashHidden ? null : activeTransientError();
        if (error != null) {
            int available = width - 8 - (right.getString().isEmpty()
                    ? 0 : font.width(right) + 8);
            if (font.width(left) + font.width("  ") + font.width(error) <= available) {
                left.append(Component.literal("  ")).append(error);
            } else {
                left = error.copy();
                right = Component.empty();
                fill = 0;
            }
        }
        if (mode == Mode.EDITOR) {
            ProgramStyle.drawStatusBar(graphics, font, x, statusY(), width - 2, STATUS_HEIGHT,
                    left, right, fill, fillColor);
        } else {
            ProgramStyle.drawStatusBar(graphics, font, x + 1, statusY() - 1, width - 4, STATUS_HEIGHT,
                    left, right, fill, fillColor);
            if (mouseX >= x && mouseX < x + width && mouseY >= statusY()
                    && mouseY < statusY() + STATUS_HEIGHT) {
                hoverTooltip.add(Component.translatable("gui.elements-plus.file.status.hint"));
            }
        }
    }

    private void buildRowTooltip(FileEntry entry) {
        syncLocalSizeVersion();
        hoverTooltip.add(Component.literal(entry.name()));
        if (entry.folder()) {
            Long size = folderSizes.get(entry.path());
            // 服务端列表已经带有文件夹大小（实验桌内存等），避免一直显示“统计中”。
            if (size == null && entry.size() >= 0) {
                size = (long) entry.size();
            }
            if (size != null) {
                hoverTooltip.add(Component.translatable("gui.elements-plus.file.tooltip.folder_size",
                        formatSize((int) Math.min(Integer.MAX_VALUE, size))));
            } else {
                requestFolderSize(entry);
                hoverTooltip.add(Component.translatable("gui.elements-plus.file.tooltip.folder_size_loading"));
            }
        } else {
            hoverTooltip.add(Component.translatable("gui.elements-plus.file.tooltip.size",
                    formatSize(entry.size())));
        }
        if (entry.ownerId() != null) {
            hoverTooltip.add(Component.translatable("gui.elements-plus.file.tooltip.owner",
                    entry.ownerName()));
        }
        if (entry.editing()) {
            String names = editorNamesByPath.get(entry.path());
            if (names != null && !names.isEmpty()) {
                hoverTooltip.add(Component.translatable("gui.elements-plus.file.tooltip.editors", names));
            } else {
                hoverTooltip.add(Component.translatable("gui.elements-plus.file.tooltip.being_edited"));
            }
        }
    }

    private void requestFolderSize(FileEntry entry) {
        if (entry.folder() && !pendingFolderSizes.contains(entry.path())) {
            if (root == FileRoot.CLIENT) {
                pendingFolderSizes.add(entry.path());
                long version = ClientFileSystem.sizeVersion();
                ClientFileSystem.folderSize(entry.path()).thenAccept(size ->
                        Minecraft.getInstance().execute(() -> {
                            pendingFolderSizes.remove(entry.path());
                            if (version == ClientFileSystem.sizeVersion()) {
                                folderSizes.put(entry.path(), size);
                            }
                        }));
            }
        }
    }

    private void renderDragGhost(GuiGraphics graphics, Font font, int mouseX, int mouseY) {
        if (dragPaths.size() > 1) {
            int width = 96;
            Component label = Component.translatable("gui.elements-plus.file.drag.multiple", dragPaths.size());
            GuiUtil.raisePose(graphics);
            GuiUtil.drawSubPanel(graphics, mouseX + 8, mouseY + 8, mouseX + 8 + width, mouseY + 26,
                    0xD0C0C0C0, GuiUtil.SubPanelType.BORDERED);
            graphics.drawString(font, label, mouseX + 14, mouseY + 13, ProgramStyle.TEXT, false);
            return;
        }
        FileEntry entry = null;
        for (FileEntry candidate : visibleEntries()) {
            if (candidate.path().equals(dragPath)) {
                entry = candidate;
                break;
            }
        }
        if (entry == null) {
            return;
        }
        int ghostX = mouseX + 8;
        int ghostY = mouseY + 8;
        int width = Math.min(160, font.width(entry.name()) + 28);
        GuiUtil.raisePose(graphics);
        GuiUtil.drawSubPanel(graphics, ghostX, ghostY, ghostX + width, ghostY + 18,
                0xD0C0C0C0, GuiUtil.SubPanelType.BORDERED);
        ProgramStyle.drawEntryIcon(graphics, ghostX + 4, ghostY + 4, entry.folder());
        graphics.drawString(font, ProgramStyle.fit(font, entry.name(), width - 26),
                ghostX + 22, ghostY + 5, ProgramStyle.TEXT, false);
    }

    private void renderMenu(GuiGraphics graphics, Font font, int mouseX, int mouseY) {
        if (menu.isEmpty()) {
            return;
        }
        updateMenuWidth();
        GuiUtil.raisePose(graphics);
        GuiUtil.drawSubPanel(graphics, menuX, menuY, menuX + menuWidth,
                menuY + menu.size() * 12 + 4, 0xFFA0A0A0, GuiUtil.SubPanelType.BORDERED);
        for (int i = 0; i < menu.size(); i++) {
            MenuEntry entry = menu.get(i);
            int rowY = menuY + 2 + i * 12;
            boolean hovered = mouseX >= menuX + 1 && mouseX < menuX + menuWidth - 1
                    && mouseY >= rowY && mouseY < rowY + 12;
            if (hovered) {
                graphics.fill(menuX + 1, rowY, menuX + menuWidth - 1, rowY + 12, 0xFF808080);
            }
            graphics.drawString(font, entry.label(), menuX + 5, rowY + 1,
                    entry.enabled() ? (hovered ? ProgramStyle.SELECTED_TEXT : ProgramStyle.TEXT)
                            : 0xFF808080, false);
        }
    }

    private boolean canGoBack() {
        Deque<String> stack = stack(backStacks, root);
        return !stack.isEmpty();
    }

    private boolean canGoForward() {
        Deque<String> stack = stack(forwardStacks, root);
        return !stack.isEmpty();
    }

    private void renderTooltipLines(GuiGraphics graphics, int mouseX, int mouseY, List<Component> lines) {
        if (lines.isEmpty()) {
            return;
        }
        // 传列表而不是拼接 \\n：Component 的换行符会被当成字面字符显示。
        graphics.renderComponentTooltip(Minecraft.getInstance().font, lines, mouseX, mouseY);
    }

    private static String formatSize(int bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024 * 1024) {
            return String.format(Locale.ROOT, "%.1f KB", bytes / 1024.0);
        }
        return String.format(Locale.ROOT, "%.1f MB", bytes / (1024.0 * 1024.0));
    }

    /* ============================================================
     *  工具栏按钮
     * ============================================================ */

    private enum ManagerButton {
        BACK("back", "toolbar.back"),
        FORWARD("forward", "toolbar.forward"),
        PARENT("parent", "toolbar.parent"),
        NEW_FILE("new_file", "toolbar.new_file"),
        NEW_FOLDER("new_folder", "toolbar.new_folder");

        private final String icon;
        private final String tooltipKey;

        ManagerButton(String icon, String tooltipKey) {
            this.icon = icon;
            this.tooltipKey = tooltipKey;
        }
    }

    private int managerButtonX(ManagerButton button) {
        return rightX() + 25 + button.ordinal() * (ICON + 1);
    }

    private boolean managerButtonEnabled(ManagerButton button) {
        return switch (button) {
            case BACK -> canGoBack();
            case FORWARD -> canGoForward();
            case PARENT -> !dir.isEmpty();
            case NEW_FILE, NEW_FOLDER -> root.canCreate();
        };
    }

    private void managerButtonClick(ManagerButton button) {
        switch (button) {
            case BACK -> goBack();
            case FORWARD -> goForward();
            case PARENT -> goUp();
            case NEW_FILE -> createFile();
            case NEW_FOLDER -> createFolder();
        }
    }

    private @Nullable ManagerButton managerButtonAt(double mouseX, double mouseY) {
        if (mouseY < toolbarY() + 1 || mouseY >= toolbarY() + 1 + ICON) {
            return null;
        }
        for (ManagerButton button : ManagerButton.values()) {
            int x = managerButtonX(button);
            if (mouseX >= x && mouseX < x + ICON) {
                return managerButtonEnabled(button) ? button : null;
            }
        }
        return null;
    }

    private enum EditorButton {
        CLOSE("close", "toolbar.close"),
        ASSEMBLE("assemble", "toolbar.assemble"),
        DISASSEMBLE("disassemble", "toolbar.disassemble"),
        PRINT("print", "toolbar.print"),
        MODE("text", "toolbar.mode"),
        SHARE("share", "toolbar.share"),
        RUN("debug", "toolbar.run");

        private final String icon;
        private final String tooltipKey;

        EditorButton(String icon, String tooltipKey) {
            this.icon = icon;
            this.tooltipKey = tooltipKey;
        }

        private String icon(ProgrammingPage page) {
            if (this == MODE) {
                return page.textMode ? "hex" : "text";
            }
            return icon;
        }
    }

    private int editorButtonX(EditorButton button) {
        return rightX() + 25 + button.ordinal() * (ICON + 1);
    }

    private boolean editorButtonEnabled(EditorButton button) {
        if (!editorReady) {
            return button == EditorButton.CLOSE;
        }
        return switch (button) {
            case CLOSE, MODE -> true;
            case ASSEMBLE -> textMode && !readOnly;
            case DISASSEMBLE -> {
                byte[] data = currentBytes();
                yield data.length > 0 && data.length <= 256;
            }
            case PRINT -> root != FileRoot.EXTERNAL_DEVICE;
            case SHARE -> root == FileRoot.CLIENT && !readOnly && editorTarget != null;
            case RUN -> false;
        };
    }

    private boolean editorButtonActive(EditorButton button) {
        if (button == EditorButton.SHARE && editorTarget != null) {
            return sharedPaths.contains(editorTarget.path());
        }
        return false;
    }

    private void editorButtonClick(EditorButton button) {
        switch (button) {
            case CLOSE -> closeEditor(true);
            case ASSEMBLE -> assemble();
            case DISASSEMBLE -> disassemble();
            case PRINT -> printCurrent();
            case MODE -> toggleMode();
            case SHARE -> toggleEditorShare();
            case RUN -> setStatus(false,
                    Component.translatable("gui.elements-plus.file.run_unsupported"), true);
        }
    }

    private @Nullable EditorButton editorButtonAt(double mouseX, double mouseY) {
        if (mouseY < toolbarY() + 1 || mouseY >= toolbarY() + 1 + ICON) {
            return null;
        }
        for (EditorButton button : EditorButton.values()) {
            int x = editorButtonX(button);
            if (mouseX >= x && mouseX < x + ICON) {
                return editorButtonEnabled(button) ? button : null;
            }
        }
        return null;
    }

    /* ============================================================
     *  编辑器渲染
     * ============================================================ */

    private void renderEditor(GuiGraphics graphics, Font font, int mouseX, int mouseY) {
        int x = rightX();
        int width = rightW();
        GuiUtil.drawSubPanel(graphics, x + 22, toolbarY(), rightRight() - 2, toolbarY() + TOOLBAR_HEIGHT,
                0xFFB0B0B0, GuiUtil.SubPanelType.BORDERED);
        for (EditorButton button : EditorButton.values()) {
            boolean hovered = ProgramStyle.drawIconButton(graphics, editorButtonX(button),
                    toolbarY() + 1, ICON, ProgramStyle.icon(button.icon(this)), mouseX, mouseY,
                    editorButtonEnabled(button), editorButtonActive(button));
            if (hovered) {
                hoverTooltip.add(Component.translatable("gui.elements-plus.file." + button.tooltipKey));
            }
        }
        int nameX = editorButtonX(EditorButton.RUN) + ICON + 4;
        if (editorTarget != null) {
            List<Component> markers = new ArrayList<>();
            if (offline) {
                markers.add(Component.translatable("gui.elements-plus.file.editor.offline")
                        .withStyle(net.minecraft.ChatFormatting.RED));
            } else if (readOnly) {
                markers.add(Component.translatable("gui.elements-plus.file.editor.readonly"));
            }
            if (editorTarget.root() == FileRoot.CLIENT && sharedPaths.contains(editorTarget.path())) {
                markers.add(Component.translatable("gui.elements-plus.file.editor.shared"));
            }
            int available = pageX + pageWidth - 20 - nameX;
            int markerWidth = 0;
            for (Component marker : markers) {
                markerWidth += font.width(marker.getString());
            }
            // 优先保证“离线/只读/共享”标记显示，文件名放不下时截断。
            int nameWidth = Math.max(0, available - markerWidth);
            String shownName = ProgramStyle.fit(font, editorTarget.name(), nameWidth);
            int nameY = toolbarY() + (TOOLBAR_HEIGHT - font.lineHeight) / 2 + 1;
            graphics.drawString(font, shownName, nameX, nameY, ProgramStyle.TEXT, false);
            int markerX = nameX + font.width(shownName);
            for (Component marker : markers) {
                int color = marker.getStyle().getColor() == null
                        ? ProgramStyle.TEXT : 0xFF000000 | marker.getStyle().getColor().getValue();
                graphics.drawString(font, marker.getString(), markerX, nameY, color, false);
                markerX += font.width(marker.getString());
            }
        }

        int statusBarY = statusY();
        int consoleHeaderY = consoleExpanded
                ? statusBarY - CONSOLE_HEADER - CONSOLE_HEIGHT
                : statusBarY - CONSOLE_HEADER;
        int editorTop = contentTop();
        int editorHeight = Math.max(10, consoleHeaderY - editorTop - 1);
        if (!editorReady) {
            graphics.fill(x, editorTop, x + width - 2, editorTop + editorHeight, ProgramStyle.EDITOR_BG);
            graphics.drawCenteredString(font,
                    Component.translatable("gui.elements-plus.file.editor.loading"),
                    x + (width - 2) / 2, editorTop + editorHeight / 2 - 4, ProgramStyle.LINE_NUMBER);
        } else if (textMode) {
            textEditor.setMouse(mouseX, mouseY);
            textEditor.render(graphics, font, x, editorTop, width - 2, editorHeight);
        } else {
            hexEditor.setMouse(mouseX, mouseY);
            hexEditor.render(graphics, font, x, editorTop, width - 2, editorHeight);
        }

        // 控制台标题
        boolean headerHovered = mouseX >= x && mouseX < x + width - 2
                && mouseY >= consoleHeaderY && mouseY < consoleHeaderY + CONSOLE_HEADER;
        graphics.fill(x, consoleHeaderY, x + width - 2, consoleHeaderY + CONSOLE_HEADER,
                headerHovered ? 0xFFC0C0C0 : 0xFFB0B0B0);
        graphics.fill(x, consoleHeaderY, x + width - 2, consoleHeaderY + 1, 0xFF808080);
        String arrow = consoleExpanded ? "v " : "> ";
        graphics.drawString(font, arrow, x + 2, consoleHeaderY + 1, ProgramStyle.TEXT, false);
        graphics.drawString(font, Component.translatable("gui.elements-plus.file.console.title",
                        errorLines.size()), x + 12, consoleHeaderY + 1, ProgramStyle.TEXT, false);
        if (consoleExpanded) {
            int consoleY = consoleHeaderY + CONSOLE_HEADER;
            graphics.fill(x, consoleY, x + width - 2, consoleY + CONSOLE_HEIGHT, ProgramStyle.EDITOR_BG);
            List<Component> lines = consoleLines.isEmpty()
                    ? List.of(Component.translatable("gui.elements-plus.file.console.empty"))
                    : consoleLines;
            int lineHeight = font.lineHeight + 1;
            int visible = Math.max(1, (CONSOLE_HEIGHT - 3) / lineHeight);
            int start = Math.max(0, lines.size() - visible);
            int lineY = consoleY + 2;
            for (int i = start; i < lines.size(); i++) {
                Component line = lines.get(i);
                graphics.drawString(font, ProgramStyle.fit(font, line.getString(), width - 6),
                        x + 3, lineY, ProgramStyle.ERROR_TEXT, false);
                lineY += lineHeight;
            }
        }
        if (headerHovered) {
            hoverTooltip.add(Component.translatable("gui.elements-plus.file.console.hint"));
        }

        // 状态栏
        MutableComponent left;
        byte[] data = currentBytes();
        if (!editorReady) {
            left = Component.translatable("gui.elements-plus.file.editor.loading");
        } else if (textMode) {
            left = Component.translatable("gui.elements-plus.file.status.line_column",
                    textEditor.getLineNumber(), textEditor.getColumnNumber());
        } else {
            left = Component.translatable("gui.elements-plus.file.status.bytes", hexEditor.getCaret());
        }
        left.append(Component.literal("  "))
                .append(Component.translatable("gui.elements-plus.file.status.size",
                        formatSize(data.length)));
        if (textEditor.isLimitReached()) {
            left.append(Component.literal("  "))
                    .append(Component.translatable("gui.elements-plus.file.status.limit_reached"));
        }
        int editorCount = Math.max(1, editors.size());
        MutableComponent right = editorTarget != null
                && (editorTarget.root() != FileRoot.CLIENT || editorTarget.shared())
                ? Component.translatable("gui.elements-plus.file.status.editors", editorCount)
                : Component.empty();
        boolean flashHidden = errorFlashActive() && !errorFlashVisible();
        Component error = flashHidden ? null : activeTransientError();
        if (error != null) {
            int available = width - 8 - (right.getString().isEmpty()
                    ? 0 : font.width(right) + 8);
            if (font.width(left) + font.width("  ") + font.width(error) <= available) {
                left.append(Component.literal("  ")).append(error);
            } else {
                left = error.copy();
                right = Component.empty();
            }
        }
        ProgramStyle.drawStatusBar(graphics, font, x, statusBarY, width - 2, STATUS_HEIGHT,
                left, right, 0, 0);
        if (mouseX >= x && mouseX < x + width - 2 && mouseY >= statusBarY
                && mouseY < statusBarY + STATUS_HEIGHT && editorCount > 1) {
            for (FileEditorStatePayload.EditorInfo info : editors) {
                hoverTooltip.add(Component.literal(info.name()));
            }
        }
    }

    /* ============================================================
     *  编辑器生命周期
     * ============================================================ */

    private void openTarget(EditorTarget target) {
        openTarget(target, false, false, false);
    }

    private void openTarget(EditorTarget target, boolean assemble, boolean disassemble, boolean print) {
        this.assembleOnOpen = assemble;
        this.disassembleOnOpen = disassemble;
        this.printOnOpen = print;
        if (target.root() == FileRoot.CLIENT) {
            byte[] data = ClientFileSystem.read(target.path());
            if (data == null) {
                setStatus(false, Component.translatable("gui.elements-plus.file.error.not_found"), true);
                return;
            }
            this.editorTarget = target;
            this.editors = List.of();
            beginEditor(target, data);
            if (target.shared()) {
                sendEditorOpen(target);
            }
            return;
        }
        this.editorTarget = target;
        this.editorReady = false;
        this.mode = Mode.EDITOR;
        this.readOnly = target.readOnly();
        this.editors = List.of();
        this.textEditor.setContent("", 0);
        this.hexEditor.setData(new byte[0], 0);
        clearErrors();
        sendEditorOpen(target);
    }

    private void sendEditorOpen(EditorTarget target) {
        BlockPos pos = tablePos();
        if (pos == null) {
            return;
        }
        byte[] data = target.root() == FileRoot.CLIENT ? currentBytes() : new byte[0];
        ClientPlayNetworking.send(new FileEditorActionPayload(pos, target.root(), target.path(),
                FileEditorActionPayload.Mode.OPEN, data,
                textMode ? textEditor.getCaret() : hexEditor.getCaret(),
                textMode ? textEditor.getSelectionEnd() : hexEditor.getSelectionEnd(),
                textMode));
    }

    private void initializeEditor(byte[] data, @Nullable String errorKey) {
        if (editorTarget == null) {
            return;
        }
        if (errorKey != null && !errorKey.isEmpty()) {
            if (errorKey.endsWith("not_found")) {
                beginEditor(editorTarget, data);
                markOffline(errorKey);
            } else {
                setStatus(false, Component.translatable(errorKey), true);
                closeEditor(false);
            }
            return;
        }
        beginEditor(editorTarget, data);
    }

    private void markOffline(String errorKey) {
        this.offline = true;
        this.readOnly = true;
        this.dirty = false;
        applyEditability();
        setStatus(false, Component.translatable(errorKey), true);
    }

    private void beginEditor(EditorTarget target, byte[] data) {
        this.editorTarget = target;
        this.mode = Mode.EDITOR;
        this.editorReady = true;
        this.readOnly = target.readOnly();
        this.offline = false;
        this.remoteActivity.clear();
        this.maxBytes = computeLimit(target);
        applyEditability();
        this.revision = -1;
        this.dirty = false;
        clearErrors();
        this.consoleExpanded = true;
        this.pendingReadForEditor = false;
        textEditor.clearLimitFlag();
        String text = null;
        if (!target.forceHex() && FileText.canOpenAsText(data,
                ClientConfig.get().maxTextFileOpenBytes)) {
            text = FileText.decodeLossless(data);
        }
        if (text != null) {
            this.textMode = true;
            textEditor.setContent(text, 0);
            textEditor.setMaxBytes(maxBytes);
            textEditor.focus();
            hexEditor.blur();
        } else {
            this.textMode = false;
            hexEditor.setData(data, 0);
            hexEditor.setMaxBytes(maxBytes);
            hexEditor.focus();
            textEditor.blur();
        }
        updateRemoteCursors();
        if (assembleOnOpen) {
            assembleOnOpen = false;
            assemble();
        }
        if (disassembleOnOpen) {
            disassembleOnOpen = false;
            disassemble();
        }
        if (printOnOpen) {
            printOnOpen = false;
            printCurrent();
        }
    }

    private int computeLimit(EditorTarget target) {
        if (target.readOnly()) {
            return 0;
        }
        return switch (target.root()) {
            case CLIENT -> target.shared()
                    ? ElementsPlusServerConfig.sharedFileMaxBytes() : Integer.MAX_VALUE;
            case SHARED -> ElementsPlusServerConfig.sharedFileMaxBytes();
            case TABLE_MEMORY -> {
                int current = 0;
                for (FileEntry entry : entries) {
                    if (entry.path().equals(target.path()) && !entry.folder()) {
                        current = entry.size();
                    }
                }
                yield Math.max(0, memoryCapacity - memoryUsed + current);
            }
            case EXTERNAL_DEVICE -> com.elementsplus.core.file.DiskData.CAPACITY;
        };
    }

    private void applyEditability() {
        if (!editorReady) {
            return;
        }
        textEditor.setEditable(!readOnly);
        hexEditor.setEditable(!readOnly);
    }

    private void applyLimits() {
        if (!editorReady) {
            return;
        }
        textEditor.setMaxBytes(maxBytes);
        hexEditor.setMaxBytes(maxBytes);
    }

    private byte[] currentBytes() {
        if (!editorReady) {
            return new byte[0];
        }
        return textMode ? FileText.encode(textEditor.getContent()) : hexEditor.getData();
    }

    private void applyEditorContent(byte[] data) {
        if (!editorReady) {
            return;
        }
        if (textMode) {
            String text = FileText.decodeLossless(data);
            if (text == null) {
                text = new String(data, java.nio.charset.StandardCharsets.UTF_8);
            }
            textEditor.applyRemoteContent(text);
        } else {
            hexEditor.applyRemoteContent(data);
        }
    }

    private void markDirty() {
        this.dirty = true;
        this.lastEditAt = Util.getMillis();
    }

    private void flushEditor() {
        if (!editorReady || editorTarget == null) {
            return;
        }
        byte[] data = currentBytes();
        if (readOnly) {
            dirty = false;
            return;
        }
        if (editorTarget.root() == FileRoot.CLIENT) {
            ClientFileSystem.write(editorTarget.path(), data, true);
            if (editorTarget.shared()) {
                sendEditorUpdate(data);
            }
            if (root == FileRoot.CLIENT && FilePaths.parent(editorTarget.path()).equals(dir)) {
                refreshLocal();
            }
        } else {
            sendEditorUpdate(data);
        }
        dirty = false;
    }

    private void sendEditorUpdate(byte[] data) {
        BlockPos pos = tablePos();
        if (pos == null || editorTarget == null) {
            return;
        }
        ClientPlayNetworking.send(new FileEditorActionPayload(pos, editorTarget.root(),
                editorTarget.path(), FileEditorActionPayload.Mode.UPDATE, data,
                textMode ? textEditor.getCaret() : hexEditor.getCaret(),
                textMode ? textEditor.getSelectionEnd() : hexEditor.getSelectionEnd(), textMode));
        lastSentAt = Util.getMillis();
        lastSentCaret = currentCaret();
        lastCursorAt = lastSentAt;
    }

    private void sendEditorCursor() {
        BlockPos pos = tablePos();
        if (pos == null || editorTarget == null || !syncsEditorWithServer()) {
            return;
        }
        ClientPlayNetworking.send(new FileEditorActionPayload(pos, editorTarget.root(),
                editorTarget.path(), FileEditorActionPayload.Mode.CURSOR, new byte[0],
                textMode ? textEditor.getCaret() : hexEditor.getCaret(),
                textMode ? textEditor.getSelectionEnd() : hexEditor.getSelectionEnd(), textMode));
        lastCursorAt = Util.getMillis();
        lastSentCaret = currentCaret();
    }

    private int currentCaret() {
        return textMode ? textEditor.getCaret() : hexEditor.getCaret();
    }

    private void updateRemoteCursors() {
        UUID self = Minecraft.getInstance().player == null ? null
                : Minecraft.getInstance().player.getUUID();
        long now = Util.getMillis();
        List<TextEditor.RemoteCursor> textCursors = new ArrayList<>();
        List<HexEditor.RemoteCursor> hexCursors = new ArrayList<>();
        for (FileEditorStatePayload.EditorInfo info : editors) {
            if (self != null && info.player().equals(self)) {
                continue;
            }
            RemoteActivity previous = remoteActivity.get(info.player());
            long lastActive = previous == null || previous.start() != info.start()
                    || previous.end() != info.end() ? now
                    : previous.lastActive();
            remoteActivity.put(info.player(),
                    new RemoteActivity(info.start(), info.end(), lastActive));
            int color = ProgramStyle.cursorColor(info.player());
            if (info.textMode()) {
                textCursors.add(new TextEditor.RemoteCursor(info.player(), info.name(),
                        info.start(), info.end(), color, lastActive));
            } else {
                hexCursors.add(new HexEditor.RemoteCursor(info.player(), info.name(),
                        info.start(), info.end(), color, lastActive));
            }
        }
        textEditor.setRemote(textCursors);
        hexEditor.setRemote(hexCursors);
    }

    private void closeEditor(boolean flush) {
        if (flush && dirty) {
            flushEditor();
        }
        if (editorTarget != null
                && (editorTarget.root() != FileRoot.CLIENT || editorTarget.shared())) {
            sendEditorClose();
        }
        this.mode = Mode.MANAGER;
        this.editorTarget = null;
        this.editorReady = false;
        this.dirty = false;
        this.revision = -1;
        this.editors = List.of();
        this.remoteActivity.clear();
        this.textMode = false;
        this.offline = false;
        textEditor.blur();
        hexEditor.blur();
        clearErrors();
        requestListing();
    }

    public void tick() {
        updateErrorFlash();
        if (mode != Mode.EDITOR || !editorReady) {
            return;
        }
        long now = Util.getMillis();
        if (dirty && now - lastEditAt >= 300) {
            flushEditor();
        }
        if (editorTarget != null && (editorTarget.root() != FileRoot.CLIENT || editorTarget.shared())
                && now - lastCursorAt >= 300 && currentCaret() != lastSentCaret) {
            sendEditorCursor();
        }
    }

    /** 失败时状态按钮闪烁：显示-隐藏-显示-隐藏-显示，每 0.2s 一次。 */
    private void updateErrorFlash() {
        if (transientError == null || errorFlashStart < 0) {
            return;
        }
        long elapsed = Util.getMillis() - errorFlashStart;
        if (elapsed >= ERROR_FLASH_TOTAL_MS) {
            errorFlashStart = -1L;
            screen.setProgrammingStatusBlinkVisible(true);
            return;
        }
        boolean visible = elapsed >= ERROR_FLASH_BLINK_MS
                || (elapsed / ERROR_FLASH_STEP_MS) % 2 == 0;
        screen.setProgrammingStatusBlinkVisible(visible);
    }

    public void onScreenClosed() {
        if (mode == Mode.EDITOR) {
            closeEditor(true);
        }
        FileClipboard.clear();
    }

    public void onOpen() {
        requestListing();
    }

    /** 离开编程标签页时保存并关闭编辑器。 */
    public void onTabHidden() {
        if (mode == Mode.EDITOR) {
            closeEditor(true);
        }
    }

    /* ============================================================
     *  汇编 / 反汇编 / 打印 / 共享
     * ============================================================ */

    private void assemble() {
        if (!editorReady || !textMode || readOnly) {
            setStatus(false, Component.translatable("gui.elements-plus.file.assemble.no_text"), true);
            return;
        }
        ProgramAssembler.Result result = ProgramAssembler.assemble(textEditor.getContent());
        if (!result.ok()) {
            addErrorLines(result.errors());
            setStatus(false, Component.translatable("gui.elements-plus.file.assemble.failed",
                    result.errors().size()), true, false);
            return;
        }
        clearErrors();
        showOutputDialog(result.image(), ".eppbc", true);
    }

    private void disassemble() {
        if (!editorReady) {
            return;
        }
        byte[] data = currentBytes();
        if (data.length == 0 || data.length > 256) {
            setStatus(false, Component.translatable("gui.elements-plus.file.disassemble.too_large"), true);
            return;
        }
        String text = ProgramDisassembler.disassemble(data);
        showOutputDialog(FileText.encode(text), ".eppasm", false);
    }

    private void showOutputDialog(byte[] data, String extension, boolean asProgram) {
        String base = FilePaths.baseName(editorTarget == null ? "output" : editorTarget.name());
        if (base.isEmpty()) {
            base = "output";
        }
        String defaultName = base + extension;
        Component title = Component.translatable(asProgram
                ? "gui.elements-plus.file.assemble.title"
                : "gui.elements-plus.file.disassemble.title");
        ProgramDialog dialog = new ProgramDialog(title,
                Component.translatable("gui.elements-plus.file.output.size", data.length), false, null)
                .button(Component.translatable("gui.elements-plus.toolbox.dialog.cancel"), this::closeDialog)
                .button(Component.translatable("gui.elements-plus.file.output.slot"),
                        () -> {
                            closeDialog();
                            writeOutputToSlot(data, !asProgram);
                        })
                .button(Component.translatable("gui.elements-plus.file.output.sibling"),
                        () -> {
                            closeDialog();
                            promptOutputName(defaultName, data);
                        });
        showDialog(dialog);
    }

    private void promptOutputName(String defaultName, byte[] data) {
        SingleLineInput input = new SingleLineInput();
        input.setMaxLength(FilePaths.MAX_NAME_LENGTH);
        input.setValue(defaultName);
        showDialog(new ProgramDialog(Component.translatable("gui.elements-plus.file.output.name_title"),
                Component.translatable("gui.elements-plus.file.output.name"), false, input,
                Component.translatable("gui.elements-plus.file.dialog.name_hint"))
                .button(Component.translatable("gui.elements-plus.toolbox.dialog.cancel"), this::closeDialog)
                .button(Component.translatable("gui.elements-plus.toolbox.dialog.ok"), () -> {
                    String name = FilePaths.validateName(input.getValue());
                    if (name == null) {
                        setStatus(false, Component.translatable(
                                "gui.elements-plus.file.error.invalid_name"), true);
                        return;
                    }
                    closeDialog();
                    writeOutputFile(name, data);
                }));
    }

    private void writeOutputFile(String rawName, byte[] data) {
        String name = FilePaths.validateName(rawName);
        if (name == null) {
            setStatus(false, Component.translatable("gui.elements-plus.file.error.invalid_name"), true);
            return;
        }
        if (root == FileRoot.TABLE_MEMORY) {
            String path = FilePaths.join(dir, name);
            boolean exists = false;
            for (FileEntry entry : entries) {
                if (entry.path().equals(path)) {
                    exists = true;
                }
            }
            if (exists) {
                confirm(Component.translatable("gui.elements-plus.file.dialog.overwrite", path), () ->
                        sendAction(FileActionPayload.Action.WRITE_FILE, List.of(path), "",
                                "", data, false, true));
            } else {
                sendAction(FileActionPayload.Action.WRITE_FILE, List.of(path), "",
                        "", data, false, false);
            }
            return;
        }
        if (root == FileRoot.CLIENT) {
            String path = FilePaths.join(dir, name);
            if (ClientFileSystem.exists(path)) {
                confirm(Component.translatable("gui.elements-plus.file.dialog.overwrite", path), () -> {
                    ClientFileSystem.write(path, data, true);
                    refreshLocal();
                });
            } else {
                var result = ClientFileSystem.write(path, data, false);
                if (!result.success()) {
                    setStatus(false, Component.translatable(result.failureKey()), true);
                }
                refreshLocal();
            }
            return;
        }
        setStatus(false, Component.translatable("gui.elements-plus.file.error.not_allowed"), true);
    }

    private void writeOutputToSlot(byte[] data, boolean asText) {
        ItemStack slot = screen.experimentMenu().extraSlot.getItem();
        if (slot.isEmpty()) {
            setStatus(false, Component.translatable("gui.elements-plus.file.error.no_device"), true);
            return;
        }
        if (slot.is(ModItems.DISK)) {
            if (data.length > com.elementsplus.core.file.DiskData.CAPACITY) {
                setStatus(false, Component.translatable("gui.elements-plus.file.error.too_large"), true);
                return;
            }
            confirm(Component.translatable("gui.elements-plus.file.dialog.overwrite_disk"), () ->
                    sendPrint(data, ""));
            return;
        }
        if (slot.is(net.minecraft.world.item.Items.PAPER)) {
            if (asText) {
                if (!FileText.canOpenAsText(data, 0)
                        || data.length > ElementsPlusServerConfig.textFileMaxBytes()) {
                    setStatus(false, Component.translatable("gui.elements-plus.file.error.not_text"), true);
                    return;
                }
                sendPrint(data, "text");
            } else {
                if (data.length > 256) {
                    setStatus(false, Component.translatable("gui.elements-plus.file.error.too_large"), true);
                    return;
                }
                confirm(Component.translatable("gui.elements-plus.file.dialog.punch_paper"), () ->
                        sendPrint(data, "punched"));
            }
            return;
        }
        setStatus(false, Component.translatable("gui.elements-plus.file.error.not_printable"), true);
    }

    private void sendPrint(byte[] data, String kind) {
        sendPrint(data, kind, editorTarget == null ? "" : editorTarget.name());
    }

    private void sendPrint(byte[] data, String kind, String sourceName) {
        BlockPos pos = tablePos();
        if (pos == null) {
            return;
        }
        if (data.length > MAX_TRANSFER_BYTES) {
            setStatus(false, Component.translatable("gui.elements-plus.file.error.too_large"), true);
            return;
        }
        this.pendingPrint = true;
        this.pendingPrintData = data.clone();
        this.pendingPrintToOwnExternalDisk = editorTarget != null
                && editorTarget.root() == FileRoot.EXTERNAL_DEVICE
                && screen.experimentMenu().extraSlot.getItem().is(ModItems.DISK);
        ClientPlayNetworking.send(new FileActionPayload(pos, FileRoot.CLIENT,
                FileActionPayload.Action.PRINT, List.of(sourceName == null ? "" : sourceName),
                "", kind, data, false, false));
    }

    private void printCurrent() {
        if (!editorReady) {
            return;
        }
        if (root == FileRoot.EXTERNAL_DEVICE) {
            setStatus(false, Component.translatable("gui.elements-plus.file.error.not_printable"), true);
            return;
        }
        byte[] data = currentBytes();
        ItemStack slot = screen.experimentMenu().extraSlot.getItem();
        if (slot.isEmpty()) {
            setStatus(false, Component.translatable("gui.elements-plus.file.error.no_device"), true);
            return;
        }
        if (slot.is(ModItems.DISK)) {
            if (data.length > com.elementsplus.core.file.DiskData.CAPACITY) {
                setStatus(false, Component.translatable("gui.elements-plus.file.error.too_large"), true);
                return;
            }
            confirm(Component.translatable("gui.elements-plus.file.dialog.overwrite_disk"), () ->
                    sendPrint(data, ""));
            return;
        }
        if (slot.is(net.minecraft.world.item.Items.PAPER)) {
            showDialog(new ProgramDialog(Component.translatable("gui.elements-plus.file.print.paper"),
                    Component.translatable("gui.elements-plus.file.print.paper_choice"), false, null)
                    .button(Component.translatable("gui.elements-plus.toolbox.dialog.cancel"), this::closeDialog)
                    .button(Component.translatable("gui.elements-plus.file.print.as_text"), () -> {
                        closeDialog();
                        if (!FileText.canOpenAsText(data, 0)
                                || data.length > ElementsPlusServerConfig.textFileMaxBytes()) {
                            setStatus(false, Component.translatable(
                                    "gui.elements-plus.file.error.not_text"), true);
                            return;
                        }
                        sendPrint(data, "text");
                    })
                    .button(Component.translatable("gui.elements-plus.file.print.as_punched"), () -> {
                        closeDialog();
                        if (data.length > 256) {
                            setStatus(false, Component.translatable(
                                    "gui.elements-plus.file.error.too_large"), true);
                            return;
                        }
                        sendPrint(data, "punched");
                    }));
            return;
        }
        if (slot.is(ModItems.CIRCUIT_DIAGRAM)) {
            if (com.elementsplus.core.circuit.CircuitItemData.isCompiled(slot)) {
                setStatus(false, Component.translatable(
                        "gui.elements-plus.file.error.diagram_compiled"), true);
                return;
            }
            if (!ElementsPlusServerConfig.allowCircuitUpload()) {
                setStatus(false, Component.translatable(
                        "gui.elements-plus.file.error.upload_disabled"), true);
                return;
            }
            if (CircuitDiagramFile.decode(data) == null) {
                setStatus(false, Component.translatable(
                        "gui.elements-plus.file.error.invalid_diagram"), true);
                return;
            }
            confirm(Component.translatable("gui.elements-plus.file.dialog.upload_diagram"), () -> {
                BlockPos pos = tablePos();
                if (pos != null) {
                    this.pendingCircuitUpload = true;
                    ClientPlayNetworking.send(new FileActionPayload(pos, FileRoot.CLIENT,
                            FileActionPayload.Action.UPLOAD_CIRCUIT, List.of(editorTarget == null
                            ? "" : editorTarget.path()), "", "", data, false, false));
                }
            });
            return;
        }
        setStatus(false, Component.translatable("gui.elements-plus.file.error.not_printable"), true);
    }

    private void toggleEditorShare() {
        if (!editorReady || editorTarget == null || editorTarget.root() != FileRoot.CLIENT) {
            return;
        }
        flushEditor();
        String path = editorTarget.path();
        if (sharedPaths.contains(path)) {
            sendEditorClose();
            unshare(path);
            this.editorTarget = new EditorTarget(FileRoot.CLIENT, editorTarget.path(),
                    editorTarget.name(), false, false, false);
            return;
        }
        byte[] data = currentBytes();
        if (data.length > ElementsPlusServerConfig.sharedFileMaxBytes()) {
            setStatus(false, Component.translatable("gui.elements-plus.file.error.too_large"), true);
            return;
        }
        BlockPos pos = tablePos();
        if (pos == null) {
            return;
        }
        ClientPlayNetworking.send(new FileActionPayload(pos, FileRoot.CLIENT,
                FileActionPayload.Action.SHARE, List.of(path), "", "", data, false, false));
        this.pendingShareJoinPath = path;
    }

    private void sendEditorClose() {
        BlockPos pos = tablePos();
        if (pos == null || editorTarget == null || !syncsEditorWithServer()) {
            return;
        }
        ClientPlayNetworking.send(new FileEditorActionPayload(pos, editorTarget.root(),
                editorTarget.path(), FileEditorActionPayload.Mode.CLOSE, new byte[0], 0, 0, false));
    }

    /** 本地客户端文件（未共享）没有服务端会话，不应发送光标/模式/关闭同步。 */
    private boolean syncsEditorWithServer() {
        return editorTarget != null
                && (editorTarget.root() != FileRoot.CLIENT || editorTarget.shared());
    }

    private void confirm(Component message, Runnable onConfirm) {
        showDialog(new ProgramDialog(Component.translatable("gui.elements-plus.file.dialog.confirm"),
                message, false, null)
                .button(Component.translatable("gui.elements-plus.toolbox.dialog.cancel"), this::closeDialog)
                .button(Component.translatable("gui.elements-plus.file.dialog.continue"), () -> {
                    closeDialog();
                    onConfirm.run();
                }));
    }

    /* ============================================================
     *  模式切换
     * ============================================================ */

    private void toggleMode() {
        if (!editorReady || editorTarget == null) {
            return;
        }
        if (textMode) {
            byte[] data = FileText.encode(textEditor.getContent());
            this.textMode = false;
            hexEditor.setData(data, 0);
            hexEditor.setMaxBytes(maxBytes);
            hexEditor.focus();
            textEditor.blur();
            updateRemoteCursors();
            sendEditorMode();
            return;
        }
        byte[] data = hexEditor.getData();
        String text = FileText.decodeLossless(data);
        boolean tooLarge = data.length > ClientConfig.get().maxTextFileOpenBytes;
        if (text == null || tooLarge) {
            showDialog(new ProgramDialog(
                    Component.translatable("gui.elements-plus.file.mode.warning_title"),
                    Component.translatable("gui.elements-plus.file.mode.warning"), true, null)
                    .button(Component.translatable("gui.elements-plus.toolbox.dialog.cancel"), this::closeDialog)
                    .button(Component.translatable("gui.elements-plus.file.mode.force"), () -> {
                        closeDialog();
                        forceTextMode(data);
                    }));
            return;
        }
        switchToText(text, data);
    }

    private void forceTextMode(byte[] data) {
        String text = new String(data, java.nio.charset.StandardCharsets.UTF_8);
        switchToText(text, data);
        markDirty();
    }

    private void switchToText(String text, byte[] data) {
        this.textMode = true;
        textEditor.setContent(text, 0);
        textEditor.setMaxBytes(maxBytes);
        textEditor.focus();
        hexEditor.blur();
        updateRemoteCursors();
        sendEditorMode();
    }

    private void applySyncedMode(boolean text, byte[] data) {
        if (text == textMode) {
            return;
        }
        if (text) {
            String decoded = FileText.decodeLossless(data);
            if (decoded == null) {
                decoded = new String(data, java.nio.charset.StandardCharsets.UTF_8);
            }
            this.textMode = true;
            textEditor.setContent(decoded, 0);
            textEditor.setMaxBytes(maxBytes);
            textEditor.focus();
            hexEditor.blur();
        } else {
            this.textMode = false;
            hexEditor.setData(data, 0);
            hexEditor.setMaxBytes(maxBytes);
            hexEditor.focus();
            textEditor.blur();
        }
        updateRemoteCursors();
    }

    private void sendEditorMode() {
        BlockPos pos = tablePos();
        if (pos == null || editorTarget == null || !syncsEditorWithServer()) {
            return;
        }
        ClientPlayNetworking.send(new FileEditorActionPayload(pos, editorTarget.root(),
                editorTarget.path(), FileEditorActionPayload.Mode.MODE, new byte[0],
                currentCaret(), currentCaret(), textMode));
    }

    /* ============================================================
     *  鼠标
     * ============================================================ */

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible || !active) {
            return false;
        }
        containerSlotPress = false;
        editorMouseDown = false;
        ProgramDialog dialog = currentDialog();
        if (dialog != null) {
            return dialog.mouseClicked(mouseX, mouseY, button);
        }
        if (!menu.isEmpty()) {
            if (clickMenu(mouseX, mouseY, button)) {
                return true;
            }
            menu.clear();
            return true;
        }
        if (mouseX < pageX || mouseX >= pageX + pageWidth
                || mouseY < pageY || mouseY >= pageY + pageHeight) {
            return false;
        }
        if (screen.isOverProgrammingInventory(mouseX, mouseY)) {
            containerSlotPress = true;
            return false;
        }
        // 外接设备槽位由容器界面处理。
        if (mouseX >= deviceSlotX() - 1 && mouseX < deviceSlotX() + 17
                && mouseY >= deviceSlotY() - 1 && mouseY < deviceSlotY() + 17) {
            containerSlotPress = true;
            return false;
        }
        // 右上角状态图标是独立控件。
        if (mouseX >= statusIconX() - 1 && mouseX < statusIconX() + 17
                && mouseY >= statusIconY() - 1 && mouseY < statusIconY() + 17) {
            return false;
        }
        if (mode == Mode.MANAGER && mouseX >= pathX && mouseX < pathX + pathWidth
                && mouseY >= pathY && mouseY < pathY + TOOLBAR_HEIGHT) {
            openPathMenu();
            return true;
        }
        if (clickNav(mouseX, mouseY)) {
            return true;
        }
        if (mode == Mode.EDITOR) {
            return clickEditor(mouseX, mouseY, button);
        }
        return clickManager(mouseX, mouseY, button);
    }

    private boolean clickNav(double mouseX, double mouseY) {
        if (mouseX < navX() + 1 || mouseX >= navX() + NAV_WIDTH - 1
                || mouseY >= navBottom()) {
            return false;
        }
        int offset = Mth.clamp(navScroll, 0, maxNavScroll());
        FileRoot[] roots = FileRoot.values();
        for (int i = 0; i < roots.length; i++) {
            int rowY = pageY + 5 + i * NAV_ROW - offset;
            if (mouseY >= rowY && mouseY < rowY + NAV_ROW - 1) {
                selectRoot(roots[i], true);
                return true;
            }
        }
        return mouseY < pageY + pageHeight;
    }

    private boolean clickManager(double mouseX, double mouseY, int button) {
        ManagerButton buttonHit = managerButtonAt(mouseX, mouseY);
        if (buttonHit != null && button == 0) {
            managerButtonClick(buttonHit);
            return true;
        }
        int searchY = contentTop();
        if (mouseY >= searchY && mouseY < searchY + SEARCH_HEIGHT
                && mouseX >= rightX() && mouseX < rightRight()) {
            searchInput.mouseClicked(mouseX, mouseY, rightX(), searchY, rightW() - 2, SEARCH_HEIGHT);
            return true;
        }
        searchInput.setFocused(false);
        if (isOnManagerScrollbar(mouseX, mouseY)) {
            managerScrollbarDrag = true;
            if (mouseY >= managerThumbY && mouseY < managerThumbY + managerThumbHeight) {
                managerScrollbarGrabOffset = (int) (mouseY - managerThumbY);
            } else {
                managerScrollbarGrabOffset = -1;
                scrollManagerToMouse(mouseY);
            }
            return true;
        }
        int headerY = searchY + SEARCH_HEIGHT + 2;
        if (button == 0 && mouseY >= headerY && mouseY < headerY + HEADER_HEIGHT) {
            SortColumn column = headerAt(mouseX);
            if (column != null) {
                if (sortColumn == column) {
                    ascending = !ascending;
                } else {
                    sortColumn = column;
                    ascending = true;
                }
                tableScroll = 0;
            }
            return true;
        }
        FileEntry row = rowAt(mouseX, mouseY);
        if (row != null) {
            if (button == 1) {
                if (!selected.contains(row.path())) {
                    selected.clear();
                    selected.add(row.path());
                    anchorPath = row.path();
                }
                openContextMenu(mouseX, mouseY);
                return true;
            }
            boolean ctrl = net.minecraft.client.gui.screens.Screen.hasControlDown();
            boolean shift = net.minecraft.client.gui.screens.Screen.hasShiftDown();
            boolean keepMulti = button == 0 && !ctrl && !shift
                    && selected.contains(row.path()) && selected.size() > 1;
            if (!keepMulti) {
                clickRow(row, button, ctrl, shift);
            }
            dragPaths.clear();
            if (keepMulti) {
                dragPaths.addAll(selected);
            } else if (selected.contains(row.path())) {
                dragPaths.addAll(selected);
            } else {
                dragPaths.add(row.path());
            }
            dragPath = dragPaths.isEmpty() ? row.path() : dragPaths.get(0);
            dragStartX = mouseX;
            dragStartY = mouseY;
            dragEndX = mouseX;
            dragEndY = mouseY;
            draggingFiles = false;
            return true;
        }
        if (button == 1) {
            selected.clear();
            openContextMenu(mouseX, mouseY);
            return true;
        }
        if (button == 0) {
            selected.clear();
            anchorPath = null;
        }
        return true;
    }

    private boolean clickEditor(double mouseX, double mouseY, int button) {
        EditorButton buttonHit = editorButtonAt(mouseX, mouseY);
        if (buttonHit != null && button == 0) {
            editorButtonClick(buttonHit);
            return true;
        }
        int statusBarY = statusY();
        int consoleHeaderY = consoleExpanded
                ? statusBarY - CONSOLE_HEADER - CONSOLE_HEIGHT
                : statusBarY - CONSOLE_HEADER;
        if (mouseX >= rightX() && mouseX < rightRight()
                && mouseY >= consoleHeaderY && mouseY < consoleHeaderY + CONSOLE_HEADER) {
            consoleExpanded = !consoleExpanded;
            return true;
        }
        int editorTop = contentTop();
        int editorHeight = Math.max(10, consoleHeaderY - editorTop - 1);
        if (mouseX >= rightX() && mouseX < rightRight()
                && mouseY >= editorTop && mouseY < editorTop + editorHeight) {
            editorMouseDown = button == 0;
            if (textMode) {
                return textEditor.mouseClicked(mouseX, mouseY, button);
            }
            return hexEditor.mouseClicked(mouseX, mouseY, button);
        }
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!visible || !active) {
            return false;
        }
        if (currentDialog() != null) {
            return true;
        }
        if (containerSlotPress) {
            return false;
        }
        if (screen.isOverProgrammingInventory(mouseX, mouseY)) {
            return false;
        }
        if (mode == Mode.EDITOR && editorReady) {
            if (!editorMouseDown) {
                return false;
            }
            if (textMode) {
                return textEditor.mouseDragged(mouseX, mouseY, button);
            }
            return hexEditor.mouseDragged(mouseX, mouseY, button);
        }
        if (managerScrollbarDrag) {
            scrollManagerToMouse(mouseY);
            return true;
        }
        if (dragPath == null) {
            // 没有开始拖动文件时把拖拽让给容器界面（外接设备槽位）。
            return false;
        }
        if (dragPath != null) {
            dragEndX = mouseX;
            dragEndY = mouseY;
            if (Math.abs(dragEndX - dragStartX) > 4 || Math.abs(dragEndY - dragStartY) > 4) {
                draggingFiles = true;
            }
            if (draggingFiles) {
                FileEntry target = rowAt(mouseX, mouseY);
                dropTargetFolder = target != null && target.folder() ? target.path() : null;
            }
        }
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!visible || !active) {
            return false;
        }
        if (currentDialog() != null) {
            return true;
        }
        if (containerSlotPress) {
                containerSlotPress = false;
            return false;
        }
        if (managerScrollbarDrag) {
            managerScrollbarDrag = false;
            managerScrollbarGrabOffset = -1;
            return true;
        }
        boolean outsidePage = mouseX < pageX || mouseX >= pageX + pageWidth
                || mouseY < pageY || mouseY >= pageY + pageHeight;
        if (outsidePage) {
            if (mode == Mode.EDITOR && editorReady && editorMouseDown) {
                if (textMode) {
                    textEditor.mouseReleased(button);
                } else {
                    hexEditor.mouseReleased(button);
                }
            }
            editorMouseDown = false;
            return false;
        }
        if (screen.isOverProgrammingInventory(mouseX, mouseY)) {
            if (mode == Mode.EDITOR && editorReady && editorMouseDown) {
                if (textMode) {
                    textEditor.mouseReleased(button);
                } else {
                    hexEditor.mouseReleased(button);
                }
            }
            editorMouseDown = false;
            return false;
        }
        if (mode == Mode.EDITOR && editorReady) {
            if (editorMouseDown) {
                if (textMode) {
                    textEditor.mouseReleased(button);
                } else {
                    hexEditor.mouseReleased(button);
                }
                editorMouseDown = false;
                return true;
            }
            return false;
        }
        if (dragPath == null && !draggingFiles) {
            // 没有文件拖拽/选择时，让容器界面处理槽位释放。
            return false;
        }
        if (draggingFiles && dropTargetFolder != null && !dragPaths.isEmpty()) {
            boolean copy = net.minecraft.client.gui.screens.Screen.hasControlDown();
            for (String path : new ArrayList<>(dragPaths)) {
                if (path.equals(dropTargetFolder) || dropTargetFolder.startsWith(path + "/")
                        || dropTargetFolder.equals(FilePaths.parent(path))) {
                    continue;
                }
                transferByDrag(path, dropTargetFolder, copy);
            }
        }
        draggingFiles = false;
        dragPath = null;
        dragPaths.clear();
        dropTargetFolder = null;
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (!visible || !active) {
            return false;
        }
        if (currentDialog() != null) {
            return true;
        }
        if (isOverNav(mouseX, mouseY)) {
            int maxScroll = maxNavScroll();
            if (maxScroll > 0) {
                navScroll = Mth.clamp(navScroll - (int) Math.signum(verticalAmount) * NAV_ROW,
                        0, maxScroll);
                return true;
            }
        }
        if (mode == Mode.EDITOR && editorReady) {
            if (textMode) {
                return textEditor.mouseScrolled(mouseY, horizontalAmount, verticalAmount);
            }
            return hexEditor.mouseScrolled(verticalAmount);
        }
        if (mouseX >= rightX() && mouseX < rightRight() && mouseY >= contentTop()
                && mouseY < statusY()) {
            int rows = Math.max(1, (statusY() - (contentTop() + SEARCH_HEIGHT + 2
                    + HEADER_HEIGHT)) / TABLE_ROW);
            tableScroll = Math.max(0, tableScroll - (int) Math.signum(verticalAmount));
            tableScroll = Mth.clamp(tableScroll, 0,
                    Math.max(0, visibleEntries().size() - rows));
            return true;
        }
        return false;
    }

    /* ============================================================
     *  键盘
     * ============================================================ */

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        ProgramDialog dialog = currentDialog();
        if (dialog != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                closeDialog();
                return true;
            }
            return dialog.keyPressed(keyCode, scanCode, modifiers);
        }
        if (!menu.isEmpty()) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                menu.clear();
            }
            return true;
        }
        if (mode == Mode.EDITOR && editorReady) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                closeEditor(true);
                return true;
            }
            int before = textMode ? textEditor.modCount() : hexEditor.modCount();
            boolean handled = textMode
                    ? textEditor.keyPressed(keyCode, scanCode, modifiers)
                    : hexEditor.keyPressed(keyCode, scanCode, modifiers);
            int after = textMode ? textEditor.modCount() : hexEditor.modCount();
            if (handled && after != before) {
                markDirty();
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            return false;
        }
        boolean control = net.minecraft.client.gui.screens.Screen.hasControlDown();
        if (control && keyCode == GLFW.GLFW_KEY_A) {
            selectAll();
            return true;
        }
        if (control && keyCode == GLFW.GLFW_KEY_C) {
            copySelected(false);
            return true;
        }
        if (control && keyCode == GLFW.GLFW_KEY_X) {
            copySelected(true);
            return true;
        }
        if (control && keyCode == GLFW.GLFW_KEY_V) {
            startPaste();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_DELETE) {
            deleteSelected();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            FileEntry entry = singleSelected();
            if (entry != null) {
                openEntry(entry);
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_F2) {
            renameSelected();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_F5) {
            refreshListing();
            return true;
        }
        if (searchInput.isFocused()) {
            if (searchInput.keyPressed(keyCode, scanCode, modifiers)) {
                syncSearch();
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        ProgramDialog dialog = currentDialog();
        if (dialog != null) {
            return dialog.charTyped(c, modifiers);
        }
        if (mode == Mode.EDITOR && editorReady) {
            int before = textMode ? textEditor.modCount() : hexEditor.modCount();
            boolean handled = textMode ? textEditor.charTyped(c) : hexEditor.charTyped(c);
            int after = textMode ? textEditor.modCount() : hexEditor.modCount();
            if (handled && after != before) {
                markDirty();
            }
            return true;
        }
        if (searchInput.isFocused() && searchInput.charTyped(c)) {
            syncSearch();
            return true;
        }
        return false;
    }

    private void syncSearch() {
        this.search = searchInput.getValue();
        this.tableScroll = 0;
    }

    /* ============================================================
     *  命中测试
     * ============================================================ */

    private @Nullable FileEntry rowAt(double mouseX, double mouseY) {
        int x = rightX();
        int width = rightW();
        int tableY = contentTop() + SEARCH_HEIGHT + 2 + HEADER_HEIGHT;
        int rows = Math.max(1, (statusY() - tableY - 1) / TABLE_ROW);
        if (mouseX < x + 1 || mouseX >= x + width - 3 || mouseY < tableY + 1
                || mouseY >= tableY + 1 + rows * TABLE_ROW) {
            return null;
        }
        int row = (int) ((mouseY - tableY - 1) / TABLE_ROW);
        int index = tableScroll + row;
        List<FileEntry> visible = visibleEntries();
        return index >= 0 && index < visible.size() ? visible.get(index) : null;
    }

    private @Nullable FileEntry entryAt(double mouseX, double mouseY) {
        return rowAt(mouseX, mouseY);
    }

    private @Nullable SortColumn headerAt(double mouseX) {
        int x = rightX();
        int width = rightW();
        int rightEdge = x + width - 4;
        int ownerWidth = root == FileRoot.SHARED ? 64 : 0;
        int sizeX = rightEdge - ownerWidth - 52;
        int extX = sizeX - 44;
        int nameX = x + 4;
        if (mouseX >= nameX && mouseX < extX) {
            return SortColumn.NAME;
        }
        if (mouseX >= extX && mouseX < sizeX) {
            return SortColumn.EXTENSION;
        }
        if (mouseX >= sizeX && mouseX < rightEdge - ownerWidth) {
            return SortColumn.SIZE;
        }
        if (ownerWidth > 0 && mouseX >= rightEdge - ownerWidth && mouseX < rightEdge) {
            return SortColumn.OWNER;
        }
        return null;
    }

    private boolean clickMenu(double mouseX, double mouseY, int button) {
        if (menu.isEmpty()) {
            return false;
        }
        int height = menu.size() * 12 + 4;
        if (mouseX < menuX || mouseX >= menuX + menuWidth || mouseY < menuY || mouseY >= menuY + height) {
            return false;
        }
        int index = (int) ((mouseY - menuY - 2) / 12);
        if (index >= 0 && index < menu.size()) {
            MenuEntry entry = menu.get(index);
            menu.clear();
            if (entry.enabled()) {
                entry.action().run();
            }
            return true;
        }
        return true;
    }
}
