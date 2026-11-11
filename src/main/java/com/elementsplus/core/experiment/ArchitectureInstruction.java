package com.elementsplus.core.experiment;

import java.util.Locale;

/**
 * 计算机架构指令的客户端显示辅助方法。
 *
 * <p>测试只使用当前章节已经实现的指令；保留编码按 NOP 显示。后续章节可以在这里继续扩展。</p>
 */
public final class ArchitectureInstruction {

    private static final String[] ALU_NAMES = {
            "ADD", "SUB", "AND", "OR", "NOT", "XOR", "SHL", "SHR"
    };

    private ArchitectureInstruction() {
    }

    /** 把机器码翻译成汇编文本；未知/保留编码按 NOP 处理。 */
    public static String assembly(int instruction) {
        int ins = instruction & 0xFF;
        if (ins == 0x00) {
            return "NOP";
        }
        if ((ins & 0xC0) == 0x40) {
            int register = (ins >> 4) & 0x3;
            if (register <= 1) {
                return "LDL R" + register + ", #0x" + hex(ins & 0xF);
            }
            return "NOP";
        }
        if ((ins & 0xC0) == 0x80) {
            int register = (ins >> 4) & 0x3;
            if (register <= 1) {
                return "LDH R" + register + ", #0x" + hex(ins & 0xF);
            }
            return "NOP";
        }
        if (ins >= 0x30 && ins <= 0x3F) {
            int destination = ins & 0x3;
            int source = (ins >> 2) & 0x3;
            return "MOV R" + destination + ", R" + source;
        }
        if (ins >= 0x20 && ins <= 0x27) {
            return ALU_NAMES[ins - 0x20];
        }
        if (ins >= 0xE0 && ins <= 0xE2) {
            return "SETTARGET R" + (ins & 0x3);
        }
        return switch (ins) {
            case 0xC0 -> "JMP";
            case 0xC1 -> "JZ";
            case 0xC2 -> "JN";
            case 0xC3 -> "JP";
            case 0xC4 -> "JEQ";
            case 0xC5 -> "JLT.U";
            case 0xC6 -> "JGT.U";
            case 0xC9 -> "JNZ";
            case 0xCA -> "JNN";
            case 0xCB -> "JNP";
            case 0xCC -> "JNE";
            case 0xCD -> "JGE.U";
            case 0xCE -> "JLE.U";
            case 0xD0 -> "JVI";
            case 0xD1 -> "JVO";
            case 0xD8 -> "JNVI";
            case 0xD9 -> "JNVO";
            case 0xE4 -> "JLT";
            case 0xE5 -> "JGT";
            case 0xE6 -> "JGE";
            case 0xE7 -> "JLE";
            default -> "NOP";
        };
    }

    private static String hex(int value) {
        return Integer.toHexString(value & 0xF).toUpperCase(Locale.ROOT);
    }
}
