package com.elementsplus.core.experiment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.elementsplus.core.experiment.CircuitExperiment.ConstantCombinationalTestCase;
import com.elementsplus.core.experiment.CircuitExperiment.PinValue;

public class BuiltinExperiments {
    public static final List<BaseExperiment> BUILTIN_EXPERIMENTS = new ArrayList<>();

    public static final CircuitExperiment AND_GATE = register("and_gate", new CircuitExperiment(List.of(
            new ConstantCombinationalTestCase(Map.of("A", new PinValue(0), "B", new PinValue(0)), Map.of("Y", new PinValue(0))),
            new ConstantCombinationalTestCase(Map.of("A", new PinValue(0), "B", new PinValue(1)), Map.of("Y", new PinValue(0))),
            new ConstantCombinationalTestCase(Map.of("A", new PinValue(1), "B", new PinValue(0)), Map.of("Y", new PinValue(0))),
            new ConstantCombinationalTestCase(Map.of("A", new PinValue(1), "B", new PinValue(1)), Map.of("Y", new PinValue(1)))
    )));

    public static final CircuitExperiment OR_GATE = register("or_gate", new CircuitExperiment(List.of(
            new ConstantCombinationalTestCase(Map.of("A", new PinValue(0), "B", new PinValue(0)), Map.of("Y", new PinValue(0))),
            new ConstantCombinationalTestCase(Map.of("A", new PinValue(0), "B", new PinValue(1)), Map.of("Y", new PinValue(1))),
            new ConstantCombinationalTestCase(Map.of("A", new PinValue(1), "B", new PinValue(0)), Map.of("Y", new PinValue(1))),
            new ConstantCombinationalTestCase(Map.of("A", new PinValue(1), "B", new PinValue(1)), Map.of("Y", new PinValue(1)))
    )));

    public static final CircuitExperiment NOT_GATE = register("not_gate", new CircuitExperiment(List.of(
            new ConstantCombinationalTestCase(Map.of("A", new PinValue(0)), Map.of("Y", new PinValue(1))),
            new ConstantCombinationalTestCase(Map.of("A", new PinValue(1)), Map.of("Y", new PinValue(0)))
    )));

    public static final CircuitExperiment NAND_GATE = register("nand_gate", new CircuitExperiment(List.of(
            new ConstantCombinationalTestCase(Map.of("A", new PinValue(0), "B", new PinValue(0)), Map.of("Y", new PinValue(1))),
            new ConstantCombinationalTestCase(Map.of("A", new PinValue(0), "B", new PinValue(1)), Map.of("Y", new PinValue(1))),
            new ConstantCombinationalTestCase(Map.of("A", new PinValue(1), "B", new PinValue(0)), Map.of("Y", new PinValue(1))),
            new ConstantCombinationalTestCase(Map.of("A", new PinValue(1), "B", new PinValue(1)), Map.of("Y", new PinValue(0)))
    )));

    public static final CircuitExperiment NOR_GATE = register("nor_gate", new CircuitExperiment(List.of(
            new ConstantCombinationalTestCase(Map.of("A", new PinValue(0), "B", new PinValue(0)), Map.of("Y", new PinValue(1))),
            new ConstantCombinationalTestCase(Map.of("A", new PinValue(0), "B", new PinValue(1)), Map.of("Y", new PinValue(0))),
            new ConstantCombinationalTestCase(Map.of("A", new PinValue(1), "B", new PinValue(0)), Map.of("Y", new PinValue(0))),
            new ConstantCombinationalTestCase(Map.of("A", new PinValue(1), "B", new PinValue(1)), Map.of("Y", new PinValue(0)))
    )));

    public static final CircuitExperiment XOR_GATE = register("xor_gate", new CircuitExperiment(List.of(
            new ConstantCombinationalTestCase(Map.of("A", new PinValue(0), "B", new PinValue(0)), Map.of("Y", new PinValue(0))),
            new ConstantCombinationalTestCase(Map.of("A", new PinValue(0), "B", new PinValue(1)), Map.of("Y", new PinValue(1))),
            new ConstantCombinationalTestCase(Map.of("A", new PinValue(1), "B", new PinValue(0)), Map.of("Y", new PinValue(1))),
            new ConstantCombinationalTestCase(Map.of("A", new PinValue(1), "B", new PinValue(1)), Map.of("Y", new PinValue(0)))
    )));

    public static final CircuitExperiment XNOR_GATE = register("xnor_gate", new CircuitExperiment(List.of(
            new ConstantCombinationalTestCase(Map.of("A", new PinValue(0), "B", new PinValue(0)), Map.of("Y", new PinValue(1))),
            new ConstantCombinationalTestCase(Map.of("A", new PinValue(0), "B", new PinValue(1)), Map.of("Y", new PinValue(0))),
            new ConstantCombinationalTestCase(Map.of("A", new PinValue(1), "B", new PinValue(0)), Map.of("Y", new PinValue(0))),
            new ConstantCombinationalTestCase(Map.of("A", new PinValue(1), "B", new PinValue(1)), Map.of("Y", new PinValue(1)))
    )));

    public static final CircuitExperiment HALF_ADDER = register("half_adder", new CircuitExperiment(List.of(
            new ConstantCombinationalTestCase(Map.of("A", new PinValue(0), "B", new PinValue(0)), Map.of("Y", new PinValue(0), "C", new PinValue(0))),
            new ConstantCombinationalTestCase(Map.of("A", new PinValue(0), "B", new PinValue(1)), Map.of("Y", new PinValue(1), "C", new PinValue(0))),
            new ConstantCombinationalTestCase(Map.of("A", new PinValue(1), "B", new PinValue(0)), Map.of("Y", new PinValue(1), "C", new PinValue(0))),
            new ConstantCombinationalTestCase(Map.of("A", new PinValue(1), "B", new PinValue(1)), Map.of("Y", new PinValue(0), "C", new PinValue(1)))
    )));

    public static final CircuitExperiment FULL_ADDER = register("full_adder", new CircuitExperiment(List.of(
            new ConstantCombinationalTestCase(Map.of("A", new PinValue(0), "B", new PinValue(0), "C", new PinValue(0)), Map.of("Y", new PinValue(0), "C", new PinValue(0))),
            new ConstantCombinationalTestCase(Map.of("A", new PinValue(0), "B", new PinValue(0), "C", new PinValue(1)), Map.of("Y", new PinValue(1), "C", new PinValue(0))),
            new ConstantCombinationalTestCase(Map.of("A", new PinValue(0), "B", new PinValue(1), "C", new PinValue(0)), Map.of("Y", new PinValue(1), "C", new PinValue(0))),
            new ConstantCombinationalTestCase(Map.of("A", new PinValue(0), "B", new PinValue(1), "C", new PinValue(1)), Map.of("Y", new PinValue(0), "C", new PinValue(1))),
            new ConstantCombinationalTestCase(Map.of("A", new PinValue(1), "B", new PinValue(0), "C", new PinValue(0)), Map.of("Y", new PinValue(1), "C", new PinValue(0))),
            new ConstantCombinationalTestCase(Map.of("A", new PinValue(1), "B", new PinValue(0), "C", new PinValue(1)), Map.of("Y", new PinValue(0), "C", new PinValue(1))),
            new ConstantCombinationalTestCase(Map.of("A", new PinValue(1), "B", new PinValue(1), "C", new PinValue(0)), Map.of("Y", new PinValue(0), "C", new PinValue(1))),
            new ConstantCombinationalTestCase(Map.of("A", new PinValue(1), "B", new PinValue(1), "C", new PinValue(1)), Map.of("Y", new PinValue(1), "C", new PinValue(1)))
    )));

    public static <T extends BaseExperiment> T register(String name, T experiment) {
        experiment.setName(name);
        BUILTIN_EXPERIMENTS.add(experiment);
        return experiment;
    }
}
