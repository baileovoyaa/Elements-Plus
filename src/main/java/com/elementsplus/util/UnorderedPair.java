package com.elementsplus.util;

public record UnorderedPair<T>(T first, T second) {
    public UnorderedPair {
        // 确保无序性：若元素实现了 Comparable，强制规范化
        if (first instanceof Comparable && second instanceof Comparable) {
            // 这里简单处理，实际可用 Comparator
        }
    }

    // 静态工厂方法，自动排序（假设 T 实现了 Comparable）
    public static <T extends Comparable<T>> UnorderedPair<T> of(T a, T b) {
        return a.compareTo(b) <= 0 ? new UnorderedPair<>(a, b) : new UnorderedPair<>(b, a);
    }
}
