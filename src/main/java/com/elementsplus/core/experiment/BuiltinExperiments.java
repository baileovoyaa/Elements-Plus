package com.elementsplus.core.experiment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.elementsplus.core.circuit.BuiltinCircuitComponents;
import com.elementsplus.core.experiment.CircuitExperiment.ConstantCombinationalTestCase;
import com.elementsplus.core.experiment.CircuitExperiment.PinValue;
import net.minecraft.resources.ResourceLocation;

public class BuiltinExperiments {
    public static final List<BaseExperiment> BUILTIN_EXPERIMENTS = new ArrayList<>();

    public static final CircuitExperiment AND_GATE = register("and_gate", BuiltinCircuitComponents.AND_GATE.getIcon(), new CircuitExperiment(List.of(
            new ConstantCombinationalTestCase(Map.of("A", new PinValue(0), "B", new PinValue(0)), Map.of("Y", new PinValue(0))),
            new ConstantCombinationalTestCase(Map.of("A", new PinValue(0), "B", new PinValue(1)), Map.of("Y", new PinValue(0))),
            new ConstantCombinationalTestCase(Map.of("A", new PinValue(1), "B", new PinValue(0)), Map.of("Y", new PinValue(0))),
            new ConstantCombinationalTestCase(Map.of("A", new PinValue(1), "B", new PinValue(1)), Map.of("Y", new PinValue(1)))
    )));

    public static final CircuitExperiment OR_GATE = register("or_gate", BuiltinCircuitComponents.OR_GATE.getIcon(), new CircuitExperiment(List.of(
            new ConstantCombinationalTestCase(Map.of("A", new PinValue(0), "B", new PinValue(0)), Map.of("Y", new PinValue(0))),
            new ConstantCombinationalTestCase(Map.of("A", new PinValue(0), "B", new PinValue(1)), Map.of("Y", new PinValue(1))),
            new ConstantCombinationalTestCase(Map.of("A", new PinValue(1), "B", new PinValue(0)), Map.of("Y", new PinValue(1))),
            new ConstantCombinationalTestCase(Map.of("A", new PinValue(1), "B", new PinValue(1)), Map.of("Y", new PinValue(1)))
    )));

    public static final CircuitExperiment NOT_GATE = register("not_gate", BuiltinCircuitComponents.NOT_GATE.getIcon(), new CircuitExperiment(List.of(
            new ConstantCombinationalTestCase(Map.of("A", new PinValue(0)), Map.of("Y", new PinValue(1))),
            new ConstantCombinationalTestCase(Map.of("A", new PinValue(1)), Map.of("Y", new PinValue(0)))
    )));

    public static final CircuitExperiment NAND_GATE = register("nand_gate", null, new CircuitExperiment(List.of(
            new ConstantCombinationalTestCase(Map.of("A", new PinValue(0), "B", new PinValue(0)), Map.of("Y", new PinValue(1))),
            new ConstantCombinationalTestCase(Map.of("A", new PinValue(0), "B", new PinValue(1)), Map.of("Y", new PinValue(1))),
            new ConstantCombinationalTestCase(Map.of("A", new PinValue(1), "B", new PinValue(0)), Map.of("Y", new PinValue(1))),
            new ConstantCombinationalTestCase(Map.of("A", new PinValue(1), "B", new PinValue(1)), Map.of("Y", new PinValue(0)))
    )));

    public static final CircuitExperiment NOR_GATE = register("nor_gate", null, new CircuitExperiment(List.of(
            new ConstantCombinationalTestCase(Map.of("A", new PinValue(0), "B", new PinValue(0)), Map.of("Y", new PinValue(1))),
            new ConstantCombinationalTestCase(Map.of("A", new PinValue(0), "B", new PinValue(1)), Map.of("Y", new PinValue(0))),
            new ConstantCombinationalTestCase(Map.of("A", new PinValue(1), "B", new PinValue(0)), Map.of("Y", new PinValue(0))),
            new ConstantCombinationalTestCase(Map.of("A", new PinValue(1), "B", new PinValue(1)), Map.of("Y", new PinValue(0)))
    )));

    public static final CircuitExperiment XOR_GATE = register("xor_gate", null, new CircuitExperiment(List.of(
            new ConstantCombinationalTestCase(Map.of("A", new PinValue(0), "B", new PinValue(0)), Map.of("Y", new PinValue(0))),
            new ConstantCombinationalTestCase(Map.of("A", new PinValue(0), "B", new PinValue(1)), Map.of("Y", new PinValue(1))),
            new ConstantCombinationalTestCase(Map.of("A", new PinValue(1), "B", new PinValue(0)), Map.of("Y", new PinValue(1))),
            new ConstantCombinationalTestCase(Map.of("A", new PinValue(1), "B", new PinValue(1)), Map.of("Y", new PinValue(0)))
    )));

    public static final CircuitExperiment XNOR_GATE = register("xnor_gate", null, new CircuitExperiment(List.of(
            new ConstantCombinationalTestCase(Map.of("A", new PinValue(0), "B", new PinValue(0)), Map.of("Y", new PinValue(1))),
            new ConstantCombinationalTestCase(Map.of("A", new PinValue(0), "B", new PinValue(1)), Map.of("Y", new PinValue(0))),
            new ConstantCombinationalTestCase(Map.of("A", new PinValue(1), "B", new PinValue(0)), Map.of("Y", new PinValue(0))),
            new ConstantCombinationalTestCase(Map.of("A", new PinValue(1), "B", new PinValue(1)), Map.of("Y", new PinValue(1)))
    )));

    public static final CircuitExperiment HALF_ADDER = register("half_adder", null, new CircuitExperiment(List.of(
            new ConstantCombinationalTestCase(Map.of("A", new PinValue(0), "B", new PinValue(0)), Map.of("Y", new PinValue(0), "C", new PinValue(0))),
            new ConstantCombinationalTestCase(Map.of("A", new PinValue(0), "B", new PinValue(1)), Map.of("Y", new PinValue(1), "C", new PinValue(0))),
            new ConstantCombinationalTestCase(Map.of("A", new PinValue(1), "B", new PinValue(0)), Map.of("Y", new PinValue(1), "C", new PinValue(0))),
            new ConstantCombinationalTestCase(Map.of("A", new PinValue(1), "B", new PinValue(1)), Map.of("Y", new PinValue(0), "C", new PinValue(1)))
    )));

    public static final CircuitExperiment FULL_ADDER = register("full_adder", null, new CircuitExperiment(List.of(
            new ConstantCombinationalTestCase(Map.of("A", new PinValue(0), "B", new PinValue(0), "C", new PinValue(0)), Map.of("Y", new PinValue(0), "C", new PinValue(0))),
            new ConstantCombinationalTestCase(Map.of("A", new PinValue(0), "B", new PinValue(0), "C", new PinValue(1)), Map.of("Y", new PinValue(1), "C", new PinValue(0))),
            new ConstantCombinationalTestCase(Map.of("A", new PinValue(0), "B", new PinValue(1), "C", new PinValue(0)), Map.of("Y", new PinValue(1), "C", new PinValue(0))),
            new ConstantCombinationalTestCase(Map.of("A", new PinValue(0), "B", new PinValue(1), "C", new PinValue(1)), Map.of("Y", new PinValue(0), "C", new PinValue(1))),
            new ConstantCombinationalTestCase(Map.of("A", new PinValue(1), "B", new PinValue(0), "C", new PinValue(0)), Map.of("Y", new PinValue(1), "C", new PinValue(0))),
            new ConstantCombinationalTestCase(Map.of("A", new PinValue(1), "B", new PinValue(0), "C", new PinValue(1)), Map.of("Y", new PinValue(0), "C", new PinValue(1))),
            new ConstantCombinationalTestCase(Map.of("A", new PinValue(1), "B", new PinValue(1), "C", new PinValue(0)), Map.of("Y", new PinValue(0), "C", new PinValue(1))),
            new ConstantCombinationalTestCase(Map.of("A", new PinValue(1), "B", new PinValue(1), "C", new PinValue(1)), Map.of("Y", new PinValue(1), "C", new PinValue(1)))
    )));

    public static <T extends BaseExperiment> T register(String name, ResourceLocation icon, T experiment) {
        experiment.setName(name);
        experiment.setIcon(icon);
        BUILTIN_EXPERIMENTS.add(experiment);
        return experiment;
    }
}
