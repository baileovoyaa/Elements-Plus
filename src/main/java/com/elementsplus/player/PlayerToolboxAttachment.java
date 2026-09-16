package com.elementsplus.player;

import com.elementsplus.ElementsPlus;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

import java.util.Optional;

public final class PlayerToolboxAttachment {
    public static final AttachmentType<PlayerToolbox> PLAYER_TOOLBOX =
            AttachmentRegistry.<PlayerToolbox>builder()
                    .persistent(PlayerToolbox.CODEC)
                    .buildAndRegister(ElementsPlus.id("player_toolbox"));

    public static PlayerToolbox get(net.minecraft.world.entity.player.Player player) {
        PlayerToolbox toolbox = player.getAttachedOrElse(PLAYER_TOOLBOX, null);
        if (toolbox == null) {
            toolbox = PlayerToolbox.createDefault();
            player.setAttached(PLAYER_TOOLBOX, toolbox);
        }
        return toolbox;
    }
}
