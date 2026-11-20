package com.elementsplus.core.file;

import com.elementsplus.ModDataComponents;
import com.elementsplus.ModItems;
import com.elementsplus.item.PunchedFileItem;
import com.elementsplus.item.TextFileItem;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** 文件夹物品上的数据访问：收纳文本文件、打孔文件，允许嵌套文件夹。 */
public final class FileFolderData {
    /** 防止损坏/恶意的深层嵌套造成栈溢出。 */
    private static final int MAX_DEPTH = 32;

    private static final Comparator<ItemStack> ORDER = Comparator
            .comparing((ItemStack stack) -> !isFolder(stack))
            .thenComparing(stack -> stack.getHoverName().getString().toLowerCase(Locale.ROOT));

    private FileFolderData() {
    }

    /** 返回按“文件夹在前、其余按名称”排序的副本；客户端索引与服务端操作保持一致。 */
    public static List<ItemStack> contents(ItemStack folder) {
        if (folder == null || folder.isEmpty()) {
            return List.of();
        }
        List<ItemStack> list = new ArrayList<>(
                folder.getOrDefault(ModDataComponents.FILE_FOLDER_CONTENTS, List.of()));
        list.sort(ORDER);
        return list;
    }

    public static void setContents(ItemStack folder, List<ItemStack> contents) {
        if (contents == null || contents.isEmpty()) {
            folder.remove(ModDataComponents.FILE_FOLDER_CONTENTS);
            return;
        }
        List<ItemStack> sorted = new ArrayList<>(contents);
        sorted.sort(ORDER);
        folder.set(ModDataComponents.FILE_FOLDER_CONTENTS, List.copyOf(sorted));
    }

    public static boolean isFolder(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.is(ModItems.FILE_FOLDER);
    }

    public static boolean isStorable(ItemStack stack) {
        return stack != null && !stack.isEmpty()
                && (stack.is(ModItems.TEXT_FILE) || stack.is(ModItems.PUNCHED_FILE)
                || isFolder(stack));
    }

    /** 单个物品占用的“文件数”：文件为 1，文件夹为自身 + 内部所有物品的文件数。 */
    public static int itemCount(ItemStack stack) {
        return itemCount(stack, 0);
    }

    private static int itemCount(ItemStack stack, int depth) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        if (!isFolder(stack)) {
            return 1;
        }
        if (depth >= MAX_DEPTH) {
            return 1;
        }
        int total = 1;
        for (ItemStack child : contents(stack)) {
            total += itemCount(child, depth + 1);
        }
        return total;
    }

    public static int itemCount(List<ItemStack> contents) {
        int total = 0;
        for (ItemStack stack : contents) {
            total += itemCount(stack);
        }
        return total;
    }

    /** 单个物品占用的数据字节数；文件夹为内部所有文件的总和。 */
    public static int fileBytes(ItemStack stack) {
        return fileBytes(stack, 0);
    }

    private static int fileBytes(ItemStack stack, int depth) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        if (isFolder(stack)) {
            if (depth >= MAX_DEPTH) {
                return 0;
            }
            return totalBytes(contents(stack), depth + 1);
        }
        byte[] data = stack.is(ModItems.TEXT_FILE)
                ? TextFileItem.getData(stack) : PunchedFileItem.getData(stack);
        return data == null ? 0 : data.length;
    }

    /** 文件夹中所有物品占用的数据字节总数。 */
    public static int totalBytes(List<ItemStack> contents) {
        return totalBytes(contents, 0);
    }

    private static int totalBytes(List<ItemStack> contents, int depth) {
        int total = 0;
        for (ItemStack stack : contents) {
            total += fileBytes(stack, depth);
        }
        return total;
    }
}
