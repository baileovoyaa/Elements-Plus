package com.elementsplus.core.file;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 内存文件系统的容量与覆盖语义。
 *
 * <p>回归重点：覆盖式粘贴必须是“先确认能写成功、再删除旧目标”。曾经实现成
 * “先 delete 目标、再 write”，一旦容量不足就会永久丢失用户文件。</p>
 */
class MemoryFileSystemTest {

    /** 一个路径占用的字节数：文件头（按文件名算）+ 内容。 */
    private static int cost(String path, int dataLength) {
        return FileHeader.sizeFor(FilePaths.name(path)) + dataLength;
    }

    private static MemoryFileSystem fullFileSystem() {
        // b.txt(10B) + dir/b.txt(1B) 恰好填满
        MemoryFileSystem fs = new MemoryFileSystem(cost("b.txt", 10) + cost("dir/b.txt", 1));
        assertTrue(fs.createFolder("dir").success());
        assertTrue(fs.write("b.txt", new byte[10]).success());
        assertTrue(fs.write("dir/b.txt", new byte[]{7}).success());
        assertEquals(fs.getCapacity(), fs.usedBytes(), "前置条件：文件系统已满");
        return fs;
    }

    @Test
    void overwriteCopyKeepsTargetWhenCapacityIsInsufficient() {
        MemoryFileSystem fs = fullFileSystem();

        MemoryFileSystem.WriteResult result = fs.transfer("b.txt", "dir", true, true);

        assertEquals(MemoryFileSystem.WriteResult.Failure.NO_SPACE, result.failure());
        assertTrue(fs.exists("b.txt"), "复制失败时源文件必须保留");
        assertEquals(1, fs.fileSize("dir/b.txt"), "覆盖失败时旧目标必须保留");
        assertArrayEquals(new byte[]{7}, fs.read("dir/b.txt"));
        assertEquals(fs.getCapacity(), fs.usedBytes(), "失败的操作不能改变占用");
    }

    @Test
    void transferToOverwriteKeepsTargetWhenCapacityIsInsufficient() {
        MemoryFileSystem fs = fullFileSystem();

        MemoryFileSystem.WriteResult result = fs.transferTo("b.txt", "dir/b.txt", true, true);

        assertEquals(MemoryFileSystem.WriteResult.Failure.NO_SPACE, result.failure());
        assertEquals(1, fs.fileSize("dir/b.txt"));
        assertArrayEquals(new byte[]{7}, fs.read("dir/b.txt"));
        assertTrue(fs.exists("b.txt"));
    }

    @Test
    void moveSucceedsWhenFileSystemIsFull() {
        MemoryFileSystem fs = fullFileSystem();
        assertTrue(fs.createFolder("other").success());

        MemoryFileSystem.WriteResult result = fs.transfer("b.txt", "other", false, false);

        assertTrue(result.success(), "移动不改变总占用，满容量时也应成功");
        assertEquals(10, fs.fileSize("other/b.txt"));
        assertFalse(fs.exists("b.txt"));
        assertEquals(fs.getCapacity(), fs.usedBytes());
    }

    @Test
    void overwriteCopyReplacesTargetWhenThereIsRoom() {
        MemoryFileSystem fs = new MemoryFileSystem(1024);
        fs.createFolder("dir");
        byte[] source = {1, 2, 3};
        fs.write("b.txt", source);
        fs.write("dir/b.txt", new byte[]{9, 9, 9, 9, 9});

        MemoryFileSystem.WriteResult result = fs.transfer("b.txt", "dir", true, true);

        assertTrue(result.success());
        assertArrayEquals(source, fs.read("dir/b.txt"));
        assertTrue(fs.exists("b.txt"), "复制时源文件保留");
        assertEquals(cost("b.txt", 3) + cost("dir/b.txt", 3), fs.usedBytes());
    }

    @Test
    void overwriteFolderCopyKeepsTargetWhenCapacityIsInsufficient() {
        MemoryFileSystem fs = new MemoryFileSystem(cost("src/a.txt", 10) + cost("dst/src/a.txt", 1));
        fs.createFolder("src");
        fs.createFolder("dst");
        fs.write("src/a.txt", new byte[10]);
        fs.write("dst/src/a.txt", new byte[]{5});
        assertEquals(fs.getCapacity(), fs.usedBytes(), "前置条件：文件系统已满");

        MemoryFileSystem.WriteResult result = fs.transfer("src", "dst", true, true);

        assertEquals(MemoryFileSystem.WriteResult.Failure.NO_SPACE, result.failure());
        assertEquals(1, fs.fileSize("dst/src/a.txt"), "覆盖失败时旧目录内容必须保留");
        assertEquals(10, fs.fileSize("src/a.txt"));
    }

    @Test
    void transferWithoutOverwriteReportsAlreadyExists() {
        MemoryFileSystem fs = fullFileSystem();

        MemoryFileSystem.WriteResult result = fs.transfer("b.txt", "dir", false, false);

        assertEquals(MemoryFileSystem.WriteResult.Failure.ALREADY_EXISTS, result.failure());
        assertEquals(10, fs.fileSize("b.txt"));
        assertEquals(1, fs.fileSize("dir/b.txt"));
    }

    @Test
    void transferIntoOwnSubtreeIsRejected() {
        MemoryFileSystem fs = new MemoryFileSystem(1024);
        fs.createFolder("a");

        MemoryFileSystem.WriteResult result = fs.transferTo("a", "a/copy", true, false);

        assertEquals(MemoryFileSystem.WriteResult.Failure.INVALID_PATH, result.failure());
        assertFalse(fs.exists("a/copy"));
    }

    @Test
    void transferOfMissingSourceReportsNotFound() {
        MemoryFileSystem fs = new MemoryFileSystem(1024);
        fs.createFolder("dir");

        MemoryFileSystem.WriteResult result = fs.transfer("nope.txt", "dir", false, false);

        assertEquals(MemoryFileSystem.WriteResult.Failure.NOT_FOUND, result.failure());
    }

    @Test
    void overwritingSameFileIsAllowedWhenFull() {
        MemoryFileSystem fs = new MemoryFileSystem(cost("a.txt", 5));
        assertTrue(fs.write("a.txt", new byte[5]).success());

        MemoryFileSystem.WriteResult result = fs.write("a.txt", new byte[]{1, 2, 3, 4, 5});

        assertTrue(result.success(), "等长覆盖不改变占用，满容量时也应成功");
        assertArrayEquals(new byte[]{1, 2, 3, 4, 5}, fs.read("a.txt"));
    }

    @Test
    void writeRejectsWhenContentExceedsCapacity() {
        MemoryFileSystem fs = new MemoryFileSystem(cost("a.txt", 3));

        MemoryFileSystem.WriteResult result = fs.write("a.txt", new byte[4]);

        assertEquals(MemoryFileSystem.WriteResult.Failure.NO_SPACE, result.failure());
        assertFalse(fs.exists("a.txt"));
    }

    /**
     * 用固定种子的混合操作验证容量记账不变量：占用永不超过容量，且
     * {@code usedBytes()} 与逐文件重新计算的占用一致（防止重复计数/漏计数）。
     */
    @Test
    void capacityAccountingStaysConsistentUnderMixedOperations() {
        int capacity = FileHeader.FIXED_SIZE * 4 + 200;
        MemoryFileSystem fs = new MemoryFileSystem(capacity);
        java.util.Random random = new java.util.Random(20261127L);
        String[] names = {"a.txt", "b.txt", "dir/c.txt", "dir/d.txt", "other/e.bin"};
        for (int step = 0; step < 500; step++) {
            String name = names[random.nextInt(names.length)];
            switch (random.nextInt(4)) {
                case 0 -> fs.write(name, new byte[random.nextInt(40)]);
                case 1 -> fs.delete(names[random.nextInt(names.length)]);
                case 2 -> fs.transfer(names[random.nextInt(names.length)], "dir", true, random.nextBoolean());
                default -> fs.transferTo(names[random.nextInt(names.length)],
                        names[random.nextInt(names.length)], random.nextBoolean(), random.nextBoolean());
            }
            assertTrue(fs.usedBytes() <= capacity, "占用不得超过容量（第 " + step + " 步）");
            assertTrue(fs.usedBytes() >= 0);
            assertEquals(recomputeUsed(fs), fs.usedBytes(), "usedBytes 必须与实际内容一致（第 " + step + " 步）");
            for (String path : fs.filePaths()) {
                assertNotNull(fs.read(path), "文件必须可读: " + path);
            }
        }
    }

    private static int recomputeUsed(MemoryFileSystem fs) {
        int used = 0;
        for (String path : fs.filePaths()) {
            used += FileHeader.sizeFor(FilePaths.name(path)) + fs.fileSize(path);
        }
        return used;
    }

    @Test
    void saveAndLoadRoundTripKeepsFilesAndFolders() {
        MemoryFileSystem fs = new MemoryFileSystem(512);
        assertTrue(fs.write("a.txt", new byte[]{1, 2, 3}).success());
        assertTrue(fs.write("dir/b.bin", new byte[]{9}).success());
        assertTrue(fs.createFolder("empty").success());
        CompoundTag tag = new CompoundTag();
        fs.save(tag);

        MemoryFileSystem loaded = new MemoryFileSystem(512);
        loaded.load(tag);

        assertNotNull(loaded.read("a.txt"));
        assertArrayEquals(new byte[]{1, 2, 3}, loaded.read("a.txt"));
        assertArrayEquals(new byte[]{9}, loaded.read("dir/b.bin"));
        assertTrue(loaded.isFolder("empty"), "空文件夹也要持久化");
        assertEquals(fs.usedBytes(), loaded.usedBytes());
    }
}
