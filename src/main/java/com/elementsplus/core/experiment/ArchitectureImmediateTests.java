package com.elementsplus.core.experiment;

import com.elementsplus.core.experiment.ArchitectureTestSupport.ProgramBuilder;
import com.elementsplus.core.experiment.ArchitectureTestSupport.ScheduleBuilder;
import com.elementsplus.core.experiment.CircuitExperiment.ConstantArchitectureTestCase;

import java.util.List;

/** 计算机架构第二章：立即数（LDL/LDH）。 */
final class ArchitectureImmediateTests {

    private ArchitectureImmediateTests() {
    }

    static List<TestCase> build() {
        return List.of(
                loadAndOutput(),
                nibblePreservation(),
                loadR1AndCopy(),
                boundaryValues()
        );
    }

    private static TestCase loadAndOutput() {
        ProgramBuilder program = new ProgramBuilder()
                .ldi(ArchitectureTestSupport.R0, 0xA3)
                .mov(ArchitectureTestSupport.R3, ArchitectureTestSupport.R0)
                .nop();
        return linear(program);
    }

    /** LDL/LDH 只修改半个字节，另一半点必须保留。 */
    private static TestCase nibblePreservation() {
        ProgramBuilder program = new ProgramBuilder()
                .ldl(ArchitectureTestSupport.R0, 0xF)
                .ldh(ArchitectureTestSupport.R0, 0x5)
                .ldl(ArchitectureTestSupport.R0, 0x3)
                .ldh(ArchitectureTestSupport.R0, 0xA)
                .mov(ArchitectureTestSupport.R3, ArchitectureTestSupport.R0)
                .nop();
        return linear(program);
    }

    /** 立即数设置 R1，再通过 MOV 复制到 R0 输出。 */
    private static TestCase loadR1AndCopy() {
        ProgramBuilder program = new ProgramBuilder()
                .ldi(ArchitectureTestSupport.R1, 0x20)
                .mov(ArchitectureTestSupport.R0, ArchitectureTestSupport.R1)
                .mov(ArchitectureTestSupport.R3, ArchitectureTestSupport.R0)
                .nop();
        return linear(program);
    }

    /** 0x00/0xFF 边界，以及覆盖 R0 后另一半立即数仍能正确组合。 */
    private static TestCase boundaryValues() {
        ProgramBuilder program = new ProgramBuilder()
                .ldi(ArchitectureTestSupport.R0, 0xFF)
                .mov(ArchitectureTestSupport.R3, ArchitectureTestSupport.R0)
                .nop()
                .ldi(ArchitectureTestSupport.R0, 0x00)
                .mov(ArchitectureTestSupport.R3, ArchitectureTestSupport.R0)
                .nop();
        return linear(program);
    }

    private static ConstantArchitectureTestCase linear(ProgramBuilder program) {
        ScheduleBuilder schedule = new ScheduleBuilder().fill(program.size(), 0, 0, 1);
        return ArchitectureTestSupport.buildCase(program, schedule);
    }
}
