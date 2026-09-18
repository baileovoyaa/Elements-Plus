package com.elementsplus.core.experiment;

import java.util.List;
import java.util.Map;

public class CircuitExperiment implements BaseExperiment {

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

    public interface TestCase {

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
    public String name;

    public CircuitExperiment(List<TestCase> testCases) {
        this.testCases = testCases;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean tick(Context context) {
        // TODO
        return false;
    }

    public boolean preCheck(Context context) {
        // 检查电路是否无错误（组合环路）

        // 检查是否有重复出现的输入/输出标签

        // 检查位宽不匹配
        for (Map.Entry<String, Integer> entry : bitWidthsPrecheck.entrySet()) {
            // TODO
            return false;
        }
        return true;
    }
}
