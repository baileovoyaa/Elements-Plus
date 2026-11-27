package com.elementsplus.network;

import com.elementsplus.blocks.entity.ExperimentTableBlockEntity;
import com.elementsplus.menu.ExperimentTableMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * 服务端载荷门禁：所有 C2S 载荷在改动世界状态之前都必须经过这里。
 *
 * <p>把三类判据集中到一处，避免每个 handler 各写一套（这正是此前实验桌载荷漏掉校验的原因）：</p>
 * <ul>
 *   <li>菜单：玩家当前打开的确实是对应类型的菜单，可选校验 {@code containerId} 以防陈旧菜单；</li>
 *   <li>实验桌：玩家确实打开了这张实验桌（{@link ExperimentTableMenu#getOpenTable}）；</li>
 *   <li>距离：目标方块在 {@link #MAX_REACH} 格以内，服务端不信任客户端视角。</li>
 * </ul>
 *
 * <p>所有方法都在服务端线程调用，不读写客户端状态。</p>
 */
public final class ServerPayloadGate {
    /**
     * 远程操作方块实体的最大距离。
     *
     * <p>与既有的红石接口 / 扳手载荷保持一致（64 格）。菜单类载荷另有
     * {@link AbstractContainerMenu#stillValid} 的近距离约束。</p>
     */
    public static final double MAX_REACH = 64.0;

    private ServerPayloadGate() {
    }

    /** 玩家当前打开的菜单是 {@code type} 时返回它，否则返回 {@code null}。 */
    @Nullable
    public static <T extends AbstractContainerMenu> T menu(ServerPlayer player, Class<T> type) {
        return type.isInstance(player.containerMenu) ? type.cast(player.containerMenu) : null;
    }

    /** 同上，并要求 {@code containerId} 一致（客户端可能在收到关窗包前继续发包）。 */
    @Nullable
    public static <T extends AbstractContainerMenu> T menu(ServerPlayer player, Class<T> type, int containerId) {
        T menu = menu(player, type);
        return menu != null && menu.containerId == containerId ? menu : null;
    }

    /** 玩家是否确实打开了这张实验桌。 */
    public static boolean isOpenTable(ServerPlayer player, @Nullable BlockPos pos) {
        return pos != null && pos.equals(ExperimentTableMenu.getOpenTable(player.getUUID()));
    }

    /** 目标方块是否在交互距离内。 */
    public static boolean withinReach(ServerPlayer player, @Nullable BlockPos pos) {
        if (pos == null) {
            return false;
        }
        double max = MAX_REACH * MAX_REACH;
        return player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= max;
    }

    /**
     * 取玩家正在交互的方块实体：同时校验类型与距离。
     */
    @Nullable
    public static <T extends BlockEntity> T blockEntity(ServerPlayer player, @Nullable BlockPos pos, Class<T> type) {
        if (pos == null || !withinReach(player, pos)) {
            return null;
        }
        BlockEntity entity = player.level().getBlockEntity(pos);
        return type.isInstance(entity) ? type.cast(entity) : null;
    }

    /**
     * 取玩家确实打开着的实验桌：菜单绑定 + 距离 + 方块实体类型，任一不满足返回 {@code null}。
     *
     * <p>实验桌的所有 C2S 载荷（启停、编译自定义元件、切换选中项、文件系统）都必须先经过这里，
     * 否则任意玩家可以向任意坐标发包含实验桌操作，从而远程操作他人的实验桌。</p>
     */
    @Nullable
    public static ExperimentTableBlockEntity openExperimentTable(ServerPlayer player, @Nullable BlockPos pos) {
        if (!isOpenTable(player, pos)) {
            return null;
        }
        return blockEntity(player, pos, ExperimentTableBlockEntity.class);
    }

    /** 同 {@link #openExperimentTable(ServerPlayer, BlockPos)}，坐标取自玩家当前打开的菜单。 */
    @Nullable
    public static ExperimentTableBlockEntity openExperimentTable(ServerPlayer player) {
        return openExperimentTable(player, ExperimentTableMenu.getOpenTable(player.getUUID()));
    }
}
