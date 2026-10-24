package com.elementsplus.client.config;

import java.util.Locale;

/**
 * 数字显示格式：
 * <ul>
 *   <li>{@link #UNSIGNED}：无符号，保持原有显示行为；</li>
 *   <li>{@link #COMPLEMENT}：补码，8 位显示 -128~127，4 位信号强度显示 -8~7；</li>
 *   <li>{@link #HEX}：十六进制，8 位显示 00~FF，4 位信号强度显示 0~F。</li>
 * </ul>
 */
public enum NumberFormat {
    UNSIGNED,
    COMPLEMENT,
    HEX;

    public NumberFormat next() {
        NumberFormat[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public NumberFormat previous() {
        NumberFormat[] values = values();
        return values[(ordinal() + values.length - 1) % values.length];
    }

    /** 8 位总线数值（0~255）的显示文本。 */
    public String formatDigital(int value) {
        int v = value & 0xFF;
        return switch (this) {
            case UNSIGNED -> Integer.toString(v);
            case COMPLEMENT -> Integer.toString(v >= 0x80 ? v - 0x100 : v);
            case HEX -> String.format(Locale.ROOT, "%02X", v);
        };
    }

    /** 4 位信号强度（0~15）的显示文本。 */
    public String formatAnalog(int value) {
        int v = value & 0xF;
        return switch (this) {
            case UNSIGNED -> Integer.toString(v);
            case COMPLEMENT -> Integer.toString(v >= 8 ? v - 16 : v);
            case HEX -> Integer.toHexString(v).toUpperCase(Locale.ROOT);
        };
    }

    /** 8 位总线输入的范围提示。 */
    public String rangeTextDigital() {
        return switch (this) {
            case UNSIGNED -> "0~255";
            case COMPLEMENT -> "-128~127";
            case HEX -> "00~FF";
        };
    }

    /** 8 位总线输入的输入框最大长度。 */
    public int inputMaxLength() {
        return switch (this) {
            case UNSIGNED -> 3;
            case COMPLEMENT -> 4;
            case HEX -> 2;
        };
    }

    /**
     * 输入框逐字符过滤：允许输入尚未完整的合法前缀，例如补码的“-”。
     */
    public boolean acceptsPartial(String text) {
        if (text == null) {
            return false;
        }
        if (text.isEmpty()) {
            return true;
        }
        return switch (this) {
            case UNSIGNED -> text.length() <= 3 && allDecimal(text, 0);
            case COMPLEMENT -> {
                if (text.length() > 4) {
                    yield false;
                }
                int start = text.charAt(0) == '-' ? 1 : 0;
                yield allDecimal(text, start);
            }
            case HEX -> text.length() <= 2 && allHex(text);
        };
    }

    /** 完整输入是否合法（范围内且格式正确）。 */
    public boolean isValidText(String text) {
        try {
            parseDigital(text);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 解析 8 位总线输入，返回无符号 0~255。
     *
     * @throws NumberFormatException 格式错误或超出范围
     */
    public int parseDigital(String text) {
        String value = text == null ? "" : text.trim();
        return switch (this) {
            case UNSIGNED -> {
                int parsed = Integer.parseInt(value);
                if (parsed < 0 || parsed > 255) {
                    throw new NumberFormatException("out of unsigned 8-bit range");
                }
                yield parsed;
            }
            case COMPLEMENT -> {
                int parsed = Integer.parseInt(value);
                if (parsed < -128 || parsed > 127) {
                    throw new NumberFormatException("out of signed 8-bit range");
                }
                yield parsed & 0xFF;
            }
            case HEX -> {
                if (value.isEmpty() || value.length() > 2) {
                    throw new NumberFormatException("invalid hex length");
                }
                int parsed = Integer.parseInt(value, 16);
                if (parsed < 0 || parsed > 255) {
                    throw new NumberFormatException("out of hex 8-bit range");
                }
                yield parsed;
            }
        };
    }

    /** 工具栏按钮图标路径。 */
    public String iconPath(boolean digital) {
        return "textures/gui/number_format/"
                + (digital ? "digital_" : "analog_")
                + name().toLowerCase(Locale.ROOT)
                + ".png";
    }

    private static boolean allDecimal(String text, int start) {
        if (start == text.length()) {
            return true;
        }
        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }

    private static boolean allHex(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            boolean hex = (c >= '0' && c <= '9')
                    || (c >= 'a' && c <= 'f')
                    || (c >= 'A' && c <= 'F');
            if (!hex) {
                return false;
            }
        }
        return true;
    }
}
