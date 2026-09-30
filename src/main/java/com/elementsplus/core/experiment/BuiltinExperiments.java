package com.elementsplus.core.experiment;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.elementsplus.core.circuit.BuiltinCircuitComponents;
import com.elementsplus.core.experiment.CircuitExperiment.ConstantCombinationalTestCase;
import com.elementsplus.core.experiment.CircuitExperiment.PinValue;
import com.elementsplus.core.experiment.CircuitExperiment.RandomizedCombinationalTestCase;
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
