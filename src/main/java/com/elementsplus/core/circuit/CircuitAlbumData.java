package com.elementsplus.core.circuit;

import com.elementsplus.ModDataComponents;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** 电路图册物品上的数据访问。 */
public final class CircuitAlbumData {
    private CircuitAlbumData() {
    }

    public static List<ItemStack> contents(ItemStack album) {
        if (album == null || album.isEmpty()) {
            return List.of();
        }
        return album.getOrDefault(ModDataComponents.CIRCUIT_ALBUM_CONTENTS, List.of());
    }

    public static void setContents(ItemStack album, List<ItemStack> contents) {
        if (contents == null || contents.isEmpty()) {
            album.remove(ModDataComponents.CIRCUIT_ALBUM_CONTENTS);
        } else {
            album.set(ModDataComponents.CIRCUIT_ALBUM_CONTENTS, List.copyOf(contents));
        }
    }

    public static boolean isAlbum(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.is(com.elementsplus.ModItems.CIRCUIT_ALBUM);
    }
}
