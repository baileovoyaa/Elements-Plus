package com.elementsplus.core.experiment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * 多字节存储器章节中与存档世界种子绑定的实验数据。
 * 同一个存档每次打开都得到同一组 ROM 初始内容不同存档互不影响。
 */
public final class MemoryExperimentData {
    private MemoryExperimentData() {
    }

    /** 从世界种子生成 16 个互不重复的 ROM 字节。 */
    public static byte[] romValues(long worldSeed) {
        List<Integer> pool = new ArrayList<>(256);
        for (int i = 0; i < 256; i++) {
            pool.add(i);
        }
        Collections.shuffle(pool, new Random(worldSeed ^ 0x5EED_10A7L));
        byte[] result = new byte[16];
        for (int i = 0; i < result.length; i++) {
            result[i] = (byte) (int) pool.get(i);
        }
        return result;
    }
}
