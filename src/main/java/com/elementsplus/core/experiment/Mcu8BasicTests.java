package com.elementsplus.core.experiment;

import com.elementsplus.core.experiment.CircuitExperiment.ConstantArchitectureTestCase;

import java.util.ArrayList;
import java.util.List;

/**
 * 8 位基础单片机的架构测例。
 *
 * <p>测例通过外部程序 ROM 提供指令，覆盖立即数、RAM、ALU、I/O、跳转和保留指令。
 * 每个程序都保持 PC 在程序范围内，避免测例本身越界。</p>
 */
final class Mcu8BasicTests {
    private Mcu8BasicTests() {
    }

    static List<TestCase> build() {
        List<TestCase> tests = new ArrayList<>();
        tests.add(aluAndImmediateCase());
        tests.add(ioEchoCase());
        tests.add(ioEchoDirectCase());
        tests.add(ioMemoryCase());
        for (int opcode : new int[]{
                0xC0, 0xC1, 0xC2, 0xC3, 0xC4, 0xC5, 0xC6, 0xC9, 0xCA, 0xCB, 0xCC, 0xCD, 0xCE,
                0xD0, 0xD1, 0xD8, 0xD9, 0xE4, 0xE5, 0xE6, 0xE7
        }) {
            tests.add(jumpCase(opcode));
        }
        return tests;
    }

    private static ConstantArchitectureTestCase aluAndImmediateCase() {
        ArchitectureTestSupport.ProgramBuilder p = new ArchitectureTestSupport.ProgramBuilder();
        p.ldi(0, 0x12);
        p.ldi(1, 0x34);
        p.raw(0x08);                 // undefined -> NOP
        p.mov(2, 1);                 // mem[0x34] = 0x34
        p.setTarget(2);              // SETTARGET R2 reads mem[R1]
        p.mov(0, 1);                 // R0 = 0x34
        p.raw(0x20);                 // ADD
        p.raw(0x21);                 // SUB
        p.raw(0x22);                 // AND
        p.raw(0x23);                 // OR
        p.raw(0x25);                 // XOR
        p.raw(0x24);                 // NOT
        p.raw(0x26);                 // SHL
        p.raw(0x27);                 // SHR
        p.raw(0x2F);                 // undefined -> NOP
        p.ldl(0, 0x5);
        p.ldh(0, 0x6);               // R0 = 0x65
        p.mov(1, 0);                 // R1 = 0x65
        p.mov(3, 0);                 // write DO
        p.mov(3, 3);                 // VI=0: no read
        p.raw(0xE3);                 // reserved SETTARGET R3
        p.raw(0x7F);                 // reserved LDL R3
        p.nop();
        ArchitectureTestSupport.ScheduleBuilder s = new ArchitectureTestSupport.ScheduleBuilder();
        s.fill(p.size(), 0, 0, 1);
        return ArchitectureTestSupport.buildCase(p, s);
    }

    private static ConstantArchitectureTestCase ioEchoCase() {
        ArchitectureTestSupport.ProgramBuilder p = new ArchitectureTestSupport.ProgramBuilder();
        p.mov(0, 3);                 // DI -> R0, raises RI
        p.mov(3, 0);                 // R0 -> DO, raises VO
        p.ldi(0, 0);
        p.setTarget(0);              // TR = 0
        p.jump(0xC0);                // JMP loop
        ArchitectureTestSupport.ScheduleBuilder s = new ArchitectureTestSupport.ScheduleBuilder();
        for (int i = 0; i < 8; i++) {
            s.tick(15, i < 4 ? 0x40 + i : 0, 1);
        }
        return ArchitectureTestSupport.buildCase(p, s);
    }

    private static ConstantArchitectureTestCase ioEchoDirectCase() {
        ArchitectureTestSupport.ProgramBuilder p = new ArchitectureTestSupport.ProgramBuilder();
        p.mov(3, 3);                 // DI -> DO，同时检查 RI
        p.ldi(0, 0);
        p.setTarget(0);
        p.jump(0xC0);
        ArchitectureTestSupport.ScheduleBuilder s = new ArchitectureTestSupport.ScheduleBuilder();
        for (int i = 0; i < 8; i++) {
            s.tick(15, 0x70 + i, 1);
        }
        return ArchitectureTestSupport.buildCase(p, s);
    }

    private static ConstantArchitectureTestCase ioMemoryCase() {
        ArchitectureTestSupport.ProgramBuilder p = new ArchitectureTestSupport.ProgramBuilder();
        p.mov(2, 3);                 // DI -> mem[R1]
        p.mov(1, 2);                 // R1 = mem[R1]
        p.mov(3, 1);                 // DO = R1
        p.ldi(0, 0);
        p.setTarget(0);
        p.jump(0xC0);
        ArchitectureTestSupport.ScheduleBuilder s = new ArchitectureTestSupport.ScheduleBuilder();
        for (int i = 0; i < 10; i++) {
            s.tick(15, 0x60 + i, 1);
        }
        return ArchitectureTestSupport.buildCase(p, s);
    }

    private static ConstantArchitectureTestCase jumpCase(int opcode) {
        ArchitectureTestSupport.ProgramBuilder p = new ArchitectureTestSupport.ProgramBuilder();
        p.ldi(1, 0x10);              // target and memory address
        p.ldi(0, 0x80);              // memory data
        p.mov(2, 0);                 // mem[0x10] = 0x80
        p.setTarget(1);              // TR = 0x10
        p.ldi(0, 0x01);              // compare data
        if (opcode == 0xD1 || opcode == 0xD9) {
            p.mov(3, 0);             // establish VO for JVO/JNVO
        }
        p.jump(opcode);
        while (p.size() < 16) {
            p.nop();
        }
        p.nop();
        p.nop();
        p.nop();
        p.nop();
        ArchitectureTestSupport.ScheduleBuilder s = new ArchitectureTestSupport.ScheduleBuilder();
        int vi = opcode == 0xD0 ? 15 : 0;
        int ro = opcode == 0xD1 || opcode == 0xD9 ? 0 : 1;
        s.fill(10, vi, 0, ro);
        return ArchitectureTestSupport.buildCase(p, s);
    }
}
