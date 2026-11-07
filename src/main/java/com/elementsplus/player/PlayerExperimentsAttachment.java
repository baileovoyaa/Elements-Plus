package com.elementsplus.player;

import com.elementsplus.ElementsPlus;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class PlayerExperimentsAttachment {
    public static final Codec<Set<String>> CODEC =
            Codec.STRING.listOf().xmap(PlayerExperimentsAttachment::linkedSet, ArrayList::new);

    public static final AttachmentType<Set<String>> COMPLETED_EXPERIMENTS =
            AttachmentRegistry.<Set<String>>builder()
                    .persistent(CODEC)
                    .copyOnDeath()
                    .buildAndRegister(ElementsPlus.id("completed_experiments"));

    /** 玩家被强制解锁的章节名称；这些章节不受依赖关系限制。 */
    public static final AttachmentType<Set<String>> FORCED_CHAPTER_UNLOCKS =
            AttachmentRegistry.<Set<String>>builder()
                    .persistent(CODEC)
                    .copyOnDeath()
                    .buildAndRegister(ElementsPlus.id("forced_chapter_unlocks"));

    private static Set<String> linkedSet(List<String> list) {
        return new LinkedHashSet<>(list);
    }

    /** 加入服务器时同时恢复两个实验相关附件。 */
    public static void ensureLoaded(Player player) {
        get(player);
        getForced(player);
    }

    public static Set<String> get(Player player) {
        Set<String> completed = player.getAttached(COMPLETED_EXPERIMENTS);
        if (completed != null) {
            return completed;
        }

        Set<String> restored = loadFromSavedData(player, COMPLETED_EXPERIMENTS);
        if (restored != null) {
            player.setAttached(COMPLETED_EXPERIMENTS, restored);
            return restored;
        }

        Set<String> fresh = new LinkedHashSet<>();
        player.setAttached(COMPLETED_EXPERIMENTS, fresh);
        return fresh;
    }

    public static Set<String> getForced(Player player) {
        Set<String> forced = player.getAttached(FORCED_CHAPTER_UNLOCKS);
        if (forced != null) {
            return forced;
        }

        Set<String> restored = loadFromSavedData(player, FORCED_CHAPTER_UNLOCKS);
        if (restored != null) {
            player.setAttached(FORCED_CHAPTER_UNLOCKS, restored);
            return restored;
        }

        Set<String> fresh = new LinkedHashSet<>();
        player.setAttached(FORCED_CHAPTER_UNLOCKS, fresh);
        return fresh;
    }

    /** Reads a persistent attachment out of the player's saved data (playerdata/{uuid}.dat). */
    private static Set<String> loadFromSavedData(Player player, AttachmentType<Set<String>> attachment) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return null;
        }
        MinecraftServer server = serverPlayer.server;
        if (server == null) {
            return null;
        }

        Optional<CompoundTag> saved;
        try {
            saved = server.getPlayerList().load(serverPlayer);
        } catch (Exception e) {
            ElementsPlus.LOGGER.error("[experiments] failed to read saved player data for " + player.getName().getString(), e);
            return null;
        }
        if (saved.isEmpty()) {
            return null;
        }

        CompoundTag attachments = saved.get().getCompound(AttachmentTarget.NBT_ATTACHMENT_KEY);
        String key = attachment.identifier().toString();
        if (!attachments.contains(key)) {
            return null;
        }

        RegistryOps<Tag> registryOps = RegistryOps.create(NbtOps.INSTANCE, server.registryAccess());
        DataResult<Set<String>> result = CODEC.parse(registryOps, attachments.get(key));
        return result.result().orElse(null);
    }
}