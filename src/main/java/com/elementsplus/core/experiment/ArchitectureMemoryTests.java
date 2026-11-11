package com.elementsplus.core.experiment;

import com.elementsplus.core.experiment.ArchitectureTestSupport.ProgramBuilder;
import com.elementsplus.core.experiment.ArchitectureTestSupport.ScheduleBuilder;
import com.elementsplus.core.experiment.CircuitExperiment.ConstantArchitectureTestCase;

import java.util.List;

/** 计算机架构第三章：连接内存（R2 = mem[R1]）。 */
final class ArchitectureMemoryTests {

    private ArchitectureMemoryTests() {
    }

    static List<TestCase> build() {
        return List.of(
                writeAndReadRoundTrip(),
                initialMemoryIsZero(),
                addressIsolation(),
                r1AndR2Copies(),
                writeFromInput(),
                outputFromR2(),
                r2ToR2IsNoOp()
        );
    }

    private static TestCase writeAndReadRoundTrip() {
        ProgramBuilder program = new ProgramBuilder()
                .ldi(ArchitectureTestSupport.R1, 0x20)
                .ldi(ArchitectureTestSupport.R0, 0x5A)
                .mov(ArchitectureTestSupport.R2, ArchitectureTestSupport.R0)
                .nop()
                .mov(ArchitectureTestSupport.R0, ArchitectureTestSupport.R2)
                .mov(ArchitectureTestSupport.R3, ArchitectureTestSupport.R0)
                .nop();
        return linear(program);
    }

    private static TestCase initialMemoryIsZero() {
        ProgramBuilder program = new ProgramBuilder()
                .ldi(ArchitectureTestSupport.R1, 0x40)
                .mov(ArchitectureTestSupport.R0, ArchitectureTestSupport.R2)
                .mov(ArchitectureTestSupport.R3, ArchitectureTestSupport.R0)
                .nop();
        return linear(program);
    }

    /** 改变 R1 后访问的是另一个单元，不应读到上一个地址的数据。 */
    private static TestCase addressIsolation() {
        ProgramBuilder program = new ProgramBuilder()
                .ldi(ArchitectureTestSupport.R1, 0x08)
                .ldi(ArchitectureTestSupport.R0, 0x11)
                .mov(ArchitectureTestSupport.R2, ArchitectureTestSupport.R0)
                .ldi(ArchitectureTestSupport.R1, 0x09)
                .ldi(ArchitectureTestSupport.R0, 0x22)
                .mov(ArchitectureTestSupport.R2, ArchitectureTestSupport.R0)
                .ldi(ArchitectureTestSupport.R1, 0x08)
                .mov(ArchitectureTestSupport.R0, ArchitectureTestSupport.R2)
                .mov(ArchitectureTestSupport.R3, ArchitectureTestSupport.R0)
                .nop()
                .ldi(ArchitectureTestSupport.R1, 0x09)
                .mov(ArchitectureTestSupport.R0, ArchitectureTestSupport.R2)
                .mov(ArchitectureTestSupport.R3, ArchitectureTestSupport.R0)
                .nop();
        return linear(program);
    }

    /** 覆盖 MOV R2, R1 与 MOV R1, R2 两个方向。 */
    private static TestCase r1AndR2Copies() {
        ProgramBuilder program = new ProgramBuilder()
                .ldi(ArchitectureTestSupport.R1, 0x10)
                .mov(ArchitectureTestSupport.R2, ArchitectureTestSupport.R1)
                .mov(ArchitectureTestSupport.R0, ArchitectureTestSupport.R2)
                .mov(ArchitectureTestSupport.R3, ArchitectureTestSupport.R0)
                .nop()
                .ldi(ArchitectureTestSupport.R1, 0x30)
                .ldi(ArchitectureTestSupport.R0, 0x44)
                .mov(ArchitectureTestSupport.R2, ArchitectureTestSupport.R0)
                .mov(ArchitectureTestSupport.R1, ArchitectureTestSupport.R2)
                .mov(ArchitectureTestSupport.R0, ArchitectureTestSupport.R1)
                .mov(ArchitectureTestSupport.R3, ArchitectureTestSupport.R0)
                .nop();
        return linear(program);
    }

    /** MOV R2, R3：先完成输入握手，再把 DI 写入内存。 */
    private static TestCase writeFromInput() {
        ProgramBuilder program = new ProgramBuilder()
                .ldi(ArchitectureTestSupport.R1, 0x60)
                .mov(ArchitectureTestSupport.R2, ArchitectureTestSupport.R3)
                .nop()
                .mov(ArchitectureTestSupport.R0, ArchitectureTestSupport.R2)
                .mov(ArchitectureTestSupport.R3, ArchitectureTestSupport.R0)
                .nop();
        ScheduleBuilder schedule = new ScheduleBuilder()
                .fill(2, 0, 0, 1)
                .tick(1, 0x77, 1)
                .fill(program.size() - 3, 0, 0, 1);
        return ArchitectureTestSupport.buildCase(program, schedule);
    }

    private static TestCase outputFromR2() {
        ProgramBuilder program = new ProgramBuilder()
                .ldi(ArchitectureTestSupport.R1, 0x20)
                .ldi(ArchitectureTestSupport.R0, 0x5A)
                .mov(ArchitectureTestSupport.R2, ArchitectureTestSupport.R0)
                .nop()
                .mov(ArchitectureTestSupport.R3, ArchitectureTestSupport.R2)
                .nop();
        return linear(program);
    }

    private static TestCase r2ToR2IsNoOp() {
        ProgramBuilder program = new ProgramBuilder()
                .ldi(ArchitectureTestSupport.R1, 0x20)
                .ldi(ArchitectureTestSupport.R0, 0x5A)
                .mov(ArchitectureTestSupport.R2, ArchitectureTestSupport.R0)
                .nop()
                .mov(ArchitectureTestSupport.R2, ArchitectureTestSupport.R2)
                .mov(ArchitectureTestSupport.R0, ArchitectureTestSupport.R2)
                .mov(ArchitectureTestSupport.R3, ArchitectureTestSupport.R0)
                .nop();
        return linear(program);
    }

    private static ConstantArchitectureTestCase linear(ProgramBuilder program) {
        ScheduleBuilder schedule = new ScheduleBuilder().fill(program.size(), 0, 0, 1);
        return ArchitectureTestSupport.buildCase(program, schedule);
    }
}
