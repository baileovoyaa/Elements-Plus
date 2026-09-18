package com.elementsplus.core.experiment;

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

    public interface TestCase {

    }

    public static class CombinationalTestCase implements TestCase {
        public Map<String, PinValue> inputs;
        public Map<String, PinValue> expectedOutputs;

        public CombinationalTestCase(Map<String, PinValue> inputs, Map<String, PinValue> expectedOutputs) {
            this.inputs = inputs;
            this.expectedOutputs = expectedOutputs;
        }
    }

    public List<TestCase> testCases;

    public CircuitExperiment(List<TestCase> testCases) {
        this.testCases = testCases;
    }
}
