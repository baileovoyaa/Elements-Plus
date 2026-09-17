package com.elementsplus.player;

import com.elementsplus.ElementsPlus;
import com.mojang.serialization.DataResult;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

public final class PlayerToolboxAttachment {
    public static final AttachmentType<PlayerToolbox> PLAYER_TOOLBOX =
            AttachmentRegistry.<PlayerToolbox>builder()
                    .persistent(PlayerToolbox.CODEC)
                    .copyOnDeath()
                    .syncWith(PlayerToolbox.STREAM_CODEC, AttachmentSyncPredicate.targetOnly())
                    .buildAndRegister(ElementsPlus.id("player_toolbox"));

    public static PlayerToolbox get(net.minecraft.world.entity.player.Player player) {
        PlayerToolbox toolbox = player.getAttached(PLAYER_TOOLBOX);
        if (toolbox != null) {
            return toolbox;
        }

        // On join, Fabric only deserializes persistent attachments inside Entity.readNbt, but in
        // Vanilla 1.21.1 a player's save data is applied via Entity.load (which ends in
        // readAdditionalSaveData), so the in-memory attachment can be empty even though the player
        // save (dat) already contains a toolbox. Read the player data directly from disk so we never
        // clobber persisted data with a freshly created default.
        PlayerToolbox restored = loadFromSavedData(player);
        if (restored != null) {
            player.setAttached(PLAYER_TOOLBOX, restored);
            ElementsPlus.LOGGER.info("[toolbox] restored {} groups from saved player data for {} (Fabric does not auto-load persistent player attachments on join in MC 1.21.1)",
                    restored.groups.size(), player.getName().getString());
            return restored;
        }

        PlayerToolbox fresh = PlayerToolbox.createDefault();
        player.setAttached(PLAYER_TOOLBOX, fresh);
        return fresh;
    }

    /** Reads the persistent attachment out of the player's saved data (playerdata/{uuid}.dat). */
    private static PlayerToolbox loadFromSavedData(net.minecraft.world.entity.player.Player player) {
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
            ElementsPlus.LOGGER.error("[toolbox] failed to read saved player data for " + player.getName().getString(), e);
            return null;
        }
        if (saved.isEmpty()) {
            return null;
        }

        CompoundTag attachments = saved.get().getCompound(AttachmentTarget.NBT_ATTACHMENT_KEY);
        String key = PLAYER_TOOLBOX.identifier().toString();
        if (!attachments.contains(key)) {
            return null;
        }

        RegistryOps<Tag> registryOps = RegistryOps.create(NbtOps.INSTANCE, server.registryAccess());
        DataResult<PlayerToolbox> result = PlayerToolbox.CODEC.parse(registryOps, attachments.get(key));
        return result.result().orElse(null);
    }
}