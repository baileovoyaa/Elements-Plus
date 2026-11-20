package com.elementsplus.core.file;

import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 8 位指令集汇编器。实现标签解析、{@code LDI}/{@code SETTARGET #imm}/{@code JMP #label}
 * 伪指令展开以及保留编码检查。
 *
 * <p>程序最长 256 字节；汇编失败时返回错误行号，供编辑器高亮。</p>
 */
public final class ProgramAssembler {
    public static final int MAX_PROGRAM_SIZE = 256;

    private static final String[] ALU_NAMES = {
            "ADD", "SUB", "AND", "OR", "NOT", "XOR", "SHL", "SHR"
    };

    private static final Map<String, Integer> ALU = new HashMap<>();
    private static final Map<String, Integer> JUMPS = new HashMap<>();
    private static final Map<String, String> ALIASES = Map.of(
            "JLTU", "JLT.U", "JGTU", "JGT.U", "JGEU", "JGE.U", "JLEU", "JLE.U"
    );

    static {
        for (int i = 0; i < ALU_NAMES.length; i++) {
            ALU.put(ALU_NAMES[i], 0x20 + i);
        }
        JUMPS.put("JMP", 0xC0);
        JUMPS.put("JZ", 0xC1);
        JUMPS.put("JN", 0xC2);
        JUMPS.put("JP", 0xC3);
        JUMPS.put("JEQ", 0xC4);
        JUMPS.put("JLT.U", 0xC5);
        JUMPS.put("JGT.U", 0xC6);
        JUMPS.put("JNZ", 0xC9);
        JUMPS.put("JNN", 0xCA);
        JUMPS.put("JNP", 0xCB);
        JUMPS.put("JNE", 0xCC);
        JUMPS.put("JGE.U", 0xCD);
        JUMPS.put("JLE.U", 0xCE);
        JUMPS.put("JVI", 0xD0);
        JUMPS.put("JVO", 0xD1);
        JUMPS.put("JNVI", 0xD8);
        JUMPS.put("JNVO", 0xD9);
        JUMPS.put("JLT", 0xE4);
        JUMPS.put("JGT", 0xE5);
        JUMPS.put("JGE", 0xE6);
        JUMPS.put("JLE", 0xE7);
    }

    private static final Pattern LABEL_PATTERN =
            Pattern.compile("^\\s*([A-Za-z_.$][A-Za-z0-9_.$]*)\\s*:");
    private static final Pattern MNEMONIC_PATTERN =
            Pattern.compile("^([A-Za-z][A-Za-z0-9.]*)(?:\\s+(.*))?$");
    private static final Pattern LABEL_NAME_PATTERN =
            Pattern.compile("^[A-Za-z_.$][A-Za-z0-9_.$]*$");

    private ProgramAssembler() {
    }

    /** 一条汇编错误：行号从 0 开始（与编辑器行号对齐）。 */
    public record Error(int line, Component message) {
    }

    /**
     * 汇编结果。{@code image} 长度等于 {@code size}，失败时为 {@code null}。
     */
    public record Result(@Nullable byte[] image, int size, List<Error> errors,
                         Map<String, Integer> labels) {
        public boolean ok() {
            return errors.isEmpty() && image != null && size > 0;
        }
    }

    private enum Kind {
        RAW, JUMP, MOV, LDL, LDH, LDI, SETTARGET, JMP_IMM
    }

    private record OpDef(Kind kind, int op) {
    }

    private static OpDef definition(String mnemonic) {
        Integer alu = ALU.get(mnemonic);
        if (alu != null) {
            return new OpDef(Kind.RAW, alu);
        }
        Integer jump = JUMPS.get(mnemonic);
        if (jump != null) {
            return new OpDef(Kind.JUMP, jump);
        }
        return switch (mnemonic) {
            case "NOP" -> new OpDef(Kind.RAW, 0x00);
            case "MOV" -> new OpDef(Kind.MOV, 0);
            case "LDL" -> new OpDef(Kind.LDL, 0);
            case "LDH" -> new OpDef(Kind.LDH, 0);
            case "LDI" -> new OpDef(Kind.LDI, 0);
            case "SETTARGET" -> new OpDef(Kind.SETTARGET, 0);
            default -> null;
        };
    }

    private static @Nullable String normalizeMnemonic(String raw) {
        String upper = raw.toUpperCase(Locale.ROOT);
        String mapped = ALIASES.getOrDefault(upper, upper);
        return definition(mapped) == null ? null : mapped;
    }

    private static int parseRegister(String token) {
        if (token == null) {
            return -1;
        }
        String text = token.trim();
        if (text.length() == 2 && (text.charAt(0) == 'R' || text.charAt(0) == 'r')
                && text.charAt(1) >= '0' && text.charAt(1) <= '3') {
            return text.charAt(1) - '0';
        }
        return -1;
    }

    private static @Nullable Long parseNumber(String body) {
        String text = body.trim();
        try {
            if (text.startsWith("0x") || text.startsWith("0X")) {
                return Long.parseLong(text.substring(2), 16);
            }
            if (text.startsWith("0b") || text.startsWith("0B")) {
                return Long.parseLong(text.substring(2), 2);
            }
            if (!text.isEmpty() && text.chars().allMatch(c -> c >= '0' && c <= '9')) {
                return Long.parseLong(text);
            }
        } catch (NumberFormatException ignored) {
            return null;
        }
        return null;
    }

    private static final class LineErrors {
        private final List<Error> errors;
        private boolean tooMany;

        private LineErrors(List<Error> errors) {
            this.errors = errors;
        }

        void add(int line, String key, Object... args) {
            if (errors.size() >= 40) {
                tooMany = true;
                return;
            }
            errors.add(new Error(line, Component.translatable(key, args)));
        }
    }

    /** 汇编一首程序。 */
    public static Result assemble(String source) {
        List<Error> errors = new ArrayList<>();
        LineErrors reporter = new LineErrors(errors);
        Map<String, Integer> labels = new LinkedHashMap<>();
        List<Item> items = new ArrayList<>();
        String text = source == null ? "" : source;
        if (text.startsWith("\uFEFF")) {
            text = text.substring(1);
        }
        String[] lines = text.split("\r?\n", -1);
        int address = 0;

        for (int lineIndex = 0; lineIndex < lines.length; lineIndex++) {
            String code = lines[lineIndex];
            int semicolon = code.indexOf(';');
            if (semicolon >= 0) {
                code = code.substring(0, semicolon);
            }
            Matcher labelMatcher = LABEL_PATTERN.matcher(code);
            while (labelMatcher.find()) {
                String name = labelMatcher.group(1);
                if (labels.containsKey(name)) {
                    reporter.add(lineIndex, "gui.elements-plus.file.asm.error.label_duplicate", name);
                } else {
                    labels.put(name, address);
                }
                code = code.substring(labelMatcher.end());
                labelMatcher = LABEL_PATTERN.matcher(code);
            }
            String statement = code.trim();
            if (statement.isEmpty()) {
                continue;
            }
            Matcher mnemonicMatcher = MNEMONIC_PATTERN.matcher(statement);
            if (!mnemonicMatcher.matches()) {
                reporter.add(lineIndex, "gui.elements-plus.file.asm.error.parse_line", statement);
                continue;
            }
            String mnemonic = normalizeMnemonic(mnemonicMatcher.group(1));
            if (mnemonic == null) {
                reporter.add(lineIndex, "gui.elements-plus.file.asm.error.unknown_mnemonic",
                        mnemonicMatcher.group(1));
                continue;
            }
            OpDef def = definition(mnemonic);
            String rest = mnemonicMatcher.group(2) == null ? "" : mnemonicMatcher.group(2).trim();
            List<String> operands = new ArrayList<>();
            if (!rest.isEmpty()) {
                for (String part : rest.split(",", -1)) {
                    operands.add(part.trim());
                }
            }
            Item item = parseItem(mnemonic, def, operands, lineIndex, labels, reporter);
            if (item != null) {
                if (address + item.size > MAX_PROGRAM_SIZE) {
                    reporter.add(lineIndex, "gui.elements-plus.file.asm.error.too_large",
                            address + item.size);
                }
                items.add(item);
                address += item.size;
            } else {
                address += 1;
            }
        }

        if (items.isEmpty() && errors.isEmpty()) {
            reporter.add(0, "gui.elements-plus.file.asm.error.empty");
        }
        if (!errors.isEmpty()) {
            return new Result(null, 0, errors, labels);
        }

        byte[] image = new byte[MAX_PROGRAM_SIZE];
        int position = 0;
        byte[] buffer = new byte[MAX_PROGRAM_SIZE];
        for (Item item : items) {
            switch (item.kind) {
                case RAW -> buffer[position++] = (byte) item.op;
                case MOV -> buffer[position++] = (byte) (0x30 | (item.source << 2) | item.destination);
                case LDL -> buffer[position++] = (byte) (0x40 | (item.register << 4) | item.value);
                case LDH -> buffer[position++] = (byte) (0x80 | (item.register << 4) | item.value);
                case LDI -> {
                    buffer[position++] = (byte) (0x40 | (item.register << 4) | (item.value & 0x0F));
                    buffer[position++] = (byte) (0x80 | (item.register << 4) | ((item.value >> 4) & 0x0F));
                }
                case SETTARGET -> {
                    if (item.target != null) {
                        Integer value = resolveTarget(item.target, item.line, labels, reporter);
                        if (value != null) {
                            buffer[position++] = (byte) (0x40 | (value & 0x0F));
                            buffer[position++] = (byte) (0x80 | ((value >> 4) & 0x0F));
                        }
                    }
                    buffer[position++] = (byte) (0xE0 | item.register);
                }
                case JUMP -> buffer[position++] = (byte) item.op;
                case JMP_IMM -> {
                    Integer value = resolveTarget(item.target, item.line, labels, reporter);
                    if (value != null) {
                        buffer[position++] = (byte) (0x40 | (value & 0x0F));
                        buffer[position++] = (byte) (0x80 | ((value >> 4) & 0x0F));
                        buffer[position++] = (byte) 0xE0;
                        buffer[position++] = (byte) 0xC0;
                    }
                }
            }
        }
        if (!errors.isEmpty()) {
            return new Result(null, 0, errors, labels);
        }
        if (position == 0) {
            reporter.add(0, "gui.elements-plus.file.asm.error.empty");
            return new Result(null, 0, errors, labels);
        }
        System.arraycopy(buffer, 0, image, 0, position);
        byte[] result = new byte[position];
        System.arraycopy(image, 0, result, 0, position);
        return new Result(result, position, errors, labels);
    }

    private static @Nullable Integer resolveTarget(String target, int line,
                                                   Map<String, Integer> labels, LineErrors reporter) {
        Long number = parseNumber(target);
        if (number != null) {
            return number.intValue();
        }
        Integer value = labels.get(target);
        if (value == null) {
            reporter.add(line, "gui.elements-plus.file.asm.error.label_undefined", target);
            return null;
        }
        if (value < 0 || value > 0xFF) {
            reporter.add(line, "gui.elements-plus.file.asm.error.label_range", target);
            return null;
        }
        return value;
    }

    private static @Nullable Item parseItem(String mnemonic, OpDef def, List<String> operands,
                                            int line, Map<String, Integer> labels,
                                            LineErrors reporter) {
        switch (def.kind) {
            case RAW -> {
                if (!operands.isEmpty()) {
                    reporter.add(line, "gui.elements-plus.file.asm.error.no_operands", mnemonic);
                    return null;
                }
                return Item.raw(line, def.op);
            }
            case JUMP -> {
                if (mnemonic.equals("JMP") && operands.size() == 1) {
                    String target = parseTargetOperand(operands.get(0), line, reporter);
                    return target == null ? null : Item.jmpImm(line, target);
                }
                if (!operands.isEmpty()) {
                    reporter.add(line, "gui.elements-plus.file.asm.error.jump_operands", mnemonic);
                    return null;
                }
                return Item.raw(line, def.op);
            }
            case MOV -> {
                if (operands.size() != 2) {
                    reporter.add(line, "gui.elements-plus.file.asm.error.mov_operands");
                    return null;
                }
                int destination = parseRegister(operands.get(0));
                int source = parseRegister(operands.get(1));
                if (destination < 0 || source < 0) {
                    reporter.add(line, "gui.elements-plus.file.asm.error.mov_register");
                    return null;
                }
                return Item.mov(line, destination, source);
            }
            case LDL, LDH -> {
                if (operands.size() != 2) {
                    reporter.add(line, "gui.elements-plus.file.asm.error.ldx_operands", mnemonic, mnemonic);
                    return null;
                }
                int register = parseRegister(operands.get(0));
                Long value = parseImmediate(operands.get(1), 0, 15, line,
                        "gui.elements-plus.file.asm.error.ldx_range", reporter);
                if (register < 0 || register > 1) {
                    reporter.add(line, "gui.elements-plus.file.asm.error.ldx_register", mnemonic);
                    return null;
                }
                if (value == null) {
                    return null;
                }
                return Item.ld(line, def.kind, register, value.intValue());
            }
            case LDI -> {
                if (operands.size() != 2) {
                    reporter.add(line, "gui.elements-plus.file.asm.error.ldi_operands");
                    return null;
                }
                int register = parseRegister(operands.get(0));
                Long value = parseImmediate(operands.get(1), 0, 255, line,
                        "gui.elements-plus.file.asm.error.ldi_range", reporter);
                if (register < 0 || register > 1) {
                    reporter.add(line, "gui.elements-plus.file.asm.error.ldi_register");
                    return null;
                }
                if (value == null) {
                    return null;
                }
                return Item.ldi(line, register, value.intValue());
            }
            case SETTARGET -> {
                if (operands.size() != 1) {
                    reporter.add(line, "gui.elements-plus.file.asm.error.settarget_operands");
                    return null;
                }
                int register = parseRegister(operands.get(0));
                if (register >= 0) {
                    if (register == 3) {
                        reporter.add(line, "gui.elements-plus.file.asm.error.settarget_r3");
                        return null;
                    }
                    return Item.settargetReg(line, register);
                }
                String target = parseTargetOperand(operands.get(0), line, reporter);
                return target == null ? null : Item.settargetImm(line, target);
            }
        }
        return null;
    }

    private static @Nullable Long parseImmediate(String operand, int min, int max, int line,
                                                 String rangeKey, LineErrors reporter) {
        String raw = operand == null ? "" : operand.trim();
        if (!raw.startsWith("#")) {
            reporter.add(line, "gui.elements-plus.file.asm.error.immediate_hash");
            return null;
        }
        Long value = parseNumber(raw.substring(1));
        if (value == null) {
            reporter.add(line, "gui.elements-plus.file.asm.error.immediate_invalid", raw);
            return null;
        }
        if (value < min || value > max) {
            reporter.add(line, rangeKey, min, max);
            return null;
        }
        return value;
    }

    private static @Nullable String parseTargetOperand(String operand, int line, LineErrors reporter) {
        String raw = operand == null ? "" : operand.trim();
        if (!raw.startsWith("#")) {
            reporter.add(line, "gui.elements-plus.file.asm.error.target_operand");
            return null;
        }
        String body = raw.substring(1).trim();
        Long number = parseNumber(body);
        if (number != null) {
            if (number < 0 || number > 0xFF) {
                reporter.add(line, "gui.elements-plus.file.asm.error.target_range");
                return null;
            }
            return body;
        }
        if (!LABEL_NAME_PATTERN.matcher(body).matches()) {
            reporter.add(line, "gui.elements-plus.file.asm.error.target_invalid", raw);
            return null;
        }
        return body;
    }

    /** 内部汇编单元。 */
    private static final class Item {
        private final int line;
        private final int size;
        private final Kind kind;
        private final int op;
        private int register;
        private int value;
        private int destination;
        private int source;
        private String target;

        private Item(int line, int size, Kind kind, int op) {
            this.line = line;
            this.size = size;
            this.kind = kind;
            this.op = op;
        }

        static Item raw(int line, int op) {
            return new Item(line, 1, Kind.RAW, op);
        }

        static Item mov(int line, int destination, int source) {
            Item item = new Item(line, 1, Kind.MOV, 0);
            item.destination = destination;
            item.source = source;
            return item;
        }

        static Item ld(int line, Kind kind, int register, int value) {
            Item item = new Item(line, 1, kind, 0);
            item.register = register;
            item.value = value;
            return item;
        }

        static Item ldi(int line, int register, int value) {
            Item item = new Item(line, 2, Kind.LDI, 0);
            item.register = register;
            item.value = value;
            return item;
        }

        static Item settargetReg(int line, int register) {
            Item item = new Item(line, 1, Kind.SETTARGET, 0);
            item.register = register;
            return item;
        }

        static Item settargetImm(int line, String target) {
            Item item = new Item(line, 3, Kind.SETTARGET, 0);
            item.register = 0;
            item.target = target;
            return item;
        }

        static Item jmpImm(int line, String target) {
            Item item = new Item(line, 4, Kind.JMP_IMM, 0);
            item.target = target;
            return item;
        }
    }

    /** 十六进制大写两位，供反汇编/日志复用。 */
    public static String hex2(int value) {
        return String.format(Locale.ROOT, "%02X", value & 0xFF);
    }

    /** 单字节反汇编文本；未知编码返回 {@code NOP}。 */
    public static String disassembleByte(int instruction, boolean[] reserved) {
        int ins = instruction & 0xFF;
        if (reserved != null) {
            reserved[0] = false;
        }
        if (ins == 0x00) {
            return "NOP";
        }
        if ((ins & 0xC0) == 0x40) {
            int register = (ins >> 4) & 0x3;
            if (register <= 1) {
                return "LDL R" + register + ", #0x" + Integer.toHexString(ins & 0xF).toUpperCase(Locale.ROOT);
            }
            if (reserved != null) {
                reserved[0] = true;
            }
            return "NOP";
        }
        if ((ins & 0xC0) == 0x80) {
            int register = (ins >> 4) & 0x3;
            if (register <= 1) {
                return "LDH R" + register + ", #0x" + Integer.toHexString(ins & 0xF).toUpperCase(Locale.ROOT);
            }
            if (reserved != null) {
                reserved[0] = true;
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
        for (Map.Entry<String, Integer> entry : JUMPS.entrySet()) {
            if (entry.getValue() == ins) {
                return entry.getKey();
            }
        }
        if (reserved != null) {
            reserved[0] = true;
        }
        return "NOP";
    }

    public static Set<String> knownMnemonics() {
        Set<String> result = new java.util.LinkedHashSet<>();
        result.add("NOP");
        result.addAll(ALU.keySet());
        result.addAll(JUMPS.keySet());
        result.addAll(List.of("MOV", "LDL", "LDH", "LDI", "SETTARGET"));
        return result;
    }
}
