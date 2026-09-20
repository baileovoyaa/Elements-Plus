package com.elementsplus.core.experiment;

import com.elementsplus.ModDataComponents;
import com.elementsplus.core.circuit.BuiltinCircuitComponents;
import com.elementsplus.core.circuit.CircuitComponent;
import com.elementsplus.core.circuit.CircuitSimulator;
import com.elementsplus.core.circuit.component.InputComponentInstance;
import com.elementsplus.core.circuit.component.OutputComponentInstance;
import com.elementsplus.core.circuit.diagram.CircuitDiagram;
import net.minecraft.network.chat.Component;
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

        public PinValue(int bitWidth, byte[] values) {
            this.bitWidth = bitWidth;
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
        public boolean ignoreAnalog = true;

        public ConstantCombinationalTestCase(Map<String, PinValue> inputs, Map<String, PinValue> expectedOutputs, boolean ignoreAnalog) {
            this.inputs = inputs;
            this.expectedOutputs = expectedOutputs;
            this.ignoreAnalog = ignoreAnalog;
        }

        public ConstantCombinationalTestCase(Map<String, PinValue> inputs, Map<String, PinValue> expectedOutputs) {
            this(inputs, expectedOutputs, true);
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

    public List<TestCase> testCases;
    public Map<String, Integer> bitWidthsPrecheck;
    public CircuitComponent circuitComponent;

    private final Set<String> neededInputLabels = new LinkedHashSet<>();
    private final Set<String> neededOutputLabels = new LinkedHashSet<>();

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
                if (combinationalTestCase instanceof ConstantCombinationalTestCase constant) {
                    neededOutputLabels.addAll(constant.expectedOutputs.keySet());
                }
            }
        }
        this.circuitComponent = component;
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

        Map<String, PinValue> outputs = evaluate(combinationalTestCase.getInputs(), diagram);
        boolean pass = combinationalTestCase.judgeOutputs(outputs);
        context.reportTestResult(index, pass);

        if (!pass) {
            context.setErrorLines(buildFailureLines(combinationalTestCase, outputs));
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
     * 在副本上设置输入元件信号（与实验无关的输入全部置0）并求值，返回标签 -> 输出值。
     */
    private Map<String, PinValue> evaluate(Map<String, PinValue> inputs, CircuitDiagram source) {
        CircuitDiagram diagram = source.copy();
        for (CircuitDiagram.Chunk chunk : diagram.chunks.values()) {
            for (CircuitDiagram.Component comp : chunk.components) {
                if (comp.component == BuiltinCircuitComponents.INPUT && comp.instance instanceof InputComponentInstance in) {
                    String label = in.getLabel();
                    PinValue pinValue = inputs.get(label);
                    in.setSignal(pinValue != null && pinValue.values.length > 0 ? pinValue.values[0] : 0);
                }
            }
        }

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
                }
            }
        }
        return outputs;
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

        // 不存在组合环路
        CircuitSimulator simulator = new CircuitSimulator();
        simulator.setDiagram(diagram.copy());
        if (simulator.hasCycle()) {
            reasons.add(Component.translatable("experiment.elements-plus.precheck.cycle"));
        }

        return reasons;
    }

    private List<Component> buildFailureLines(CombinationalTestCase testCase, Map<String, PinValue> outputs) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("experiment.elements-plus.testfail.header"));
        if (testCase instanceof ConstantCombinationalTestCase constant) {
            lines.add(Component.translatable("experiment.elements-plus.testfail.input", formatPins(constant.inputs)));
            lines.add(Component.translatable("experiment.elements-plus.testfail.expected", formatPins(constant.expectedOutputs)));
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