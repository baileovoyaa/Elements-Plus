package com.elementsplus.core.experiment;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;
import java.util.function.Function;

public abstract class BaseExperiment {
    public static class Context {
        public int time = 0;
        public BlockEntity blockEntity;
        public ItemStack itemStack;
        /**
         * 当前执行的测例索引（由宿主维护）。
         */
        public int testIndex = 0;

        private final ExperimentHost host;

        public Context(ExperimentHost host, BlockEntity blockEntity, ItemStack itemStack) {
            this.host = host;
            this.blockEntity = blockEntity;
            this.itemStack = itemStack;
        }

        /**
         * 结束实验
         */
        public void complete(boolean success) {
            if (host != null) {
                host.finishExperiment(success);
            }
        }

        public ItemStack getItemStack() {
            return itemStack;
        }

        /**
         * 设置实验转化的物品
         *
         * @param itemStackFunction
         */
        public void setItemStack(Function<ItemStack, ItemStack> itemStackFunction) {
            if (host != null) {
                host.transformActiveItem(itemStackFunction);
            }
        }

        /**
         * 报告一个测例的结果，用于刷新进度条与测例列表。
         */
        public void reportTestResult(int index, boolean pass) {
            if (host != null) {
                host.reportTestResult(index, pass);
            }
        }

        /**
         * 设置失败原因（statusButton 悬停提示）。
         */
        public void setErrorLines(List<Component> lines) {
            if (host != null) {
                host.setErrorLines(lines);
            }
        }
    }

    /**
     * 会在实验的每个tick执行
     *
     * @param context
     * @return 返回false会中止实验
     */
    public abstract boolean tick(Context context);

    /**
     * 预检：返回不通过的原因（空的集合表示通过）。由子类重写。
     */
    public List<Component> preCheckReasons(Context context) {
        return List.of();
    }

    public boolean preCheck(Context context) {
        return preCheckReasons(context).isEmpty();
    }

    /**
     * 测例总数，用于初始化进度显示；默认值 0 表示无测例。
     */
    public int getTestCaseCount() {
        return 0;
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

    /**
     * 由子类重写，在实验结束时执行（如转化物品）
     *
     * @param ctx
     */
    public void onComplete(Context ctx, boolean success) {

    }
}
