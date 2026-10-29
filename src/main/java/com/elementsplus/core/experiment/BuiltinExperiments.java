package com.elementsplus.core.experiment;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntBinaryOperator;
import java.util.function.IntPredicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.elementsplus.ElementsPlus;
import com.elementsplus.core.circuit.BuiltinCircuitComponents;
import com.elementsplus.core.experiment.CircuitExperiment.ConstantCombinationalTestCase;
import com.elementsplus.core.experiment.CircuitExperiment.ConstantSequentialTestCase;
import com.elementsplus.core.experiment.CircuitExperiment.PinValue;
import com.elementsplus.core.experiment.CircuitExperiment.RandomizedCombinationalTestCase;
import com.elementsplus.core.experiment.CircuitExperiment.SequentialTick;
import net.minecraft.resources.ResourceLocation;

public class BuiltinExperiments {
    public static final List<BaseExperiment> BUILTIN_EXPERIMENTS = new ArrayList<>();

    public static final CircuitExperiment AMPLIFIER = register("amplifier", BuiltinCircuitComponents.AMPLIFIER.getIcon(), new CircuitExperiment(IntStream.rangeClosed(0, 15)
            .mapToObj(i -> new ConstantCombinationalTestCase(
                    Map.of("A", new PinValue(i)),
                    Map.of("Y", new PinValue(i == 0 ? 0 : 15)),
                    false))
            .collect(Collectors.toList()), BuiltinCircuitComponents.AMPLIFIER));

    public static final CircuitExperiment AND_GATE = register("and_gate", BuiltinCircuitComponents.AND_GATE.getIcon(), new CircuitExperiment(cartesian(List.of(List.of(0, 1, 8, 15), List.of(0, 1, 8, 15))).stream().<TestCase>map(c -> {
        int a = c.get(0), b = c.get(1);
        return new ConstantCombinationalTestCase(
                Map.of("A", new PinValue(a), "B", new PinValue(b)),
                Map.of("Y", new PinValue((a > 0 && b > 0) ? 15 : 0)));
    }).toList(), BuiltinCircuitComponents.AND_GATE));

    public static final CircuitExperiment OR_GATE = register("or_gate", BuiltinCircuitComponents.OR_GATE.getIcon(), new CircuitExperiment(cartesian(List.of(List.of(0, 1, 8, 15), List.of(0, 1, 8, 15))).stream().<TestCase>map(c -> {
        int a = c.get(0), b = c.get(1);
        return new ConstantCombinationalTestCase(
                Map.of("A", new PinValue(a), "B", new PinValue(b)),
                Map.of("Y", new PinValue((a > 0 || b > 0) ? 15 : 0)));
    }).toList(), BuiltinCircuitComponents.OR_GATE));

    public static final CircuitExperiment NOT_GATE = register("not_gate", BuiltinCircuitComponents.NOT_GATE.getIcon(), new CircuitExperiment(IntStream.rangeClosed(0, 15)
            .mapToObj(i -> new ConstantCombinationalTestCase(
                    Map.of("A", new PinValue(i)),
                    Map.of("Y", new PinValue(i == 0 ? 15 : 0)),
                    false))
            .collect(Collectors.toList()), BuiltinCircuitComponents.NOT_GATE));

    public static final CircuitExperiment AND_GATE_SC = register("and_gate_sc", BuiltinCircuitComponents.AND_GATE_SC.getIcon(), new CircuitExperiment(cartesian(List.of(List.of(0, 1, 8, 15), List.of(0, 1, 8, 15))).stream().<TestCase>map(c -> {
        int a = c.get(0), b = c.get(1);
        return new ConstantCombinationalTestCase(
                Map.of("A", new PinValue(a), "B", new PinValue(b)),
                Map.of("Y", new PinValue(a > 0 ? b : a)));
    }).toList(), BuiltinCircuitComponents.AND_GATE_SC));

    public static final CircuitExperiment OR_GATE_SC = register("or_gate_sc", BuiltinCircuitComponents.OR_GATE_SC.getIcon(), new CircuitExperiment(cartesian(List.of(List.of(0, 1, 8, 15), List.of(0, 1, 8, 15))).stream().<TestCase>map(c -> {
        int a = c.get(0), b = c.get(1);
        return new ConstantCombinationalTestCase(
                Map.of("A", new PinValue(a), "B", new PinValue(b)),
                Map.of("Y", new PinValue(a == 0 ? b : a)));
    }).toList(), BuiltinCircuitComponents.OR_GATE_SC));

    public static final CircuitExperiment ANALOG_NOT = register("analog_not", BuiltinCircuitComponents.ANALOG_NOT.getIcon(), new CircuitExperiment(IntStream.rangeClosed(0, 15)
            .mapToObj(i -> new ConstantCombinationalTestCase(
                    Map.of("A", new PinValue(i)),
                    Map.of("Y", new PinValue(15 - i)),
                    false))
            .collect(Collectors.toList()), BuiltinCircuitComponents.ANALOG_NOT));

    public static final CircuitExperiment NAND_GATE = register("nand_gate", BuiltinCircuitComponents.NAND_GATE.getIcon(), new CircuitExperiment(cartesian(List.of(List.of(0, 1, 8, 15), List.of(0, 1, 8, 15))).stream().<TestCase>map(c -> {
        int a = c.get(0), b = c.get(1);
        return new ConstantCombinationalTestCase(
                Map.of("A", new PinValue(a), "B", new PinValue(b)),
                Map.of("Y", new PinValue((a > 0 && b > 0) ? 0 : 15)));
    }).toList(), BuiltinCircuitComponents.NAND_GATE));

    public static final CircuitExperiment NOR_GATE = register("nor_gate", BuiltinCircuitComponents.NOR_GATE.getIcon(), new CircuitExperiment(cartesian(List.of(List.of(0, 1, 8, 15), List.of(0, 1, 8, 15))).stream().<TestCase>map(c -> {
        int a = c.get(0), b = c.get(1);
        return new ConstantCombinationalTestCase(
                Map.of("A", new PinValue(a), "B", new PinValue(b)),
                Map.of("Y", new PinValue((a > 0 || b > 0) ? 0 : 15)));
    }).toList(), BuiltinCircuitComponents.NOR_GATE));

    public static final CircuitExperiment XOR_GATE = register("xor_gate", BuiltinCircuitComponents.XOR_GATE.getIcon(), new CircuitExperiment(cartesian(List.of(List.of(0, 1, 8, 15), List.of(0, 1, 8, 15))).stream().<TestCase>map(c -> {
        int a = c.get(0), b = c.get(1);
        return new ConstantCombinationalTestCase(
                Map.of("A", new PinValue(a), "B", new PinValue(b)),
                Map.of("Y", new PinValue((a > 0) ^ (b > 0) ? 15 : 0)));
    }).toList(), BuiltinCircuitComponents.XOR_GATE));

    public static final CircuitExperiment XNOR_GATE = register("xnor_gate", BuiltinCircuitComponents.XNOR_GATE.getIcon(), new CircuitExperiment(cartesian(List.of(List.of(0, 1, 8, 15), List.of(0, 1, 8, 15))).stream().<TestCase>map(c -> {
        int a = c.get(0), b = c.get(1);
        return new ConstantCombinationalTestCase(
                Map.of("A", new PinValue(a), "B", new PinValue(b)),
                Map.of("Y", new PinValue((a > 0) ^ (b > 0) ? 0 : 15)));
    }).toList(), BuiltinCircuitComponents.XNOR_GATE));

    public static final CircuitExperiment OR_GATE_3 = register("or_gate_3", BuiltinCircuitComponents.OR_GATE_3.getIcon(), new CircuitExperiment(cartesian(List.of(List.of(0, 1, 8, 15), List.of(0, 1, 8, 15), List.of(0, 1, 8, 15))).stream().<TestCase>map(inputs -> {
        int a = inputs.get(0), b = inputs.get(1), c = inputs.get(2);
        return new ConstantCombinationalTestCase(
                Map.of("A", new PinValue(a), "B", new PinValue(b), "C", new PinValue(c)),
                Map.of("Y", new PinValue((a > 0 || b > 0 || c > 0) ? 15 : 0)));
    }).toList(), BuiltinCircuitComponents.OR_GATE_3));

    public static final CircuitExperiment AND_GATE_3 = register("and_gate_3", BuiltinCircuitComponents.AND_GATE_3.getIcon(), new CircuitExperiment(cartesian(List.of(List.of(0, 1, 8, 15), List.of(0, 1, 8, 15), List.of(0, 1, 8, 15))).stream().<TestCase>map(inputs -> {
        int a = inputs.get(0), b = inputs.get(1), c = inputs.get(2);
        return new ConstantCombinationalTestCase(
                Map.of("A", new PinValue(a), "B", new PinValue(b), "C", new PinValue(c)),
                Map.of("Y", new PinValue((a > 0 && b > 0 && c > 0) ? 15 : 0)));
    }).toList(), BuiltinCircuitComponents.AND_GATE_3));

    public static final CircuitExperiment XOR_GATE_3 = register("xor_gate_3", BuiltinCircuitComponents.XOR_GATE_3.getIcon(), new CircuitExperiment(cartesian(List.of(List.of(0, 1, 8, 15), List.of(0, 1, 8, 15), List.of(0, 1, 8, 15))).stream().<TestCase>map(inputs -> {
        int a = inputs.get(0), b = inputs.get(1), c = inputs.get(2);
        return new ConstantCombinationalTestCase(
                Map.of("A", new PinValue(a), "B", new PinValue(b), "C", new PinValue(c)),
                Map.of("Y", new PinValue((a > 0) ^ (b > 0) ^ (c > 0) ? 15 : 0)));
    }).toList(), BuiltinCircuitComponents.XOR_GATE_3));

    public static final CircuitExperiment XNOR_GATE_3 = register("xnor_gate_3", BuiltinCircuitComponents.XNOR_GATE_3.getIcon(), new CircuitExperiment(cartesian(List.of(List.of(0, 1, 8, 15), List.of(0, 1, 8, 15), List.of(0, 1, 8, 15))).stream().<TestCase>map(inputs -> {
        int a = inputs.get(0), b = inputs.get(1), c = inputs.get(2);
        return new ConstantCombinationalTestCase(
                Map.of("A", new PinValue(a), "B", new PinValue(b), "C", new PinValue(c)),
                Map.of("Y", new PinValue((a > 0) ^ (b > 0) ^ (c > 0) ? 0 : 15)));
    }).toList(), BuiltinCircuitComponents.XNOR_GATE_3));

    public static final CircuitExperiment HALF_ADDER = register("half_adder", BuiltinCircuitComponents.HALF_ADDER.getIcon(), new CircuitExperiment(cartesian(List.of(List.of(0, 1, 8, 15), List.of(0, 1, 8, 15))).stream().<TestCase>map(c -> {
        int a = c.get(0), b = c.get(1);
        return new ConstantCombinationalTestCase(
                Map.of("A", new PinValue(a), "B", new PinValue(b)),
                Map.of("S", new PinValue((a > 0) ^ (b > 0) ? 15 : 0), "C", new PinValue((a > 0 && b > 0) ? 15 : 0)));
    }).toList(), BuiltinCircuitComponents.HALF_ADDER));

    public static final CircuitExperiment FULL_ADDER = register("full_adder", BuiltinCircuitComponents.FULL_ADDER.getIcon(), new CircuitExperiment(cartesian(List.of(List.of(0, 1, 8, 15), List.of(0, 1, 8, 15), List.of(0, 1, 8, 15))).stream().<TestCase>map(c -> {
        int a = c.get(0), b = c.get(1), cIn = c.get(2);
        return new ConstantCombinationalTestCase(
                Map.of("A", new PinValue(a), "B", new PinValue(b), "CI", new PinValue(cIn)),
                Map.of("S", new PinValue((a > 0) ^ (b > 0) ^ (cIn > 0) ? 15 : 0), "CO", new PinValue((a > 0 && b > 0) || (b > 0 && cIn > 0) || (a > 0 && cIn > 0) ? 15 : 0)));
    }).toList(), BuiltinCircuitComponents.FULL_ADDER));

    /**
     * 8 位加法器（ADDER_8）测例：64 全加器穷举 + 32 进位链定向 + 160 组合覆盖 = 256。
     * 输入、预期输出为确定性二进制值；8 位输入的通道信号强度在运行时随机化
     * （见 RandomizedCombinationalTestCase），因此随机化不影响预期结果。
     */
    public static final CircuitExperiment ADDER_8 = register("adder_8", BuiltinCircuitComponents.ADDER_8.getIcon(), new CircuitExperiment(buildAdder8Tests(), BuiltinCircuitComponents.ADDER_8));

    private static byte[] pin8(int v) {
        byte[] lanes = new byte[8];
        for (int b = 0; b < 8; b++) {
            lanes[b] = (byte) (((v >> b) & 1) != 0 ? 15 : 0);
        }
        return lanes;
    }

    // ================= 时序逻辑：T / T' 触发器 =================

    private static SequentialTick qTick(String inputName, int input, int expectedQ) {
        return new SequentialTick(
                Map.of(inputName, new PinValue(input)),
                Map.of("Q", new PinValue(expectedQ)));
    }

    private static List<TestCase> buildTffTests() {
        List<TestCase> tests = new ArrayList<>();
        tests.add(buildTffTest(new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}));
        tests.add(buildTffTest(new int[]{15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15}));
        tests.add(buildTffTest(new int[]{0, 15, 15, 15, 0, 15, 15, 0, 15, 0, 15, 15}));
        return tests;
    }

    private static ConstantSequentialTestCase buildTffTest(int[] tValues) {
        List<SequentialTick> ticks = new ArrayList<>();
        int q = 0;
        for (int t : tValues) {
            if (t != 0) {
                q = q == 0 ? 15 : 0;
            }
            ticks.add(qTick("T", t, q));
        }
        return new ConstantSequentialTestCase(ticks);
    }

    private static List<TestCase> buildTPrimeTests() {
        List<TestCase> tests = new ArrayList<>();
        tests.add(buildTPrimeTest(new int[]{0, 0, 15, 15, 0, 15, 15, 0, 15}));
        tests.add(buildTPrimeTest(new int[]{15, 15, 15, 0, 15, 0, 15, 15, 0, 15, 15, 15}));
        return tests;
    }

    private static ConstantSequentialTestCase buildTPrimeTest(int[] cValues) {
        List<SequentialTick> ticks = new ArrayList<>();
        int q = 0;
        int previous = 0;
        for (int c : cValues) {
            if (c != 0 && previous == 0) {
                q = q == 0 ? 15 : 0;
            }
            previous = c != 0 ? 1 : 0;
            ticks.add(qTick("C", c, q));
        }
        return new ConstantSequentialTestCase(ticks);
    }

    public static final CircuitExperiment TFF = register("tff", BuiltinCircuitComponents.TFF.getIcon(),
            new CircuitExperiment(buildTffTests(), BuiltinCircuitComponents.TFF));

    public static final CircuitExperiment TFF_PRIME = register("tff_prime", BuiltinCircuitComponents.TFF_PRIME.getIcon(),
            new CircuitExperiment(buildTPrimeTests(), BuiltinCircuitComponents.TFF_PRIME));

    // ================= 时序逻辑：寄存器 =================

    private static byte[] amplifyLanes(byte[] lanes) {
        byte[] result = new byte[lanes.length];
        for (int i = 0; i < lanes.length; i++) {
            result[i] = lanes[i] != 0 ? (byte) 15 : 0;
        }
        return result;
    }

    private static SequentialTick register1Tick(int a, int we, int y) {
        return new SequentialTick(
                Map.of("A", new PinValue(a), "WE", new PinValue(we)),
                Map.of("Y", new PinValue(y)));
    }

    private static ConstantSequentialTestCase buildRegisterTest(int[] aValues, int[] weValues) {
        List<SequentialTick> ticks = new ArrayList<>();
        int q = 0;
        for (int i = 0; i < aValues.length; i++) {
            if (weValues[i] != 0) {
                q = aValues[i] != 0 ? 15 : 0;
            }
            ticks.add(register1Tick(aValues[i], weValues[i], q));
        }
        return new ConstantSequentialTestCase(ticks);
    }

    private static List<TestCase> buildRegisterTests() {
        return List.of(
                buildRegisterTest(
                        new int[]{0, 15, 8, 0, 0, 15, 8, 0, 15, 0, 15, 0},
                        new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}),
                buildRegisterTest(
                        new int[]{0, 0, 15, 0, 0, 15, 8, 0, 0, 15, 0, 0},
                        new int[]{15, 0, 15, 0, 0, 15, 15, 0, 0, 15, 0, 0}),
                buildRegisterTest(
                        new int[]{15, 15, 15, 0, 0, 0, 15, 0, 0, 8, 0, 15},
                        new int[]{15, 0, 0, 15, 0, 0, 15, 0, 0, 15, 0, 0})
        );
    }

    private static SequentialTick register8Tick(byte[] a, int we, byte[] y) {
        return new SequentialTick(
                Map.of("A", new PinValue(a), "WE", new PinValue(we)),
                Map.of("Y", new PinValue(y)));
    }

    private static ConstantSequentialTestCase buildRegister8Test(byte[][] aValues, int[] weValues) {
        List<SequentialTick> ticks = new ArrayList<>();
        byte[] q = new byte[8];
        for (int i = 0; i < aValues.length; i++) {
            if (weValues[i] != 0) {
                q = amplifyLanes(aValues[i]);
            }
            ticks.add(register8Tick(aValues[i], weValues[i], q.clone()));
        }
        return new ConstantSequentialTestCase(ticks);
    }

    private static List<TestCase> buildRegister8Tests() {
        byte[] bus1 = analogLanes(0, 1, 2, 3, 4, 5, 6, 7);
        byte[] bus2 = analogLanes(15, 0, 7, 0, 3, 0, 0, 9);
        byte[] bus3 = analogLanes(9, 8, 7, 6, 5, 4, 3, 2);
        byte[] zero = new byte[8];
        return List.of(
                buildRegister8Test(
                        new byte[][]{bus1, bus2, bus3, zero, bus1, bus2, bus3, zero},
                        new int[]{0, 0, 0, 0, 0, 0, 0, 0}),
                buildRegister8Test(
                        new byte[][]{bus1, bus2, bus3, zero, bus2, bus1, zero, bus3},
                        new int[]{15, 0, 15, 0, 15, 0, 15, 0}),
                buildRegister8Test(
                        new byte[][]{bus2, bus3, bus1, bus2, zero, bus1, bus3, bus2},
                        new int[]{15, 0, 15, 0, 15, 0, 0, 15})
        );
    }

    private static SequentialTick registerAnalogTick(int a, int we, int y) {
        return new SequentialTick(
                Map.of("A", new PinValue(a), "WE", new PinValue(we)),
                Map.of("Y", new PinValue(y)));
    }

    private static ConstantSequentialTestCase buildRegisterAnalogTest(int[] aValues, int[] weValues) {
        List<SequentialTick> ticks = new ArrayList<>();
        int q = 0;
        for (int i = 0; i < aValues.length; i++) {
            if (weValues[i] != 0) {
                q = aValues[i] & 0xF;
            }
            ticks.add(registerAnalogTick(aValues[i], weValues[i], q));
        }
        return new ConstantSequentialTestCase(ticks);
    }

    private static List<TestCase> buildRegisterAnalogTests() {
        return List.of(
                buildRegisterAnalogTest(
                        new int[]{15, 0, 9, 0, 7, 0, 3, 0},
                        new int[]{15, 0, 15, 0, 15, 0, 15, 0}),
                buildRegisterAnalogTest(
                        new int[]{0, 8, 15, 4, 0, 12, 0, 6},
                        new int[]{15, 15, 0, 15, 0, 15, 0, 15})
        );
    }

    public static final CircuitExperiment REGISTER = register("register", BuiltinCircuitComponents.REGISTER.getIcon(),
            new CircuitExperiment(buildRegisterTests(), BuiltinCircuitComponents.REGISTER));

    public static final CircuitExperiment REGISTER_8 = register("register_8", BuiltinCircuitComponents.REGISTER_8.getIcon(),
            new CircuitExperiment(buildRegister8Tests(), BuiltinCircuitComponents.REGISTER_8));

    public static final CircuitExperiment REGISTER_ANALOG = register("register_analog", BuiltinCircuitComponents.REGISTER_ANALOG.getIcon(),
            new CircuitExperiment(buildRegisterAnalogTests(), BuiltinCircuitComponents.REGISTER_ANALOG));

    // ================= 模数/数模转换 =================

    private static List<TestCase> buildAdcTests() {
        List<TestCase> tests = new ArrayList<>();
        for (int value = 0; value < 16; value++) {
            tests.add(new ConstantCombinationalTestCase(
                    Map.of("A", new PinValue(value)),
                    Map.of("Y", new PinValue(pin8(value)))));
        }
        return tests;
    }

    private static List<TestCase> buildDacTests() {
        List<TestCase> tests = new ArrayList<>();
        for (int value = 0; value < 16; value++) {
            tests.add(new ConstantCombinationalTestCase(
                    Map.of("A", new PinValue(pin8(value))),
                    Map.of("Y", new PinValue(value))));
            byte[] highLanes = pin8(value);
            for (int b = 4; b < 8; b++) {
                highLanes[b] = 15;
            }
            tests.add(new ConstantCombinationalTestCase(
                    Map.of("A", new PinValue(highLanes)),
                    Map.of("Y", new PinValue(value))));
        }
        return tests;
    }

    public static final CircuitExperiment ADC = register("adc", BuiltinCircuitComponents.ADC.getIcon(),
            new CircuitExperiment(buildAdcTests(), BuiltinCircuitComponents.ADC));

    public static final CircuitExperiment DAC = register("dac", BuiltinCircuitComponents.DAC.getIcon(),
            new CircuitExperiment(buildDacTests(), BuiltinCircuitComponents.DAC));

    // ================= 总线常量/掩码 =================
    private static final int BUS_CONST_DEFAULT = 123;
    private static final int[] BUS_ANALOG_DEFAULT_LANES = {6, 4, 2, 0, 9, 11, 13, 15};

    /** 按位掩码生成 8 通道：置位通道输出给定强度，其余为 0。 */
    private static byte[] maskLanes(int mask, int strength) {
        byte[] lanes = new byte[8];
        if (strength == 0) {
            return lanes;
        }
        for (int b = 0; b < 8; b++) {
            if (((mask >> b) & 1) != 0) {
                lanes[b] = (byte) (strength & 0xF);
            }
        }
        return lanes;
    }

    private static byte[] analogLanes(int... lanes) {
        byte[] result = new byte[8];
        for (int b = 0; b < 8 && b < lanes.length; b++) {
            result[b] = (byte) (lanes[b] & 0xF);
        }
        return result;
    }

    public static final CircuitExperiment CONST_8 = register("const_8", BuiltinCircuitComponents.CONST_8.getIcon(),
            new CircuitExperiment(List.of(new ConstantCombinationalTestCase(
                    Map.of(),
                    Map.of("Y", new PinValue(maskLanes(BUS_CONST_DEFAULT, 15))))), BuiltinCircuitComponents.CONST_8));

    public static final CircuitExperiment MASK_8 = register("mask_8", BuiltinCircuitComponents.MASK_8.getIcon(),
            new CircuitExperiment(IntStream.of(0, 1, 8, 15)
                    .mapToObj(a -> (TestCase) new ConstantCombinationalTestCase(
                            Map.of("A", new PinValue(a)),
                            Map.of("Y", new PinValue(maskLanes(BUS_CONST_DEFAULT, a)))))
                    .toList(), BuiltinCircuitComponents.MASK_8));

    public static final CircuitExperiment CONST_ANALOG_8 = register("const_analog_8", BuiltinCircuitComponents.CONST_ANALOG_8.getIcon(),
            new CircuitExperiment(List.of(new ConstantCombinationalTestCase(
                    Map.of(),
                    Map.of("Y", new PinValue(analogLanes(BUS_ANALOG_DEFAULT_LANES))))), BuiltinCircuitComponents.CONST_ANALOG_8));

    // ================= 比较器 =================
    private static final int[] COMPARATOR_8_VALUES = {0, 1, 2, 3, 15, 16, 127, 255};
    private static final int[] COMPARATOR_1_VALUES = {0, 1, 8, 15};
    private static final int[] COMPARATOR_I_VALUES = {0, 1, 15};
    private static final int[] COMPARATOR_SIGNED_VALUES = {0, 1, 127, -128, -1};

    private static Map<String, PinValue> comparatorOutputs(int comparison, int i0, int i1) {
        return Map.of(
                "Y0", new PinValue((comparison < 0) ? 15 : (comparison == 0 && i0 != 0 ? 15 : 0)),
                "Y1", new PinValue((comparison > 0) ? 15 : (comparison == 0 && i1 != 0 ? 15 : 0)));
    }

    private static List<TestCase> buildComparatorTests() {
        List<TestCase> tests = new ArrayList<>();
        for (int a : COMPARATOR_1_VALUES) {
            for (int b : COMPARATOR_1_VALUES) {
                for (int i0 : COMPARATOR_I_VALUES) {
                    for (int i1 : COMPARATOR_I_VALUES) {
                        tests.add(new ConstantCombinationalTestCase(
                                Map.of("A", new PinValue(a), "B", new PinValue(b),
                                        "I0", new PinValue(i0), "I1", new PinValue(i1)),
                                comparatorOutputs(Integer.compare(a != 0 ? 1 : 0, b != 0 ? 1 : 0), i0, i1)));
                    }
                }
            }
        }
        return tests;
    }

    private static List<TestCase> buildComparator8Tests() {
        List<TestCase> tests = new ArrayList<>();
        for (int a : COMPARATOR_8_VALUES) {
            for (int b : COMPARATOR_8_VALUES) {
                for (int i0 : COMPARATOR_I_VALUES) {
                    for (int i1 : COMPARATOR_I_VALUES) {
                        tests.add(new ConstantCombinationalTestCase(
                                Map.of("A", new PinValue(pin8(a)), "B", new PinValue(pin8(b)),
                                        "I0", new PinValue(i0), "I1", new PinValue(i1)),
                                comparatorOutputs(Integer.compare(a, b), i0, i1)));
                    }
                }
            }
        }
        return tests;
    }

    private static List<TestCase> buildComparatorSignedTests() {
        List<TestCase> tests = new ArrayList<>();
        for (int a : COMPARATOR_SIGNED_VALUES) {
            for (int b : COMPARATOR_SIGNED_VALUES) {
                for (int i0 : COMPARATOR_I_VALUES) {
                    for (int i1 : COMPARATOR_I_VALUES) {
                        tests.add(new ConstantCombinationalTestCase(
                                Map.of("A", new PinValue(pin8(a & 0xFF)), "B", new PinValue(pin8(b & 0xFF)),
                                        "I0", new PinValue(i0), "I1", new PinValue(i1)),
                                comparatorOutputs(Integer.compare(a, b), i0, i1)));
                    }
                }
            }
        }
        return tests;
    }

    private static List<TestCase> buildComparatorAnalogTests() {
        List<TestCase> tests = new ArrayList<>();
        for (int a : COMPARATOR_1_VALUES) {
            for (int b : COMPARATOR_1_VALUES) {
                for (int i0 : COMPARATOR_I_VALUES) {
                    for (int i1 : COMPARATOR_I_VALUES) {
                        tests.add(new ConstantCombinationalTestCase(
                                Map.of("A", new PinValue(a), "B", new PinValue(b),
                                        "I0", new PinValue(i0), "I1", new PinValue(i1)),
                                comparatorOutputs(Integer.compare(a, b), i0, i1)));
                    }
                }
            }
        }
        return tests;
    }

    public static final CircuitExperiment COMPARATOR = register("comparator", BuiltinCircuitComponents.COMPARATOR.getIcon(),
            new CircuitExperiment(buildComparatorTests(), BuiltinCircuitComponents.COMPARATOR));

    public static final CircuitExperiment COMPARATOR_8 = register("comparator_8", BuiltinCircuitComponents.COMPARATOR_8.getIcon(),
            new CircuitExperiment(buildComparator8Tests(), BuiltinCircuitComponents.COMPARATOR_8));

    public static final CircuitExperiment COMPARATOR_SIGNED_8 = register("comparator_signed_8", BuiltinCircuitComponents.COMPARATOR_SIGNED_8.getIcon(),
            new CircuitExperiment(buildComparatorSignedTests(), BuiltinCircuitComponents.COMPARATOR_SIGNED_8));

    public static final CircuitExperiment COMPARATOR_ANALOG = register("comparator_analog", BuiltinCircuitComponents.COMPARATOR_ANALOG.getIcon(),
            new CircuitExperiment(buildComparatorAnalogTests(), BuiltinCircuitComponents.COMPARATOR_ANALOG));

    private static RandomizedCombinationalTestCase adder8Case(int a, int b, int c) {
        int sum = a + b + c;
        return new RandomizedCombinationalTestCase(
                Map.of("A", new PinValue(pin8(a)), "B", new PinValue(pin8(b)), "CI", new PinValue(c != 0 ? 15 : 0)),
                Map.of("S", new PinValue(pin8(sum & 0xFF)), "CO", new PinValue(((sum >> 8) & 1) != 0 ? 15 : 0)));
    }

    private static long adderKey(int a, int b, int c) {
        return (long) a | ((long) b << 8) | ((long) c << 16);
    }

    private static List<TestCase> buildAdder8Tests() {
        List<TestCase> tests = new ArrayList<>();
        Set<Long> used = new LinkedHashSet<>();

        // 第 1 组：全加器局部输入穷举（64）
        for (int i = 0; i < 8; i++) {
            for (int a = 0; a <= 1; a++) {
                for (int b = 0; b <= 1; b++) {
                    for (int c = 0; c <= 1; c++) {
                        int A = 0, B = 0, Cin = 0;
                        if (c == 0) {
                            Cin = 0;
                        } else if (i == 0) {
                            Cin = 1;
                        } else {
                            A |= (1 << i) - 1;
                            B |= 1;
                            Cin = 0;
                        }
                        if (a == 1) A |= 1 << i;
                        if (b == 1) B |= 1 << i;
                        tests.add(adder8Case(A, B, Cin));
                        used.add(adderKey(A, B, Cin));
                    }
                }
            }
        }

        // 第 2 组：进位链/边界定向测试（32）
        int[][] pairs = {
                {0x00, 0x00}, {0xFF, 0x00}, {0x00, 0xFF}, {0xFF, 0x01},
                {0x01, 0xFF}, {0x80, 0x80}, {0x7F, 0x80}, {0x80, 0x7F},
                {0xAA, 0x55}, {0x55, 0xAA}, {0xF0, 0x0F}, {0x0F, 0xF0},
                {0xFE, 0x01}, {0xFF, 0xFF}, {0x00, 0x01}, {0x01, 0x00},
        };
        for (int[] pair : pairs) {
            for (int c = 0; c <= 1; c++) {
                tests.add(adder8Case(pair[0], pair[1], c));
                used.add(adderKey(pair[0], pair[1], c));
            }
        }

        // 第 3 组：线性覆盖 256 中剔除前两组已用，取前 160
        int taken = 0;
        for (int s = 0; s < 256 && taken < 160; s++) {
            int b0 = (s >> 0) & 1, b1 = (s >> 1) & 1, b2 = (s >> 2) & 1, b3 = (s >> 3) & 1;
            int b4 = (s >> 4) & 1, b5 = (s >> 5) & 1, b6 = (s >> 6) & 1, b7 = (s >> 7) & 1;
            int A = b0 | (b1 << 1) | (b2 << 2) | (b3 << 3) | (b4 << 4) | (b5 << 5) | (b6 << 6) | (b7 << 7);
            int B = (b0 ^ b1 ^ b2)
                    | ((b0 ^ b1 ^ b3) << 1)
                    | ((b0 ^ b2 ^ b4) << 2)
                    | ((b0 ^ b3 ^ b5) << 3)
                    | ((b0 ^ b4 ^ b6) << 4)
                    | ((b0 ^ b5 ^ b7) << 5)
                    | ((b1 ^ b2 ^ b5) << 6)
                    | ((b1 ^ b3 ^ b6) << 7);
            int Cin = b2 ^ b3 ^ b7;
            if (used.contains(adderKey(A, B, Cin))) continue;
            tests.add(adder8Case(A, B, Cin));
            taken++;
        }

        return tests;
    }

    // ================= 位运算器 =================

    /** 8 位按位逻辑运算测例：输入 A、B 的二进制值确定，通道强度随机化，输出按位运算结果。 */
    private static RandomizedCombinationalTestCase bitwise8Case(int a, int b, IntBinaryOperator op) {
        return new RandomizedCombinationalTestCase(
                Map.of("A", new PinValue(pin8(a)), "B", new PinValue(pin8(b))),
                Map.of("Y", new PinValue(pin8(op.applyAsInt(a, b) & 0xFF))));
    }

    private static List<TestCase> buildBitwise8Tests(IntBinaryOperator op) {
        List<TestCase> tests = new ArrayList<>();
        int[][] pairs = {
                {0x00, 0x00}, {0xFF, 0x00}, {0x00, 0xFF}, {0xFF, 0xFF},
                {0xAA, 0x55}, {0x55, 0xAA}, {0xF0, 0x0F}, {0x0F, 0xF0},
                {0x01, 0x01}, {0x80, 0x80}, {0x7F, 0x80}, {0x80, 0x7F},
                {0x00, 0x01}, {0x01, 0x00}, {0x96, 0x69}, {0x3C, 0xC3},
        };
        for (int[] pair : pairs) tests.add(bitwise8Case(pair[0], pair[1], op));
        for (int i = 0; i < 240; i++) {
            tests.add(bitwise8Case((i * 73 + 29) & 0xFF, (i * 151 + 91) & 0xFF, op));
        }
        return tests;
    }

    /** 8 位按位非测例：穷举 256 种输入。 */
    private static List<TestCase> buildBitwiseNotTests() {
        List<TestCase> tests = new ArrayList<>();
        for (int i = 0; i < 256; i++) {
            tests.add(new RandomizedCombinationalTestCase(
                    Map.of("A", new PinValue(pin8(i))),
                    Map.of("Y", new PinValue(pin8((~i) & 0xFF)))));
        }
        return tests;
    }

    /** 8 位取反器测例：输出 A 的相反数的补码，穷举 256 种输入。 */
    private static List<TestCase> buildNegTests() {
        List<TestCase> tests = new ArrayList<>();
        for (int i = 0; i < 256; i++) {
            tests.add(new RandomizedCombinationalTestCase(
                    Map.of("A", new PinValue(pin8(i))),
                    Map.of("Y", new PinValue(pin8((~i + 1) & 0xFF)))));
        }
        return tests;
    }

    /** 8 路逻辑门测例：对 8 位输入 A 的 8 个通道做逻辑运算，输出 1 位 Y，穷举 256 种输入。 */
    private static List<TestCase> buildGate8Tests(IntPredicate op) {
        List<TestCase> tests = new ArrayList<>();
        for (int i = 0; i < 256; i++) {
            tests.add(new RandomizedCombinationalTestCase(
                    Map.of("A", new PinValue(pin8(i))),
                    Map.of("Y", new PinValue(op.test(i) ? 15 : 0))));
        }
        return tests;
    }

    // ================= 位移器 =================

    /** 有符号 8 位整数的通道表示（补码）：第 b 通道对应 bit b。 */
    private static byte[] signedLanes(int value) {
        return pin8(value & 0xFF);
    }

    /**
     * 位移器期望输出：目标通道 i 取源通道 i-shift（循环位移取模 8），越界补 0。
     * analog 为 false 时按数字语义将非 0 通道放大为 15。
     */
    private static byte[] shiftLanes(byte[] data, int shift, boolean rotate, boolean analog) {
        byte[] out = new byte[8];
        for (int i = 0; i < 8; i++) {
            int src = rotate ? (((i - shift) % 8) + 8) % 8 : i - shift;
            if (src < 0 || src >= 8) {
                out[i] = 0;
                continue;
            }
            int lane = data[src] & 0xFF;
            out[i] = (byte) (analog ? lane : (lane != 0 ? 15 : 0));
        }
        return out;
    }

    private static final int[] SHIFT_AMOUNTS = {0, 1, 2, 3, 4, 5, 6, 7, -1, -2, -3, -4, -5, -6, -7, -8};

    private static final int[] SHIFT_DIGITAL_SAMPLES = {
            0x00, 0xFF, 0x01, 0x80, 0xAA, 0x55, 0x0F, 0xF0,
            0x96, 0x69, 0x3C, 0xC3, 0x81, 0x18, 0x7E, 0xE7,
    };

    private static final byte[][] SHIFT_ANALOG_SAMPLES = {
            {0, 0, 0, 0, 0, 0, 0, 0},
            {1, 2, 3, 4, 5, 6, 7, 8},
            {15, 14, 13, 12, 11, 10, 9, 8},
            {1, 0, 15, 0, 8, 0, 3, 0},
            {15, 15, 15, 15, 15, 15, 15, 15},
            {1, 1, 1, 1, 1, 1, 1, 1},
            {0, 15, 0, 15, 0, 15, 0, 15},
            {7, 7, 7, 7, 7, 7, 7, 7},
    };

    private static List<TestCase> buildShifterTests(boolean rotate, boolean analog) {
        List<TestCase> tests = new ArrayList<>();
        if (analog) {
            for (byte[] sample : SHIFT_ANALOG_SAMPLES) {
                for (int shift : SHIFT_AMOUNTS) {
                    tests.add(new ConstantCombinationalTestCase(
                            Map.of("A", new PinValue(sample.clone()), "B", new PinValue(signedLanes(shift))),
                            Map.of("Y", new PinValue(shiftLanes(sample, shift, rotate, true)))));
                }
            }
        } else {
            for (int sample : SHIFT_DIGITAL_SAMPLES) {
                byte[] data = pin8(sample);
                for (int shift : SHIFT_AMOUNTS) {
                    tests.add(new ConstantCombinationalTestCase(
                            Map.of("A", new PinValue(data.clone()), "B", new PinValue(signedLanes(shift))),
                            Map.of("Y", new PinValue(shiftLanes(data, shift, rotate, false)))));
                }
            }
        }
        return tests;
    }

    /**
     * 算术位移期望输出：左移缺失位补 0；右移缺失位补符号位（A 的第 7 通道）。
     * 输出按数字语义放大为 0/15。
     */
    private static byte[] arithmeticShiftLanes(byte[] data, int shift) {
        byte[] out = new byte[8];
        int sign = (data[7] & 0xFF) != 0 ? 15 : 0;
        for (int i = 0; i < 8; i++) {
            int src = i - shift;
            int lane;
            if (src >= 0 && src < 8) {
                lane = data[src] & 0xFF;
            } else if (shift < 0) {
                lane = sign;
            } else {
                lane = 0;
            }
            out[i] = (byte) (lane != 0 ? 15 : 0);
        }
        return out;
    }

    private static List<TestCase> buildArithmeticShiftTests() {
        List<TestCase> tests = new ArrayList<>();
        for (int sample : SHIFT_DIGITAL_SAMPLES) {
            byte[] data = pin8(sample);
            for (int shift : SHIFT_AMOUNTS) {
                tests.add(new ConstantCombinationalTestCase(
                        Map.of("A", new PinValue(data.clone()), "B", new PinValue(signedLanes(shift))),
                        Map.of("Y", new PinValue(arithmeticShiftLanes(data, shift)))));
            }
        }
        return tests;
    }

    public static final CircuitExperiment BITWISE_AND = register("bitwise_and", BuiltinCircuitComponents.BITWISE_AND.getIcon(),
            new CircuitExperiment(buildBitwise8Tests((a, b) -> a & b), BuiltinCircuitComponents.BITWISE_AND));

    public static final CircuitExperiment BITWISE_OR = register("bitwise_or", BuiltinCircuitComponents.BITWISE_OR.getIcon(),
            new CircuitExperiment(buildBitwise8Tests((a, b) -> a | b), BuiltinCircuitComponents.BITWISE_OR));

    public static final CircuitExperiment BITWISE_XOR = register("bitwise_xor", BuiltinCircuitComponents.BITWISE_XOR.getIcon(),
            new CircuitExperiment(buildBitwise8Tests((a, b) -> a ^ b), BuiltinCircuitComponents.BITWISE_XOR));

    public static final CircuitExperiment BITWISE_NAND = register("bitwise_nand", BuiltinCircuitComponents.BITWISE_NAND.getIcon(),
            new CircuitExperiment(buildBitwise8Tests((a, b) -> ~(a & b) & 0xFF), BuiltinCircuitComponents.BITWISE_NAND));

    public static final CircuitExperiment BITWISE_NOR = register("bitwise_nor", BuiltinCircuitComponents.BITWISE_NOR.getIcon(),
            new CircuitExperiment(buildBitwise8Tests((a, b) -> ~(a | b) & 0xFF), BuiltinCircuitComponents.BITWISE_NOR));

    public static final CircuitExperiment BITWISE_XNOR = register("bitwise_xnor", BuiltinCircuitComponents.BITWISE_XNOR.getIcon(),
            new CircuitExperiment(buildBitwise8Tests((a, b) -> ~(a ^ b) & 0xFF), BuiltinCircuitComponents.BITWISE_XNOR));

    public static final CircuitExperiment BITWISE_NOT = register("bitwise_not", BuiltinCircuitComponents.BITWISE_NOT.getIcon(),
            new CircuitExperiment(buildBitwiseNotTests(), BuiltinCircuitComponents.BITWISE_NOT));

    public static final CircuitExperiment OR_GATE_8 = register("or_gate_8", BuiltinCircuitComponents.OR_GATE_8.getIcon(),
            new CircuitExperiment(buildGate8Tests(i -> i != 0), BuiltinCircuitComponents.OR_GATE_8));

    public static final CircuitExperiment AND_GATE_8 = register("and_gate_8", BuiltinCircuitComponents.AND_GATE_8.getIcon(),
            new CircuitExperiment(buildGate8Tests(i -> i == 0xFF), BuiltinCircuitComponents.AND_GATE_8));

    public static final CircuitExperiment XOR_GATE_8 = register("xor_gate_8", BuiltinCircuitComponents.XOR_GATE_8.getIcon(),
            new CircuitExperiment(buildGate8Tests(i -> (Integer.bitCount(i) & 1) == 1), BuiltinCircuitComponents.XOR_GATE_8));

    public static final CircuitExperiment XNOR_GATE_8 = register("xnor_gate_8", BuiltinCircuitComponents.XNOR_GATE_8.getIcon(),
            new CircuitExperiment(buildGate8Tests(i -> (Integer.bitCount(i) & 1) == 0), BuiltinCircuitComponents.XNOR_GATE_8));

    public static final CircuitExperiment NEG = register("neg", BuiltinCircuitComponents.NEG.getIcon(),
            new CircuitExperiment(buildNegTests(), BuiltinCircuitComponents.NEG));

    public static final CircuitExperiment BITWISE_SHIFT = register("bitwise_shift", BuiltinCircuitComponents.BITWISE_SHIFT.getIcon(),
            new CircuitExperiment(buildShifterTests(false, false), BuiltinCircuitComponents.BITWISE_SHIFT));

    public static final CircuitExperiment BITWISE_SHIFT_ARITHMETIC = register("bitwise_shift_arithmetic",
            BuiltinCircuitComponents.BITWISE_SHIFT_ARITHMETIC.getIcon(),
            new CircuitExperiment(buildArithmeticShiftTests(), BuiltinCircuitComponents.BITWISE_SHIFT_ARITHMETIC));

    public static final CircuitExperiment BITWISE_ROTATE = register("bitwise_rotate", BuiltinCircuitComponents.BITWISE_ROTATE.getIcon(),
            new CircuitExperiment(buildShifterTests(true, false), BuiltinCircuitComponents.BITWISE_ROTATE));

    public static final CircuitExperiment BITWISE_SHIFT_ANALOG = register("bitwise_shift_analog", BuiltinCircuitComponents.BITWISE_SHIFT_ANALOG.getIcon(),
            new CircuitExperiment(buildShifterTests(false, true), BuiltinCircuitComponents.BITWISE_SHIFT_ANALOG));

    public static final CircuitExperiment BITWISE_ROTATE_ANALOG = register("bitwise_rotate_analog", BuiltinCircuitComponents.BITWISE_ROTATE_ANALOG.getIcon(),
            new CircuitExperiment(buildShifterTests(true, true), BuiltinCircuitComponents.BITWISE_ROTATE_ANALOG));

    // ================= 乘除器 =================

    private static final int[] ARITH_GRID_VALUES = {0, 1, 2, 3, 15, 16, 127, 255};

    private static final int[][] SPECIAL_PRODUCT_PAIRS = {
            {0xAA, 0x55}, {0x55, 0xAA}, {0xF0, 0x0F}, {0x0F, 0xF0},
            {0x80, 0x02}, {0x02, 0x80}, {0x7F, 0x02}, {0x02, 0x7F},
            {0x96, 0x69}, {0x69, 0x96}, {0x10, 0x10}, {0x20, 0x08},
            {0x40, 0x04}, {0xFF, 0x80}, {0x80, 0xFF}, {0xFE, 0xFD},
    };

    private static final int[][] SPECIAL_DIVIDEND_PAIRS = {
            {0xAA, 0x0F}, {0xFF, 0x10}, {0x96, 0x07}, {0x69, 0x0A},
            {0x7F, 0x03}, {0x80, 0x11}, {0xFE, 0x02}, {0xFD, 0x03},
            {0x64, 0x07}, {0xC8, 0x11}, {0xFF, 0x02}, {0x80, 0x7F},
    };

    private static RandomizedCombinationalTestCase multiplierCase(int a, int b) {
        int product = a * b;
        return new RandomizedCombinationalTestCase(
                Map.of("A", new PinValue(pin8(a)), "B", new PinValue(pin8(b))),
                Map.of("Y0", new PinValue(pin8(product & 0xFF)),
                        "Y1", new PinValue(pin8((product >> 8) & 0xFF))));
    }

    /**
     * 乘法器测例：8×8 网格覆盖小值、2 的幂与最大值，再补充若干特殊位型。
     */
    private static List<TestCase> buildMultiplierTests() {
        List<TestCase> tests = new ArrayList<>();
        Set<Integer> seen = new LinkedHashSet<>();
        for (int a : ARITH_GRID_VALUES) {
            for (int b : ARITH_GRID_VALUES) {
                if (seen.add((a << 8) | b)) tests.add(multiplierCase(a, b));
            }
        }
        for (int[] pair : SPECIAL_PRODUCT_PAIRS) {
            if (seen.add((pair[0] << 8) | pair[1])) tests.add(multiplierCase(pair[0], pair[1]));
        }
        return tests;
    }

    private static RandomizedCombinationalTestCase multiplierLiteCase(int a, int b) {
        int product = a * b;
        return new RandomizedCombinationalTestCase(
                Map.of("A", new PinValue(pin8(a)), "B", new PinValue(pin8(b))),
                Map.of("Y0", new PinValue(pin8(product & 0xFF))));
    }

    private static List<TestCase> buildMultiplierLiteTests() {
        List<TestCase> tests = new ArrayList<>();
        Set<Integer> seen = new LinkedHashSet<>();
        for (int a : ARITH_GRID_VALUES) {
            for (int b : ARITH_GRID_VALUES) {
                if (seen.add((a << 8) | b)) tests.add(multiplierLiteCase(a, b));
            }
        }
        for (int[] pair : SPECIAL_PRODUCT_PAIRS) {
            if (seen.add((pair[0] << 8) | pair[1])) tests.add(multiplierLiteCase(pair[0], pair[1]));
        }
        return tests;
    }

    private static RandomizedCombinationalTestCase dividerCase(int a, int b) {
        int q = b == 0 ? 0 : a / b;
        int r = b == 0 ? 0 : a % b;
        return new RandomizedCombinationalTestCase(
                Map.of("A", new PinValue(pin8(a)), "B", new PinValue(pin8(b))),
                Map.of("Q", new PinValue(pin8(q & 0xFF)),
                        "R", new PinValue(pin8(r & 0xFF))));
    }

    /**
     * 除法器测例：包含除数为 0 的测例，约定此时 Q 和 R 均输出 0。
     */
    private static List<TestCase> buildDividerTests() {
        List<TestCase> tests = new ArrayList<>();
        Set<Integer> seen = new LinkedHashSet<>();
        for (int a : ARITH_GRID_VALUES) {
            for (int b : ARITH_GRID_VALUES) {
                if (seen.add((a << 8) | b)) tests.add(dividerCase(a, b));
            }
        }
        for (int[] pair : SPECIAL_DIVIDEND_PAIRS) {
            if (seen.add((pair[0] << 8) | pair[1])) tests.add(dividerCase(pair[0], pair[1]));
        }
        // 补充除数为 0 的特殊被除数，确保各种位型下都输出全 0。
        for (int a : new int[]{0xAA, 0xFF, 0x80, 0x01}) {
            if (seen.add(a << 8)) tests.add(dividerCase(a, 0));
        }
        return tests;
    }

    // ================= 多功能运算器 =================

    private static final int[][] ALU_OPERAND_PAIRS = {
            {0x00, 0x00}, {0x01, 0x00}, {0x00, 0x01}, {0x01, 0x01},
            {0xFF, 0x00}, {0x00, 0xFF}, {0xFF, 0x01}, {0x01, 0xFF},
            {0xAA, 0x55}, {0x55, 0xAA}, {0x80, 0x80}, {0x7F, 0x80},
            {0x80, 0x02}, {0x96, 0xFE}, {0x0F, 0x04}, {0xF0, 0xF9},
    };

    /**
     * ALU 的逻辑移位：B 按 8 位补码解释，非负左移、负数右移，空位补 0。
     */
    private static int aluShift(int value, int shiftAmount) {
        int shift = (byte) (shiftAmount & 0xFF);
        if (shift >= 0) {
            return shift >= 8 ? 0 : (value << shift) & 0xFF;
        }
        int s = -shift;
        return s >= 8 ? 0 : (value & 0xFF) >>> s;
    }

    private static int aluResult(int op, int a, int b, int ci, boolean advanced) {
        return switch (op) {
            case 0 -> (a + b + ci) & 0xFF;
            case 1 -> (a - b - ci) & 0xFF;
            case 2 -> a & b;
            case 3 -> a | b;
            case 4 -> ~a & 0xFF;
            case 5 -> a ^ b;
            case 6 -> aluShift(a, b);
            case 7 -> advanced ? (a * b) & 0xFF : 0;
            default -> 0;
        };
    }

    /**
     * CO 约定：加法为进位输出；减法为借位输出（A < B + CI 表示不够减）；其余运算为 0。
     */
    private static int aluCarry(int op, int a, int b, int ci) {
        return switch (op) {
            case 0 -> ((a + b + ci) >> 8) & 1;
            case 1 -> a < b + ci ? 1 : 0;
            default -> 0;
        };
    }

    private static RandomizedCombinationalTestCase aluCase(boolean advanced, int op, int a, int b, int ci) {
        Map<String, PinValue> inputs = Map.of(
                "A", new PinValue(pin8(a)),
                "B", new PinValue(pin8(b)),
                "OP", new PinValue(pin8(op)),
                "CI", new PinValue(ci != 0 ? 15 : 0));
        int y = aluResult(op, a, b, ci, advanced);
        int co = aluCarry(op, a, b, ci);
        Map<String, PinValue> outputs;
        if (advanced) {
            int y1 = op == 7 ? ((a * b) >> 8) & 0xFF : 0;
            outputs = Map.of(
                    "Y0", new PinValue(pin8(y)),
                    "Y1", new PinValue(pin8(y1)),
                    "CO", new PinValue(co != 0 ? 15 : 0));
        } else {
            outputs = Map.of(
                    "Y", new PinValue(pin8(y)),
                    "CO", new PinValue(co != 0 ? 15 : 0));
        }
        return new RandomizedCombinationalTestCase(inputs, outputs);
    }

    /**
     * ALU 测例：操作码 0~7 × 16 组操作数 × CI(0/1)。B 的取值同时覆盖移位量与乘法操作数。
     */
    private static List<TestCase> buildAluTests(boolean advanced) {
        List<TestCase> tests = new ArrayList<>();
        for (int op = 0; op < 8; op++) {
            for (int[] pair : ALU_OPERAND_PAIRS) {
                for (int ci = 0; ci <= 1; ci++) {
                    tests.add(aluCase(advanced, op, pair[0], pair[1], ci));
                }
            }
        }
        return tests;
    }

    public static final CircuitExperiment MULTIPLIER_LITE = register("multiplier_lite", BuiltinCircuitComponents.MULTIPLIER_LITE.getIcon(),
            new CircuitExperiment(buildMultiplierLiteTests(), BuiltinCircuitComponents.MULTIPLIER_LITE));

    public static final CircuitExperiment MULTIPLIER = register("multiplier", BuiltinCircuitComponents.MULTIPLIER.getIcon(),
            new CircuitExperiment(buildMultiplierTests(), BuiltinCircuitComponents.MULTIPLIER));

    public static final CircuitExperiment DIVIDER = register("divider", BuiltinCircuitComponents.DIVIDER.getIcon(),
            new CircuitExperiment(buildDividerTests(), BuiltinCircuitComponents.DIVIDER));

    public static final CircuitExperiment ALU = register("alu", BuiltinCircuitComponents.ALU.getIcon(),
            new CircuitExperiment(buildAluTests(false), BuiltinCircuitComponents.ALU));

    public static final CircuitExperiment ALU_ADVANCED = register("alu_advanced", BuiltinCircuitComponents.ALU_ADVANCED.getIcon(),
            new CircuitExperiment(buildAluTests(true), BuiltinCircuitComponents.ALU_ADVANCED));

    public static final BaseExperiment CUSTOM_COMPONENT = register("custom_component", ElementsPlus.id("textures/item/medium_chip.png"), new CustomComponentExperiment());

    public static final CircuitExperiment MUX_2 = register("mux_2", BuiltinCircuitComponents.MUX_2.getIcon(), new CircuitExperiment(cartesian(List.of(List.of(0, 1, 8, 15), List.of(0, 1, 8, 15), List.of(0, 1, 8, 15))).stream().<TestCase>map(inputs -> {
        int d0 = inputs.get(0), d1 = inputs.get(1), a = inputs.get(2);
        return new ConstantCombinationalTestCase(
                Map.of("D0", new PinValue(d0), "D1", new PinValue(d1), "A", new PinValue(a)),
                Map.of("Y", new PinValue(a > 0 ? amplified(d1) : amplified(d0))));
    }).toList(), BuiltinCircuitComponents.MUX_2));

    private static int amplified(int input) {
        return input > 0 ? 15 : 0;
    }

    /** 8 通道总线放大：每通道非 0 即 15。 */
    private static byte[] ampLanes(byte[] lanes) {
        byte[] out = new byte[lanes.length];
        for (int i = 0; i < lanes.length; i++) {
            out[i] = (byte) amplified(lanes[i] & 0xFF);
        }
        return out;
    }

    // 数字多路选择器数据总线样本：每通道 15/0（非 0 即放大）。
    private static final byte[] B8_ZERO = {0, 0, 0, 0, 0, 0, 0, 0};
    private static final byte[] B8_WEAK = {1, 0, 0, 0, 0, 0, 0, 0};
    private static final byte[] B8_NIBBLE_HI = {15, 15, 15, 15, 0, 0, 0, 0};
    private static final byte[] B8_NIBBLE_LO = {0, 0, 0, 0, 15, 15, 15, 15};
    private static final byte[] B8_FULL = {15, 15, 15, 15, 15, 15, 15, 15};
    private static final byte[][] DIGITAL_BUS_SAMPLES = {B8_ZERO, B8_WEAK, B8_NIBBLE_HI, B8_NIBBLE_LO, B8_FULL};

    // 模拟多路选择器数据总线样本：保留任意通道强度。
    private static final byte[] A8_ZERO = {0, 0, 0, 0, 0, 0, 0, 0};
    private static final byte[] A8_LOW = {1, 2, 3, 4, 5, 6, 7, 8};
    private static final byte[] A8_MID = {8, 8, 8, 8, 0, 0, 0, 0};
    private static final byte[] A8_HIGH = {15, 14, 13, 12, 11, 10, 9, 8};
    private static final byte[] A8_ALT = {1, 0, 1, 0, 15, 0, 15, 0};
    private static final byte[][] ANALOG_BUS_SAMPLES = {A8_ZERO, A8_LOW, A8_MID, A8_HIGH, A8_ALT};

    public static final CircuitExperiment MUX_4 = register("mux_4", BuiltinCircuitComponents.MUX_4.getIcon(), new CircuitExperiment(cartesian(List.of(List.of(0, 1, 8, 15), List.of(0, 1, 8, 15), List.of(0, 15), List.of(0, 15), List.of(0, 15), List.of(0, 15))).stream().<TestCase>map(inputs -> {
        int a1 = inputs.get(0), a0 = inputs.get(1);
        int d0 = inputs.get(2), d1 = inputs.get(3), d2 = inputs.get(4), d3 = inputs.get(5);
        int data = switch ((a1 != 0 ? 1 : 0) * 2 + (a0 != 0 ? 1 : 0)) {
            case 0 -> d0;
            case 1 -> d1;
            case 2 -> d2;
            default -> d3;
        };
        return new ConstantCombinationalTestCase(
                Map.of("A0", new PinValue(a0), "A1", new PinValue(a1),
                        "D0", new PinValue(d0), "D1", new PinValue(d1), "D2", new PinValue(d2), "D3", new PinValue(d3)),
                Map.of("Y", new PinValue(amplified(data))));
    }).toList(), BuiltinCircuitComponents.MUX_4));

    public static final CircuitExperiment MUX_2_8 = register("mux_2_8", BuiltinCircuitComponents.MUX_2_8.getIcon(), new CircuitExperiment(cartesian(List.of(List.of(0, 1, 8, 15), List.of(0, 1, 2, 3, 4), List.of(0, 1, 2, 3, 4))).stream().<TestCase>map(inputs -> {
        int a = inputs.get(0);
        byte[] d0 = DIGITAL_BUS_SAMPLES[inputs.get(1)];
        byte[] d1 = DIGITAL_BUS_SAMPLES[inputs.get(2)];
        return new ConstantCombinationalTestCase(
                Map.of("A", new PinValue(a), "D0", new PinValue(d0), "D1", new PinValue(d1)),
                Map.of("Y", new PinValue(ampLanes(a != 0 ? d1 : d0))));
    }).toList(), BuiltinCircuitComponents.MUX_2_8));

    public static final CircuitExperiment MUX_4_8 = register("mux_4_8", BuiltinCircuitComponents.MUX_4_8.getIcon(), new CircuitExperiment(cartesian(List.of(List.of(0, 15), List.of(0, 15), List.of(0, 1, 2), List.of(0, 1, 2), List.of(0, 1, 2), List.of(0, 1, 2))).stream().<TestCase>map(inputs -> {
        int a1 = inputs.get(0), a0 = inputs.get(1);
        byte[] d0 = DIGITAL_BUS_SAMPLES[inputs.get(2)];
        byte[] d1 = DIGITAL_BUS_SAMPLES[inputs.get(3)];
        byte[] d2 = DIGITAL_BUS_SAMPLES[inputs.get(4)];
        byte[] d3 = DIGITAL_BUS_SAMPLES[inputs.get(5)];
        byte[] data = switch ((a1 != 0 ? 1 : 0) * 2 + (a0 != 0 ? 1 : 0)) {
            case 0 -> d0;
            case 1 -> d1;
            case 2 -> d2;
            default -> d3;
        };
        return new ConstantCombinationalTestCase(
                Map.of("A0", new PinValue(a0), "A1", new PinValue(a1),
                        "D0", new PinValue(d0), "D1", new PinValue(d1), "D2", new PinValue(d2), "D3", new PinValue(d3)),
                Map.of("Y", new PinValue(ampLanes(data))));
    }).toList(), BuiltinCircuitComponents.MUX_4_8));

    public static final CircuitExperiment TRI_STATE_BUFFER_8 = register("tri_state_buffer_8", BuiltinCircuitComponents.TRI_STATE_BUFFER_8.getIcon(), new CircuitExperiment(cartesian(List.of(List.of(0, 1, 8, 15), List.of(0, 1, 2, 3, 4))).stream().<TestCase>map(inputs -> {
        int a = inputs.get(0);
        byte[] d = DIGITAL_BUS_SAMPLES[inputs.get(1)];
        return new ConstantCombinationalTestCase(
                Map.of("A", new PinValue(a), "D", new PinValue(d)),
                Map.of("Y", new PinValue(a != 0 ? ampLanes(d) : B8_ZERO)));
    }).toList(), BuiltinCircuitComponents.TRI_STATE_BUFFER_8));

    public static final CircuitExperiment MUX_ANALOG_2 = register("mux_analog_2", BuiltinCircuitComponents.MUX_ANALOG_2.getIcon(), new CircuitExperiment(cartesian(List.of(List.of(0, 1, 8, 15), List.of(0, 1, 5, 8, 15), List.of(0, 1, 5, 8, 15))).stream().<TestCase>map(inputs -> {
        int a = inputs.get(0), d0 = inputs.get(1), d1 = inputs.get(2);
        return new ConstantCombinationalTestCase(
                Map.of("A", new PinValue(a), "D0", new PinValue(d0), "D1", new PinValue(d1)),
                Map.of("Y", new PinValue(a != 0 ? d1 : d0)));
    }).toList(), BuiltinCircuitComponents.MUX_ANALOG_2));

    public static final CircuitExperiment MUX_ANALOG_4 = register("mux_analog_4", BuiltinCircuitComponents.MUX_ANALOG_4.getIcon(), new CircuitExperiment(cartesian(List.of(List.of(0, 15), List.of(0, 15), List.of(0, 1, 8, 15), List.of(0, 1, 8, 15), List.of(0, 1, 8, 15), List.of(0, 1, 8, 15))).stream().<TestCase>map(inputs -> {
        int a1 = inputs.get(0), a0 = inputs.get(1);
        int d0 = inputs.get(2), d1 = inputs.get(3), d2 = inputs.get(4), d3 = inputs.get(5);
        int data = switch ((a1 != 0 ? 1 : 0) * 2 + (a0 != 0 ? 1 : 0)) {
            case 0 -> d0;
            case 1 -> d1;
            case 2 -> d2;
            default -> d3;
        };
        return new ConstantCombinationalTestCase(
                Map.of("A0", new PinValue(a0), "A1", new PinValue(a1),
                        "D0", new PinValue(d0), "D1", new PinValue(d1), "D2", new PinValue(d2), "D3", new PinValue(d3)),
                Map.of("Y", new PinValue(data)));
    }).toList(), BuiltinCircuitComponents.MUX_ANALOG_4));

    public static final CircuitExperiment MUX_ANALOG_2_8 = register("mux_analog_2_8", BuiltinCircuitComponents.MUX_ANALOG_2_8.getIcon(), new CircuitExperiment(cartesian(List.of(List.of(0, 1, 8, 15), List.of(0, 1, 2, 3, 4), List.of(0, 1, 2, 3, 4))).stream().<TestCase>map(inputs -> {
        int a = inputs.get(0);
        byte[] d0 = ANALOG_BUS_SAMPLES[inputs.get(1)];
        byte[] d1 = ANALOG_BUS_SAMPLES[inputs.get(2)];
        return new ConstantCombinationalTestCase(
                Map.of("A", new PinValue(a), "D0", new PinValue(d0), "D1", new PinValue(d1)),
                Map.of("Y", new PinValue(a != 0 ? d1 : d0)));
    }).toList(), BuiltinCircuitComponents.MUX_ANALOG_2_8));

    public static final CircuitExperiment MUX_ANALOG_4_8 = register("mux_analog_4_8", BuiltinCircuitComponents.MUX_ANALOG_4_8.getIcon(), new CircuitExperiment(cartesian(List.of(List.of(0, 15), List.of(0, 15), List.of(0, 1, 2, 3), List.of(0, 1, 2, 3), List.of(0, 1, 2, 3), List.of(0, 1, 2, 3))).stream().<TestCase>map(inputs -> {
        int a1 = inputs.get(0), a0 = inputs.get(1);
        byte[] d0 = ANALOG_BUS_SAMPLES[inputs.get(2)];
        byte[] d1 = ANALOG_BUS_SAMPLES[inputs.get(3)];
        byte[] d2 = ANALOG_BUS_SAMPLES[inputs.get(4)];
        byte[] d3 = ANALOG_BUS_SAMPLES[inputs.get(5)];
        byte[] data = switch ((a1 != 0 ? 1 : 0) * 2 + (a0 != 0 ? 1 : 0)) {
            case 0 -> d0;
            case 1 -> d1;
            case 2 -> d2;
            default -> d3;
        };
        return new ConstantCombinationalTestCase(
                Map.of("A0", new PinValue(a0), "A1", new PinValue(a1),
                        "D0", new PinValue(d0), "D1", new PinValue(d1), "D2", new PinValue(d2), "D3", new PinValue(d3)),
                Map.of("Y", new PinValue(data)));
    }).toList(), BuiltinCircuitComponents.MUX_ANALOG_4_8));

    // ================= 译码器 =================
    private static final byte[] BUS8_ZERO = {0, 0, 0, 0, 0, 0, 0, 0};

    /** 译码器输出总线：8 个 4 位通道中第 index 通道为 15，其余为 0。 */
    private static byte[] decoderBus(int index) {
        byte[] lanes = new byte[8];
        lanes[index] = 15;
        return lanes;
    }

    public static final CircuitExperiment DECODER_2_4 = register("decoder_2_4", BuiltinCircuitComponents.DECODER_2_4.getIcon(), new CircuitExperiment(
            IntStream.range(0, 4).mapToObj(x -> (TestCase) new ConstantCombinationalTestCase(
                    Map.of("A0", new PinValue(x & 1), "A1", new PinValue((x >> 1) & 1)),
                    Map.of("Y", new PinValue(decoderBus(x)))))
            .toList(), BuiltinCircuitComponents.DECODER_2_4));

    public static final CircuitExperiment DECODER_3_8 = register("decoder_3_8", BuiltinCircuitComponents.DECODER_3_8.getIcon(), new CircuitExperiment(
            IntStream.range(0, 8).mapToObj(x -> (TestCase) new ConstantCombinationalTestCase(
                    Map.of("A0", new PinValue(x & 1), "A1", new PinValue((x >> 1) & 1), "A2", new PinValue((x >> 2) & 1)),
                    Map.of("Y", new PinValue(decoderBus(x)))))
            .toList(), BuiltinCircuitComponents.DECODER_3_8));

    public static final CircuitExperiment DECODER_4_16 = register("decoder_4_16", BuiltinCircuitComponents.DECODER_4_16.getIcon(), new CircuitExperiment(
            IntStream.range(0, 16).mapToObj(x -> (TestCase) new ConstantCombinationalTestCase(
                    Map.of("A0", new PinValue(x & 1), "A1", new PinValue((x >> 1) & 1),
                            "A2", new PinValue((x >> 2) & 1), "A3", new PinValue((x >> 3) & 1)),
                    x < 8
                            ? Map.of("Y0", new PinValue(decoderBus(x)), "Y1", new PinValue(BUS8_ZERO))
                            : Map.of("Y0", new PinValue(BUS8_ZERO), "Y1", new PinValue(decoderBus(x - 8)))))
            .toList(), BuiltinCircuitComponents.DECODER_4_16));

    // ================= 编码器 =================
    /** 编码器输入总线：低 lineCount 个通道按 value 的对应位取 0/1。 */
    private static byte[] encoderBus(int value, int lineCount) {
        byte[] lanes = new byte[8];
        for (int b = 0; b < lineCount && b < 8; b++) {
            lanes[b] = (byte) (((value >> b) & 1) != 0 ? 1 : 0);
        }
        return lanes;
    }

    /** 普通编码：所有非 0 输入下标的按位或。 */
    private static int encodeOr(int value, int lineCount) {
        int acc = 0;
        for (int i = 0; i < lineCount; i++) {
            if (((value >> i) & 1) != 0) acc |= i;
        }
        return acc;
    }

    /** 优先编码：最大非 0 输入下标；全 0 返回 0。 */
    private static int encodePriority(int value, int lineCount) {
        for (int i = lineCount - 1; i >= 0; i--) {
            if (((value >> i) & 1) != 0) return i;
        }
        return 0;
    }

    /** 编码器单个 1 位输出：编码值第 bit 位为 1 则 15，否则 0。 */
    private static PinValue bitOut(int encoded, int bit) {
        return new PinValue(((encoded >> bit) & 1) != 0 ? 15 : 0);
    }

    private static ConstantCombinationalTestCase encoder4Case(int value, boolean priority) {
        int index = priority ? encodePriority(value, 4) : encodeOr(value, 4);
        return new ConstantCombinationalTestCase(
                Map.of("A", new PinValue(encoderBus(value, 4))),
                Map.of("Y0", bitOut(index, 0), "Y1", bitOut(index, 1)));
    }

    private static ConstantCombinationalTestCase encoder8Case(int value, boolean priority) {
        int index = priority ? encodePriority(value, 8) : encodeOr(value, 8);
        return new ConstantCombinationalTestCase(
                Map.of("A", new PinValue(encoderBus(value, 8))),
                Map.of("Y0", bitOut(index, 0), "Y1", bitOut(index, 1), "Y2", bitOut(index, 2)));
    }

    private static ConstantCombinationalTestCase encoder16Case(int value, boolean priority) {
        int index = priority ? encodePriority(value, 16) : encodeOr(value, 16);
        return new ConstantCombinationalTestCase(
                Map.of("A0", new PinValue(encoderBus(value & 0xFF, 8)),
                        "A1", new PinValue(encoderBus((value >> 8) & 0xFF, 8))),
                Map.of("Y0", bitOut(index, 0), "Y1", bitOut(index, 1),
                        "Y2", bitOut(index, 2), "Y3", bitOut(index, 3)));
    }

    /** 16 输入编码：单点测试 + 多路组合（覆盖按位或 / 优先两种语义）。 */
    private static List<TestCase> buildEncoder16Tests(boolean priority) {
        List<Integer> values = new ArrayList<>();
        for (int i = 0; i < 16; i++) values.add(1 << i);
        values.add(0xFFFF);
        values.add((1 << 5) | (1 << 6));
        values.add((1 << 0) | (1 << 15));
        values.add((1 << 3) | (1 << 12));
        values.add((1 << 7) | (1 << 8));
        values.add((1 << 0) | (1 << 2) | (1 << 4) | (1 << 6));
        values.add(0);
        List<TestCase> tests = new ArrayList<>();
        for (int v : values) tests.add(encoder16Case(v, priority));
        return tests;
    }

    public static final CircuitExperiment ENCODER_4_2 = register("encoder_4_2", BuiltinCircuitComponents.ENCODER_4_2.getIcon(), new CircuitExperiment(
            IntStream.range(0, 16).mapToObj(v -> (TestCase) encoder4Case(v, false)).toList(), BuiltinCircuitComponents.ENCODER_4_2));

    public static final CircuitExperiment ENCODER_8_3 = register("encoder_8_3", BuiltinCircuitComponents.ENCODER_8_3.getIcon(), new CircuitExperiment(
            IntStream.range(0, 256).mapToObj(v -> (TestCase) encoder8Case(v, false)).toList(), BuiltinCircuitComponents.ENCODER_8_3));

    public static final CircuitExperiment ENCODER_16_4 = register("encoder_16_4", BuiltinCircuitComponents.ENCODER_16_4.getIcon(), new CircuitExperiment(
            buildEncoder16Tests(false), BuiltinCircuitComponents.ENCODER_16_4));

    public static final CircuitExperiment PRIORITY_ENCODER_4_2 = register("priority_encoder_4_2", BuiltinCircuitComponents.PRIORITY_ENCODER_4_2.getIcon(), new CircuitExperiment(
            IntStream.range(0, 16).mapToObj(v -> (TestCase) encoder4Case(v, true)).toList(), BuiltinCircuitComponents.PRIORITY_ENCODER_4_2));

    public static final CircuitExperiment PRIORITY_ENCODER_8_3 = register("priority_encoder_8_3", BuiltinCircuitComponents.PRIORITY_ENCODER_8_3.getIcon(), new CircuitExperiment(
            IntStream.range(0, 256).mapToObj(v -> (TestCase) encoder8Case(v, true)).toList(), BuiltinCircuitComponents.PRIORITY_ENCODER_8_3));

    public static final CircuitExperiment PRIORITY_ENCODER_16_4 = register("priority_encoder_16_4", BuiltinCircuitComponents.PRIORITY_ENCODER_16_4.getIcon(), new CircuitExperiment(
            buildEncoder16Tests(true), BuiltinCircuitComponents.PRIORITY_ENCODER_16_4));

    public static final CircuitExperiment TRI_STATE_BUFFER_ANALOG_8 = register("tri_state_buffer_analog_8", BuiltinCircuitComponents.TRI_STATE_BUFFER_ANALOG_8.getIcon(), new CircuitExperiment(cartesian(List.of(List.of(0, 1, 8, 15), List.of(0, 1, 2, 3, 4))).stream().<TestCase>map(inputs -> {
        int a = inputs.get(0);
        byte[] d = ANALOG_BUS_SAMPLES[inputs.get(1)];
        return new ConstantCombinationalTestCase(
                Map.of("A", new PinValue(a), "D", new PinValue(d)),
                Map.of("Y", new PinValue(a != 0 ? d : A8_ZERO)));
    }).toList(), BuiltinCircuitComponents.TRI_STATE_BUFFER_ANALOG_8));

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
