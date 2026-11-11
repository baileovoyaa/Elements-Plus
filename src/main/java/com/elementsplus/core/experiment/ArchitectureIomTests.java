package com.elementsplus.core.experiment;

import com.elementsplus.core.experiment.CircuitExperiment.ArchitectureTick;
import com.elementsplus.core.experiment.CircuitExperiment.ConstantArchitectureTestCase;
import com.elementsplus.core.experiment.CircuitExperiment.PinValue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 计算机架构第一章（输入输出寄存器）的测例。
 *
 * <p>本章只实现 NOP 和 MOV。测例通过外部虚拟 ROM 顺序执行程序，验证 R0/R1 的复制、
 * R3 的读写握手、输入无效时的忽略、输出堵塞时的忽略，以及 MOV R3, R3 直通。
 * 未实现的 ALU/内存/跳转指令不会出现在程序中，因此后续章节的电路也可以先通过本章。</p>
 */
final class ArchitectureIomTests {

    private static final int R0 = 0;
    private static final int R1 = 1;
    private static final int R3 = 3;

    private ArchitectureIomTests() {
    }

    static List<TestCase> build() {
        return List.of(
                basicRegisterCopy(),
                registerNoOpsAndReverseCopy(),
                ignoredInputReadR0(),
                ignoredInputReadR1(),
                outputBlockedAndHandshake(),
                directIoAndBlocking(),
                directIoHandshake(),
                nopAndZeroData(),
                outputFromR1()
        );
    }

    /** MOV R0, R3 -> MOV R1, R0 -> MOV R3, R1，覆盖两个普通寄存器和输出握手。 */
    private static TestCase basicRegisterCopy() {
        ProgramBuilder program = new ProgramBuilder()
                .mov(R0, R3)
                .mov(R1, R0)
                .mov(R3, R1)
                .nop()
                .nop();
        ScheduleBuilder schedule = new ScheduleBuilder()
                .tick(1, 0x12, 0)
                .tick(0, 0x00, 0)
                .tick(0, 0x00, 0)
                .tick(0, 0x00, 1)
                .tick(0, 0x00, 0);
        return buildCase(program, schedule);
    }

    /** MOV R0, R0 / MOV R1, R1 不应改变寄存器；同时覆盖 R0 <- R1。 */
    private static TestCase registerNoOpsAndReverseCopy() {
        ProgramBuilder program = new ProgramBuilder()
                .mov(R0, R3)
                .mov(R1, R3)
                .mov(R0, R0)
                .mov(R1, R1)
                .mov(R0, R1)
                .mov(R3, R0)
                .nop();
        ScheduleBuilder schedule = new ScheduleBuilder()
                .tick(1, 0xA5, 0)
                .tick(1, 0x5A, 0)
                .tick(0, 0x00, 0)
                .tick(0, 0x00, 0)
                .tick(0, 0x00, 0)
                .tick(0, 0x00, 0)
                .tick(0, 0x00, 1);
        return buildCase(program, schedule);
    }

    /** VI=0 时 MOV R0, R3 必须整条忽略：R0 不变且 RI 不拉高。 */
    private static TestCase ignoredInputReadR0() {
        ProgramBuilder program = new ProgramBuilder()
                .mov(R0, R3)
                .mov(R0, R3)
                .mov(R1, R3)
                .mov(R3, R0)
                .nop();
        ScheduleBuilder schedule = new ScheduleBuilder()
                .tick(1, 0x33, 0)
                .tick(0, 0x99, 0)
                .tick(0, 0x77, 0)
                .tick(0, 0x00, 0)
                .tick(0, 0x00, 1);
        return buildCase(program, schedule);
    }

    /** VI=0 时 MOV R1, R3 同样忽略，随后输出 R1 应保持旧值。 */
    private static TestCase ignoredInputReadR1() {
        ProgramBuilder program = new ProgramBuilder()
                .mov(R1, R3)
                .mov(R1, R3)
                .mov(R3, R1)
                .nop();
        ScheduleBuilder schedule = new ScheduleBuilder()
                .tick(1, 0x11, 0)
                .tick(0, 0x22, 0)
                .tick(0, 0x00, 0)
                .tick(0, 0x00, 1);
        return buildCase(program, schedule);
    }

    /** 输出堵塞时写被忽略；RO=1 时握手并接受新写，同时验证输出满时仍能读输入。 */
    private static TestCase outputBlockedAndHandshake() {
        ProgramBuilder program = new ProgramBuilder()
                .mov(R0, R3)
                .mov(R3, R0)
                .mov(R1, R3)
                .mov(R3, R1)
                .mov(R3, R1)
                .nop();
        ScheduleBuilder schedule = new ScheduleBuilder()
                .tick(1, 0x21, 0)
                .tick(0, 0x00, 0)
                .tick(1, 0x22, 0)
                .tick(0, 0x00, 0)
                .tick(0, 0x00, 1)
                .tick(0, 0x00, 1);
        return buildCase(program, schedule);
    }

    /** MOV R3, R3 直通；输出堵塞时整条忽略，VI=0 时也整条忽略。 */
    private static TestCase directIoAndBlocking() {
        ProgramBuilder program = new ProgramBuilder()
                .mov(R3, R3)
                .mov(R3, R3)
                .mov(R3, R3)
                .nop()
                .mov(R3, R3)
                .nop();
        ScheduleBuilder schedule = new ScheduleBuilder()
                .tick(1, 0x5A, 0)
                .tick(1, 0x99, 0)
                .tick(0, 0x77, 0)
                .tick(0, 0x00, 1)
                .tick(1, 0xA5, 0)
                .tick(0, 0x00, 1);
        return buildCase(program, schedule);
    }

    /** 同一 tick 完成输出握手和新直通写：VO 保持有效，DO 更新为新的 DI。 */
    private static TestCase directIoHandshake() {
        ProgramBuilder program = new ProgramBuilder()
                .mov(R3, R3)
                .mov(R3, R3)
                .nop();
        ScheduleBuilder schedule = new ScheduleBuilder()
                .tick(1, 0x11, 0)
                .tick(1, 0x22, 1)
                .tick(0, 0x00, 1);
        return buildCase(program, schedule);
    }

    /** NOP 不改变 R0，覆盖 0 数据、PC 顺序递增和输出握手。 */
    private static TestCase nopAndZeroData() {
        ProgramBuilder program = new ProgramBuilder()
                .nop()
                .nop()
                .mov(R0, R3)
                .nop()
                .mov(R3, R0)
                .nop();
        ScheduleBuilder schedule = new ScheduleBuilder()
                .tick(0, 0x00, 0)
                .tick(0, 0x00, 0)
                .tick(1, 0x00, 0)
                .tick(0, 0x00, 0)
                .tick(0, 0x00, 0)
                .tick(0, 0x00, 1);
        return buildCase(program, schedule);
    }

    /** R1 读取后写输出，使用与前面不同的数据。 */
    private static TestCase outputFromR1() {
        ProgramBuilder program = new ProgramBuilder()
                .mov(R1, R3)
                .mov(R3, R1)
                .nop();
        ScheduleBuilder schedule = new ScheduleBuilder()
                .tick(1, 0x7E, 0)
                .tick(0, 0x00, 0)
                .tick(0, 0x00, 1);
        return buildCase(program, schedule);
    }

    private static ConstantArchitectureTestCase buildCase(ProgramBuilder programBuilder,
                                                          ScheduleBuilder schedule) {
        byte[] program = programBuilder.build();
        if (program.length != schedule.size()) {
            throw new IllegalStateException("Program length " + program.length
                    + " does not match schedule length " + schedule.size());
        }

        ReferenceCpu cpu = new ReferenceCpu(program);
        List<ArchitectureTick> ticks = new ArrayList<>();
        for (int i = 0; i < program.length; i++) {
            int expectedPc = cpu.pc;
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
        return new ConstantArchitectureTestCase(program, ticks);
    }

    private static byte[] pin8(int value) {
        byte[] lanes = new byte[8];
        for (int b = 0; b < 8; b++) {
            lanes[b] = (byte) (((value >> b) & 1) != 0 ? 15 : 0);
        }
        return lanes;
    }

    private static final class ProgramBuilder {
        private final List<Byte> bytes = new ArrayList<>();

        ProgramBuilder nop() {
            bytes.add((byte) 0x00);
            return this;
        }

        /** MOV D, S 的机器码是 0011 SS DD。 */
        ProgramBuilder mov(int destination, int source) {
            bytes.add((byte) (0x30 | ((source & 0x3) << 2) | (destination & 0x3)));
            return this;
        }

        byte[] build() {
            byte[] result = new byte[bytes.size()];
            for (int i = 0; i < result.length; i++) {
                result[i] = bytes.get(i);
            }
            return result;
        }
    }

    private static final class ScheduleBuilder {
        private final List<int[]> ticks = new ArrayList<>();

        /** 添加一个 tick 的外部输入：VI、DI、RO。 */
        ScheduleBuilder tick(int vi, int di, int ro) {
            ticks.add(new int[]{vi, di, ro});
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
     * 第一章的参考模型。只处理 NOP 和 MOV；保留编码按 NOP。
     * 输出寄存器在时钟沿先处理 RO 握手，再应用本 tick 接受的 pending 写。
     */
    private static final class ReferenceCpu {
        private final byte[] program;
        private int pc;
        private int r0;
        private int r1;
        private int doValue;
        private boolean vo;

        ReferenceCpu(byte[] program) {
            this.program = program;
        }

        CpuStep step(int vi, int di, int ro) {
            int instruction = program[pc & 0xFF] & 0xFF;
            boolean ri = false;
            boolean write = false;
            int writeValue = 0;

            int opcode = instruction >> 4;
            if (opcode == 0x3) {
                int destination = instruction & 0x3;
                int source = (instruction >> 2) & 0x3;

                if (destination == R3 && source == R3) {
                    // MOV R3, R3：同时采样 VI/输出堵塞，输入直接送入输出寄存器。
                    if (vi != 0 && !(vo && ro == 0)) {
                        ri = true;
                        write = true;
                        writeValue = di;
                    }
                } else if (destination == R3) {
                    // MOV R3, Rn：输出不堵塞时接受写。
                    if (!(vo && ro == 0)) {
                        write = true;
                        writeValue = source == R0 ? r0 : source == R1 ? r1 : 0;
                    }
                } else if (source == R3) {
                    // MOV Rn, R3：VI=0 时整条忽略。
                    if (vi != 0) {
                        ri = true;
                        writeRegister(destination, di);
                    }
                } else {
                    writeRegister(destination, source == R0 ? r0 : source == R1 ? r1 : 0);
                }
            }

            // 时钟沿：RO 与 VO 握手优先清 VO；随后应用本 tick 接受的输出写。
            if (vo && ro != 0) {
                vo = false;
            }
            if (write) {
                doValue = writeValue & 0xFF;
                vo = true;
            }
            pc = (pc + 1) & 0xFF;
            return new CpuStep(ri, vo, doValue);
        }

        private void writeRegister(int register, int value) {
            if (register == R0) {
                r0 = value & 0xFF;
            } else if (register == R1) {
                r1 = value & 0xFF;
            }
            // R2 在本章保留，忽略。
        }
    }
}
