package com.elementsplus.core.experiment;

import java.util.List;

/**
 * 时序测例失败的完整逐 tick 数据。只发送到失败 tick 为止。
 * failedTick 是失败 tick 的 0 基下标；它前面的行都已经通过。
 */
public record SequentialFailureInfo(int testIndex, int totalTicks, int failedTick, List<Row> rows) {

    public record Row(int tick,
                      List<ExperimentFailureInfo.Pin> inputs,
                      List<ExperimentFailureInfo.Pin> expected,
                      List<ExperimentFailureInfo.Pin> actual) {
    }
}
