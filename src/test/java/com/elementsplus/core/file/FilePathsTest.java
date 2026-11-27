package com.elementsplus.core.file;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 路径规范化的边界：这些规则同时保护服务端内存文件系统和客户端本地目录，
 * 任何一条放宽都会变成路径穿越或跨平台文件名问题。
 */
class FilePathsTest {

    @Test
    void normalizeTrimsSeparatorsAndCollapsesRepeats() {
        assertEquals("a/b", FilePaths.normalize(" /a//b/ "));
        assertEquals("a", FilePaths.normalize("a"));
        assertEquals("", FilePaths.normalize("/"));
        assertNull(FilePaths.normalize(null), "null 表示路径非法，空串才表示根目录");
    }

    @Test
    void normalizeRejectsTraversalAndReservedCharacters() {
        assertNull(FilePaths.normalize("a/../b"), ".. 段必须被拒绝");
        assertNull(FilePaths.normalize("../x"));
        assertNull(FilePaths.normalize("a/./b"), ". 段必须被拒绝");
        assertNull(FilePaths.normalize("C:/x"), "冒号会被 Windows 当作盘符");
        assertNull(FilePaths.normalize("a\\..\\b"), "反斜杠先归一化为分隔符，再拒绝 ..");
        assertNull(FilePaths.normalize("a*b"));
        assertNull(FilePaths.normalize("a\u0001b"), "控制字符必须被拒绝");
    }

    @Test
    void validateNameTrimsTrailingDotsAndSpaces() {
        assertEquals("name", FilePaths.validateName("name."));
        assertEquals("name", FilePaths.validateName("name "));
        assertNull(FilePaths.validateName(""));
        assertNull(FilePaths.validateName("  "));
        assertNull(FilePaths.validateName("."));
        assertNull(FilePaths.validateName(".."));
        assertNull(FilePaths.validateName("a/b"));
        assertNull(FilePaths.validateName("x".repeat(FilePaths.MAX_NAME_LENGTH + 1)));
    }

    @Test
    void normalizeDirFallsBackToRoot() {
        assertEquals("", FilePaths.normalizeDir(null));
        assertEquals("", FilePaths.normalizeDir("a/../b"));
        assertEquals("a/b", FilePaths.normalizeDir("/a/b/"));
    }

    @Test
    void isUnderDoesNotMatchSiblingPrefixes() {
        assertTrue(FilePaths.isUnder("a/b", "a"));
        assertTrue(FilePaths.isUnder("a", "a"));
        assertTrue(FilePaths.isUnder("a", ""), "空祖先代表根目录");
        assertFalse(FilePaths.isUnder("ab", "a"), "同前缀但不是子路径");
        assertFalse(FilePaths.isUnder("a", "a/b"));
    }

    @Test
    void parentAndNameSplitOnLastSeparator() {
        assertEquals("", FilePaths.parent("a"));
        assertEquals("a", FilePaths.parent("a/b"));
        assertEquals("c", FilePaths.name("a/b/c"));
        assertEquals("", FilePaths.name(""));
        assertEquals("a/b", FilePaths.join("a", "b"));
        assertEquals("b", FilePaths.join("", "b"));
    }

    @Test
    void extensionAndBaseNameIgnoreLeadingDot() {
        assertEquals("txt", FilePaths.extension("a.txt"));
        assertEquals("", FilePaths.extension(".hidden"));
        assertEquals("", FilePaths.extension("a."));
        assertEquals("a", FilePaths.baseName("a.txt"));
        assertEquals(".hidden", FilePaths.baseName(".hidden"));
    }
}
