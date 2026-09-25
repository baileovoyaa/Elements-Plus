package com.elementsplus.core.circuit;

import com.elementsplus.ElementsPlus;
import com.elementsplus.ModItems;
import com.elementsplus.core.circuit.component.*;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class BuiltinCircuitComponents {
    private static final Map<ResourceLocation, CircuitComponent> BY_ID = new LinkedHashMap<>();

    // 基本元件
    public static final CircuitComponent TRANSISTOR = register(new CircuitComponent(ElementsPlus.id("transistor"), Component.translatable("circuit.elements-plus.component.transistor"), Component.translatable("circuit.elements-plus.component.transistor.description"), ElementsPlus.id("textures/item/amethyst_transistor.png")) {{
        setPin(Direction.WEST, 0, PinType.INPUT);
        setPin(Direction.SOUTH, 0, PinType.INPUT);
        setPin(Direction.EAST, 0, PinType.OUTPUT);
    }});
    public static final CircuitComponent DIODE = register(new CircuitComponent(ElementsPlus.id("diode"), Component.translatable("circuit.elements-plus.component.diode"), Component.translatable("circuit.elements-plus.component.diode.description"), ElementsPlus.id("textures/item/amethyst_diode.png")) {{
        setPin(Direction.WEST, 0, PinType.INPUT);
        setPin(Direction.EAST, 0, PinType.OUTPUT);
    }});
    public static final CircuitComponent CAPACITOR = register(new CircuitComponent(ElementsPlus.id("capacitor"), Component.translatable("circuit.elements-plus.component.capacitor"), Component.translatable("circuit.elements-plus.component.capacitor.description"), ElementsPlus.id("textures/item/amethyst_capacitor.png")) {{
        setPin(Direction.WEST, 0, PinType.INPUT);
        setPin(Direction.EAST, 0, PinType.OUTPUT);
        setInstanceFactory(CapacitorComponentInstance::new);
    }});
    public static final CircuitComponent RESISTOR = register(new CircuitComponent(ElementsPlus.id("resistor"), Component.translatable("circuit.elements-plus.component.resistor"), Component.translatable("circuit.elements-plus.component.resistor.description"), ElementsPlus.id("textures/item/amethyst_resistor.png")) {{
        setPin(Direction.WEST, 0, PinType.INPUT);
        setPin(Direction.EAST, 0, PinType.OUTPUT);
        setInstanceFactory(ResistorComponentInstance::new);
    }});
    public static final CircuitComponent RESONATOR = register(new CircuitComponent(ElementsPlus.id("resonator"), Component.translatable("circuit.elements-plus.component.resonator"), Component.translatable("circuit.elements-plus.component.resonator.description"), ElementsPlus.id("textures/item/amethyst_resonator.png")) {{
        setPin(Direction.WEST, 0, PinType.INPUT);
        setPin(Direction.EAST, 0, PinType.OUTPUT);
        setInstanceFactory(ResonatorComponentInstance::new);
    }});
    public static final CircuitComponent BATTERY = register(new CircuitComponent(ElementsPlus.id("battery"), Component.translatable("circuit.elements-plus.component.battery"), Component.translatable("circuit.elements-plus.component.battery.description"), ElementsPlus.id("textures/item/amethyst_battery.png")) {{
        setPin(Direction.NORTH, 0, PinType.OUTPUT);
    }});
    // 模拟
    public static final CircuitComponent AMPLIFIER = register(new CircuitComponent(ElementsPlus.id("amplifier"), Component.translatable("circuit.elements-plus.component.amplifier"), Component.translatable("circuit.elements-plus.component.amplifier.description"), null) {{
        setPin(Direction.WEST, 0, PinType.INPUT);
        setPin(Direction.EAST, 0, PinType.OUTPUT);
    }});
    // 输入输出
    public static final CircuitComponent INPUT = register(new CircuitComponent(ElementsPlus.id("input"), Component.translatable("circuit.elements-plus.component.input"), Component.translatable("circuit.elements-plus.component.input.description"), ElementsPlus.id("textures/circuit/input.png")) {{
        setPin(Direction.EAST, 0, PinType.OUTPUT);
        setInstanceFactory(InputComponentInstance::new);
        clearIngredientSupplier();
    }});
    public static final CircuitComponent OUTPUT = register(new CircuitComponent(ElementsPlus.id("output"), Component.translatable("circuit.elements-plus.component.output"), Component.translatable("circuit.elements-plus.component.output.description"), ElementsPlus.id("textures/circuit/output.png")) {{
        setPin(Direction.WEST, 0, PinType.INPUT);
        setInstanceFactory(OutputComponentInstance::new);
        clearIngredientSupplier();
    }});
    // 逻辑门
    public static final CircuitComponent AND_GATE = register(new CircuitComponent(ElementsPlus.id("and_gate"), Component.nullToEmpty("与门"), Component.nullToEmpty("与门描述"), ElementsPlus.id("textures/circuit/and_gate.png")) {{
        setPin(Direction.NORTH, 0, PinType.INPUT);
        setPin(Direction.SOUTH, 0, PinType.INPUT);
        setPin(Direction.EAST, 0, PinType.OUTPUT);
    }});
    public static final CircuitComponent AND_GATE_SC = register(new CircuitComponent(ElementsPlus.id("and_gate_sc"), Component.translatable("circuit.elements-plus.component.and_gate_sc"), Component.translatable("circuit.elements-plus.component.and_gate_sc.description"), ElementsPlus.id("textures/circuit/and_gate_sc.png")) {{
        setPin(Direction.NORTH, 0, PinType.INPUT);
        setPin(Direction.SOUTH, 0, PinType.INPUT);
        setPin(Direction.EAST, 0, PinType.OUTPUT);
    }});  // 短路与
    public static final CircuitComponent OR_GATE = register(new CircuitComponent(ElementsPlus.id("or_gate"), "或门") {{
        setPin(Direction.NORTH, 0, PinType.INPUT);
        setPin(Direction.SOUTH, 0, PinType.INPUT);
        setPin(Direction.EAST, 0, PinType.OUTPUT);
    }});
    public static final CircuitComponent OR_GATE_SC = register(new CircuitComponent(ElementsPlus.id("or_gate_sc"), Component.translatable("circuit.elements-plus.component.or_gate_sc"), Component.translatable("circuit.elements-plus.component.or_gate_sc.description"), ElementsPlus.id("textures/circuit/or_gate_sc.png")) {{
        setPin(Direction.NORTH, 0, PinType.INPUT);
        setPin(Direction.SOUTH, 0, PinType.INPUT);
        setPin(Direction.EAST, 0, PinType.OUTPUT);
    }});  // 短路或
    public static final CircuitComponent NOT_GATE = register(new CircuitComponent(ElementsPlus.id("not_gate"), "非门") {{
        setPin(Direction.WEST, 0, PinType.INPUT);
        setPin(Direction.EAST, 0, PinType.OUTPUT);
    }});
    public static final CircuitComponent ANALOG_NOT = register(new CircuitComponent(ElementsPlus.id("analog_not"), "非门") {{
        setPin(Direction.WEST, 0, PinType.INPUT);
        setPin(Direction.EAST, 0, PinType.OUTPUT);
    }});
    public static final CircuitComponent NAND_GATE = register(new CircuitComponent(ElementsPlus.id("nand_gate"), Component.translatable("circuit.elements-plus.component.nand_gate"), Component.translatable("circuit.elements-plus.component.nand_gate.description"), ElementsPlus.id("textures/circuit/nand_gate.png")) {{
        setPin(Direction.NORTH, 0, PinType.INPUT);
        setPin(Direction.SOUTH, 0, PinType.INPUT);
        setPin(Direction.EAST, 0, PinType.OUTPUT);
    }});
    public static final CircuitComponent NOR_GATE = register(new CircuitComponent(ElementsPlus.id("nor_gate"), Component.translatable("circuit.elements-plus.component.nor_gate"), Component.translatable("circuit.elements-plus.component.nor_gate.description"), ElementsPlus.id("textures/circuit/nor_gate.png")) {{
        setPin(Direction.NORTH, 0, PinType.INPUT);
        setPin(Direction.SOUTH, 0, PinType.INPUT);
        setPin(Direction.EAST, 0, PinType.OUTPUT);
    }});
    public static final CircuitComponent XOR_GATE = register(new CircuitComponent(ElementsPlus.id("xor_gate"), Component.translatable("circuit.elements-plus.component.xor_gate"), Component.translatable("circuit.elements-plus.component.xor_gate.description"), ElementsPlus.id("textures/circuit/xor_gate.png")) {{
        setPin(Direction.NORTH, 0, PinType.INPUT);
        setPin(Direction.SOUTH, 0, PinType.INPUT);
        setPin(Direction.EAST, 0, PinType.OUTPUT);
    }});
    public static final CircuitComponent XNOR_GATE = register(new CircuitComponent(ElementsPlus.id("xnor_gate"), Component.translatable("circuit.elements-plus.component.xnor_gate"), Component.translatable("circuit.elements-plus.component.xnor_gate.description"), ElementsPlus.id("textures/circuit/xnor_gate.png")) {{
        setPin(Direction.NORTH, 0, PinType.INPUT);
        setPin(Direction.SOUTH, 0, PinType.INPUT);
        setPin(Direction.EAST, 0, PinType.OUTPUT);
    }});
    public static final CircuitComponent AND_GATE_3 = register(new CircuitComponent(ElementsPlus.id("and_gate_3"), Component.translatable("circuit.elements-plus.component.and_gate_3"), Component.translatable("circuit.elements-plus.component.and_gate_3.description"), ElementsPlus.id("textures/circuit/and_gate_3.png")) {{
        setPin(Direction.NORTH, 0, PinType.INPUT);
        setPin(Direction.WEST, 0, PinType.INPUT);
        setPin(Direction.SOUTH, 0, PinType.INPUT);
        setPin(Direction.EAST, 0, PinType.OUTPUT);
    }});
    public static final CircuitComponent OR_GATE_3 = register(new CircuitComponent(ElementsPlus.id("or_gate_3"), Component.translatable("circuit.elements-plus.component.or_gate_3"), Component.translatable("circuit.elements-plus.component.or_gate_3.description"), ElementsPlus.id("textures/circuit/or_gate_3.png")) {{
        setPin(Direction.NORTH, 0, PinType.INPUT);
        setPin(Direction.WEST, 0, PinType.INPUT);
        setPin(Direction.SOUTH, 0, PinType.INPUT);
        setPin(Direction.EAST, 0, PinType.OUTPUT);
    }});
    public static final CircuitComponent XOR_GATE_3 = register(new CircuitComponent(ElementsPlus.id("xor_gate_3"), Component.translatable("circuit.elements-plus.component.xor_gate_3"), Component.translatable("circuit.elements-plus.component.xor_gate_3.description"), ElementsPlus.id("textures/circuit/xor_gate_3.png")) {{
        setPin(Direction.NORTH, 0, PinType.INPUT);
        setPin(Direction.WEST, 0, PinType.INPUT);
        setPin(Direction.SOUTH, 0, PinType.INPUT);
        setPin(Direction.EAST, 0, PinType.OUTPUT);
    }});
    public static final CircuitComponent XNOR_GATE_3 = register(new CircuitComponent(ElementsPlus.id("xnor_gate_3"), Component.translatable("circuit.elements-plus.component.xnor_gate_3"), Component.translatable("circuit.elements-plus.component.xnor_gate_3.description"), ElementsPlus.id("textures/circuit/xnor_gate_3.png")) {{
        setPin(Direction.NORTH, 0, PinType.INPUT);
        setPin(Direction.WEST, 0, PinType.INPUT);
        setPin(Direction.SOUTH, 0, PinType.INPUT);
        setPin(Direction.EAST, 0, PinType.OUTPUT);
    }});
    // MUX
    public static final CircuitComponent MUX = register(new CircuitComponent(ElementsPlus.id("mux"), Component.translatable("circuit.elements-plus.component.mux"), Component.translatable("circuit.elements-plus.component.mux.description"), ElementsPlus.id("textures/circuit/mux.png")) {{
        setPin(Direction.NORTH, 0, PinType.INPUT);
        setPin(Direction.WEST, 0, PinType.INPUT);
        setPin(Direction.SOUTH, 0, PinType.INPUT);
        setPin(Direction.EAST, 0, PinType.OUTPUT);
    }});
    public static final CircuitComponent MUX_8 = register(new CircuitComponent(ElementsPlus.id("mux_8"), Component.translatable("circuit.elements-plus.component.mux_8"), Component.translatable("circuit.elements-plus.component.mux_8.description"), ElementsPlus.id("textures/circuit/mux_8.png")) {{
        setPin(Direction.NORTH, 0, PinType.INPUT);
        setPin(Direction.WEST, 0, PinType.INPUT);
        setPin(Direction.SOUTH, 0, PinType.INPUT);
        setPin(Direction.EAST, 0, PinType.OUTPUT);
        setPinBitWidth(Direction.NORTH, 0, 8);
        setPinBitWidth(Direction.SOUTH, 0, 8);
        setPinBitWidth(Direction.EAST, 0, 8);
    }});
    // 编码器和译码器
    public static final CircuitComponent ENCODER = register(new CircuitComponent(ElementsPlus.id("encoder"), Component.translatable("circuit.elements-plus.component.encoder"), Component.translatable("circuit.elements-plus.component.encoder.description"), ElementsPlus.id("textures/circuit/encoder.png")) {{
        setPin(Direction.WEST, 0, PinType.INPUT);
        setPin(Direction.NORTH, 0, PinType.OUTPUT);
        setPin(Direction.EAST, 0, PinType.OUTPUT);
        setPin(Direction.SOUTH, 0, PinType.OUTPUT);
        setPinBitWidth(Direction.WEST, 0, 8);
    }});
    public static final CircuitComponent DECODER = register(new CircuitComponent(ElementsPlus.id("decoder"), Component.translatable("circuit.elements-plus.component.decoder"), Component.translatable("circuit.elements-plus.component.decoder.description"), ElementsPlus.id("textures/circuit/decoder.png")) {{
        setPin(Direction.WEST, 0, PinType.INPUT);
        setPin(Direction.NORTH, 0, PinType.INPUT);
        setPin(Direction.EAST, 0, PinType.INPUT);
        setPin(Direction.SOUTH, 0, PinType.OUTPUT);
        setPinBitWidth(Direction.EAST, 0, 8);
    }});
    // 加法器
    public static final CircuitComponent HALF_ADDER = register(new CircuitComponent(ElementsPlus.id("half_adder"), Component.translatable("circuit.elements-plus.component.half_adder"), Component.translatable("circuit.elements-plus.component.half_adder.description"), ElementsPlus.id("textures/circuit/half_adder.png"), 1, 2) {{
        setPin(Direction.WEST, 0, PinType.INPUT);
        setPin(Direction.WEST, 1, PinType.INPUT);
        setPin(Direction.EAST, 0, PinType.OUTPUT);
        setPin(Direction.SOUTH, 0, PinType.OUTPUT);
    }});
    public static final CircuitComponent FULL_ADDER = register(new CircuitComponent(ElementsPlus.id("full_adder"), Component.translatable("circuit.elements-plus.component.full_adder"), Component.translatable("circuit.elements-plus.component.full_adder.description"), ElementsPlus.id("textures/circuit/full_adder.png"), 1, 2) {{
        setPin(Direction.WEST, 0, PinType.INPUT);
        setPin(Direction.WEST, 1, PinType.INPUT);
        setPin(Direction.NORTH, 0, PinType.INPUT);
        setPin(Direction.EAST, 0, PinType.OUTPUT);
        setPin(Direction.SOUTH, 0, PinType.OUTPUT);
    }});
    public static final CircuitComponent ADDER_8 = register(new CircuitComponent(ElementsPlus.id("adder_8"), Component.translatable("circuit.elements-plus.component.adder_8"), Component.translatable("circuit.elements-plus.component.adder_8.description"), ElementsPlus.id("textures/circuit/adder.png"), 1, 2) {{
        setPin(Direction.WEST, 0, PinType.INPUT);  // 加数1（8位）
        setPin(Direction.WEST, 1, PinType.INPUT);  // 加数2（8位）
        setPin(Direction.NORTH, 0, PinType.INPUT); // 进位输入（1位）
        setPin(Direction.EAST, 0, PinType.OUTPUT); // 和（8位）
        setPin(Direction.SOUTH, 0, PinType.OUTPUT); // 进位输出（1位）
        setPinBitWidth(Direction.WEST, 0, 8);
        setPinBitWidth(Direction.WEST, 1, 8);
        setPinBitWidth(Direction.EAST, 0, 8);
    }});
    // 总线
    public static final CircuitComponent BUS_JOINER_8 = register(new CircuitComponent(ElementsPlus.id("bus_joiner"), Component.translatable("circuit.elements-plus.component.bus_joiner_8"), Component.translatable("circuit.elements-plus.component.bus_joiner_8.description"), ElementsPlus.id("textures/circuit/bus_joiner.png"), 1, 8) {{
        setPin(Direction.WEST, 0, PinType.INPUT); // 第7位
        setPin(Direction.WEST, 1, PinType.INPUT); // 第6位
        setPin(Direction.WEST, 2, PinType.INPUT); // 第5位
        setPin(Direction.WEST, 3, PinType.INPUT); // 第4位
        setPin(Direction.WEST, 4, PinType.INPUT); // 第3位
        setPin(Direction.WEST, 5, PinType.INPUT); // 第2位
        setPin(Direction.WEST, 6, PinType.INPUT); // 第1位
        setPin(Direction.WEST, 7, PinType.INPUT); // 第0位
        setPin(Direction.EAST, 0, PinType.OUTPUT); // 8位总线
        setPinBitWidth(Direction.EAST, 0, 8);
        clearIngredientSupplier();
    }});
    public static final CircuitComponent BUS_SPLITTER_8 = register(new CircuitComponent(ElementsPlus.id("bus_splitter"), Component.translatable("circuit.elements-plus.component.bus_splitter_8"), Component.translatable("circuit.elements-plus.component.bus_splitter_8.description"), ElementsPlus.id("textures/circuit/bus_splitter.png"), 1, 8) {{
        setPin(Direction.WEST, 7, PinType.INPUT); // 8位总线
        setPin(Direction.EAST, 0, PinType.OUTPUT); // 第0位
        setPin(Direction.EAST, 1, PinType.OUTPUT); // 第1位
        setPin(Direction.EAST, 2, PinType.OUTPUT); // 第2位
        setPin(Direction.EAST, 3, PinType.OUTPUT); // 第3位
        setPin(Direction.EAST, 4, PinType.OUTPUT); // 第4位
        setPin(Direction.EAST, 5, PinType.OUTPUT); // 第5位
        setPin(Direction.EAST, 6, PinType.OUTPUT); // 第6位
        setPin(Direction.EAST, 7, PinType.OUTPUT); // 第7位
        setPinBitWidth(Direction.WEST, 7, 8);
        clearIngredientSupplier();
    }});
    // 模数转换
    public static final CircuitComponent DAC = register(new CircuitComponent(ElementsPlus.id("dac"), Component.translatable("circuit.elements-plus.component.dac"), Component.translatable("circuit.elements-plus.component.dac.description"), ElementsPlus.id("textures/circuit/dac.png"), 1, 4) {{
        setPin(Direction.WEST, 0, PinType.INPUT);
        setPin(Direction.WEST, 1, PinType.INPUT);
        setPin(Direction.WEST, 2, PinType.INPUT);
        setPin(Direction.WEST, 3, PinType.INPUT);
        setPin(Direction.EAST, 0, PinType.OUTPUT);
    }});
    public static final CircuitComponent ADC = register(new CircuitComponent(ElementsPlus.id("adc"), Component.translatable("circuit.elements-plus.component.adc"), Component.translatable("circuit.elements-plus.component.adc.description"), ElementsPlus.id("textures/circuit/adc.png"), 1, 4) {{
        setPin(Direction.EAST, 0, PinType.INPUT);
        setPin(Direction.EAST, 1, PinType.INPUT);
        setPin(Direction.EAST, 2, PinType.INPUT);
        setPin(Direction.EAST, 3, PinType.INPUT);
        setPin(Direction.WEST, 3, PinType.OUTPUT);
    }});
    // 位运算器
    public static final CircuitComponent BITWISE_AND = register(new CircuitComponent(ElementsPlus.id("bitwise_and"), Component.translatable("circuit.elements-plus.component.bitwise_and"), Component.translatable("circuit.elements-plus.component.bitwise_and.description"), ElementsPlus.id("textures/circuit/bitwise_and.png"), 1, 2) {{
        setPin(Direction.WEST, 0, PinType.INPUT);
        setPin(Direction.WEST, 1, PinType.INPUT);
        setPin(Direction.EAST, 0, PinType.OUTPUT);
        setPinBitWidth(Direction.WEST, 0, 8);
        setPinBitWidth(Direction.WEST, 1, 8);
        setPinBitWidth(Direction.EAST, 0, 8);
    }});
    public static final CircuitComponent BITWISE_OR = register(new CircuitComponent(ElementsPlus.id("bitwise_or"), Component.translatable("circuit.elements-plus.component.bitwise_or"), Component.translatable("circuit.elements-plus.component.bitwise_or.description"), ElementsPlus.id("textures/circuit/bitwise_or.png"), 1, 2) {{
        setPin(Direction.WEST, 0, PinType.INPUT);
        setPin(Direction.WEST, 1, PinType.INPUT);
        setPin(Direction.EAST, 0, PinType.OUTPUT);
        setPinBitWidth(Direction.WEST, 0, 8);
        setPinBitWidth(Direction.WEST, 1, 8);
        setPinBitWidth(Direction.EAST, 0, 8);
    }});
    public static final CircuitComponent BITWISE_XOR = register(new CircuitComponent(ElementsPlus.id("bitwise_xor"), Component.translatable("circuit.elements-plus.component.bitwise_xor"), Component.translatable("circuit.elements-plus.component.bitwise_xor.description"), ElementsPlus.id("textures/circuit/bitwise_xor.png"), 1, 2) {{
        setPin(Direction.WEST, 0, PinType.INPUT);
        setPin(Direction.WEST, 1, PinType.INPUT);
        setPin(Direction.EAST, 0, PinType.OUTPUT);
        setPinBitWidth(Direction.WEST, 0, 8);
        setPinBitWidth(Direction.WEST, 1, 8);
        setPinBitWidth(Direction.EAST, 0, 8);
    }});
    public static final CircuitComponent BITWISE_NOT = register(new CircuitComponent(ElementsPlus.id("bitwise_not"), Component.translatable("circuit.elements-plus.component.bitwise_not"), Component.translatable("circuit.elements-plus.component.bitwise_not.description"), ElementsPlus.id("textures/circuit/bitwise_not.png"), 1, 1) {{
        setPin(Direction.WEST, 0, PinType.INPUT);
        setPin(Direction.EAST, 0, PinType.OUTPUT);
        setPinBitWidth(Direction.WEST, 0, 8);
        setPinBitWidth(Direction.EAST, 0, 8);
    }});
    public static final CircuitComponent NEG = register(new CircuitComponent(ElementsPlus.id("neg"), Component.translatable("circuit.elements-plus.component.neg"), Component.translatable("circuit.elements-plus.component.neg.description"), ElementsPlus.id("textures/circuit/neg.png"), 1, 1) {{
        setPin(Direction.WEST, 0, PinType.INPUT);
        setPin(Direction.EAST, 0, PinType.OUTPUT);
        setPinBitWidth(Direction.WEST, 0, 8);
        setPinBitWidth(Direction.EAST, 0, 8);
    }});
    public static final CircuitComponent BITWISE_MOVE = register(new CircuitComponent(ElementsPlus.id("bitwise_move"), Component.translatable("circuit.elements-plus.component.bitwise_move"), Component.translatable("circuit.elements-plus.component.bitwise_move.description"), ElementsPlus.id("textures/circuit/bitwise_move.png"), 1, 2) {{
        setPin(Direction.WEST, 0, PinType.INPUT);
        setPin(Direction.WEST, 1, PinType.INPUT);
        setPin(Direction.EAST, 0, PinType.OUTPUT);
        setPinBitWidth(Direction.WEST, 0, 8);
        setPinBitWidth(Direction.WEST, 1, 8);
        setPinBitWidth(Direction.EAST, 0, 8);
    }});
    // 高级元件
    public static final CircuitComponent ALU = register(new CircuitComponent(ElementsPlus.id("alu"), Component.translatable("circuit.elements-plus.component.alu"), Component.translatable("circuit.elements-plus.component.alu.description"), ElementsPlus.id("textures/circuit/alu.png"), 1, 3) {{
        setPin(Direction.NORTH, 0, PinType.INPUT);
        setPin(Direction.WEST, 0, PinType.INPUT);
        setPin(Direction.WEST, 1, PinType.INPUT);
        setPin(Direction.WEST, 2, PinType.INPUT);
        setPin(Direction.EAST, 0, PinType.OUTPUT);
        setPin(Direction.SOUTH, 0, PinType.OUTPUT);
        setPinBitWidth(Direction.WEST, 0, 8);
        setPinBitWidth(Direction.WEST, 1, 8);
        setPinBitWidth(Direction.WEST, 2, 8);
        setPinBitWidth(Direction.EAST, 0, 8);
    }});
    public static final CircuitComponent ALU_ADVANCED = register(new CircuitComponent(ElementsPlus.id("alu_advanced"), Component.translatable("circuit.elements-plus.component.alu_advanced"), Component.translatable("circuit.elements-plus.component.alu_advanced.description"), ElementsPlus.id("textures/circuit/alu.png"), 1, 3) {{
        setPin(Direction.NORTH, 0, PinType.INPUT);
        setPin(Direction.WEST, 0, PinType.INPUT);
        setPin(Direction.WEST, 1, PinType.INPUT);
        setPin(Direction.WEST, 2, PinType.INPUT);
        setPin(Direction.EAST, 0, PinType.OUTPUT);
        setPin(Direction.SOUTH, 0, PinType.OUTPUT);
        setPinBitWidth(Direction.WEST, 0, 8);
        setPinBitWidth(Direction.WEST, 1, 8);
        setPinBitWidth(Direction.WEST, 2, 8);
        setPinBitWidth(Direction.EAST, 0, 8);
    }});
    public static final CircuitComponent TFF = register(new CircuitComponent(ElementsPlus.id("tff"), Component.translatable("circuit.elements-plus.component.tff"), Component.translatable("circuit.elements-plus.component.tff.description"), ElementsPlus.id("textures/circuit/tff.png"), 1, 1) {{
        setPin(Direction.WEST, 0, PinType.INPUT);
        setPin(Direction.EAST, 0, PinType.OUTPUT);
        setPinBitWidth(Direction.WEST, 0, 1);
        setPinBitWidth(Direction.EAST, 0, 1);
    }});
    public static final CircuitComponent REGISTER = register(new CircuitComponent(ElementsPlus.id("register"), "寄存器") {{
        setPin(Direction.NORTH, 0, PinType.INPUT);
        setPin(Direction.WEST, 0, PinType.INPUT);
        setPin(Direction.SOUTH, 0, PinType.INPUT);
        setPin(Direction.EAST, 0, PinType.OUTPUT);
    }});
    public static final CircuitComponent REGISTER_8 = register(new CircuitComponent(ElementsPlus.id("register_8"), "8位寄存器") {{
        setPin(Direction.NORTH, 0, PinType.INPUT);
        setPin(Direction.WEST, 0, PinType.INPUT);
        setPin(Direction.SOUTH, 0, PinType.INPUT);
        setPin(Direction.EAST, 0, PinType.OUTPUT);
        setPinBitWidth(Direction.WEST, 0, 8);
        setPinBitWidth(Direction.EAST, 0, 8);
    }});
    public static final CircuitComponent RAM = register(new CircuitComponent(ElementsPlus.id("ram"), Component.translatable("circuit.elements-plus.component.ram"), Component.translatable("circuit.elements-plus.component.ram.description"), ElementsPlus.id("textures/circuit/ram.png"), 1, 2) {{
        setPin(Direction.NORTH, 0, PinType.INPUT);
        setPin(Direction.WEST, 0, PinType.INPUT);  // 数据
        setPin(Direction.WEST, 1, PinType.INPUT);  // 寻址
        setPin(Direction.SOUTH, 0, PinType.INPUT);
        setPin(Direction.EAST, 0, PinType.OUTPUT);
        setPinBitWidth(Direction.WEST, 0, 8);
        setPinBitWidth(Direction.WEST, 1, 8);
        setPinBitWidth(Direction.EAST, 0, 8);
    }});
    public static final CircuitComponent MULTIPLIER = register(new CircuitComponent(ElementsPlus.id("multiplier"), "乘法器"));
    public static final CircuitComponent DIVIDER = register(new CircuitComponent(ElementsPlus.id("divider"), "除法器"));
    public static final CircuitComponent ANALOG_COUNTER = register(new CircuitComponent(ElementsPlus.id("analog_counter"), "模拟计数器"));
    public static final CircuitComponent COUNTER_8 = register(new CircuitComponent(ElementsPlus.id("counter_8"), "8位计数器"));

    public static CircuitComponent register(CircuitComponent circuitComponent) {
        BY_ID.put(circuitComponent.getId(), circuitComponent);
        return circuitComponent;
    }

    public static CircuitComponent byId(ResourceLocation id) {
        return id == null ? null : BY_ID.get(id);
    }

    public static Collection<CircuitComponent> getAll() {
        return BY_ID.values();
    }

    public static void bindIngredients() {
        TRANSISTOR.setIngredientSupplier(ModItems.AMETHYST_TRANSISTOR);
        DIODE.setIngredientSupplier(ModItems.AMETHYST_DIODE);
        CAPACITOR.setIngredientSupplier(ModItems.AMETHYST_CAPACITOR);
        RESISTOR.setIngredientSupplier(ModItems.AMETHYST_RESISTOR);
        RESONATOR.setIngredientSupplier(ModItems.AMETHYST_RESONATOR);
        BATTERY.setIngredientSupplier(ModItems.AMETHYST_BATTERY);
    }

    public static final CircuitComponentToolbox EXAMPLE_TOOLBOX = CircuitComponentToolbox.create()
            .category(CircuitComponentToolbox.Category.create("基本元件")
                    .component(TRANSISTOR)
                    .component(DIODE)
                    .component(RESISTOR)
                    .component(CAPACITOR)
                    .component(RESONATOR)
                    .component(BATTERY)
            )
            .category(CircuitComponentToolbox.Category.create("输入输出")
                    .component(INPUT)
                    .component(OUTPUT)
            )
            .category(CircuitComponentToolbox.Category.create("逻辑门")
                    .component(AND_GATE)
                    .component(OR_GATE)
                    .component(NOT_GATE)
            )
            .category(CircuitComponentToolbox.Category.create("总线")
                    .component(BUS_JOINER_8)
                    .component(BUS_SPLITTER_8)
            )
            .category(CircuitComponentToolbox.Category.create("高级元件")
                    .component(ADDER_8)
                    .component(BITWISE_MOVE)
                    .component(MULTIPLIER)
                    .component(REGISTER)
                    .component(COUNTER_8)
            );
}