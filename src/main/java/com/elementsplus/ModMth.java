package com.elementsplus;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ModMth {
    public static List<Direction> orderedByNearest(Entity entity, Collection<Direction> directions) {
        if (directions == null || directions.isEmpty()) {
            return Collections.emptyList();
        }
        // 获取当前实体的视线方向向量（单位向量）
        Vec3 lookVec = entity.getViewVector(1.0F);
        // 将集合转为列表并排序
        List<Direction> sorted = new ArrayList<>(directions);
        sorted.sort((d1, d2) -> {
            double dot1 = Vec3.atLowerCornerOf(d1.getNormal()).dot(lookVec);
            double dot2 = Vec3.atLowerCornerOf(d2.getNormal()).dot(lookVec);
            return Double.compare(dot2, dot1); // 点积越大表示方向越接近，降序排列
        });
        return sorted;
    }
}
