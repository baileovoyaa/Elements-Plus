package com.elementsplus.core.experiment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BuiltinExperiments {
    public static final List<BaseExperiment> BUILTIN_EXPERIMENTS = new ArrayList<>();

    public static final CircuitExperiment AND_GATE = register(new CircuitExperiment(List.of(
            new CircuitExperiment.CombinationalTestCase(Map.of("A", new CircuitExperiment.PinValue(0), "B", new CircuitExperiment.PinValue(0)), Map.of("Y", new CircuitExperiment.PinValue(0))),
            new CircuitExperiment.CombinationalTestCase(Map.of("A", new CircuitExperiment.PinValue(0), "B", new CircuitExperiment.PinValue(1)), Map.of("Y", new CircuitExperiment.PinValue(0))),
            new CircuitExperiment.CombinationalTestCase(Map.of("A", new CircuitExperiment.PinValue(1), "B", new CircuitExperiment.PinValue(0)), Map.of("Y", new CircuitExperiment.PinValue(0))),
            new CircuitExperiment.CombinationalTestCase(Map.of("A", new CircuitExperiment.PinValue(1), "B", new CircuitExperiment.PinValue(1)), Map.of("Y", new CircuitExperiment.PinValue(1)))
    )));

    public static CircuitExperiment register(CircuitExperiment experiment) {
        BUILTIN_EXPERIMENTS.add(experiment);
        return experiment;
    }
}
