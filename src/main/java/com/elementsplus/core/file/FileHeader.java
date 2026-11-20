package com.elementsplus.core.file;

import java.nio.charset.StandardCharsets;

/**
 * 文件头。文件系统的每个文件都会额外占用一段文件头空间，实验桌内存的容量
 * 统计包含文件头。
 *
 * <p>布局（大端）：</p>
 * <pre>
 *   0..3   magic  "EPF1"
 *   4      version
 *   5      nameLength (unsigned byte)
 *   6..7   dataLength (unsigned short)
 *   8..    name (UTF-8)
 * </pre>
 *
 * <p>固定部分 8 字节，加上名字的 UTF-8 长度即为文件头大小。</p>
 */
public final class FileHeader {
    public static final int MAGIC = 0x45504631; // "EPF1"
    public static final int VERSION = 1;
    public static final int FIXED_SIZE = 8;
    /** 16 位无符号数据长度上限。 */
    public static final int MAX_DATA_LENGTH = 0xFFFF;
    public static final int MAX_NAME_LENGTH = 255;

    private FileHeader() {
    }

    public record Decoded(int version, String name, int dataLength) {
    }

    public static int sizeFor(String name) {
        return FIXED_SIZE + utf8Length(name);
    }

    public static int utf8Length(String name) {
        return name == null ? 0 : name.getBytes(StandardCharsets.UTF_8).length;
    }

    /** 生成文件头字节。名字过长或数据过长时返回 {@code null}。 */
    public static byte[] encode(String name, int dataLength) {
        byte[] nameBytes = name == null ? new byte[0] : name.getBytes(StandardCharsets.UTF_8);
        if (nameBytes.length > MAX_NAME_LENGTH || dataLength < 0 || dataLength > MAX_DATA_LENGTH) {
            return null;
        }
        byte[] result = new byte[FIXED_SIZE + nameBytes.length];
        result[0] = (byte) (MAGIC >>> 24);
        result[1] = (byte) (MAGIC >>> 16);
        result[2] = (byte) (MAGIC >>> 8);
        result[3] = (byte) MAGIC;
        result[4] = (byte) VERSION;
        result[5] = (byte) nameBytes.length;
        result[6] = (byte) (dataLength >>> 8);
        result[7] = (byte) dataLength;
        System.arraycopy(nameBytes, 0, result, FIXED_SIZE, nameBytes.length);
        return result;
    }

    /** 解析文件头；magic 不匹配时返回 {@code null}。 */
    public static Decoded decode(byte[] data) {
        if (data == null || data.length < FIXED_SIZE) {
            return null;
        }
        int magic = ((data[0] & 0xFF) << 24) | ((data[1] & 0xFF) << 16) | ((data[2] & 0xFF) << 8) | (data[3] & 0xFF);
        if (magic != MAGIC) {
            return null;
        }
        int version = data[4] & 0xFF;
        int nameLength = data[5] & 0xFF;
        int dataLength = ((data[6] & 0xFF) << 8) | (data[7] & 0xFF);
        if (data.length < FIXED_SIZE + nameLength) {
            return null;
        }
        String name = new String(data, FIXED_SIZE, nameLength, StandardCharsets.UTF_8);
        return new Decoded(version, name, dataLength);
    }
}
