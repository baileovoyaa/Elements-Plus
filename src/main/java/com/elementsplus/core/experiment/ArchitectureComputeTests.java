package com.elementsplus.core.experiment;

import com.elementsplus.core.experiment.ArchitectureTestSupport.ProgramBuilder;
import com.elementsplus.core.experiment.ArchitectureTestSupport.ScheduleBuilder;

import java.util.List;

/** 计算机架构第四章：基础 ALU 运算。 */
final class ArchitectureComputeTests {

    private ArchitectureComputeTests() {
    }

    static List<TestCase> build() {
        return List.of(
                aluCase(ArchitectureTestSupport.ADD, 0x0A, 0x03),
                aluCase(ArchitectureTestSupport.SUB, 0x0A, 0x03),
                aluCase(ArchitectureTestSupport.AND, 0xAA, 0x0F),
                aluCase(ArchitectureTestSupport.OR, 0x50, 0x0F),
                aluCase(ArchitectureTestSupport.NOT, 0x0F, 0x5A),
                aluCase(ArchitectureTestSupport.XOR, 0xAA, 0xFF),
                aluCase(ArchitectureTestSupport.SHL, 0x81, 0x03),
                aluCase(ArchitectureTestSupport.SHR, 0x81, 0x03),
                aluCase(ArchitectureTestSupport.SHL, 0x01, 0x07),
                aluCase(ArchitectureTestSupport.SHR, 0x80, 0x07),
                aluCase(ArchitectureTestSupport.ADD, 0xFF, 0x01),
                aluCase(ArchitectureTestSupport.SUB, 0x00, 0x01)
        );
    }

    /**
     * 先把 B 写到 mem[0x20]，再把 A 装入 R0，执行 ALU，最后输出结果。
     * 这样每个测例都使用独立模拟器中的干净 RAM。
     */
    private static TestCase aluCase(int opcode, int a, int b) {
        ProgramBuilder program = new ProgramBuilder()
                .ldi(ArchitectureTestSupport.R1, 0x20)
                .ldi(ArchitectureTestSupport.R0, b)
                .mov(ArchitectureTestSupport.R2, ArchitectureTestSupport.R0)
                .ldi(ArchitectureTestSupport.R0, a)
                .alu(opcode)
                .mov(ArchitectureTestSupport.R3, ArchitectureTestSupport.R0)
                .nop();
        ScheduleBuilder schedule = new ScheduleBuilder().fill(program.size(), 0, 0, 1);
        return ArchitectureTestSupport.buildCase(program, schedule);
    }
}
