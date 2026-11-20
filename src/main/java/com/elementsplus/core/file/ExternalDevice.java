package com.elementsplus.core.file;

import com.elementsplus.ModItems;
import com.elementsplus.core.circuit.CircuitComponent;
import com.elementsplus.core.circuit.CircuitComponentRegistry;
import com.elementsplus.core.circuit.CircuitItemData;
import com.elementsplus.core.circuit.diagram.CircuitDiagram;
import com.elementsplus.core.circuit.storage.CircuitStorage;
import com.elementsplus.core.circuit.storage.StoredCircuit;
import com.elementsplus.item.PunchedFileItem;
import com.elementsplus.item.TextFileItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 外接设备槽位中物品对应的虚拟文件。服务端与客户端共用同一套命名规则。
 */
public final class ExternalDevice {

    /**
     * @param name     虚拟文件名
     * @param data     文件内容
     * @param writable 是否可写
     * @param listed   是否显示在外接设备列表中（纸只作为打印目标）
     */
    public record Entry(String name, byte[] data, boolean writable, boolean listed) {
    }

    /** 文件夹设备展开后的虚拟节点；path 为外接设备根内的相对路径。 */
    public record VirtualNode(String name, String path, ItemStack stack, boolean folder,
                              int size, List<VirtualNode> children) {
    }

    private ExternalDevice() {
    }

    public static @Nullable Entry describe(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        if (stack.is(ModItems.DISK)) {
            return new Entry(deviceName(stack, "disk", ".bin"), DiskData.read(stack), true, true);
        }
        if (stack.is(ModItems.TEXT_FILE)) {
            return new Entry(deviceName(stack, "text", ".txt"), TextFileItem.getData(stack), false, true);
        }
        if (stack.is(ModItems.PUNCHED_FILE)) {
            return new Entry(deviceName(stack, "punched", ".bin"),
                    PunchedFileItem.getData(stack), false, true);
        }
        if (stack.is(ModItems.CIRCUIT_DIAGRAM)) {
            return new Entry(circuitDiagramName(stack), circuitDiagramData(stack), false, true);
        }
        if (stack.is(Items.PAPER)) {
            return new Entry(deviceName(stack, "paper", ".txt"), new byte[0], true, false);
        }
        if (stack.is(ModItems.PAPER_TAPE_READER)) {
            return new Entry(deviceName(stack, "tape", ".bin"), new byte[0], false, false);
        }
        return null;
    }

    /** 外接设备槽位中的文件夹：根目录只显示一个文件夹，内容按虚拟树只读展开。 */
    public static boolean isFolderDevice(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.is(ModItems.FILE_FOLDER);
    }

    public static String folderRootName(ItemStack stack) {
        Component custom = stack.get(DataComponents.CUSTOM_NAME);
        if (custom != null && !custom.getString().isBlank()) {
            return safeFileName(custom.getString());
        }
        return "folder";
    }

    private static String displayName(ItemStack stack) {
        Component custom = stack.get(DataComponents.CUSTOM_NAME);
        if (custom != null && !custom.getString().isBlank()) {
            return safeFileName(custom.getString());
        }
        if (isFolderDevice(stack)) {
            return "folder";
        }
        if (stack.is(ModItems.TEXT_FILE)) {
            return "text.txt";
        }
        if (stack.is(ModItems.PUNCHED_FILE)) {
            return "punched.bin";
        }
        return safeFileName(stack.getHoverName().getString());
    }

    /** 同一目录重名时追加 (1)、(2)…；文件后缀保留在末尾。 */
    private static String uniqueName(String base, Set<String> used, boolean folder) {
        String candidate = base;
        if (used.add(candidate)) {
            return candidate;
        }
        int dot = folder ? -1 : base.lastIndexOf('.');
        String stem = dot > 0 ? base.substring(0, dot) : base;
        String extension = dot > 0 ? base.substring(dot) : "";
        for (int i = 1; i < 10000; i++) {
            candidate = limitName(stem + " (" + i + ")" + extension);
            if (used.add(candidate)) {
                return candidate;
            }
        }
        candidate = limitName(base + " (" + used.size() + ")");
        used.add(candidate);
        return candidate;
    }

    private static String limitName(String name) {
        return name.length() <= FilePaths.MAX_NAME_LENGTH
                ? name : name.substring(0, FilePaths.MAX_NAME_LENGTH);
    }

    private static VirtualNode rootNode(ItemStack folder) {
        String rootName = folderRootName(folder);
        List<VirtualNode> children = buildChildren(folder, rootName, 0);
        return new VirtualNode(rootName, rootName, folder, true,
                FileFolderData.fileBytes(folder), children);
    }

    private static List<VirtualNode> buildChildren(ItemStack folder, String parent, int depth) {
        if (depth >= 32) {
            return List.of();
        }
        List<VirtualNode> result = new ArrayList<>();
        Set<String> used = new HashSet<>();
        for (ItemStack child : FileFolderData.contents(folder)) {
            boolean isFolder = isFolderDevice(child);
            String name = uniqueName(displayName(child), used, isFolder);
            String path = parent + "/" + name;
            List<VirtualNode> children = isFolder
                    ? buildChildren(child, path, depth + 1) : List.of();
            result.add(new VirtualNode(name, path, child, isFolder,
                    FileFolderData.fileBytes(child), children));
        }
        return result;
    }

    public static @Nullable VirtualNode resolve(ItemStack device, @Nullable String rawPath) {
        if (!isFolderDevice(device)) {
            return null;
        }
        String path = FilePaths.normalize(rawPath);
        if (path == null || path.isEmpty()) {
            return null;
        }
        VirtualNode root = rootNode(device);
        if (path.equals(root.path())) {
            return root;
        }
        if (!path.startsWith(root.path() + "/")) {
            return null;
        }
        String[] parts = path.substring(root.path().length() + 1).split("/");
        VirtualNode current = root;
        for (String part : parts) {
            VirtualNode next = null;
            for (VirtualNode child : current.children()) {
                if (child.name().equals(part)) {
                    next = child;
                    break;
                }
            }
            if (next == null) {
                return null;
            }
            current = next;
        }
        return current;
    }

    public static List<FileEntry> list(ItemStack device, @Nullable String rawPath) {
        String path = FilePaths.normalize(rawPath);
        if (path == null) {
            return List.of();
        }
        if (!isFolderDevice(device)) {
            Entry entry = describe(device);
            if (entry == null || !entry.listed()
                    || (!path.isEmpty() && !entry.name().equals(path))) {
                return List.of();
            }
            return List.of(FileEntry.file(entry.name(), entry.name(), entry.data().length,
                    entry.writable()));
        }
        VirtualNode root = rootNode(device);
        if (path.isEmpty()) {
            return List.of(new FileEntry(root.path(), root.name(), true, root.size(),
                    null, "", false, false, false));
        }
        VirtualNode node = resolve(device, path);
        if (node == null || !node.folder()) {
            return List.of();
        }
        List<FileEntry> entries = new ArrayList<>();
        for (VirtualNode child : node.children()) {
            entries.add(new FileEntry(child.path(), child.name(), child.folder(), child.size(),
                    null, "", false, false, false));
        }
        return entries;
    }

    public static byte @Nullable [] read(ItemStack device, @Nullable String rawPath) {
        String path = FilePaths.normalize(rawPath);
        if (path == null) {
            return null;
        }
        if (!isFolderDevice(device)) {
            Entry entry = describe(device);
            return entry != null && entry.listed() && entry.name().equals(path)
                    ? entry.data() : null;
        }
        VirtualNode node = resolve(device, path);
        if (node == null || node.folder()) {
            return null;
        }
        ItemStack stack = node.stack();
        if (stack.is(ModItems.TEXT_FILE)) {
            return TextFileItem.getData(stack);
        }
        if (stack.is(ModItems.PUNCHED_FILE)) {
            return PunchedFileItem.getData(stack);
        }
        return null;
    }

    public static String deviceName(ItemStack stack, String key, String extension) {        Component custom = stack.get(DataComponents.CUSTOM_NAME);
        if (custom != null && !custom.getString().isBlank()) {
            // 自定义物品名直接作为外接设备文件名，不再自动追加后缀。
            return safeFileName(custom.getString());
        }
        // 默认名必须与客户端/服务端语言无关，否则打开文件时路径无法对应。
        return key + extension;
    }

    public static String circuitDiagramName(ItemStack stack) {
        Component custom = stack.get(DataComponents.CUSTOM_NAME);
        if (custom != null && !custom.getString().isBlank()) {
            return safeFileName(custom.getString()) + ".dat";
        }
        StoredCircuit stored = CircuitItemData.stored(stack);
        if (stored != null) {
            return safeFileName(CircuitStorage.get().displayName(stored).getString()) + ".dat";
        }
        CircuitComponent component = CircuitComponentRegistry.byId(CircuitItemData.componentId(stack));
        if (component != null) {
            return safeFileName(component.getName().getString()) + ".dat";
        }
        return Component.translatable("gui.elements-plus.file.device.circuit_diagram").getString() + ".dat";
    }

    public static byte[] circuitDiagramData(ItemStack stack) {
        CircuitDiagram diagram = CircuitItemData.diagram(stack);
        byte[] data = CircuitDiagramFile.encode(diagram);
        return data == null ? new byte[0] : data;
    }

    public static String safeFileName(String raw) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < raw.length() && builder.length() < FilePaths.MAX_NAME_LENGTH; i++) {
            char c = raw.charAt(i);
            if (c < 0x20 || c == '/' || c == '\\' || c == ':' || c == '*' || c == '?'
                    || c == '"' || c == '<' || c == '>' || c == '|') {
                builder.append('_');
            } else {
                builder.append(c);
            }
        }
        String result = builder.toString().trim();
        while (!result.isEmpty() && (result.endsWith(".") || result.endsWith(" "))) {
            result = result.substring(0, result.length() - 1);
        }
        return result.isEmpty() ? "file" : result;
    }
}
