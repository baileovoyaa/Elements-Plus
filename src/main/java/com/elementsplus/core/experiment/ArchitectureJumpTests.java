package com.elementsplus.core.experiment;

import com.elementsplus.core.experiment.ArchitectureTestSupport.ProgramBuilder;
import com.elementsplus.core.experiment.ArchitectureTestSupport.ScheduleBuilder;
import com.elementsplus.core.experiment.CircuitExperiment.ConstantArchitectureTestCase;

import java.util.List;

import static com.elementsplus.core.experiment.ArchitectureTestSupport.*;

/** 计算机架构第五章：跳转、TR、条件跳转和 IO 等待。 */
final class ArchitectureJumpTests {

    private ArchitectureJumpTests() {
    }

    static List<TestCase> build() {
        return List.of(
                // JMP / SETTARGET R0
                branchCase(JMP, 0, false, 0, 0, false, 0),

                // R0 与 0 的条件
                branchCase(JZ, 0, false, 0, 0, false, 0),
                branchCase(JZ, 1, false, 0, 0, false, 0),
                branchCase(JNZ, 1, false, 0, 0, false, 0),
                branchCase(JNZ, 0, false, 0, 0, false, 0),
                branchCase(JN, 0x80, false, 0, 0, false, 0),
                branchCase(JNN, 0x7F, false, 0, 0, false, 0),
                branchCase(JP, 0x01, false, 0, 0, false, 0),
                branchCase(JNP, 0x80, false, 0, 0, false, 0),

                // R0 与 mem[R1] 的条件
                branchCase(JEQ, 0x12, true, 0x12, 0, false, 0),
                branchCase(JEQ, 0x12, true, 0x34, 0, false, 0),
                branchCase(JNE, 0x12, true, 0x34, 0, false, 0),
                branchCase(JNE, 0x12, true, 0x12, 0, false, 0),
                branchCase(JLT_U, 0x01, true, 0x80, 0, false, 0),
                branchCase(JLT_U, 0x80, true, 0x01, 0, false, 0),
                branchCase(JGT_U, 0x80, true, 0x01, 0, false, 0),
                branchCase(JGE_U, 0x80, true, 0x80, 0, false, 0),
                branchCase(JLE_U, 0x01, true, 0x80, 0, false, 0),
                branchCase(JLT, 0x80, true, 0x01, 0, false, 0),
                branchCase(JLT, 0x01, true, 0x80, 0, false, 0),
                branchCase(JGT, 0x01, true, 0x80, 0, false, 0),
                branchCase(JGE, 0x01, true, 0x01, 0, false, 0),
                branchCase(JLE, 0x80, true, 0x01, 0, false, 0),

                // IO 条件
                branchCase(JVI, 0, false, 0, 1, false, 0),
                branchCase(JVI, 0, false, 0, 0, false, 0),
                branchCase(JNVI, 0, false, 0, 0, false, 0),
                branchCase(JNVI, 0, false, 0, 1, false, 0),
                branchCase(JVO, 0, false, 0, 0, true, 0x11),
                branchCase(JVO, 0, false, 0, 0, false, 0),
                branchCase(JNVO, 0, false, 0, 0, false, 0),
                branchCase(JNVO, 0, false, 0, 0, true, 0x11),

                // 真正的等待循环
                inputWaitLoop(),
                outputWaitLoop(),

                // SETTARGET R1/R2
                setTargetR1(),
                setTargetR2()
        );
    }

    /**
     * 构造一个分支测例：先设置 TR 指向 target，再准备条件，执行待测跳转。
     * 不跳转时执行 fallback 输出 0x55，跳转时执行 target 输出 0xAA。
     * 两条路径最后都进入 halt 自循环，固定多跑几个 tick 以验证循环 PC。
     */
    private static TestCase branchCase(int opcode, int r0Value, boolean prepareMemory, int memValue,
                                       int vi, boolean preOutput, int preValue) {
        ProgramBuilder program = new ProgramBuilder();
        program.setTargetLabel(R0, "target");
        if (prepareMemory) {
            program.ldi(R1, 0x40);
            program.ldi(R0, memValue);
            program.mov(R2, R0);
        }
        if (preOutput) {
            // JVO/JNVO 必须在输出写生效后的下一 tick 立即执行；中间不能再插入 LDI。
            program.ldi(R0, preValue);
            program.mov(R3, R0);
        } else {
            program.ldi(R0, r0Value);
        }
        program.jump(opcode);

        // 条件不成立路径
        program.ldi(R0, 0x55);
        program.mov(R3, R0);
        program.nop();
        program.setTargetLabel(R0, "halt");
        program.jump(JMP);

        // 条件成立路径
        program.label("target");
        program.ldi(R0, 0xAA);
        program.mov(R3, R0);
        program.nop();
        program.setTargetLabel(R0, "halt");

        program.label("halt");
        program.jump(JMP);

        ScheduleBuilder schedule = new ScheduleBuilder().fill(program.size() + 4, vi, 0, 1);
        return ArchitectureTestSupport.buildCase(program, schedule, true);
    }

    /** JNVI 等待循环：VI=0 时反复跳回，VI=1 后读取数据并输出。 */
    private static TestCase inputWaitLoop() {
        ProgramBuilder program = new ProgramBuilder();
        program.setTargetLabel(R0, "wait");
        program.label("wait");
        program.jump(JNVI);
        program.mov(R0, R3);
        program.mov(R3, R0);
        program.nop();
        program.setTargetLabel(R0, "halt");
        program.label("halt");
        program.jump(JMP);

        ScheduleBuilder schedule = new ScheduleBuilder()
                .fill(4, 0, 0, 1)
                .tick(1, 0x42, 1)
                .tick(1, 0x42, 1)
                .fill(program.size() + 8 - 6, 0, 0, 1);
        return ArchitectureTestSupport.buildCase(program, schedule, true);
    }

    /** JVO 等待循环：VO=1 时反复跳回，RO 取走输出后再写下一笔。 */
    private static TestCase outputWaitLoop() {
        ProgramBuilder program = new ProgramBuilder();
        program.setTargetLabel(R1, "wait");
        program.ldi(R0, 0x5A);
        program.mov(R3, R0);
        program.label("wait");
        program.jump(JVO);
        program.mov(R3, R0);
        program.nop();
        program.setTargetLabel(R0, "halt");
        program.label("halt");
        program.jump(JMP);

        ScheduleBuilder schedule = new ScheduleBuilder()
                .fill(8, 0, 0, 0)
                .fill(program.size() + 8 - 8, 0, 0, 1);
        return ArchitectureTestSupport.buildCase(program, schedule, true);
    }

    /** SETTARGET R1：用 R1 保存目标地址，不覆盖 R0。 */
    private static TestCase setTargetR1() {
        ProgramBuilder program = new ProgramBuilder();
        program.setTargetLabel(R1, "target");
        program.jump(JMP);
        program.ldi(R0, 0x55);
        program.mov(R3, R0);
        program.nop();
        program.setTargetLabel(R0, "halt");
        program.jump(JMP);
        program.label("target");
        program.ldi(R0, 0xAA);
        program.mov(R3, R0);
        program.nop();
        program.setTargetLabel(R0, "halt");
        program.label("halt");
        program.jump(JMP);
        return finish(program);
    }

    /** SETTARGET R2：把目标地址写到 mem[0x40]，再从 R2 装入 TR。 */
    private static TestCase setTargetR2() {
        ProgramBuilder program = new ProgramBuilder();
        program.ldiLabel(R0, "target");
        program.ldi(R1, 0x40);
        program.mov(R2, R0);
        program.setTarget(R2);
        program.jump(JMP);
        program.ldi(R0, 0x55);
        program.mov(R3, R0);
        program.nop();
        program.setTargetLabel(R0, "halt");
        program.jump(JMP);
        program.label("target");
        program.ldi(R0, 0xAA);
        program.mov(R3, R0);
        program.nop();
        program.setTargetLabel(R0, "halt");
        program.label("halt");
        program.jump(JMP);
        return finish(program);
    }

    private static ConstantArchitectureTestCase finish(ProgramBuilder program) {
        ScheduleBuilder schedule = new ScheduleBuilder().fill(program.size() + 4, 0, 0, 1);
        return ArchitectureTestSupport.buildCase(program, schedule, true);
    }
}
