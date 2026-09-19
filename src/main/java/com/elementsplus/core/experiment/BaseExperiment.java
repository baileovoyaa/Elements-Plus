package com.elementsplus.core.experiment;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public abstract class BaseExperiment {
    public static class Context {
        public int time = 0;
        public BlockEntity blockEntity;
        public ItemStack itemStack;

        /**
         * 结束实验
         */
        public void complete(boolean success) {
            // TODO
        }
    }

    /**
     * 会在实验的每个tick执行
     *
     * @param context
     * @return 返回false会中止实验
     */
    public abstract boolean tick(Context context);

    public boolean preCheck(Context context) {
        return true;
    }

    private String name;
    private ResourceLocation icon;

    /**
     * 实验的显示名称（翻译键后缀），用于实验列表等界面。
     */
    public String getName() {
        return name;
    }

    public Component getDisplayName() {
        return name == null ? Component.literal("?") : Component.translatable("experiment.elements-plus." + name);
    }

    public ResourceLocation getIcon() {
        return icon;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setIcon(ResourceLocation icon) {
        this.icon = icon;
    }
}
