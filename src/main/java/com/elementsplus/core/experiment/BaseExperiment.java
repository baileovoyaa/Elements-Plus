package com.elementsplus.core.experiment;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public interface BaseExperiment {
    class Context {
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
    boolean tick(Context context);

    default boolean preCheck(Context context) {
        return true;
    }

    /**
     * 实验的显示名称（翻译键后缀），用于实验列表等界面。
     */
    default String getName() {
        return null;
    }

    default void setName(String name) {
    }
}
