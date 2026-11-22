package com.elementsplus.core.circuit.component;

import net.minecraft.nbt.CompoundTag;

import java.util.List;

/**
 * 8 位基础单片机：与架构章节参考 CPU 相同的 8 位指令集，
 * 包含 R0/R1/TR、256 字节 RAM，以及 VI/RI/DI/VO/RO/DO 接口。
 */
public class Mcu8BasicComponentInstance extends AbstractComponentInstance {
    public static final int RAM_SIZE = 256;

    private int pc;
    private int r0;
    private int r1;
    private int tr;
    private int doValue;
    private boolean vo;
    private boolean lastRead;
    private boolean lastWrite;
    private final int[] memory = new int[RAM_SIZE];

    public Mcu8BasicComponentInstance() {
        super();
    }

    public Mcu8BasicComponentInstance(Mcu8BasicComponentInstance source) {
        super(source);
        copyFrom(source);
    }

    @Override
    protected List<Config> defaultConfigs() {
        return List.of();
    }

    @Override
    protected CircuitComponentInstance createEmptyCopy() {
        return new Mcu8BasicComponentInstance();
    }

    @Override
    public CircuitComponentInstance copy() {
        return new Mcu8BasicComponentInstance(this);
    }

    private void copyFrom(Mcu8BasicComponentInstance source) {
        this.pc = source.pc;
        this.r0 = source.r0;
        this.r1 = source.r1;
        this.tr = source.tr;
        this.doValue = source.doValue;
        this.vo = source.vo;
        this.lastRead = source.lastRead;
        this.lastWrite = source.lastWrite;
        System.arraycopy(source.memory, 0, this.memory, 0, RAM_SIZE);
    }

    public int getPc() {
        return pc;
    }

    public int getR0() {
        return r0;
    }

    public int getR1() {
        return r1;
    }

    public int getTr() {
        return tr;
    }

    public int getDoValue() {
        return doValue;
    }

    public boolean isVo() {
        return vo;
    }

    public boolean lastRead() {
        return lastRead;
    }

    public boolean lastWrite() {
        return lastWrite;
    }

    public int getMemory(int address) {
        return memory[address & (RAM_SIZE - 1)] & 0xFF;
    }

    /** 供客户端状态对话框读取的只读内存快照。 */
    public int[] getMemorySnapshot() {
        return memory.clone();
    }

    public void setMemory(int address, int value) {
        memory[address & (RAM_SIZE - 1)] = value & 0xFF;
    }

    public void reset() {
        pc = 0;
        r0 = 0;
        r1 = 0;
        tr = 0;
        doValue = 0;
        vo = false;
        lastRead = false;
        lastWrite = false;
        java.util.Arrays.fill(memory, 0);
    }

    /**
     * RI 是执行阶段的组合输出：当前指令是否会读取 DI。
     * 与参考 CPU 一致，MOV R3,R3 在 VO=1 且 RO=0 时不会读取。
     */
    public boolean readsInput(int instruction, boolean vi, boolean ro) {
        int ins = instruction & 0xFF;
        if (!vi || (ins < 0x30 || ins > 0x3F)) {
            return false;
        }
        int destination = ins & 0x3;
        int source = (ins >> 2) & 0x3;
        if (source != 3) {
            return false;
        }
        if (destination == 3) {
            return !(vo && !ro);
        }
        return true;
    }

    /**
     * 执行一条指令。返回时 lastRead/lastWrite 表示本 tick 是否发生读取/写出。
     */
    public void step(int instruction, boolean vi, int di, boolean ro) {
        int ins = instruction & 0xFF;
        int nextPc = (pc + 1) & 0xFF;
        boolean read = false;
        boolean outputWrite = false;
        int outputValue = 0;
        boolean memoryWrite = false;
        int memoryValue = 0;
        int newR0 = r0;
        int newR1 = r1;
        int newTr = tr;

        if ((ins & 0xC0) == 0x40) {
            int register = (ins >> 4) & 0x3;
            int value = ins & 0xF;
            if (register == 0) {
                newR0 = (r0 & 0xF0) | value;
            } else if (register == 1) {
                newR1 = (r1 & 0xF0) | value;
            }
        } else if ((ins & 0xC0) == 0x80) {
            int register = (ins >> 4) & 0x3;
            int value = ins & 0xF;
            if (register == 0) {
                newR0 = (r0 & 0x0F) | (value << 4);
            } else if (register == 1) {
                newR1 = (r1 & 0x0F) | (value << 4);
            }
        } else if (ins >= 0x30 && ins <= 0x3F) {
            int destination = ins & 0x3;
            int source = (ins >> 2) & 0x3;
            if (destination == 3 && source == 3) {
                if (vi && !(vo && !ro)) {
                    read = true;
                    outputWrite = true;
                    outputValue = di;
                }
            } else if (destination == 3) {
                if (!(vo && !ro)) {
                    outputWrite = true;
                    outputValue = sourceValue(source);
                }
            } else if (source == 3) {
                if (vi) {
                    read = true;
                    if (destination == 2) {
                        memoryWrite = true;
                        memoryValue = di;
                    } else if (destination == 0) {
                        newR0 = di;
                    } else if (destination == 1) {
                        newR1 = di;
                    }
                }
            } else {
                int value = sourceValue(source);
                if (destination == 2) {
                    memoryWrite = true;
                    memoryValue = value;
                } else if (destination == 0) {
                    newR0 = value;
                } else if (destination == 1) {
                    newR1 = value;
                }
            }
        } else if (ins >= 0x20 && ins <= 0x27) {
            int operand = memory[r1 & 0xFF];
            newR0 = switch (ins) {
                case 0x20 -> (r0 + operand) & 0xFF;
                case 0x21 -> (r0 - operand) & 0xFF;
                case 0x22 -> r0 & operand;
                case 0x23 -> r0 | operand;
                case 0x24 -> (~r0) & 0xFF;
                case 0x25 -> r0 ^ operand;
                case 0x26 -> (r0 << (operand & 0x7)) & 0xFF;
                case 0x27 -> (r0 & 0xFF) >>> (operand & 0x7);
                default -> r0;
            };
        } else if (ins >= 0xE0 && ins <= 0xE2) {
            int register = ins & 0x3;
            newTr = register == 0 ? r0 : register == 1 ? r1 : memory[r1 & 0xFF];
        } else if (isJump(ins)) {
            if (jumpCondition(ins, vi, ro)) {
                nextPc = tr;
            }
        }

        // 时钟沿：先处理 VO/RO 握手，再应用本 tick 的输出和内存写入。
        if (vo && ro) {
            vo = false;
        }
        if (outputWrite) {
            doValue = outputValue & 0xFF;
            vo = true;
        }
        if (memoryWrite) {
            memory[r1 & 0xFF] = memoryValue & 0xFF;
        }
        r0 = newR0 & 0xFF;
        r1 = newR1 & 0xFF;
        tr = newTr & 0xFF;
        pc = nextPc & 0xFF;
        lastRead = read;
        lastWrite = outputWrite;
    }

    private int sourceValue(int source) {
        return switch (source) {
            case 0 -> r0;
            case 1 -> r1;
            case 2 -> memory[r1 & 0xFF];
            default -> 0;
        };
    }

    private static boolean isJump(int instruction) {
        return switch (instruction) {
            case 0xC0, 0xC1, 0xC2, 0xC3, 0xC4, 0xC5, 0xC6,
                    0xC9, 0xCA, 0xCB, 0xCC, 0xCD, 0xCE,
                    0xD0, 0xD1, 0xD8, 0xD9, 0xE4, 0xE5, 0xE6, 0xE7 -> true;
            default -> false;
        };
    }

    private boolean jumpCondition(int instruction, boolean vi, boolean ro) {
        int memoryValue = memory[r1 & 0xFF];
        return switch (instruction) {
            case 0xC0 -> true;
            case 0xC1 -> r0 == 0;
            case 0xC9 -> r0 != 0;
            case 0xC2 -> (byte) r0 < 0;
            case 0xCA -> (byte) r0 >= 0;
            case 0xC3 -> (byte) r0 > 0;
            case 0xCB -> (byte) r0 <= 0;
            case 0xC4 -> r0 == memoryValue;
            case 0xCC -> r0 != memoryValue;
            case 0xC5 -> r0 < memoryValue;
            case 0xC6 -> r0 > memoryValue;
            case 0xCD -> r0 >= memoryValue;
            case 0xCE -> r0 <= memoryValue;
            case 0xD0 -> vi;
            case 0xD8 -> !vi;
            case 0xD1 -> vo;
            case 0xD9 -> !vo;
            case 0xE4 -> (byte) r0 < (byte) memoryValue;
            case 0xE5 -> (byte) r0 > (byte) memoryValue;
            case 0xE6 -> (byte) r0 >= (byte) memoryValue;
            case 0xE7 -> (byte) r0 <= (byte) memoryValue;
            default -> false;
        };
    }

    public void saveState(CompoundTag tag) {
        tag.putInt("pc", pc);
        tag.putInt("r0", r0);
        tag.putInt("r1", r1);
        tag.putInt("tr", tr);
        tag.putInt("do", doValue);
        tag.putBoolean("vo", vo);
        tag.putIntArray("ram", memory);
    }

    public void loadState(CompoundTag tag) {
        pc = tag.getInt("pc") & 0xFF;
        r0 = tag.getInt("r0") & 0xFF;
        r1 = tag.getInt("r1") & 0xFF;
        tr = tag.getInt("tr") & 0xFF;
        doValue = tag.getInt("do") & 0xFF;
        vo = tag.getBoolean("vo");
        int[] saved = tag.getIntArray("ram");
        java.util.Arrays.fill(memory, 0);
        System.arraycopy(saved, 0, memory, 0, Math.min(saved.length, RAM_SIZE));
        lastRead = false;
        lastWrite = false;
    }
}
