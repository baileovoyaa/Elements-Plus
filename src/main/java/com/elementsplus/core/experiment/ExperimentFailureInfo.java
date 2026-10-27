package com.elementsplus.core.experiment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 失败测例的结构化数值，供客户端按本地数字显示格式重新渲染。
 * 服务端仍然会发送普通 tooltip 文本，用于预检错误等非测例信息。
 */
public record ExperimentFailureInfo(List<Pin> inputs, List<Pin> expected, List<Pin> actual) {

    public record Pin(String label, int bitWidth, byte[] values) {
    }

    public static ExperimentFailureInfo of(Map<String, CircuitExperiment.PinValue> inputs,
                                           Map<String, CircuitExperiment.PinValue> expected,
                                           Map<String, CircuitExperiment.PinValue> actual) {
        return new ExperimentFailureInfo(pins(inputs), pins(expected), pins(actual));
    }

    public static List<Pin> pins(Map<String, CircuitExperiment.PinValue> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        List<Pin> result = new ArrayList<>();
        for (Map.Entry<String, CircuitExperiment.PinValue> entry : values.entrySet()) {
            CircuitExperiment.PinValue value = entry.getValue();
            if (value == null) {
                continue;
            }
            result.add(new Pin(entry.getKey(), value.bitWidth, value.values.clone()));
        }
        return result;
    }
}
