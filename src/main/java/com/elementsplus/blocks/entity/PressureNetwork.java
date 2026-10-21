package com.elementsplus.blocks.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * 服务端共享压力网络。
 * <p>
 * 以密闭空间的最小角坐标为键：同一片密闭空间被多台加压机连接时，
 * 所有机器读写同一份压强，不会各算各的。
 * <p>
 * 当前为内存实现（按 {@link Level} 弱引用保存），世界重载后压强会重置；
 * 后续如果需要持久化，可以改为 {@code SavedData}。
 */
public final class PressureNetwork {
    private static final Map<Level, PressureNetwork> NETWORKS = new WeakHashMap<>();

    private final Map<BlockPos, Double> pressures = new HashMap<>();
    private final Map<BlockPos, Long> lastLeakTick = new HashMap<>();
    private final Map<BlockPos, Long> lastEffectSecond = new HashMap<>();

    private PressureNetwork() {
    }

    public static PressureNetwork get(Level level) {
        return NETWORKS.computeIfAbsent(level, ignored -> new PressureNetwork());
    }

    /** 空间当前压强；未记录时按标准大气压 1.0 处理。 */
    public double get(BlockPos key) {
        return this.pressures.getOrDefault(key, 1.0);
    }

    public void set(BlockPos key, double pressure) {
        this.pressures.put(key, pressure);
    }

    public void remove(BlockPos key) {
        this.pressures.remove(key);
        this.lastLeakTick.remove(key);
        this.lastEffectSecond.remove(key);
    }

    /**
     * 同一 tick 内同一空间只泄漏一次。返回 true 表示由调用方执行本次泄漏。
     */
    public boolean markLeak(BlockPos key, long gameTime) {
        Long last = this.lastLeakTick.get(key);
        if (last != null && last == gameTime) {
            return false;
        }
        this.lastLeakTick.put(key, gameTime);
        return true;
    }

    /**
     * 同一秒内同一空间只施加一次生物效果。返回 true 表示由调用方施加。
     */
    public boolean markEffect(BlockPos key, long second) {
        Long last = this.lastEffectSecond.get(key);
        if (last != null && last == second) {
            return false;
        }
        this.lastEffectSecond.put(key, second);
        return true;
    }
}
