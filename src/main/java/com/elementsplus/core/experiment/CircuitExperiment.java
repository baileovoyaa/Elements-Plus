package com.elementsplus.core.experiment;

import com.elementsplus.ModDataComponents;
import com.elementsplus.core.circuit.BuiltinCircuitComponents;
import com.elementsplus.core.circuit.CircuitComponent;
import com.elementsplus.core.circuit.CircuitItemData;
import com.elementsplus.core.circuit.CircuitSimulator;
import com.elementsplus.core.circuit.storage.CircuitStorage;
import com.elementsplus.core.circuit.storage.StoredCircuit;
import com.elementsplus.core.circuit.component.Input8ComponentInstance;
import com.elementsplus.core.circuit.component.InputComponentInstance;
import com.elementsplus.core.circuit.component.Output8ComponentInstance;
import com.elementsplus.core.circuit.component.OutputComponentInstance;
import com.elementsplus.core.circuit.diagram.CircuitDiagram;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CircuitExperiment extends BaseExperiment {

    public static class PinValue {
        public int bitWidth;
        public byte[] values;

        public PinValue(byte[] values) {
            this.bitWidth = values.length;
            this.values = values;
        }

        public PinValue(byte value) {
            this.bitWidth = 1;
            this.values = new byte[]{value};
        }

        public PinValue(int value) {
            this((byte) value);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof PinValue other)) {
                return false;
            }
            return bitWidth == other.bitWidth && java.util.Arrays.equals(values, other.values);
        }

        @Override
        public int hashCode() {
            return 31 * bitWidth + java.util.Arrays.hashCode(values);
        }
    }

    /**
     * 组合逻辑电路实验：按顺序运行每个测例，一个 tick 只运行一个测例。
     * 每个测例在服务端进行一次组合求值（使用 CircuitSimulator）。
     */
    public interface CombinationalTestCase extends TestCase {
        Map<String, PinValue> getInputs();

        boolean judgeOutputs(Map<String, PinValue> outputs);
    }

    /**
     * 预期输出依赖世界种子的组合测例（例如 ROM 章节中随机生成的 16 字节表）。
     * 预检只读取占位输出以确定标签与位宽，真正判定时使用当前存档种子。
     */
    public interface SeededCombinationalTestCase extends CombinationalTestCase {
        Map<String, PinValue> getExpectedOutputs();

        Map<String, PinValue> getExpectedOutputs(long worldSeed);

        boolean judgeOutputs(Map<String, PinValue> outputs, long worldSeed);
    }

    public static class ConstantCombinationalTestCase implements CombinationalTestCase {
        public Map<String, PinValue> inputs;
        public Map<String, PinValue> expectedOutputs;
        public boolean ignoreAnalog;

        public ConstantCombinationalTestCase(Map<String, PinValue> inputs, Map<String, PinValue> expectedOutputs, boolean ignoreAnalog) {
            this.inputs = inputs;
            this.expectedOutputs = expectedOutputs;
            this.ignoreAnalog = ignoreAnalog;
        }

        public ConstantCombinationalTestCase(Map<String, PinValue> inputs, Map<String, PinValue> expectedOutputs) {
            this(inputs, expectedOutputs, false);
        }

        @Override
        public Map<String, PinValue> getInputs() {
            return inputs;
        }

        @Override
        public boolean judgeOutputs(Map<String, PinValue> outputs) {
            if (!ignoreAnalog) {
                return expectedOutputs.equals(outputs);
            } else {
                boolean result = true;
                // 只比较为0/非0
                for (Map.Entry<String, PinValue> entry : expectedOutputs.entrySet()) {
                    PinValue expected = entry.getValue();
                    PinValue actual = outputs.get(entry.getKey());
                    if (actual == null || expected.bitWidth != actual.bitWidth) {
                        result = false;
                        break;
                    }
                    for (int i = 0; i < expected.values.length; i++) {
                        if (expected.values[i] != 0 && actual.values[i] == 0 || expected.values[i] == 0 && actual.values[i] != 0) {
                            result = false;
                            break;
                        }
                    }
                }
                return result;
            }
        }
    }

    /**
     * 随机测例：每次运行时，对 8 位输入的每个"非 0 通道"随机赋一个 1~15 的信号强度。
     * 输入/预期输出的二进制值在构造时确定；随机化只在测例运行时进行，
     * 不会在静态初始化阶段生成随机数。
     */
    public static class RandomizedCombinationalTestCase implements CombinationalTestCase {
        public Map<String, PinValue> inputs;
        public Map<String, PinValue> expectedOutputs;

        public RandomizedCombinationalTestCase(Map<String, PinValue> inputs, Map<String, PinValue> expectedOutputs) {
            this.inputs = inputs;
            this.expectedOutputs = expectedOutputs;
        }

        @Override
        public Map<String, PinValue> getInputs() {
            return inputs;
        }

        @Override
        public boolean judgeOutputs(Map<String, PinValue> outputs) {
            return expectedOutputs.equals(outputs);
        }

        /**
         * 生成本次运行的输入：输入的每个非 0 通道随机为 1~15，0 通道保持 0。
         */
        public Map<String, PinValue> randomize(RandomSource random) {
            Map<String, PinValue> result = new LinkedHashMap<>();
            for (Map.Entry<String, PinValue> entry : inputs.entrySet()) {
                PinValue pinValue = entry.getValue();
                byte[] lanes = new byte[pinValue.values.length];
                for (int b = 0; b < pinValue.values.length; b++) {
                    int lane = pinValue.values[b] & 0xFF;
                    lanes[b] = (byte) (lane == 0 ? 0 : 1 + random.nextInt(15));
                }
                result.put(entry.getKey(), new PinValue(lanes));
            }
            return result;
        }
    }

    /** 时序测例的一帧：输入和该 tick 结束后的预期输出。 */
    public static class SequentialTick {
        public final Map<String, PinValue> inputs;
        public final Map<String, PinValue> expectedOutputs;

        public SequentialTick(Map<String, PinValue> inputs, Map<String, PinValue> expectedOutputs) {
            this.inputs = inputs;
            this.expectedOutputs = expectedOutputs;
        }
    }

    /** 需要连续多个时钟 tick 的测例。每个测例开始时会创建全新的模拟器，状态互不延续。 */
    public interface SequentialTestCase extends TestCase {
        List<SequentialTick> getTicks();

        default int getTickCount() {
            return getTicks().size();
        }

        /**
         * 是否只检查 expectedOutputs 中列出的输出标签。
         * 默认严格比较全部输出；极限 A+B 等协议测例可以忽略未声明的辅助输出。
         */
        default boolean compareOnlyExpectedOutputs() {
            return false;
        }
    }

    public static class ConstantSequentialTestCase implements SequentialTestCase {
        public final List<SequentialTick> ticks;
        private final boolean compareOnlyExpectedOutputs;

        public ConstantSequentialTestCase(List<SequentialTick> ticks) {
            this(ticks, false);
        }

        public ConstantSequentialTestCase(List<SequentialTick> ticks, boolean compareOnlyExpectedOutputs) {
            this.ticks = ticks;
            this.compareOnlyExpectedOutputs = compareOnlyExpectedOutputs;
        }

        @Override
        public List<SequentialTick> getTicks() {
            return ticks;
        }

        @Override
        public boolean compareOnlyExpectedOutputs() {
            return compareOnlyExpectedOutputs;
        }
    }

    /**
     * 架构测例的一帧。expectedPc 是该 tick 开始时应当执行的程序地址，
     * inputs 包含外部提供的 VI/DI/RO，expectedOutputs 包含 RI/VO/DO。
     */
    public static class ArchitectureTick {
        public final int expectedPc;
        public final Map<String, PinValue> inputs;
        public final Map<String, PinValue> expectedOutputs;

        public ArchitectureTick(int expectedPc, Map<String, PinValue> inputs,
                                Map<String, PinValue> expectedOutputs) {
            this.expectedPc = expectedPc;
            this.inputs = inputs;
            this.expectedOutputs = expectedOutputs;
        }
    }

    /**
     * 计算机架构测例：测试系统提供外部虚拟程序 ROM，每个 tick 执行一条指令。
     * 与普通时序测例不同，INS 由实际 PC 从 program 中取出后作为输入施加。
     */
    public interface ArchitectureTestCase extends TestCase {
        byte[] getProgram();

        List<ArchitectureTick> getTicks();

        default int getTickCount() {
            return getTicks().size();
        }

        /** 跳转测例的 PC 不单调，失败时复制整个程序而不是已执行的字节前缀。 */
        default boolean copyWholeProgram() {
            return false;
        }
    }

    public static class ConstantArchitectureTestCase implements ArchitectureTestCase {
        public final byte[] program;
        public final List<ArchitectureTick> ticks;
        private final boolean copyWholeProgram;

        public ConstantArchitectureTestCase(byte[] program, List<ArchitectureTick> ticks) {
            this(program, ticks, false);
        }

        public ConstantArchitectureTestCase(byte[] program, List<ArchitectureTick> ticks,
                                            boolean copyWholeProgram) {
            this.program = program.clone();
            this.ticks = List.copyOf(ticks);
            this.copyWholeProgram = copyWholeProgram;
        }

        @Override
        public byte[] getProgram() {
            return program.clone();
        }

        @Override
        public List<ArchitectureTick> getTicks() {
            return ticks;
        }

        @Override
        public boolean copyWholeProgram() {
            return copyWholeProgram;
        }
    }

    /**
     * 交互式测例：测试系统通过 VI/RI/DI 和 VO/RO/DO 与电路交互，
     * 依次送入 A、B 两个 8 位数据，再读取一次 8 位输出。
     */
    public interface InteractiveTestCase extends TestCase {
        int getInputA();

        int getInputB();

        int getExpectedOutput();

        default int getMaxTicks() {
            return 20;
        }

        /** 第一次输入前等待的 tick 数；测试系统可以按测例设置不同值。 */
        default int getFirstInputDelay() {
            return 2;
        }

        /** 两次输入之间的最小间隔 tick 数。 */
        default int getInputGap() {
            return 1;
        }
    }

    public static class ConstantInteractiveTestCase implements InteractiveTestCase {
        private final int inputA;
        private final int inputB;
        private final int expectedOutput;
        private final int maxTicks;
        private final int firstInputDelay;
        private final int inputGap;

        public ConstantInteractiveTestCase(int inputA, int inputB, int expectedOutput) {
            this(inputA, inputB, expectedOutput, 20, 2, 1);
        }

        public ConstantInteractiveTestCase(int inputA, int inputB, int expectedOutput, int maxTicks) {
            this(inputA, inputB, expectedOutput, maxTicks, 2, 1);
        }

        public ConstantInteractiveTestCase(int inputA, int inputB, int expectedOutput,
                                           int maxTicks, int firstInputDelay, int inputGap) {
            this.inputA = inputA;
            this.inputB = inputB;
            this.expectedOutput = expectedOutput;
            this.maxTicks = maxTicks;
            this.firstInputDelay = Math.max(0, firstInputDelay);
            this.inputGap = Math.max(0, inputGap);
        }

        @Override
        public int getInputA() {
            return inputA;
        }

        @Override
        public int getInputB() {
            return inputB;
        }

        @Override
        public int getExpectedOutput() {
            return expectedOutput;
        }

        @Override
        public int getMaxTicks() {
            return maxTicks;
        }

        @Override
        public int getFirstInputDelay() {
            return firstInputDelay;
        }

        @Override
        public int getInputGap() {
            return inputGap;
        }
    }

    private static final class SequentialState {
        final int testIndex;
        final int totalTicks;
        final CircuitDiagram diagram;
        final CircuitSimulator simulator;
        final List<SequentialFailureInfo.Row> rows = new ArrayList<>();
        int tickIndex;

        SequentialState(int testIndex, int totalTicks, CircuitDiagram diagram, CircuitSimulator simulator) {
            this.testIndex = testIndex;
            this.totalTicks = totalTicks;
            this.diagram = diagram;
            this.simulator = simulator;
        }
    }

    private static final class InteractiveState {
        final int testIndex;
        final CircuitDiagram diagram;
        final CircuitSimulator simulator;
        int inputIndex;
        int nextInputTick;
        int ticks;
        boolean outputRequested;
        final List<SequentialFailureInfo.Row> rows = new ArrayList<>();

        InteractiveState(int testIndex, CircuitDiagram diagram, CircuitSimulator simulator, int firstInputDelay) {
            this.testIndex = testIndex;
            this.diagram = diagram;
            this.simulator = simulator;
            this.nextInputTick = Math.max(0, firstInputDelay);
        }
    }

    private static final class ArchitectureState {
        final int testIndex;
        final int totalTicks;
        final byte[] program;
        final CircuitDiagram diagram;
        final CircuitSimulator simulator;
        final List<ArchitectureFailureInfo.Row> rows = new ArrayList<>();
        int tickIndex;

        ArchitectureState(int testIndex, int totalTicks, byte[] program,
                          CircuitDiagram diagram, CircuitSimulator simulator) {
            this.testIndex = testIndex;
            this.totalTicks = totalTicks;
            this.program = program;
            this.diagram = diagram;
            this.simulator = simulator;
        }
    }

    public List<TestCase> testCases;
    public Map<String, Integer> bitWidthsPrecheck;
    public CircuitComponent circuitComponent;

    private final Set<String> neededInputLabels = new LinkedHashSet<>();
    private final Set<String> neededOutputLabels = new LinkedHashSet<>();
    private final Map<String, Integer> neededOutputWidths = new LinkedHashMap<>();
    private boolean hasArchitectureTests;

    public CircuitExperiment(List<TestCase> testCases, CircuitComponent component) {
        this.testCases = testCases;
        this.bitWidthsPrecheck = new HashMap<>();
        for (TestCase testCase : testCases) {
            if (testCase instanceof CombinationalTestCase combinationalTestCase) {
                combinationalTestCase.getInputs().forEach((pinName, pinValue) -> {
                    int bitWidth = pinValue.bitWidth;
                    if (bitWidthsPrecheck.containsKey(pinName) && bitWidthsPrecheck.get(pinName) != bitWidth) {
                        throw new IllegalStateException("Bit width ambiguous for pin " + pinName);
                    }
                    bitWidthsPrecheck.put(pinName, bitWidth);
                    neededInputLabels.add(pinName);
                });
                Map<String, PinValue> expectedOutputs = testExpectedOutputs(combinationalTestCase);
                if (expectedOutputs != null) {
                    expectedOutputs.forEach((pinName, pinValue) -> {
                        neededOutputLabels.add(pinName);
                        int bitWidth = pinValue.bitWidth;
                        if (neededOutputWidths.containsKey(pinName) && neededOutputWidths.get(pinName) != bitWidth) {
                            throw new IllegalStateException("Bit width ambiguous for output pin " + pinName);
                        }
                        neededOutputWidths.put(pinName, bitWidth);
                    });
                }
            } else if (testCase instanceof SequentialTestCase sequentialTestCase) {
                for (SequentialTick tick : sequentialTestCase.getTicks()) {
                    tick.inputs.forEach((pinName, pinValue) -> {
                        int bitWidth = pinValue.bitWidth;
                        if (bitWidthsPrecheck.containsKey(pinName) && bitWidthsPrecheck.get(pinName) != bitWidth) {
                            throw new IllegalStateException("Bit width ambiguous for pin " + pinName);
                        }
                        bitWidthsPrecheck.put(pinName, bitWidth);
                        neededInputLabels.add(pinName);
                    });
                    tick.expectedOutputs.forEach((pinName, pinValue) -> {
                        neededOutputLabels.add(pinName);
                        int bitWidth = pinValue.bitWidth;
                        if (neededOutputWidths.containsKey(pinName) && neededOutputWidths.get(pinName) != bitWidth) {
                            throw new IllegalStateException("Bit width ambiguous for output pin " + pinName);
                        }
                        neededOutputWidths.put(pinName, bitWidth);
                    });
                }
            } else if (testCase instanceof InteractiveTestCase) {
                addNeededInput("VI", 1);
                addNeededInput("RO", 1);
                addNeededInput("DI", 8);
                addNeededOutput("VO", 1);
                addNeededOutput("RI", 1);
                addNeededOutput("DO", 8);
            } else if (testCase instanceof ArchitectureTestCase architectureTestCase) {
                hasArchitectureTests = true;
                addNeededInput("INS", 8);
                addNeededInput("VI", 1);
                addNeededInput("DI", 8);
                addNeededInput("RO", 1);
                addNeededOutput("PC", 8);
                addNeededOutput("RI", 1);
                addNeededOutput("VO", 1);
                addNeededOutput("DO", 8);
                if (architectureTestCase.getProgram().length > 256) {
                    throw new IllegalStateException("Architecture test program is longer than 256 bytes");
                }
            }
        }
        this.circuitComponent = component;
    }

    private void addNeededInput(String label, int bitWidth) {
        Integer previous = bitWidthsPrecheck.get(label);
        if (previous != null && previous != bitWidth) {
            throw new IllegalStateException("Bit width ambiguous for pin " + label);
        }
        bitWidthsPrecheck.put(label, bitWidth);
        neededInputLabels.add(label);
    }

    private void addNeededOutput(String label, int bitWidth) {
        Integer previous = neededOutputWidths.get(label);
        if (previous != null && previous != bitWidth) {
            throw new IllegalStateException("Bit width ambiguous for output pin " + label);
        }
        neededOutputWidths.put(label, bitWidth);
        neededOutputLabels.add(label);
    }

    private static Map<String, PinValue> testExpectedOutputs(CombinationalTestCase testCase) {
        if (testCase instanceof ConstantCombinationalTestCase constant) {
            return constant.expectedOutputs;
        }
        if (testCase instanceof RandomizedCombinationalTestCase randomized) {
            return randomized.expectedOutputs;
        }
        if (testCase instanceof SeededCombinationalTestCase seeded) {
            return seeded.getExpectedOutputs();
        }
        return null;
    }

    public void addTestCase(TestCase testCase) {
        testCases.add(testCase);
    }

    public CircuitExperiment(List<TestCase> testCases) {
        this(testCases, null);
    }

    @Override
    public int getTestCaseCount() {
        return testCases.size();
    }

    @Override
    public boolean tick(Context context) {
        int index = context.testIndex;
        if (index < 0 || index >= testCases.size()) {
            return false;
        }
        TestCase testCase = testCases.get(index);
        if (testCase instanceof ArchitectureTestCase architectureTestCase) {
            return tickArchitecture(context, index, architectureTestCase);
        }
        if (testCase instanceof InteractiveTestCase interactiveTestCase) {
            return tickInteractive(context, index, interactiveTestCase);
        }
        if (testCase instanceof SequentialTestCase sequentialTestCase) {
            return tickSequential(context, index, sequentialTestCase);
        }
        if (!(testCase instanceof CombinationalTestCase combinationalTestCase)) {
            // 未知测例类型：跳过。
            context.testIndex = index + 1;
            return true;
        }

        CircuitDiagram diagram = context.getItemStack() != null ? context.getItemStack().get(ModDataComponents.CIRCUIT_DIAGRAM) : null;
        if (diagram == null) {
            return false;
        }

        Map<String, PinValue> inputs = buildRuntimeInputs(context, combinationalTestCase);
        Map<String, PinValue> outputs = evaluate(inputs, diagram);
        long worldSeed = worldSeed(context);
        boolean pass = combinationalTestCase instanceof SeededCombinationalTestCase seeded
                ? seeded.judgeOutputs(outputs, worldSeed)
                : combinationalTestCase.judgeOutputs(outputs);
        context.reportTestResult(index, pass);

        if (!pass) {
            // 将失败测例的输入写回电路图（对应输入元件修改信号值，无关输入置0），便于之后在光刻机中检查失败逻辑
            writeBackFailedInputs(context, inputs);
            context.setErrorLines(buildFailureLines(combinationalTestCase, inputs, outputs, worldSeed));
            Map<String, PinValue> expectedOutputs = combinationalTestCase instanceof SeededCombinationalTestCase seeded
                    ? seeded.getExpectedOutputs(worldSeed)
                    : testExpectedOutputs(combinationalTestCase);
            context.setFailureInfo(ExperimentFailureInfo.of(inputs, expectedOutputs, outputs));
            context.complete(false);
            return false;
        }

        if (index == testCases.size() - 1) {
            context.complete(true);
            return true;
        }

        context.testIndex = index + 1;
        return true;
    }

    /**
     * 架构测例：测试系统根据实际 PC 从虚拟 ROM 取出 INS，再驱动电路一个时钟。
     *
     * <p>PC、INS 和执行阶段的 RI 在时钟步进前采样；VO/DO 是输出寄存器在步进后的状态。
     * 任一值不匹配时立即结束测例，并记录从开始到当前 tick 的完整数据。</p>
     */
    private boolean tickArchitecture(Context context, int index, ArchitectureTestCase testCase) {
        List<ArchitectureTick> ticks = testCase.getTicks();
        if (ticks.isEmpty()) {
            context.reportTestResult(index, true);
            context.experimentState = null;
            if (index == testCases.size() - 1) {
                context.complete(true);
            } else {
                context.testIndex = index + 1;
            }
            return true;
        }

        CircuitDiagram source = context.getItemStack() != null
                ? context.getItemStack().get(ModDataComponents.CIRCUIT_DIAGRAM)
                : null;
        if (source == null) {
            return false;
        }

        ArchitectureState state = context.experimentState instanceof ArchitectureState s && s.testIndex == index
                ? s : null;
        if (state == null) {
            CircuitDiagram diagram = source.copy();
            CircuitSimulator simulator = new CircuitSimulator();
            simulator.setDiagram(diagram);
            state = new ArchitectureState(index, ticks.size(), testCase.getProgram(), diagram, simulator);
            context.experimentState = state;
        }

        ArchitectureTick tick = ticks.get(state.tickIndex);
        state.simulator.evaluate();
        Map<String, PinValue> before = collectOutputs(state.diagram, state.simulator);
        int actualPc = pinDigital(before.get("PC"));
        int actualIns = programByte(state.program, actualPc);

        Map<String, PinValue> inputs = new LinkedHashMap<>();
        inputs.put("INS", new PinValue(busValue(actualIns)));
        inputs.putAll(tick.inputs);
        applyInputs(state.diagram, inputs);
        state.simulator.evaluate();

        Map<String, PinValue> during = collectOutputs(state.diagram, state.simulator);
        int actualRi = pinHigh(during.get("RI")) ? 1 : 0;

        state.simulator.step();
        Map<String, PinValue> after = collectOutputs(state.diagram, state.simulator);
        int actualVo = pinHigh(after.get("VO")) ? 1 : 0;
        int actualDo = pinDigital(after.get("DO"));

        int vi = pinHigh(tick.inputs.get("VI")) ? 1 : 0;
        int di = pinDigital(tick.inputs.get("DI"));
        int ro = pinHigh(tick.inputs.get("RO")) ? 1 : 0;
        int expectedPc = tick.expectedPc & 0xFF;
        int expectedIns = programByte(state.program, expectedPc);
        int expectedRi = pinHigh(tick.expectedOutputs.get("RI")) ? 1 : 0;
        int expectedVo = pinHigh(tick.expectedOutputs.get("VO")) ? 1 : 0;
        int expectedDo = pinDigital(tick.expectedOutputs.get("DO"));

        state.rows.add(new ArchitectureFailureInfo.Row(
                state.tickIndex, actualPc, actualIns, expectedPc, expectedIns,
                vi, actualRi, di, actualVo, ro, actualDo,
                expectedRi, expectedVo, expectedDo));

        boolean pass = actualPc == expectedPc
                && actualIns == expectedIns
                && actualRi == expectedRi
                && actualVo == expectedVo
                && actualDo == expectedDo;

        if (!pass) {
            context.reportTestResult(index, false);
            writeBackFailedInputs(context, inputs);
            context.setErrorLines(List.of(
                    Component.translatable("experiment.elements-plus.testfail.header"),
                    Component.translatable("experiment.elements-plus.testfail.click_for_data")));
            context.setSequentialFailureInfo(new SequentialFailureInfo(
                    index, state.totalTicks, state.tickIndex, List.of(),
                    new ArchitectureFailureInfo(state.program, List.copyOf(state.rows),
                            testCase.copyWholeProgram())));
            context.experimentState = null;
            context.complete(false);
            return false;
        }

        state.tickIndex++;
        if (state.tickIndex < state.totalTicks) {
            return true;
        }

        context.reportTestResult(index, true);
        context.experimentState = null;
        if (index == testCases.size() - 1) {
            context.complete(true);
            return true;
        }
        context.testIndex = index + 1;
        return true;
    }

    /**
     * 时序测例：每个服务器 tick 推进一次时钟。一个测例内部状态连续，
     * 切换到下一个测例时重新创建模拟器，确保测例之间互不影响。
     */
    private boolean tickSequential(Context context, int index, SequentialTestCase testCase) {
        List<SequentialTick> ticks = testCase.getTicks();
        if (ticks.isEmpty()) {
            context.reportTestResult(index, true);
            context.experimentState = null;
            if (index == testCases.size() - 1) {
                context.complete(true);
            } else {
                context.testIndex = index + 1;
            }
            return true;
        }

        CircuitDiagram source = context.getItemStack() != null
                ? context.getItemStack().get(ModDataComponents.CIRCUIT_DIAGRAM)
                : null;
        if (source == null) {
            return false;
        }

        SequentialState state = context.experimentState instanceof SequentialState s && s.testIndex == index
                ? s : null;
        if (state == null) {
            CircuitDiagram diagram = source.copy();
            CircuitSimulator simulator = new CircuitSimulator();
            simulator.setDiagram(diagram);
            state = new SequentialState(index, ticks.size(), diagram, simulator);
            context.experimentState = state;
        }

        SequentialTick tick = ticks.get(state.tickIndex);
        applyInputs(state.diagram, tick.inputs);
        state.simulator.step();
        Map<String, PinValue> outputs = collectOutputs(state.diagram, state.simulator);
        state.rows.add(new SequentialFailureInfo.Row(
                state.tickIndex,
                ExperimentFailureInfo.pins(tick.inputs),
                ExperimentFailureInfo.pins(tick.expectedOutputs),
                ExperimentFailureInfo.pins(outputs)));

        if (!sequentialOutputsMatch(testCase, tick.expectedOutputs, outputs)) {
            context.reportTestResult(index, false);
            writeBackFailedInputs(context, tick.inputs);
            context.setErrorLines(buildFailureLinesForTick(tick.inputs, tick.expectedOutputs, outputs));
            context.setFailureInfo(ExperimentFailureInfo.of(tick.inputs, tick.expectedOutputs, outputs));
            context.setSequentialFailureInfo(new SequentialFailureInfo(
                    index, state.totalTicks, state.tickIndex, List.copyOf(state.rows)));
            context.complete(false);
            return false;
        }

        state.tickIndex++;
        if (state.tickIndex < state.totalTicks) {
            return true;
        }

        context.reportTestResult(index, true);
        context.experimentState = null;
        if (index == testCases.size() - 1) {
            context.complete(true);
            return true;
        }
        context.testIndex = index + 1;
        return true;
    }

    /**
     * 交互式测例：按 VI/RI、VO/RO 同拍传输协议逐步驱动电路。
     *
     * <p>每个 tick 先设置外部输入，再读取当前组合输出；输入传输在当前时钟步进时生效。
     * 外部会先等待 VO 有效，再拉高 RO，随后在 VO 与 RO 同时有效的 tick 读取 DO。
     * 完整过程最多执行 {@link InteractiveTestCase#getMaxTicks()} 个 tick。</p>
     */
    private boolean tickInteractive(Context context, int index, InteractiveTestCase testCase) {
        CircuitDiagram source = context.getItemStack() != null
                ? context.getItemStack().get(ModDataComponents.CIRCUIT_DIAGRAM)
                : null;
        if (source == null) {
            return false;
        }

        InteractiveState state = context.experimentState instanceof InteractiveState s && s.testIndex == index
                ? s : null;
        if (state == null) {
            CircuitDiagram diagram = source.copy();
            CircuitSimulator simulator = new CircuitSimulator();
            simulator.setDiagram(diagram);
            state = new InteractiveState(index, diagram, simulator, testCase.getFirstInputDelay());
            context.experimentState = state;
        }

        boolean inputPhase = state.inputIndex < 2;
        boolean inputAvailable = inputPhase && state.ticks >= state.nextInputTick;
        int inputValue = inputAvailable ? switch (state.inputIndex) {
            case 0 -> testCase.getInputA();
            default -> testCase.getInputB();
        } : 0;
        Map<String, PinValue> inputs = new LinkedHashMap<>();
        inputs.put("VI", new PinValue(inputAvailable ? 15 : 0));
        inputs.put("RO", new PinValue(state.inputIndex >= 2 && state.outputRequested ? 15 : 0));
        inputs.put("DI", new PinValue(busValue(inputValue)));
        applyInputs(state.diagram, inputs);

        state.simulator.evaluate();
        Map<String, PinValue> outputs = collectOutputs(state.diagram, state.simulator);
        state.rows.add(new SequentialFailureInfo.Row(
                state.ticks,
                ExperimentFailureInfo.pins(inputs),
                List.of(),
                ExperimentFailureInfo.pins(outputs)));
        if (inputAvailable && outputHigh(outputs, "RI")) {
            state.inputIndex++;
            if (state.inputIndex < 2) {
                state.nextInputTick = state.ticks + testCase.getInputGap();
            }
        }

        boolean outputPhase = state.inputIndex >= 2;
        // 外部只在已经观察到 VO 后才拉高 RO，避免 RO 直接驱动 VO 的电路伪造输出。
        if (outputPhase && !state.outputRequested && outputHigh(outputs, "VO")) {
            state.outputRequested = true;
        } else if (outputPhase && state.outputRequested && outputHigh(outputs, "VO")) {
            state.ticks++;
            PinValue actual = outputs.get("DO");
            PinValue expected = new PinValue(busValue(testCase.getExpectedOutput()));
            if (!expected.equals(actual)) {
                context.reportTestResultState(index, 0);
                context.setFailureInfo(ExperimentFailureInfo.of(
                        Map.of(
                                "A", new PinValue(busValue(testCase.getInputA())),
                                "B", new PinValue(busValue(testCase.getInputB()))),
                        Map.of("DO", expected),
                        Map.of("DO", actual == null ? new PinValue(busValue(0)) : actual)));
                context.setSequentialFailureInfo(new SequentialFailureInfo(
                        index, state.rows.size(), state.ticks - 1, List.copyOf(state.rows)));
                context.experimentState = null;
                context.complete(false);
                return false;
            }

            context.setTestTicks(index, state.ticks);
            context.reportTestResultState(index, 1);
            context.experimentState = null;
            if (index == testCases.size() - 1) {
                context.complete(true);
            } else {
                context.testIndex = index + 1;
            }
            return true;
        }

        state.simulator.step();
        state.ticks++;
        if (state.ticks >= testCase.getMaxTicks()) {
            context.reportTestResultState(index, 2);
            context.setErrorLines(List.of(
                    Component.translatable("experiment.elements-plus.testfail.header"),
                    Component.translatable("experiment.elements-plus.testfail.tle", testCase.getMaxTicks()),
                    Component.translatable("experiment.elements-plus.testfail.tle_inputs",
                            testCase.getInputA(), testCase.getInputB()),
                    Component.translatable("experiment.elements-plus.testfail.click_for_data")));
            context.setSequentialFailureInfo(new SequentialFailureInfo(
                    index, state.rows.size(), state.ticks - 1, List.copyOf(state.rows)));
            context.experimentState = null;
            context.complete(false);
            return false;
        }

        context.experimentState = state;
        return true;
    }

    private static boolean outputHigh(Map<String, PinValue> outputs, String label) {
        PinValue value = outputs.get(label);
        return value != null && value.values.length > 0 && value.values[0] != 0;
    }

    private static byte[] busValue(int value) {
        byte[] lanes = new byte[8];
        for (int b = 0; b < 8; b++) {
            lanes[b] = (byte) (((value >> b) & 1) != 0 ? 15 : 0);
        }
        return lanes;
    }

    /** 8 位总线引脚值转成整数：每个通道非 0 即该位为 1，忽略具体信号强度。 */
    private static int pinDigital(PinValue value) {
        if (value == null) {
            return 0;
        }
        int result = 0;
        for (int b = 0; b < Math.min(8, value.values.length); b++) {
            if (value.values[b] != 0) {
                result |= 1 << b;
            }
        }
        return result;
    }

    /** 1 位引脚的逻辑值：任意非 0 信号强度都视为 1。 */
    private static boolean pinHigh(PinValue value) {
        if (value == null) {
            return false;
        }
        for (byte lane : value.values) {
            if (lane != 0) {
                return true;
            }
        }
        return false;
    }

    private static int programByte(byte[] program, int address) {
        int index = address & 0xFF;
        return index < program.length ? program[index] & 0xFF : 0;
    }

    private static boolean sequentialOutputsMatch(SequentialTestCase testCase,
                                                  Map<String, PinValue> expected,
                                                  Map<String, PinValue> actual) {
        if (!testCase.compareOnlyExpectedOutputs()) {
            return expected.equals(actual);
        }
        for (Map.Entry<String, PinValue> entry : expected.entrySet()) {
            PinValue value = actual.get(entry.getKey());
            if (value == null || !entry.getValue().equals(value)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 随机测例在运行时对输入做随机化（服务端随机源）；恒定测例直接返回固定输入。
     */
    private Map<String, PinValue> buildRuntimeInputs(Context context, CombinationalTestCase testCase) {
        if (testCase instanceof RandomizedCombinationalTestCase randomized) {
            RandomSource random = context.random;
            if (random == null) {
                random = RandomSource.create();
                if (context.blockEntity != null) {
                    var level = context.blockEntity.getLevel();
                    if (level != null) {
                        random = level.getRandom();
                    }
                }
            }
            return randomized.randomize(random);
        }
        return testCase.getInputs();
    }

    /**
     * 在副本上设置输入元件信号（与实验无关的输入全部置0）并求值，返回标签 -> 输出值。
     */
    private Map<String, PinValue> evaluate(Map<String, PinValue> inputs, CircuitDiagram source) {
        CircuitDiagram diagram = source.copy();
        applyInputs(diagram, inputs);

        CircuitSimulator simulator = new CircuitSimulator();
        simulator.setDiagram(diagram);
        return collectOutputs(diagram, simulator);
    }

    /** 从已经求值完成的模拟器读取带标签的输出引脚值。 */
    private static Map<String, PinValue> collectOutputs(CircuitDiagram diagram, CircuitSimulator simulator) {
        Map<String, PinValue> outputs = new LinkedHashMap<>();
        for (CircuitDiagram.Chunk chunk : diagram.chunks.values()) {
            for (CircuitDiagram.Component comp : chunk.components) {
                if (comp.component == BuiltinCircuitComponents.OUTPUT && comp.instance instanceof OutputComponentInstance out) {
                    String label = out.getLabel();
                    if (label != null && !label.isBlank()) {
                        int v = simulator.getComponentValue(comp.x, comp.y);
                        outputs.put(label, new PinValue(v));
                    }
                } else if (comp.component == BuiltinCircuitComponents.OUTPUT_8 && comp.instance instanceof Output8ComponentInstance out8) {
                    String label = out8.getLabel();
                    if (label != null && !label.isBlank()) {
                        int raw = simulator.getComponentValue(comp.x, comp.y);
                        byte[] lanes = new byte[8];
                        for (int b = 0; b < 8; b++) {
                            lanes[b] = (byte) ((raw >> (4 * b)) & 0xF);
                        }
                        outputs.put(label, new PinValue(lanes));
                    }
                }
            }
        }
        return outputs;
    }

    /**
     * 将测例输入写入电路图的输入元件（无关输入置0），并写回物品上的电路图数据。
     */
    private void writeBackFailedInputs(Context context, Map<String, PinValue> inputs) {
        ItemStack stack = context.getItemStack();
        if (stack == null || stack.isEmpty()) {
            return;
        }
        CircuitDiagram stored = stack.get(ModDataComponents.CIRCUIT_DIAGRAM);
        if (stored == null) {
            return;
        }
        CircuitDiagram failed = stored.copy();
        applyInputs(failed, inputs);
        stack.set(ModDataComponents.CIRCUIT_DIAGRAM, failed);
    }

    private static void applyInputs(CircuitDiagram diagram, Map<String, PinValue> inputs) {
        for (CircuitDiagram.Chunk chunk : diagram.chunks.values()) {
            for (CircuitDiagram.Component comp : chunk.components) {
                if (comp.component == BuiltinCircuitComponents.INPUT && comp.instance instanceof InputComponentInstance in) {
                    String label = in.getLabel();
                    if ("VCC".equals(label)) {
                        in.setSignal(15);
                        continue;
                    }
                    PinValue pinValue = inputs.get(label);
                    in.setSignal(pinValue != null && pinValue.values.length > 0 ? pinValue.values[0] : 0);
                } else if (comp.component == BuiltinCircuitComponents.INPUT_8 && comp.instance instanceof Input8ComponentInstance in8) {
                    PinValue pinValue = inputs.get(in8.getLabel());
                    if (pinValue != null && pinValue.values.length == 8) {
                        for (int b = 0; b < 8; b++) {
                            in8.setBit(b, pinValue.values[b] & 0xFF);
                        }
                    } else {
                        for (int b = 0; b < 8; b++) {
                            in8.setBit(b, 0);
                        }
                    }
                }
            }
        }
    }

    @Override
    public List<Component> preCheckReasons(Context context) {
        List<Component> reasons = new ArrayList<>();
        Map<String, Integer> inputLabels = new HashMap<>();
        Map<String, Integer> outputLabels = new HashMap<>();

        ItemStack stack = context.getItemStack();
        CircuitDiagram diagram = null;
        if (stack != null && !stack.isEmpty()) {
            if (CircuitItemData.isCompiled(stack) || stack.has(ModDataComponents.EQUIVALENT_COMPONENT)) {
                reasons.add(Component.translatable("experiment.elements-plus.precheck.has_equivalent"));
                return reasons;
            }
            diagram = CircuitItemData.diagram(stack);
        }
        if (diagram == null) {
            reasons.add(Component.translatable("experiment.elements-plus.precheck.not_diagram"));
            return reasons;
        }

        for (CircuitDiagram.Chunk chunk : diagram.chunks.values()) {
            for (CircuitDiagram.Component comp : chunk.components) {
                if (comp.component == BuiltinCircuitComponents.INPUT && comp.instance instanceof InputComponentInstance in) {
                    String label = in.getLabel();
                    if (label != null && !label.isBlank()) inputLabels.merge(label, 1, Integer::sum);
                } else if (comp.component == BuiltinCircuitComponents.OUTPUT && comp.instance instanceof OutputComponentInstance out) {
                    String label = out.getLabel();
                    if (label != null && !label.isBlank()) outputLabels.merge(label, 1, Integer::sum);
                } else if (comp.component == BuiltinCircuitComponents.INPUT_8 && comp.instance instanceof Input8ComponentInstance in8) {
                    String label = in8.getLabel();
                    if (label != null && !label.isBlank()) inputLabels.merge(label, 1, Integer::sum);
                } else if (comp.component == BuiltinCircuitComponents.OUTPUT_8 && comp.instance instanceof Output8ComponentInstance out8) {
                    String label = out8.getLabel();
                    if (label != null && !label.isBlank()) outputLabels.merge(label, 1, Integer::sum);
                }
            }
        }

        // 输入/输出标签：需要的必须存在（可以有多余）
        List<String> missingInputs = new ArrayList<>();
        for (String label : neededInputLabels) {
            if (!inputLabels.containsKey(label)) missingInputs.add(label);
        }
        if (!missingInputs.isEmpty()) {
            reasons.add(Component.translatable("experiment.elements-plus.precheck.missing_inputs", String.join(" ", missingInputs)));
        }
        List<String> missingOutputs = new ArrayList<>();
        for (String label : neededOutputLabels) {
            if (!outputLabels.containsKey(label)) missingOutputs.add(label);
        }
        if (!missingOutputs.isEmpty()) {
            reasons.add(Component.translatable("experiment.elements-plus.precheck.missing_outputs", String.join(" ", missingOutputs)));
        }

        // 测例涉及到的输入/输出标签都必须唯一对应一个元件
        Set<String> duplicates = new LinkedHashSet<>();
        for (String label : neededInputLabels) {
            int total = inputLabels.getOrDefault(label, 0) + outputLabels.getOrDefault(label, 0);
            if (total != 1 || inputLabels.getOrDefault(label, 0) != 1) duplicates.add(label);
        }
        for (String label : neededOutputLabels) {
            int total = inputLabels.getOrDefault(label, 0) + outputLabels.getOrDefault(label, 0);
            if (total != 1 || outputLabels.getOrDefault(label, 0) != 1) duplicates.add(label);
        }
        if (!duplicates.isEmpty()) {
            reasons.add(Component.translatable("experiment.elements-plus.precheck.duplicate_label", String.join(" ", duplicates)));
        }

        // 必要元件标签的位宽必须与测例要求一致（如 8 位总线标签只能用 INPUT_8/OUTPUT_8 承载）
        Set<String> widthMismatches = new LinkedHashSet<>();
        Map<String, Integer> inputWidths = new HashMap<>();
        Map<String, Integer> outputWidths = new HashMap<>();
        for (CircuitDiagram.Chunk chunk : diagram.chunks.values()) {
            for (CircuitDiagram.Component comp : chunk.components) {
                if (comp.component == BuiltinCircuitComponents.INPUT_8 && comp.instance instanceof Input8ComponentInstance in8) {
                    if (!in8.getLabel().isBlank()) inputWidths.put(in8.getLabel(), 8);
                } else if (comp.component == BuiltinCircuitComponents.OUTPUT_8 && comp.instance instanceof Output8ComponentInstance out8) {
                    if (!out8.getLabel().isBlank()) outputWidths.put(out8.getLabel(), 8);
                } else if (comp.component == BuiltinCircuitComponents.INPUT && comp.instance instanceof InputComponentInstance in) {
                    if (!in.getLabel().isBlank()) inputWidths.put(in.getLabel(), 1);
                } else if (comp.component == BuiltinCircuitComponents.OUTPUT && comp.instance instanceof OutputComponentInstance out) {
                    if (!out.getLabel().isBlank()) outputWidths.put(out.getLabel(), 1);
                }
            }
        }
        for (String label : neededInputLabels) {
            Integer needed = bitWidthsPrecheck.get(label);
            Integer present = inputWidths.get(label);
            if (needed != null && present != null && needed.intValue() != present.intValue()) {
                widthMismatches.add(label);
            }
        }
        for (Map.Entry<String, Integer> entry : neededOutputWidths.entrySet()) {
            Integer present = outputWidths.get(entry.getKey());
            if (present != null && entry.getValue().intValue() != present.intValue()) {
                widthMismatches.add(entry.getKey());
            }
        }
        if (!widthMismatches.isEmpty()) {
            reasons.add(Component.translatable("experiment.elements-plus.precheck.width_mismatch", String.join(" ", widthMismatches)));
        }

        // 不存在组合环路
        CircuitSimulator simulator = new CircuitSimulator();
        simulator.setDiagram(diagram.copy());
        if (simulator.hasCycle()) {
            reasons.add(Component.translatable("experiment.elements-plus.precheck.cycle"));
        } else if (hasArchitectureTests) {
            // 架构测试会把外部 ROM 的 PC -> INS 连接补进电路。
            // 如果电路内部已经存在 INS -> PC 的组合路径，就会形成组合环。
            CircuitDiagram.Component ins = findInput(diagram, "INS");
            CircuitDiagram.Component pc = findOutput(diagram, "PC");
            if (ins != null && pc != null
                    && simulator.hasCombinationalPath(ins.x, ins.y, pc.x, pc.y)) {
                reasons.add(Component.translatable("experiment.elements-plus.precheck.external_rom_cycle"));
            }
        }

        return reasons;
    }

    private static CircuitDiagram.Component findInput(CircuitDiagram diagram, String label) {
        for (CircuitDiagram.Chunk chunk : diagram.chunks.values()) {
            for (CircuitDiagram.Component comp : chunk.components) {
                if (comp.component == BuiltinCircuitComponents.INPUT
                        && comp.instance instanceof InputComponentInstance in
                        && label.equals(in.getLabel())) {
                    return comp;
                }
                if (comp.component == BuiltinCircuitComponents.INPUT_8
                        && comp.instance instanceof Input8ComponentInstance in8
                        && label.equals(in8.getLabel())) {
                    return comp;
                }
            }
        }
        return null;
    }

    private static CircuitDiagram.Component findOutput(CircuitDiagram diagram, String label) {
        for (CircuitDiagram.Chunk chunk : diagram.chunks.values()) {
            for (CircuitDiagram.Component comp : chunk.components) {
                if (comp.component == BuiltinCircuitComponents.OUTPUT
                        && comp.instance instanceof OutputComponentInstance out
                        && label.equals(out.getLabel())) {
                    return comp;
                }
                if (comp.component == BuiltinCircuitComponents.OUTPUT_8
                        && comp.instance instanceof Output8ComponentInstance out8
                        && label.equals(out8.getLabel())) {
                    return comp;
                }
            }
        }
        return null;
    }

    private List<Component> buildFailureLines(CombinationalTestCase testCase, Map<String, PinValue> inputs,
                                              Map<String, PinValue> outputs, long worldSeed) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("experiment.elements-plus.testfail.header"));
        if (testCase instanceof ConstantCombinationalTestCase constant) {
            lines.add(Component.translatable("experiment.elements-plus.testfail.input", formatPins(constant.inputs)));
            lines.add(Component.translatable("experiment.elements-plus.testfail.expected", formatPins(constant.expectedOutputs)));
        } else if (testCase instanceof RandomizedCombinationalTestCase randomized) {
            lines.add(Component.translatable("experiment.elements-plus.testfail.input", formatPins(inputs)));
            lines.add(Component.translatable("experiment.elements-plus.testfail.expected", formatPins(randomized.expectedOutputs)));
        } else if (testCase instanceof SeededCombinationalTestCase seeded) {
            lines.add(Component.translatable("experiment.elements-plus.testfail.input", formatPins(inputs)));
            lines.add(Component.translatable("experiment.elements-plus.testfail.expected",
                    formatPins(seeded.getExpectedOutputs(worldSeed))));
        }
        lines.add(Component.translatable("experiment.elements-plus.testfail.actual", formatPins(outputs)));
        return lines;
    }

    private static long worldSeed(Context context) {
        if (context.hasWorldSeed) {
            return context.worldSeed;
        }
        if (context.blockEntity != null
                && context.blockEntity.getLevel() instanceof ServerLevel serverLevel) {
            return serverLevel.getSeed();
        }
        return 0L;
    }

    private static List<Component> buildFailureLinesForTick(Map<String, PinValue> inputs,
                                                            Map<String, PinValue> expected,
                                                            Map<String, PinValue> actual) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("experiment.elements-plus.testfail.header"));
        lines.add(Component.translatable("experiment.elements-plus.testfail.input", formatPins(inputs)));
        lines.add(Component.translatable("experiment.elements-plus.testfail.expected", formatPins(expected)));
        lines.add(Component.translatable("experiment.elements-plus.testfail.actual", formatPins(actual)));
        return lines;
    }

    private static String formatPins(Map<String, PinValue> pins) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, PinValue> entry : pins.entrySet()) {
            PinValue pinValue = entry.getValue();
            if (pinValue == null) continue;
            if (!first) sb.append(", ");
            first = false;
            sb.append(entry.getKey()).append('=');
            if (pinValue.values.length == 1) {
                sb.append(pinValue.values[0]);
            } else {
                for (int i = 0; i < pinValue.values.length; i++) {
                    if (i > 0) sb.append(' ');
                    sb.append(pinValue.values[i]);
                }
            }
        }
        return sb.toString();
    }

    @Override
    public void onComplete(Context ctx, boolean success) {
        if (!success || circuitComponent == null) {
            return;
        }
        ItemStack stack = ctx.getItemStack();
        CircuitDiagram diagram = stack == null ? null : stack.get(ModDataComponents.CIRCUIT_DIAGRAM);
        if (diagram == null) {
            return;
        }
        java.util.UUID author = ctx.playerUuid != null ? ctx.playerUuid : new java.util.UUID(0L, 0L);
        String authorName = ctx.playerName == null || ctx.playerName.isBlank()
                ? author.toString() : ctx.playerName;
        StoredCircuit record = new StoredCircuit(java.util.UUID.randomUUID(), diagram.copy(),
                circuitComponent.getId(), null, author, authorName);
        CircuitStorage.get().put(record);
        ctx.setItemStack(itemStack -> {
            CircuitItemData.bindCompiled(itemStack, record);
            return itemStack;
        });
    }
}