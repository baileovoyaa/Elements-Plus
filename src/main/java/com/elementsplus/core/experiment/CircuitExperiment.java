package com.elementsplus.core.experiment;

import com.elementsplus.ModDataComponents;
import com.elementsplus.core.circuit.BuiltinCircuitComponents;
import com.elementsplus.core.circuit.CircuitComponent;
import com.elementsplus.core.circuit.CircuitSimulator;
import com.elementsplus.core.circuit.component.Input8ComponentInstance;
import com.elementsplus.core.circuit.component.InputComponentInstance;
import com.elementsplus.core.circuit.component.Output8ComponentInstance;
import com.elementsplus.core.circuit.component.OutputComponentInstance;
import com.elementsplus.core.circuit.diagram.CircuitDiagram;
import net.minecraft.network.chat.Component;
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

    public List<TestCase> testCases;
    public Map<String, Integer> bitWidthsPrecheck;
    public CircuitComponent circuitComponent;

    private final Set<String> neededInputLabels = new LinkedHashSet<>();
    private final Set<String> neededOutputLabels = new LinkedHashSet<>();
    private final Map<String, Integer> neededOutputWidths = new LinkedHashMap<>();

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
            }
        }
        this.circuitComponent = component;
    }

    private static Map<String, PinValue> testExpectedOutputs(CombinationalTestCase testCase) {
        if (testCase instanceof ConstantCombinationalTestCase constant) {
            return constant.expectedOutputs;
        }
        if (testCase instanceof RandomizedCombinationalTestCase randomized) {
            return randomized.expectedOutputs;
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
        if (!(testCase instanceof CombinationalTestCase combinationalTestCase)) {
            // 未来时序测例：本轮不处理，保持进行
            context.testIndex = index + 1;
            return true;
        }

        CircuitDiagram diagram = context.getItemStack() != null ? context.getItemStack().get(ModDataComponents.CIRCUIT_DIAGRAM) : null;
        if (diagram == null) {
            return false;
        }

        Map<String, PinValue> inputs = buildRuntimeInputs(context, combinationalTestCase);
        Map<String, PinValue> outputs = evaluate(inputs, diagram);
        boolean pass = combinationalTestCase.judgeOutputs(outputs);
        context.reportTestResult(index, pass);

        if (!pass) {
            // 将失败测例的输入写回电路图（对应输入元件修改信号值，无关输入置0），便于之后在光刻机中检查失败逻辑
            writeBackFailedInputs(context, inputs);
            context.setErrorLines(buildFailureLines(combinationalTestCase, inputs, outputs));
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
     * 随机测例在运行时对输入做随机化（服务端随机源）；恒定测例直接返回固定输入。
     */
    private Map<String, PinValue> buildRuntimeInputs(Context context, CombinationalTestCase testCase) {
        if (testCase instanceof RandomizedCombinationalTestCase randomized) {
            RandomSource random = RandomSource.create();
            if (context.blockEntity != null) {
                var level = context.blockEntity.getLevel();
                if (level != null) {
                    random = level.getRandom();
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
            if (stack.has(ModDataComponents.EQUIVALENT_COMPONENT)) {
                reasons.add(Component.translatable("experiment.elements-plus.precheck.has_equivalent"));
                return reasons;
            }
            diagram = stack.get(ModDataComponents.CIRCUIT_DIAGRAM);
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
        }

        return reasons;
    }

    private List<Component> buildFailureLines(CombinationalTestCase testCase, Map<String, PinValue> inputs, Map<String, PinValue> outputs) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("experiment.elements-plus.testfail.header"));
        if (testCase instanceof ConstantCombinationalTestCase constant) {
            lines.add(Component.translatable("experiment.elements-plus.testfail.input", formatPins(constant.inputs)));
            lines.add(Component.translatable("experiment.elements-plus.testfail.expected", formatPins(constant.expectedOutputs)));
        } else if (testCase instanceof RandomizedCombinationalTestCase randomized) {
            lines.add(Component.translatable("experiment.elements-plus.testfail.input", formatPins(inputs)));
            lines.add(Component.translatable("experiment.elements-plus.testfail.expected", formatPins(randomized.expectedOutputs)));
        }
        lines.add(Component.translatable("experiment.elements-plus.testfail.actual", formatPins(outputs)));
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
        if (success && circuitComponent != null) {
            ctx.setItemStack(itemStack -> {
                itemStack.set(ModDataComponents.EQUIVALENT_COMPONENT, circuitComponent.getId());
                return itemStack;
            });
        }
    }
}