package com.elementsplus.blocks.pipe;

import net.minecraft.core.Direction;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static net.minecraft.core.Direction.*;

public enum LShapePipeState {
    NORTH_DOWN(Set.of(NORTH, DOWN), Axis.X, SOUTH),
    SOUTH_DOWN(Set.of(SOUTH, DOWN), Axis.X, EAST),
    NORTH_UP(Set.of(NORTH, UP), Axis.X, WEST),
    SOUTH_UP(Set.of(SOUTH, UP), Axis.X, NORTH),
    NORTH_WEST(Set.of(NORTH, WEST), Axis.Y, NORTH),
    SOUTH_WEST(Set.of(SOUTH, WEST), Axis.Y, WEST),
    SOUTH_EAST(Set.of(SOUTH, EAST), Axis.Y, SOUTH),
    NORTH_EAST(Set.of(NORTH, EAST), Axis.Y, EAST),
    WEST_UP(Set.of(WEST, UP), Axis.Z, NORTH),
    EAST_UP(Set.of(EAST, UP), Axis.Z, WEST),
    WEST_DOWN(Set.of(WEST, DOWN), Axis.Z, EAST),
    EAST_DOWN(Set.of(EAST, DOWN), Axis.Z, SOUTH);

    private final Set<Direction> directions;
    public final Axis axis;
    public final Direction startDirection;

    LShapePipeState(Set<Direction> directions, Axis axis, Direction startDirection) {
        this.directions = directions;
        this.axis = axis;
        this.startDirection = startDirection;
    }

    public Set<Direction> getDirections() {
        return directions;
    }

    // 静态查找方法（使用 HashMap 缓存，提升频繁查找的性能）
    private static final Map<Set<Direction>, LShapePipeState> DIRECTIONS_MAP = new HashMap<>();
    private static final Map<Pair<Direction, Axis>, LShapePipeState> PAIR_MAP = new HashMap<>();

    static {
        for (LShapePipeState key : LShapePipeState.values()) {
            DIRECTIONS_MAP.put(key.directions, key);
        }
        for (LShapePipeState key : LShapePipeState.values()) {
            PAIR_MAP.put(Pair.of(key.startDirection, key.axis), key);
        }
    }

    public static LShapePipeState fromDirectionSet(Set<Direction> directions) {
        LShapePipeState result = DIRECTIONS_MAP.get(directions);
        if (result == null) {
            throw new IllegalArgumentException("未知的枚举值: " + directions);
        }
        return result;
    }

    public static LShapePipeState fromAxisAndStartDirection(Axis axis, Direction startDirection) {
        LShapePipeState result = PAIR_MAP.get(Pair.of(startDirection, axis));
        if (result == null) {
            throw new IllegalArgumentException("未知的枚举值: " + Pair.of(axis, startDirection));
        }
        return result;
    }

    // 如果需要，也可以返回 Optional 避免异常
    public static Optional<LShapePipeState> safeFromDirectionSet(Set<Direction> directions) {
        return Optional.ofNullable(DIRECTIONS_MAP.get(directions));
    }

    public static Optional<LShapePipeState> safeFromAxisAndStartDirection(Axis axis, Direction startDirection) {
        return Optional.ofNullable(PAIR_MAP.get(Pair.of(startDirection, axis)));
    }
}
