package com.elementsplus.core.file;

import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 8 位指令集反汇编器。未定义的程序字节按 {@code NOP} 处理，并在行尾注释原始
 * 字节与地址；标签、注释和伪指令无法恢复。
 */
public final class ProgramDisassembler {
    private static final int HEX_COLUMN_WIDTH = 24;

    private ProgramDisassembler() {
    }

    public static String disassemble(byte[] data) {
        if (data == null || data.length == 0) {
            return "";
        }
        List<String> lines = new ArrayList<>();
        lines.add(Component.translatable("gui.elements-plus.file.disasm.header").getString());
        lines.add(Component.translatable("gui.elements-plus.file.disasm.header_hint").getString());
        for (int address = 0; address < data.length; address++) {
            boolean[] reserved = new boolean[1];
            String text = ProgramAssembler.disassembleByte(data[address] & 0xFF, reserved);
            StringBuilder line = new StringBuilder(text);
            while (line.length() < HEX_COLUMN_WIDTH) {
                line.append(' ');
            }
            line.append("; 0x").append(String.format(Locale.ROOT, "%02X", address));
            if (reserved[0]) {
                line.append(Component.translatable("gui.elements-plus.file.disasm.reserved",
                        String.format(Locale.ROOT, "%02X", data[address] & 0xFF)).getString());
            }
            lines.add(line.toString());
        }
        return String.join("\n", lines);
    }
}
