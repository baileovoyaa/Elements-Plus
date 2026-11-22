package com.elementsplus.core.experiment;

import com.elementsplus.ModItems;
import com.elementsplus.core.circuit.component.Mcu8BasicComponentInstance;
import com.elementsplus.core.file.DiskData;
import com.elementsplus.item.PunchedFileItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 编程实验：直接解释执行外接设备中的 8 位程序，不组装电路。
 *
 * <p>程序从磁盘或打孔文件读取；输入按字节依次通过 VI/DI 提供，
 * 输出通过 VO/DO 观察。程序不要求有结束指令，完成全部预期输出即通过。</p>
 */
public class ProgrammingExperiment extends BaseExperiment {

    /** 一个固定输入输出测例。 */
    public record Case(byte[] input, byte[] expectedOutput, int maxTicks) implements TestCase {
        public Case {
            input = input == null ? new byte[0] : input.clone();
            expectedOutput = expectedOutput == null ? new byte[0] : expectedOutput.clone();
        }

        @Override
        public byte[] input() {
            return input.clone();
        }

        @Override
        public byte[] expectedOutput() {
            return expectedOutput.clone();
        }
    }

    private final List<Case> cases;
    private final boolean scoreSystem;
    private final int requiredScore;

    public ProgrammingExperiment(List<Case> cases, boolean scoreSystem, int requiredScore) {
        this.cases = List.copyOf(cases);
        this.scoreSystem = scoreSystem;
        this.requiredScore = requiredScore;
    }

    public static ProgrammingExperiment echo() {
        List<Case> cases = new ArrayList<>();
        byte[][] samples = {
                {0x48},
                {0x48, 0x65, 0x6C, 0x6C, 0x6F},
                {0x00, 0x01, 0x7F, (byte) 0x80, (byte) 0xFF},
                {0x41, 0x42, 0x43, 0x44},
                {0x10, 0x20, 0x30, 0x40, 0x50, 0x60, 0x70},
                {0x00, 0x00, 0x00, 0x00},
                {0x48, 0x65, 0x6C, 0x6C, 0x6F, 0x20, 0x57, 0x6F, 0x72, 0x6C, 0x64, 0x21},
                buildSequence(0, 32)
        };
        for (byte[] sample : samples) {
            cases.add(new Case(sample, sample, 512));
        }
        return new ProgrammingExperiment(cases, false, 100);
    }

    public static ProgrammingExperiment addByte() {
        List<Case> cases = new ArrayList<>();
        int[][] pairs = {
                {0x01, 0x02}, {0x0A, 0x0B}, {(byte) 0xFF, 0x01}, {(byte) 0xAA, (byte) 0xBB},
                {0x00, 0x00}, {0x7F, 0x01}, {(byte) 0x80, (byte) 0x80}, {0x10, 0x20},
                {0x55, 0x55}, {(byte) 0xF0, 0x0F}, {0x01, (byte) 0xFF}, {(byte) 0xFE, 0x03},
                {0x33, 0x44}, {0x12, 0x34}, {(byte) 0x90, 0x70}, {0x0F, 0x01},
                {0x40, 0x40}, {(byte) 0xCC, 0x33}, {0x05, 0x06}, {(byte) 0xFF, (byte) 0xFF}
        };
        for (int[] pair : pairs) {
            byte[] input = {(byte) pair[0], (byte) pair[1]};
            byte[] output = {(byte) ((pair[0] + pair[1]) & 0xFF)};
            cases.add(new Case(input, output, 512));
        }
        return new ProgrammingExperiment(cases, true, 60);
    }

    private static byte[] buildSequence(int start, int length) {
        byte[] result = new byte[length];
        for (int i = 0; i < length; i++) {
            result[i] = (byte) ((start + i) & 0xFF);
        }
        return result;
    }

    @Override
    public int getTestCaseCount() {
        return cases.size();
    }

    @Override
    public List<TestCase> getTestCases() {
        return List.copyOf(cases);
    }

    @Override
    public boolean usesScoreSystem() {
        return scoreSystem;
    }

    @Override
    public int requiredScore() {
        return requiredScore;
    }

    @Override
    public void onComplete(Context ctx, boolean success) {
        // 编程实验不改变物品；程序文件保留。
    }

    @Override
    public List<Component> preCheckReasons(Context context) {
        List<Component> reasons = new ArrayList<>();
        ItemStack stack = context.getItemStack();
        byte[] program = programBytes(stack);
        if (program == null) {
            reasons.add(Component.translatable("experiment.elements-plus.programming.need_device"));
        } else if (program.length == 0) {
            reasons.add(Component.translatable("experiment.elements-plus.programming.empty_program"));
        } else if (program.length > 256) {
            reasons.add(Component.translatable("experiment.elements-plus.programming.program_too_large"));
        }
        return reasons;
    }

    /** 供服务器监视线程读取外接设备中的程序快照。 */
    public static byte[] readProgramBytes(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        if (stack.is(ModItems.DISK)) {
            return DiskData.read(stack);
        }
        if (stack.is(ModItems.PUNCHED_FILE)) {
            return PunchedFileItem.getData(stack);
        }
        return null;
    }

    private static byte[] programBytes(ItemStack stack) {
        return readProgramBytes(stack);
    }

    @Override
    public boolean tick(Context context) {
        if (context.externalDeviceChanged) {
            return finishRuntimeError(context);
        }
        if (context.testIndex < 0 || context.testIndex >= cases.size()) {
            return false;
        }
        State state = context.experimentState instanceof State s
                && s.testIndex == context.testIndex ? s : null;
        if (state == null) {
            byte[] program = programBytes(context.getItemStack());
            if (program == null || program.length == 0) {
                context.reportTestResultState(context.testIndex, 0);
                context.setErrorLines(List.of(Component.translatable(
                        "experiment.elements-plus.programming.empty_program")));
                context.complete(false);
                return false;
            }
            state = new State(context.testIndex, program);
            context.experimentState = state;
        }

        Case testCase = cases.get(state.testIndex);
        if (testCase.expectedOutput().length == 0) {
            // 空预期输出会立即成功；保留该行为，但测例定义应避免空输出。
            return finishPass(context, state, 0);
        }
        if (state.ticks >= testCase.maxTicks()) {
            return finishTle(context, state, testCase.maxTicks());
        }

        boolean vi = state.inputIndex < testCase.input().length;
        int di = vi ? testCase.input()[state.inputIndex] & 0xFF : 0;
        int pc = state.cpu.getPc() & 0xFF;
        int instruction = pc < state.program.length ? state.program[pc] & 0xFF : 0;
        state.cpu.step(instruction, vi, di, true);
        state.ticks++;

        if (state.cpu.lastRead() && state.inputIndex < testCase.input().length) {
            state.inputIndex++;
        }

        if (state.cpu.lastWrite()) {
            int expectedIndex = state.outputIndex;
            int actual = state.cpu.getDoValue() & 0xFF;
            byte[] expected = testCase.expectedOutput();
            byte[] actualBytes = state.actualBytes();
            byte[] nextActual = Arrays.copyOf(actualBytes, actualBytes.length + 1);
            nextActual[nextActual.length - 1] = (byte) actual;
            state.actual = nextActual;

            int expectedValue = expectedIndex < expected.length ? expected[expectedIndex] & 0xFF : -1;
            if (expectedIndex >= expected.length || actual != expectedValue) {
                return finishWrong(context, state, testCase, expectedIndex);
            }
            state.outputIndex++;
            if (state.outputIndex >= expected.length) {
                return finishPass(context, state, state.ticks);
            }
        }

        if (state.ticks >= testCase.maxTicks()) {
            return finishTle(context, state, testCase.maxTicks());
        }
        context.experimentState = state;
        return true;
    }

    /** 外接设备变化属于运行时错误，不是输出错误（WA）；将当前测例标记为 RE。 */
    private boolean finishRuntimeError(Context context) {
        State state = context.experimentState instanceof State s ? s : null;
        int index = state != null ? state.testIndex : context.testIndex;
        context.reportTestResultState(index, 3);
        context.setErrorLines(List.of(Component.translatable(
                "experiment.elements-plus.programming.device_changed")));
        context.experimentState = null;
        context.complete(false);
        return false;
    }

    private boolean finishPass(Context context, State state, int ticks) {
        context.reportTestResultState(state.testIndex, 1);
        context.setTestTicks(state.testIndex, ticks);
        state.passed++;
        return advance(context, state, false);
    }

    private boolean finishTle(Context context, State state, int maxTicks) {
        context.reportTestResultState(state.testIndex, 2);
        context.setTestTicks(state.testIndex, maxTicks);
        context.setErrorLines(List.of(
                Component.translatable("experiment.elements-plus.testfail.header"),
                Component.translatable("experiment.elements-plus.testfail.tle", maxTicks)));
        return advance(context, state, true);
    }

    private boolean finishWrong(Context context, State state, Case testCase, int wrongIndex) {
        context.reportTestResultState(state.testIndex, 0);
        context.setTestTicks(state.testIndex, state.ticks);
        if (!state.failureReported) {
            state.failureReported = true;
            ProgrammingFailureInfo info = new ProgrammingFailureInfo(
                    state.testIndex, testCase.input(), state.inputIndex,
                    testCase.expectedOutput(), state.actualBytes(), wrongIndex);
            context.setErrorLines(List.of(
                    Component.translatable("experiment.elements-plus.testfail.header"),
                    Component.translatable("experiment.elements-plus.programming.wrong_byte",
                            wrongIndex + 1),
                    Component.translatable("experiment.elements-plus.testfail.expected",
                            hexAt(testCase.expectedOutput(), wrongIndex)),
                    Component.translatable("experiment.elements-plus.testfail.actual",
                            hexAt(state.actualBytes(), wrongIndex)),
                    Component.translatable("experiment.elements-plus.testfail.click_for_data")));
            context.setSequentialFailureInfo(new SequentialFailureInfo(
                    state.testIndex, testCase.maxTicks(), Math.max(0, state.ticks - 1),
                    List.of(), null, info));
        }
        return advance(context, state, true);
    }

    private boolean advance(Context context, State state, boolean failed) {
        if (failed && !scoreSystem) {
            context.complete(false);
            return false;
        }
        if (state.testIndex + 1 >= cases.size()) {
            if (!scoreSystem) {
                context.complete(true);
            } else {
                int score = Math.round(100f * state.passed / cases.size());
                context.complete(score >= requiredScore);
            }
            context.experimentState = null;
            return false;
        }
        // 保留累计通过数和“第一个失败”标记，再为下一测例创建独立的 CPU 状态。
        State next = new State(state.testIndex + 1, state.program);
        next.passed = state.passed;
        next.failureReported = state.failureReported;
        context.testIndex = next.testIndex;
        context.experimentState = next;
        return true;
    }

    private static String hexAt(byte[] data, int index) {
        if (data == null || index < 0 || index >= data.length) {
            return "--";
        }
        return String.format("%02X", data[index] & 0xFF);
    }

    private static final class State {
        final int testIndex;
        final byte[] program;
        final Mcu8BasicComponentInstance cpu = new Mcu8BasicComponentInstance();
        int ticks;
        int inputIndex;
        int outputIndex;
        int passed;
        boolean failureReported;
        byte[] actual = new byte[0];

        State(int testIndex, byte[] program) {
            this.testIndex = testIndex;
            this.program = program.clone();
        }

        byte[] actualBytes() {
            return actual.clone();
        }
    }
}
