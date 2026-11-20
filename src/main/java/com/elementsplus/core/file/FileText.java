package com.elementsplus.core.file;

import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * 文本 / 二进制判定。文件打开时先尝试按 UTF-8 无损解析：
 * 只有能严格解码且重新编码后字节完全一致，才作为文本打开。
 */
public final class FileText {
    private FileText() {
    }

    /**
     * 尝试无损解码为 UTF-8 文本；失败时返回 {@code null}。
     */
    public static @Nullable String decodeLossless(byte[] data) {
        if (data == null) {
            return "";
        }
        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        String text;
        try {
            CharBuffer chars = decoder.decode(ByteBuffer.wrap(data));
            text = chars.toString();
        } catch (CharacterCodingException e) {
            return null;
        }
        if (!isDisplayableText(text)) {
            return null;
        }
        // 无损校验：重新编码必须与原始字节一致（排除非法 UTF-8 被替换的情况）。
        if (!Arrays.equals(text.getBytes(StandardCharsets.UTF_8), data)) {
            return null;
        }
        return text;
    }

    /**
     * 文件内容是否可以按文本处理。{@code maxBytes <= 0} 表示不限制。
     */
    public static boolean canOpenAsText(byte[] data, int maxBytes) {
        if (data == null) {
            return true;
        }
        if (maxBytes > 0 && data.length > maxBytes) {
            return false;
        }
        return decodeLossless(data) != null;
    }

    public static byte[] encode(String text) {
        return text == null ? new byte[0] : text.getBytes(StandardCharsets.UTF_8);
    }

    public static int byteLength(String text) {
        return text == null ? 0 : text.getBytes(StandardCharsets.UTF_8).length;
    }

    /** 截断到指定字节数以内，且不切坏多字节字符。 */
    public static byte[] encodeTruncated(String text, int maxBytes) {
        if (text == null) {
            return new byte[0];
        }
        byte[] data = encode(text);
        if (maxBytes <= 0 || data.length <= maxBytes) {
            return data;
        }
        int end = maxBytes;
        while (end > 0 && (data[end] & 0xC0) == 0x80) {
            end--;
        }
        return Arrays.copyOf(data, end);
    }

    private static boolean isDisplayableText(String text) {
        for (int i = 0; i < text.length(); ) {
            int codePoint = text.codePointAt(i);
            i += Character.charCount(codePoint);
            if (codePoint == '\t' || codePoint == '\n' || codePoint == '\r') {
                continue;
            }
            if (codePoint < 0x20 || codePoint == 0x7F) {
                return false;
            }
        }
        return true;
    }

    /** 文本模式下的可见字符显示：不可打印字符显示为 {@code .}。 */
    public static char printableAscii(int value) {
        return value >= 0x20 && value <= 0x7E ? (char) value : '.';
    }
}
