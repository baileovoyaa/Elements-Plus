package com.elementsplus.core.experiment;

import com.elementsplus.core.experiment.CircuitExperiment.ArchitectureTick;
import com.elementsplus.core.experiment.CircuitExperiment.ConstantArchitectureTestCase;
import com.elementsplus.core.experiment.CircuitExperiment.PinValue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 计算机架构后续章节测例的公共工具：机器码/汇编构造器、外部输入时序和参考 CPU。
 *
 * <p>程序构造器只生成当前章节允许的指令；参考 CPU 实现完整的 8 位指令集语义，
 * 但每个章节的测例只会调用该章节及之前章节已经引入的指令。</p>
 */
final class ArchitectureTestSupport {

    static final int R0 = 0;
    static final int R1 = 1;
    static final int R2 = 2;
    static final int R3 = 3;

    static final int NOP = 0x00;
    static final int ADD = 0x20;
    static final int SUB = 0x21;
    static final int AND = 0x22;
    static final int OR = 0x23;
    static final int NOT = 0x24;
    static final int XOR = 0x25;
    static final int SHL = 0x26;
    static final int SHR = 0x27;

    static final int JMP = 0xC0;
    static final int JZ = 0xC1;
    static final int JN = 0xC2;
    static final int JP = 0xC3;
    static final int JEQ = 0xC4;
    static final int JLT_U = 0xC5;
    static final int JGT_U = 0xC6;
    static final int JNZ = 0xC9;
    static final int JNN = 0xCA;
    static final int JNP = 0xCB;
    static final int JNE = 0xCC;
    static final int JGE_U = 0xCD;
    static final int JLE_U = 0xCE;
    static final int JVI = 0xD0;
    static final int JVO = 0xD1;
    static final int JNVI = 0xD8;
    static final int JNVO = 0xD9;
    static final int JLT = 0xE4;
    static final int JGT = 0xE5;
    static final int JGE = 0xE6;
    static final int JLE = 0xE7;

    private ArchitectureTestSupport() {
    }

    static ConstantArchitectureTestCase buildCase(ProgramBuilder programBuilder, ScheduleBuilder schedule) {
        return buildCase(programBuilder, schedule, false);
    }

    static ConstantArchitectureTestCase buildCase(ProgramBuilder programBuilder, ScheduleBuilder schedule,
                                                  boolean copyWholeProgram) {
        byte[] program = programBuilder.build();
        if (schedule.size() < 1) {
            throw new IllegalStateException("Architecture test must have at least one tick");
        }
        ReferenceCpu cpu = new ReferenceCpu(program);
        List<ArchitectureTick> ticks = new ArrayList<>();
        for (int i = 0; i < schedule.size(); i++) {
            int expectedPc = cpu.pc;
            if (expectedPc >= program.length) {
                throw new IllegalStateException("Expected PC " + expectedPc
                        + " is outside program length " + program.length);
            }
            int vi = schedule.vi(i);
            int di = schedule.di(i);
            int ro = schedule.ro(i);

            CpuStep step = cpu.step(vi, di, ro);
            Map<String, PinValue> inputs = new LinkedHashMap<>();
            inputs.put("VI", new PinValue(vi != 0 ? 15 : 0));
            inputs.put("DI", new PinValue(pin8(di)));
            inputs.put("RO", new PinValue(ro != 0 ? 15 : 0));

            Map<String, PinValue> expectedOutputs = new LinkedHashMap<>();
            expectedOutputs.put("RI", new PinValue(step.ri() ? 15 : 0));
            expectedOutputs.put("VO", new PinValue(step.vo() ? 15 : 0));
            expectedOutputs.put("DO", new PinValue(pin8(step.doValue())));

            ticks.add(new ArchitectureTick(expectedPc, inputs, expectedOutputs));
        }
        return new ConstantArchitectureTestCase(program, ticks, copyWholeProgram);
    }

    static byte[] pin8(int value) {
        byte[] lanes = new byte[8];
        for (int b = 0; b < 8; b++) {
            lanes[b] = (byte) (((value >> b) & 1) != 0 ? 15 : 0);
        }
        return lanes;
    }

    /** 允许使用标签的简单程序构造器。 */
    static final class ProgramBuilder {
        private final List<Integer> bytes = new ArrayList<>();
        private final Map<String, Integer> labels = new LinkedHashMap<>();
        private final List<Fixup> fixups = new ArrayList<>();

        ProgramBuilder nop() {
            return raw(NOP);
        }

        ProgramBuilder raw(int instruction) {
            bytes.add(instruction & 0xFF);
            return this;
        }

        ProgramBuilder mov(int destination, int source) {
            return raw(0x30 | ((source & 0x3) << 2) | (destination & 0x3));
        }

        ProgramBuilder ldl(int register, int value) {
            return raw(0x40 | ((register & 0x3) << 4) | (value & 0xF));
        }

        ProgramBuilder ldh(int register, int value) {
            return raw(0x80 | ((register & 0x3) << 4) | (value & 0xF));
        }

        /** LDI 伪指令展开为 LDL + LDH，只能用于 R0/R1。 */
        ProgramBuilder ldi(int register, int value) {
            return ldl(register, value & 0xF).ldh(register, (value >> 4) & 0xF);
        }

        /** LDI 一个标签地址，展开为 LDL + LDH；地址在 build 时解析。 */
        ProgramBuilder ldiLabel(int register, String label) {
            int lowIndex = bytes.size();
            raw(0x40 | ((register & 0x3) << 4));
            int highIndex = bytes.size();
            raw(0x80 | ((register & 0x3) << 4));
            fixups.add(new Fixup(lowIndex, label, false));
            fixups.add(new Fixup(highIndex, label, true));
            return this;
        }

        ProgramBuilder setTarget(int register) {
            return raw(0xE0 | (register & 0x3));
        }

        ProgramBuilder setTargetLabel(int register, String label) {
            return ldiLabel(register, label).setTarget(register);
        }

        ProgramBuilder alu(int opcode) {
            return raw(opcode);
        }

        ProgramBuilder jump(int opcode) {
            return raw(opcode);
        }

        ProgramBuilder label(String name) {
            if (labels.containsKey(name)) {
                throw new IllegalArgumentException("Duplicate label " + name);
            }
            labels.put(name, bytes.size());
            return this;
        }

        int size() {
            return bytes.size();
        }

        byte[] build() {
            byte[] result = new byte[bytes.size()];
            for (int i = 0; i < result.length; i++) {
                result[i] = (byte) (int) bytes.get(i);
            }
            for (Fixup fixup : fixups) {
                Integer address = labels.get(fixup.label());
                if (address == null) {
                    throw new IllegalStateException("Unknown label " + fixup.label());
                }
                if (address < 0 || address > 0xFF) {
                    throw new IllegalStateException("Label address out of range: " + fixup.label());
                }
                int current = result[fixup.index()] & 0xFF;
                int nibble = fixup.high() ? (address >> 4) & 0xF : address & 0xF;
                result[fixup.index()] = (byte) ((current & 0xF0) | nibble);
            }
            if (result.length > 256) {
                throw new IllegalStateException("Program is longer than 256 bytes");
            }
            return result;
        }

        private record Fixup(int index, String label, boolean high) {
        }
    }

    /** 每个 tick 的外部输入。 */
    static final class ScheduleBuilder {
        private final List<int[]> ticks = new ArrayList<>();

        ScheduleBuilder tick(int vi, int di, int ro) {
            ticks.add(new int[]{vi, di, ro});
            return this;
        }

        ScheduleBuilder fill(int count, int vi, int di, int ro) {
            for (int i = 0; i < count; i++) {
                tick(vi, di, ro);
            }
            return this;
        }

        int size() {
            return ticks.size();
        }

        int vi(int index) {
            return ticks.get(index)[0];
        }

        int di(int index) {
            return ticks.get(index)[1];
        }

        int ro(int index) {
            return ticks.get(index)[2];
        }
    }

    private record CpuStep(boolean ri, boolean vo, int doValue) {
    }

    /**
     * 完整指令集参考模型。用于生成期望 PC/INS/RI/VO/DO，不参与被测电路求值。
     */
    private static final class ReferenceCpu {
        private final byte[] program;
        private final byte[] memory = new byte[256];
        private int pc;
        private int r0;
        private int r1;
        private int tr;
        private int doValue;
        private boolean vo;

        ReferenceCpu(byte[] program) {
            this.program = program;
        }

        CpuStep step(int vi, int di, int ro) {
            int instruction = program[pc & 0xFF] & 0xFF;
            int nextPc = (pc + 1) & 0xFF;
            boolean ri = false;
            boolean outputWrite = false;
            int outputValue = 0;
            boolean memoryWrite = false;
            int memoryValue = 0;
            int newR0 = r0;
            int newR1 = r1;
            int newTr = tr;

            if ((instruction & 0xC0) == 0x40) {
                // LDL R0/R1, #n；R2/R3 编码保留。
                int register = (instruction >> 4) & 0x3;
                int value = instruction & 0xF;
                if (register == R0) {
                    newR0 = (r0 & 0xF0) | value;
                } else if (register == R1) {
                    newR1 = (r1 & 0xF0) | value;
                }
            } else if ((instruction & 0xC0) == 0x80) {
                // LDH R0/R1, #n；R2/R3 编码保留。
                int register = (instruction >> 4) & 0x3;
                int value = instruction & 0xF;
                if (register == R0) {
                    newR0 = (r0 & 0x0F) | (value << 4);
                } else if (register == R1) {
                    newR1 = (r1 & 0x0F) | (value << 4);
                }
            } else if (instruction >= 0x30 && instruction <= 0x3F) {
                int destination = instruction & 0x3;
                int source = (instruction >> 2) & 0x3;

                if (destination == R3 && source == R3) {
                    // MOV R3, R3：同一拍采样 VI、VO、RO。
                    if (vi != 0 && !(vo && ro == 0)) {
                        ri = true;
                        outputWrite = true;
                        outputValue = di;
                    }
                } else if (destination == R3) {
                    if (!(vo && ro == 0)) {
                        outputWrite = true;
                        outputValue = sourceValue(source);
                    }
                } else if (source == R3) {
                    if (vi != 0) {
                        ri = true;
                        if (destination == R2) {
                            memoryWrite = true;
                            memoryValue = di;
                        } else if (destination == R0) {
                            newR0 = di;
                        } else if (destination == R1) {
                            newR1 = di;
                        }
                    }
                } else {
                    int value = sourceValue(source);
                    if (destination == R2) {
                        memoryWrite = true;
                        memoryValue = value;
                    } else if (destination == R0) {
                        newR0 = value;
                    } else if (destination == R1) {
                        newR1 = value;
                    }
                }
            } else if (instruction >= 0x20 && instruction <= 0x27) {
                int operand = memory[r1 & 0xFF] & 0xFF;
                newR0 = switch (instruction) {
                    case ADD -> (r0 + operand) & 0xFF;
                    case SUB -> (r0 - operand) & 0xFF;
                    case AND -> r0 & operand;
                    case OR -> r0 | operand;
                    case NOT -> (~r0) & 0xFF;
                    case XOR -> r0 ^ operand;
                    case SHL -> (r0 << (operand & 0x7)) & 0xFF;
                    case SHR -> (r0 & 0xFF) >> (operand & 0x7);
                    default -> r0;
                };
            } else if (instruction >= 0xE0 && instruction <= 0xE2) {
                int register = instruction & 0x3;
                newTr = register == R0 ? r0 : register == R1 ? r1 : memory[r1 & 0xFF] & 0xFF;
            } else if (isJump(instruction)) {
                if (jumpCondition(instruction, vi, ro)) {
                    nextPc = tr;
                }
            }

            // 时钟沿：VO/RO 握手优先，然后应用本 tick 的输出写和内存写。
            if (vo && ro != 0) {
                vo = false;
            }
            if (outputWrite) {
                doValue = outputValue & 0xFF;
                vo = true;
            }
            if (memoryWrite) {
                memory[r1 & 0xFF] = (byte) (memoryValue & 0xFF);
            }
            r0 = newR0 & 0xFF;
            r1 = newR1 & 0xFF;
            tr = newTr & 0xFF;
            pc = nextPc;

            return new CpuStep(ri, vo, doValue);
        }

        private int sourceValue(int source) {
            return switch (source) {
                case R0 -> r0;
                case R1 -> r1;
                case R2 -> memory[r1 & 0xFF] & 0xFF;
                default -> 0;
            };
        }

        private boolean isJump(int instruction) {
            return switch (instruction) {
                case JMP, JZ, JN, JP, JEQ, JLT_U, JGT_U, JNZ, JNN, JNP,
                        JNE, JGE_U, JLE_U, JVI, JVO, JNVI, JNVO,
                        JLT, JGT, JGE, JLE -> true;
                default -> false;
            };
        }

        private boolean jumpCondition(int instruction, int vi, int ro) {
            int memoryValue = memory[r1 & 0xFF] & 0xFF;
            return switch (instruction) {
                case JMP -> true;
                case JZ -> r0 == 0;
                case JNZ -> r0 != 0;
                case JN -> (r0 & 0x80) != 0;
                case JNN -> (r0 & 0x80) == 0;
                case JP -> r0 != 0 && (r0 & 0x80) == 0;
                case JNP -> r0 == 0 || (r0 & 0x80) != 0;
                case JEQ -> r0 == memoryValue;
                case JNE -> r0 != memoryValue;
                case JLT_U -> r0 < memoryValue;
                case JGT_U -> r0 > memoryValue;
                case JGE_U -> r0 >= memoryValue;
                case JLE_U -> r0 <= memoryValue;
                case JLT -> (byte) r0 < (byte) memoryValue;
                case JGT -> (byte) r0 > (byte) memoryValue;
                case JGE -> (byte) r0 >= (byte) memoryValue;
                case JLE -> (byte) r0 <= (byte) memoryValue;
                case JVI -> vi != 0;
                case JNVI -> vi == 0;
                case JVO -> vo;
                case JNVO -> !vo;
                default -> false;
            };
        }
    }
}
