package com.elementsplus.core.experiment;

import com.elementsplus.ModDataComponents;
import com.elementsplus.core.circuit.CircuitComponent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    }

    public interface CombinationalTestCase extends TestCase {
        public Map<String, PinValue> getInputs();

        public boolean judgeOutputs(Map<String, PinValue> outputs);
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
                    if (expected.bitWidth != actual.bitWidth) {
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

    public CircuitExperiment(List<TestCase> testCases, CircuitComponent component) {
        this.testCases = testCases;
        this.bitWidthsPrecheck = new HashMap<>();
        for (TestCase testCase : testCases) {
            if (testCase instanceof CombinationalTestCase combinationalTestCase) {
                for (Map.Entry<String, PinValue> entry : combinationalTestCase.getInputs().entrySet()) {
                    String pinName = entry.getKey();
                    int bitWidth = entry.getValue().bitWidth;
                    if (bitWidthsPrecheck.containsKey(pinName) && bitWidthsPrecheck.get(pinName) != bitWidth) {
                        throw new IllegalStateException("Bit width ambiguous for pin " + pinName);
                    }
                    bitWidthsPrecheck.put(pinName, bitWidth);
                }
            }
        }
        this.circuitComponent = component;
    }

    public CircuitExperiment(List<TestCase> testCases) {
        this(testCases, null);
    }

    @Override
    public boolean tick(Context context) {
        // TODO
        return false;
    }

    public boolean preCheck(Context context) {
        // 检查电路是否无错误（组合环路）

        // 检查是否有重复/缺少的输入/输出标签

        // 检查位宽不匹配
        for (Map.Entry<String, Integer> entry : bitWidthsPrecheck.entrySet()) {
            // TODO
            return false;
        }
        return true;
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
