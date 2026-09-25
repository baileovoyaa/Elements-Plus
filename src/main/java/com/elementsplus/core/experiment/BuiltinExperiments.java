package com.elementsplus.core.experiment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.elementsplus.core.circuit.BuiltinCircuitComponents;
import com.elementsplus.core.experiment.CircuitExperiment.ConstantCombinationalTestCase;
import com.elementsplus.core.experiment.CircuitExperiment.PinValue;
import net.minecraft.resources.ResourceLocation;

public class BuiltinExperiments {
    public static final List<BaseExperiment> BUILTIN_EXPERIMENTS = new ArrayList<>();

    public static final CircuitExperiment AMPLIFIER = register("amplifier", null, new CircuitExperiment(IntStream.rangeClosed(0, 15)
            .mapToObj(i -> new ConstantCombinationalTestCase(
                    Map.of("A", new PinValue(i)),
                    Map.of("Y", new PinValue(i == 0 ? 0 : 15)),
                    false))
            .collect(Collectors.toList()), BuiltinCircuitComponents.AMPLIFIER));

    public static final CircuitExperiment AND_GATE = register("and_gate", BuiltinCircuitComponents.AND_GATE.getIcon(), new CircuitExperiment(cartesian(List.of(List.of(0, 1, 8, 15), List.of(0, 1, 8, 15))).stream().<TestCase>map(c -> {
        int a = c.get(0), b = c.get(1);
        return new ConstantCombinationalTestCase(
                Map.of("A", new PinValue(a), "B", new PinValue(b)),
                Map.of("Y", new PinValue((a > 0 && b > 0) ? 1 : 0)));
    }).toList(), BuiltinCircuitComponents.AND_GATE));

    public static final CircuitExperiment OR_GATE = register("or_gate", BuiltinCircuitComponents.OR_GATE.getIcon(), new CircuitExperiment(cartesian(List.of(List.of(0, 1, 8, 15), List.of(0, 1, 8, 15))).stream().<TestCase>map(c -> {
        int a = c.get(0), b = c.get(1);
        return new ConstantCombinationalTestCase(
                Map.of("A", new PinValue(a), "B", new PinValue(b)),
                Map.of("Y", new PinValue((a > 0 || b > 0) ? 1 : 0)));
    }).toList(), BuiltinCircuitComponents.OR_GATE));

    public static final CircuitExperiment NOT_GATE = register("not_gate", BuiltinCircuitComponents.NOT_GATE.getIcon(), new CircuitExperiment(IntStream.rangeClosed(0, 15)
            .mapToObj(i -> new ConstantCombinationalTestCase(
                    Map.of("A", new PinValue(i)),
                    Map.of("Y", new PinValue(i == 0 ? 15 : 0)),
                    false))
            .collect(Collectors.toList()), BuiltinCircuitComponents.NOT_GATE));

    public static final CircuitExperiment ANALOG_NOT = register("analog_not", BuiltinCircuitComponents.ANALOG_NOT.getIcon(), new CircuitExperiment(IntStream.rangeClosed(0, 15)
            .mapToObj(i -> new ConstantCombinationalTestCase(
                    Map.of("A", new PinValue(i)),
                    Map.of("Y", new PinValue(15 - i)),
                    false))
            .collect(Collectors.toList()), BuiltinCircuitComponents.ANALOG_NOT));

    public static final CircuitExperiment NAND_GATE = register("nand_gate", null, new CircuitExperiment(cartesian(List.of(List.of(0, 1, 8, 15), List.of(0, 1, 8, 15))).stream().<TestCase>map(c -> {
        int a = c.get(0), b = c.get(1);
        return new ConstantCombinationalTestCase(
                Map.of("A", new PinValue(a), "B", new PinValue(b)),
                Map.of("Y", new PinValue((a > 0 && b > 0) ? 0 : 1)));
    }).toList(), BuiltinCircuitComponents.NAND_GATE));

    public static final CircuitExperiment NOR_GATE = register("nor_gate", null, new CircuitExperiment(cartesian(List.of(List.of(0, 1, 8, 15), List.of(0, 1, 8, 15))).stream().<TestCase>map(c -> {
        int a = c.get(0), b = c.get(1);
        return new ConstantCombinationalTestCase(
                Map.of("A", new PinValue(a), "B", new PinValue(b)),
                Map.of("Y", new PinValue((a > 0 || b > 0) ? 0 : 1)));
    }).toList(), BuiltinCircuitComponents.NOR_GATE));

    public static final CircuitExperiment XOR_GATE = register("xor_gate", null, new CircuitExperiment(cartesian(List.of(List.of(0, 1, 8, 15), List.of(0, 1, 8, 15))).stream().<TestCase>map(c -> {
        int a = c.get(0), b = c.get(1);
        return new ConstantCombinationalTestCase(
                Map.of("A", new PinValue(a), "B", new PinValue(b)),
                Map.of("Y", new PinValue((a > 0) ^ (b > 0) ? 1 : 0)));
    }).toList(), BuiltinCircuitComponents.XOR_GATE));

    public static final CircuitExperiment XNOR_GATE = register("xnor_gate", null, new CircuitExperiment(cartesian(List.of(List.of(0, 1, 8, 15), List.of(0, 1, 8, 15))).stream().<TestCase>map(c -> {
        int a = c.get(0), b = c.get(1);
        return new ConstantCombinationalTestCase(
                Map.of("A", new PinValue(a), "B", new PinValue(b)),
                Map.of("Y", new PinValue((a > 0) ^ (b > 0) ? 0 : 1)));
    }).toList(), BuiltinCircuitComponents.XNOR_GATE));

    public static final CircuitExperiment HALF_ADDER = register("half_adder", null, new CircuitExperiment(cartesian(List.of(List.of(0, 1, 8, 15), List.of(0, 1, 8, 15))).stream().<TestCase>map(c -> {
        int a = c.get(0), b = c.get(1);
        return new ConstantCombinationalTestCase(
                Map.of("A", new PinValue(a), "B", new PinValue(b)),
                Map.of("S", new PinValue((a > 0) ^ (b > 0) ? 1 : 0), "C", new PinValue((a > 0 && b > 0) ? 1 : 0)));
    }).toList(), BuiltinCircuitComponents.HALF_ADDER));

    public static final CircuitExperiment FULL_ADDER = register("full_adder", null, new CircuitExperiment(cartesian(List.of(List.of(0, 1, 8, 15), List.of(0, 1, 8, 15), List.of(0, 1, 8, 15))).stream().<TestCase>map(c -> {
        int a = c.get(0), b = c.get(1), cIn = c.get(2);
        return new ConstantCombinationalTestCase(
                Map.of("A", new PinValue(a), "B", new PinValue(b), "C", new PinValue(cIn)),
                Map.of("S", new PinValue((a > 0) ^ (b > 0) ^ (cIn > 0) ? 1 : 0), "C", new PinValue((a > 0 && b > 0) || (b > 0 && cIn > 0) || (a > 0 && cIn > 0) ? 1 : 0)));
    }).toList(), BuiltinCircuitComponents.FULL_ADDER));

    public static <T extends BaseExperiment> T register(String name, ResourceLocation icon, T experiment) {
        experiment.setName(name);
        experiment.setIcon(icon);
        BUILTIN_EXPERIMENTS.add(experiment);
        return experiment;
    }

    public static BaseExperiment byId(String name) {
        for (BaseExperiment experiment : BUILTIN_EXPERIMENTS) {
            if (name.equals(experiment.getName())) {
                return experiment;
            }
        }
        return null;
    }

    private static <T> List<List<T>> cartesian(List<List<T>> sets) {
        List<List<T>> result = new ArrayList<>();
        result.add(new ArrayList<>());          // 从"空组合"开始
        for (List<T> set : sets) {
            List<List<T>> next = new ArrayList<>();
            for (List<T> combo : result) {
                for (T item : set) {
                    List<T> newCombo = new ArrayList<>(combo);
                    newCombo.add(item);
                    next.add(newCombo);
                }
            }
            result = next;
        }
        return result;
    }
}
