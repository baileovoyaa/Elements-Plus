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
        setPin(Direction.WEST, 0, PinType.INPUT, "IN");
        setPin(Direction.SOUTH, 0, PinType.INPUT, "SIDE");
        setPin(Direction.EAST, 0, PinType.OUTPUT, "Y");
    }});
    public static final CircuitComponent DIODE = register(new CircuitComponent(ElementsPlus.id("diode"), Component.translatable("circuit.elements-plus.component.diode"), Component.translatable("circuit.elements-plus.component.diode.description"), ElementsPlus.id("textures/item/amethyst_diode.png")) {{
        setPin(Direction.WEST, 0, PinType.INPUT, "A");
        setPin(Direction.EAST, 0, PinType.OUTPUT, "Y");
    }});
    public static final CircuitComponent CAPACITOR = register(new CircuitComponent(ElementsPlus.id("capacitor"), Component.translatable("circuit.elements-plus.component.capacitor"), Component.translatable("circuit.elements-plus.component.capacitor.description"), ElementsPlus.id("textures/item/amethyst_capacitor.png")) {{
        setPin(Direction.WEST, 0, PinType.INPUT, "D");
        setPin(Direction.EAST, 0, PinType.OUTPUT, "Q");
        setInstanceFactory(CapacitorComponentInstance::new);
    }});
    public static final CircuitComponent RESISTOR = register(new CircuitComponent(ElementsPlus.id("resistor"), Component.translatable("circuit.elements-plus.component.resistor"), Component.translatable("circuit.elements-plus.component.resistor.description"), ElementsPlus.id("textures/item/amethyst_resistor.png")) {{
        setPin(Direction.WEST, 0, PinType.INPUT, "A");
        setPin(Direction.EAST, 0, PinType.OUTPUT, "Y");
        setInstanceFactory(ResistorComponentInstance::new);
    }});
    public static final CircuitComponent RESONATOR = register(new CircuitComponent(ElementsPlus.id("resonator"), Component.translatable("circuit.elements-plus.component.resonator"), Component.translatable("circuit.elements-plus.component.resonator.description"), ElementsPlus.id("textures/item/amethyst_resonator.png")) {{
        setPin(Direction.WEST, 0, PinType.INPUT, "EN");
        setPin(Direction.EAST, 0, PinType.OUTPUT, "Y");
        setInstanceFactory(ResonatorComponentInstance::new);
    }});
    public static final CircuitComponent BATTERY = register(new CircuitComponent(ElementsPlus.id("battery"), Component.translatable("circuit.elements-plus.component.battery"), Component.translatable("circuit.elements-plus.component.battery.description"), ElementsPlus.id("textures/item/amethyst_battery.png")) {{
        setPin(Direction.NORTH, 0, PinType.OUTPUT, "Y");
    }});
    // 模拟
    public static final CircuitComponent AMPLIFIER = register(new CircuitComponent(ElementsPlus.id("amplifier"), Component.translatable("circuit.elements-plus.component.amplifier"), Component.translatable("circuit.elements-plus.component.amplifier.description"), ElementsPlus.id("textures/circuit/amplifier.png")) {{
        setPin(Direction.WEST, 0, PinType.INPUT, "A");
        setPin(Direction.EAST, 0, PinType.OUTPUT, "Y");
    }});
    // 输入输出
    public static final CircuitComponent INPUT = register(new CircuitComponent(ElementsPlus.id("input"), Component.translatable("circuit.elements-plus.component.input"), Component.translatable("circuit.elements-plus.component.input.description"), ElementsPlus.id("textures/circuit/input.png")) {{
        setPin(Direction.EAST, 0, PinType.OUTPUT, "Y");
        setInstanceFactory(InputComponentInstance::new);
        clearIngredientSupplier();
    }});
    public static final CircuitComponent OUTPUT = register(new CircuitComponent(ElementsPlus.id("output"), Component.translatable("circuit.elements-plus.component.output"), Component.translatable("circuit.elements-plus.component.output.description"), ElementsPlus.id("textures/circuit/output.png")) {{
        setPin(Direction.WEST, 0, PinType.INPUT, "A");
        setInstanceFactory(OutputComponentInstance::new);
        clearIngredientSupplier();
    }});
    public static final CircuitComponent INPUT_8 = register(new CircuitComponent(ElementsPlus.id("input_8"), Component.translatable("circuit.elements-plus.component.input_8"), Component.translatable("circuit.elements-plus.component.input_8.description"), ElementsPlus.id("textures/circuit/input_8.png")) {{
        setPin(Direction.EAST, 0, PinType.OUTPUT, "Y", 8);
        setInstanceFactory(Input8ComponentInstance::new);
        clearIngredientSupplier();
    }});
    public static final CircuitComponent OUTPUT_8 = register(new CircuitComponent(ElementsPlus.id("output_8"), Component.translatable("circuit.elements-plus.component.output_8"), Component.translatable("circuit.elements-plus.component.output_8.description"), ElementsPlus.id("textures/circuit/output_8.png")) {{
        setPin(Direction.WEST, 0, PinType.INPUT, "A", 8);
        setInstanceFactory(Output8ComponentInstance::new);
        clearIngredientSupplier();
    }});
    // 逻辑门
    public static final CircuitComponent AND_GATE = register(new CircuitComponent(ElementsPlus.id("and_gate"), Component.translatable("circuit.elements-plus.component.and_gate"), Component.translatable("circuit.elements-plus.component.and_gate.description"), ElementsPlus.id("textures/circuit/and_gate.png")) {{
        setPin(Direction.NORTH, 0, PinType.INPUT, "A");
        setPin(Direction.SOUTH, 0, PinType.INPUT, "B");
        setPin(Direction.EAST, 0, PinType.OUTPUT, "Y");
    }});
    public static final CircuitComponent AND_GATE_SC = register(new CircuitComponent(ElementsPlus.id("and_gate_sc"), Component.translatable("circuit.elements-plus.component.and_gate_sc"), Component.translatable("circuit.elements-plus.component.and_gate_sc.description"), ElementsPlus.id("textures/circuit/and_gate_sc.png")) {{
        setPin(Direction.NORTH, 0, PinType.INPUT, "A");
        setPin(Direction.SOUTH, 0, PinType.INPUT, "B");
        setPin(Direction.EAST, 0, PinType.OUTPUT, "Y");
    }});  // 短路与
    public static final CircuitComponent OR_GATE = register(new CircuitComponent(ElementsPlus.id("or_gate"), Component.translatable("circuit.elements-plus.component.or_gate"), null, ElementsPlus.id("textures/circuit/or_gate.png")) {{
        setPin(Direction.NORTH, 0, PinType.INPUT, "A");
        setPin(Direction.SOUTH, 0, PinType.INPUT, "B");
        setPin(Direction.EAST, 0, PinType.OUTPUT, "Y");
    }});
    public static final CircuitComponent OR_GATE_SC = register(new CircuitComponent(ElementsPlus.id("or_gate_sc"), Component.translatable("circuit.elements-plus.component.or_gate_sc"), null, ElementsPlus.id("textures/circuit/or_gate_sc.png")) {{
        setPin(Direction.NORTH, 0, PinType.INPUT, "A");
        setPin(Direction.SOUTH, 0, PinType.INPUT, "B");
        setPin(Direction.EAST, 0, PinType.OUTPUT, "Y");
    }});  // 短路或
    public static final CircuitComponent NOT_GATE = register(new CircuitComponent(ElementsPlus.id("not_gate"), Component.translatable("circuit.elements-plus.component.not_gate"), null, ElementsPlus.id("textures/circuit/not_gate.png")) {{
        setPin(Direction.WEST, 0, PinType.INPUT, "A");
        setPin(Direction.EAST, 0, PinType.OUTPUT, "Y");
    }});
    public static final CircuitComponent ANALOG_NOT = register(new CircuitComponent(ElementsPlus.id("analog_not"), Component.translatable("circuit.elements-plus.component.analog_not"), null, ElementsPlus.id("textures/circuit/analog_not.png")) {{
        setPin(Direction.WEST, 0, PinType.INPUT, "A");
        setPin(Direction.EAST, 0, PinType.OUTPUT, "Y");
    }});
    public static final CircuitComponent NAND_GATE = register(new CircuitComponent(ElementsPlus.id("nand_gate"), Component.translatable("circuit.elements-plus.component.nand_gate"), null, ElementsPlus.id("textures/circuit/nand_gate.png")) {{
        setPin(Direction.NORTH, 0, PinType.INPUT, "A");
        setPin(Direction.SOUTH, 0, PinType.INPUT, "B");
        setPin(Direction.EAST, 0, PinType.OUTPUT, "Y");
    }});
    public static final CircuitComponent NOR_GATE = register(new CircuitComponent(ElementsPlus.id("nor_gate"), Component.translatable("circuit.elements-plus.component.nor_gate"), null, ElementsPlus.id("textures/circuit/nor_gate.png")) {{
        setPin(Direction.NORTH, 0, PinType.INPUT, "A");
        setPin(Direction.SOUTH, 0, PinType.INPUT, "B");
        setPin(Direction.EAST, 0, PinType.OUTPUT, "Y");
    }});
    public static final CircuitComponent XOR_GATE = register(new CircuitComponent(ElementsPlus.id("xor_gate"), Component.translatable("circuit.elements-plus.component.xor_gate"), null, ElementsPlus.id("textures/circuit/xor_gate.png")) {{
        setPin(Direction.NORTH, 0, PinType.INPUT, "A");
        setPin(Direction.SOUTH, 0, PinType.INPUT, "B");
        setPin(Direction.EAST, 0, PinType.OUTPUT, "Y");
    }});
    public static final CircuitComponent XNOR_GATE = register(new CircuitComponent(ElementsPlus.id("xnor_gate"), Component.translatable("circuit.elements-plus.component.xnor_gate"), null, ElementsPlus.id("textures/circuit/xnor_gate.png")) {{
        setPin(Direction.NORTH, 0, PinType.INPUT, "A");
        setPin(Direction.SOUTH, 0, PinType.INPUT, "B");
        setPin(Direction.EAST, 0, PinType.OUTPUT, "Y");
    }});
    public static final CircuitComponent AND_GATE_3 = register(new CircuitComponent(ElementsPlus.id("and_gate_3"), Component.translatable("circuit.elements-plus.component.and_gate_3"), null, ElementsPlus.id("textures/circuit/and_gate.png")) {{
        setPin(Direction.NORTH, 0, PinType.INPUT, "A");
        setPin(Direction.WEST, 0, PinType.INPUT, "B");
        setPin(Direction.SOUTH, 0, PinType.INPUT, "C");
        setPin(Direction.EAST, 0, PinType.OUTPUT, "Y");
    }});
    public static final CircuitComponent OR_GATE_3 = register(new CircuitComponent(ElementsPlus.id("or_gate_3"), Component.translatable("circuit.elements-plus.component.or_gate_3"), null, ElementsPlus.id("textures/circuit/or_gate.png")) {{
        setPin(Direction.NORTH, 0, PinType.INPUT, "A");
        setPin(Direction.WEST, 0, PinType.INPUT, "B");
        setPin(Direction.SOUTH, 0, PinType.INPUT, "C");
        setPin(Direction.EAST, 0, PinType.OUTPUT, "Y");
    }});
    public static final CircuitComponent XOR_GATE_3 = register(new CircuitComponent(ElementsPlus.id("xor_gate_3"), Component.translatable("circuit.elements-plus.component.xor_gate_3"), null, ElementsPlus.id("textures/circuit/xor_gate.png")) {{
        setPin(Direction.NORTH, 0, PinType.INPUT, "A");
        setPin(Direction.WEST, 0, PinType.INPUT, "B");
        setPin(Direction.SOUTH, 0, PinType.INPUT, "C");
        setPin(Direction.EAST, 0, PinType.OUTPUT, "Y");
    }});
    public static final CircuitComponent XNOR_GATE_3 = register(new CircuitComponent(ElementsPlus.id("xnor_gate_3"), Component.translatable("circuit.elements-plus.component.xnor_gate_3"), null, ElementsPlus.id("textures/circuit/xnor_gate.png")) {{
        setPin(Direction.NORTH, 0, PinType.INPUT, "A");
        setPin(Direction.WEST, 0, PinType.INPUT, "B");
        setPin(Direction.SOUTH, 0, PinType.INPUT, "C");
        setPin(Direction.EAST, 0, PinType.OUTPUT, "Y");
    }});
    // MUX
    public static final CircuitComponent MUX_2 = register(new CircuitComponent(ElementsPlus.id("mux_2"), Component.translatable("circuit.elements-plus.component.mux_2"), null, ElementsPlus.id("textures/circuit/mux.png")) {{
        setPin(Direction.NORTH, 0, PinType.INPUT, "D0");
        setPin(Direction.WEST, 0, PinType.INPUT, "A");
        setPin(Direction.SOUTH, 0, PinType.INPUT, "D1");
        setPin(Direction.EAST, 0, PinType.OUTPUT, "Y");
    }});
    public static final CircuitComponent MUX_2_8 = register(new CircuitComponent(ElementsPlus.id("mux_8"), Component.translatable("circuit.elements-plus.component.mux_2_8"), null, ElementsPlus.id("textures/circuit/mux_8.png")) {{
        setPin(Direction.NORTH, 0, PinType.INPUT, "D0", 8);
        setPin(Direction.WEST, 0, PinType.INPUT, "A");
        setPin(Direction.SOUTH, 0, PinType.INPUT, "D1", 8);
        setPin(Direction.EAST, 0, PinType.OUTPUT, "Y", 8);
    }});
    public static final CircuitComponent MUX_4 = register(new CircuitComponent(ElementsPlus.id("mux_4"), Component.translatable("circuit.elements-plus.component.mux_4"), null, ElementsPlus.id("textures/circuit/mux.png"), 2, 4) {{
        setPin(Direction.NORTH, 0, PinType.INPUT, "A1");
        setPin(Direction.NORTH, 1, PinType.INPUT, "A0");
        setPin(Direction.WEST, 0, PinType.INPUT, "D3");
        setPin(Direction.WEST, 1, PinType.INPUT, "D2");
        setPin(Direction.WEST, 2, PinType.INPUT, "D1");
        setPin(Direction.WEST, 3, PinType.INPUT, "D0");
        setPin(Direction.EAST, 0, PinType.OUTPUT, "Y");
    }});
    public static final CircuitComponent MUX_4_8 = register(new CircuitComponent(ElementsPlus.id("mux_4_8"), Component.translatable("circuit.elements-plus.component.mux_4_8"), null, ElementsPlus.id("textures/circuit/mux_8.png"), 2, 4) {{
        setPin(Direction.NORTH, 0, PinType.INPUT, "A1");
        setPin(Direction.NORTH, 1, PinType.INPUT, "A0");
        setPin(Direction.WEST, 0, PinType.INPUT, "D3", 8);
        setPin(Direction.WEST, 1, PinType.INPUT, "D2", 8);
        setPin(Direction.WEST, 2, PinType.INPUT, "D1", 8);
        setPin(Direction.WEST, 3, PinType.INPUT, "D0", 8);
        setPin(Direction.EAST, 0, PinType.OUTPUT, "Y", 8);
    }});
    public static final CircuitComponent MUX_ANALOG_2 = register(new CircuitComponent(ElementsPlus.id("mux_analog_2"), Component.translatable("circuit.elements-plus.component.mux_analog_2"), null, ElementsPlus.id("textures/circuit/mux_analog.png")) {{
        setPin(Direction.NORTH, 0, PinType.INPUT, "D0");
        setPin(Direction.WEST, 0, PinType.INPUT, "A");
        setPin(Direction.SOUTH, 0, PinType.INPUT, "D1");
        setPin(Direction.EAST, 0, PinType.OUTPUT, "Y");
    }});
    public static final CircuitComponent MUX_ANALOG_2_8 = register(new CircuitComponent(ElementsPlus.id("mux_analog_2_8"), Component.translatable("circuit.elements-plus.component.mux_analog_2_8"), null, ElementsPlus.id("textures/circuit/mux_analog_8.png")) {{
        setPin(Direction.NORTH, 0, PinType.INPUT, "D0", 8);
        setPin(Direction.WEST, 0, PinType.INPUT, "A");
        setPin(Direction.SOUTH, 0, PinType.INPUT, "D1", 8);
        setPin(Direction.EAST, 0, PinType.OUTPUT, "Y", 8);
    }});
    public static final CircuitComponent MUX_ANALOG_4 = register(new CircuitComponent(ElementsPlus.id("mux_analog_4"), Component.translatable("circuit.elements-plus.component.mux_analog_4"), null, ElementsPlus.id("textures/circuit/mux_analog.png"), 2, 4) {{
        setPin(Direction.NORTH, 0, PinType.INPUT, "A1");
        setPin(Direction.NORTH, 1, PinType.INPUT, "A0");
        setPin(Direction.WEST, 0, PinType.INPUT, "D3");
        setPin(Direction.WEST, 1, PinType.INPUT, "D2");
        setPin(Direction.WEST, 2, PinType.INPUT, "D1");
        setPin(Direction.WEST, 3, PinType.INPUT, "D0");
        setPin(Direction.EAST, 0, PinType.OUTPUT, "Y");
    }});
    public static final CircuitComponent MUX_ANALOG_4_8 = register(new CircuitComponent(ElementsPlus.id("mux_analog_4_8"), Component.translatable("circuit.elements-plus.component.mux_analog_4_8"), null, ElementsPlus.id("textures/circuit/mux_analog_8.png"), 2, 4) {{
        setPin(Direction.NORTH, 0, PinType.INPUT, "A1");
        setPin(Direction.NORTH, 1, PinType.INPUT, "A0");
        setPin(Direction.WEST, 0, PinType.INPUT, "D3", 8);
        setPin(Direction.WEST, 1, PinType.INPUT, "D2", 8);
        setPin(Direction.WEST, 2, PinType.INPUT, "D1", 8);
        setPin(Direction.WEST, 3, PinType.INPUT, "D0", 8);
        setPin(Direction.EAST, 0, PinType.OUTPUT, "Y", 8);
    }});
    public static final CircuitComponent TRI_STATE_BUFFER_8 = register(new CircuitComponent(ElementsPlus.id("tri_state_buffer_8"), Component.translatable("circuit.elements-plus.component.tri_state_buffer_8"), null, ElementsPlus.id("textures/circuit/tri_state_buffer_8.png")) {{
        setPin(Direction.WEST, 0, PinType.INPUT, "D", 8);
        setPin(Direction.NORTH, 0, PinType.INPUT, "EN");
        setPin(Direction.EAST, 0, PinType.OUTPUT, "Y", 8);
    }});
    public static final CircuitComponent TRI_STATE_BUFFER_ANALOG_8 = register(new CircuitComponent(ElementsPlus.id("tri_state_buffer_analog_8"), Component.translatable("circuit.elements-plus.component.tri_state_buffer_analog_8"), null, ElementsPlus.id("textures/circuit/tri_state_buffer_analog_8.png")) {{
        setPin(Direction.WEST, 0, PinType.INPUT, "D", 8);
        setPin(Direction.NORTH, 0, PinType.INPUT, "EN");
        setPin(Direction.EAST, 0, PinType.OUTPUT, "Y", 8);
    }});
    // 编码器和译码器
    public static final CircuitComponent DECODER_2_4 = register(new CircuitComponent(ElementsPlus.id("decoder_2_4"), Component.translatable("circuit.elements-plus.component.decoder_2_4"), null, ElementsPlus.id("textures/circuit/decoder_2_4.png")) {{
        setPin(Direction.NORTH, 0, PinType.INPUT, "A0");
        setPin(Direction.SOUTH, 0, PinType.INPUT, "A1");
        setPin(Direction.EAST, 0, PinType.OUTPUT, "Y", 8);
    }});
    public static final CircuitComponent DECODER_3_8 = register(new CircuitComponent(ElementsPlus.id("decoder_3_8"), Component.translatable("circuit.elements-plus.component.decoder_3_8"), null, ElementsPlus.id("textures/circuit/decoder_3_8.png")) {{
        setPin(Direction.WEST, 0, PinType.INPUT, "A1");
        setPin(Direction.NORTH, 0, PinType.INPUT, "A0");
        setPin(Direction.SOUTH, 0, PinType.INPUT, "A2");
        setPin(Direction.EAST, 0, PinType.OUTPUT, "Y", 8);
    }});
    public static final CircuitComponent DECODER_4_16 = register(new CircuitComponent(ElementsPlus.id("decoder_4_16"), Component.translatable("circuit.elements-plus.component.decoder_4_16"), null, ElementsPlus.id("textures/circuit/decoder_4_16.png"), 1, 2) {{
        setPin(Direction.WEST, 0, PinType.INPUT, "A2");
        setPin(Direction.WEST, 1, PinType.INPUT, "A1");
        setPin(Direction.NORTH, 0, PinType.INPUT, "A0");
        setPin(Direction.SOUTH, 0, PinType.INPUT, "A3");
        setPin(Direction.EAST, 0, PinType.OUTPUT, "Y0", 8);
        setPin(Direction.EAST, 1, PinType.OUTPUT, "Y1", 8);
    }});
    public static final CircuitComponent ENCODER_4_2 = register(new CircuitComponent(ElementsPlus.id("encoder_4_2"), Component.translatable("circuit.elements-plus.component.encoder_4_2"), null, ElementsPlus.id("textures/circuit/encoder_4_2.png")) {{
        setPin(Direction.WEST, 0, PinType.INPUT, "A", 8);
        setPin(Direction.NORTH, 0, PinType.OUTPUT, "Y0");
        setPin(Direction.SOUTH, 0, PinType.OUTPUT, "Y1");
    }});
    public static final CircuitComponent ENCODER_8_3 = register(new CircuitComponent(ElementsPlus.id("encoder_8_3"), Component.translatable("circuit.elements-plus.component.encoder_8_3"), null, ElementsPlus.id("textures/circuit/encoder_8_3.png")) {{
        setPin(Direction.WEST, 0, PinType.INPUT, "A", 8);
        setPin(Direction.NORTH, 0, PinType.OUTPUT, "Y0");
        setPin(Direction.EAST, 0, PinType.OUTPUT, "Y1");
        setPin(Direction.SOUTH, 0, PinType.OUTPUT, "Y2");
    }});
    public static final CircuitComponent ENCODER_16_4 = register(new CircuitComponent(ElementsPlus.id("encoder_16_4"), Component.translatable("circuit.elements-plus.component.encoder_16_4"), null, ElementsPlus.id("textures/circuit/encoder_16_4.png"), 1, 2) {{
        setPin(Direction.WEST, 0, PinType.INPUT, "A1", 8);
        setPin(Direction.WEST, 1, PinType.INPUT, "A0", 8);
        setPin(Direction.NORTH, 0, PinType.OUTPUT, "Y0");
        setPin(Direction.EAST, 0, PinType.OUTPUT, "Y1");
        setPin(Direction.EAST, 1, PinType.OUTPUT, "Y2");
        setPin(Direction.SOUTH, 0, PinType.OUTPUT, "Y3");
    }});
    public static final CircuitComponent PRIORITY_ENCODER_4_2 = register(new CircuitComponent(ElementsPlus.id("priority_encoder_4_2"), Component.translatable("circuit.elements-plus.component.priority_encoder_4_2"), null, ElementsPlus.id("textures/circuit/priority_encoder_4_2.png")) {{
        setPin(Direction.WEST, 0, PinType.INPUT, "A", 8);
        setPin(Direction.NORTH, 0, PinType.OUTPUT, "Y0");
        setPin(Direction.SOUTH, 0, PinType.OUTPUT, "Y1");
    }});
    public static final CircuitComponent PRIORITY_ENCODER_8_3 = register(new CircuitComponent(ElementsPlus.id("priority_encoder_8_3"), Component.translatable("circuit.elements-plus.component.priority_encoder_8_3"), null, ElementsPlus.id("textures/circuit/priority_encoder_8_3.png")) {{
        setPin(Direction.WEST, 0, PinType.INPUT, "A", 8);
        setPin(Direction.NORTH, 0, PinType.OUTPUT, "Y0");
        setPin(Direction.EAST, 0, PinType.OUTPUT, "Y1");
        setPin(Direction.SOUTH, 0, PinType.OUTPUT, "Y2");
    }});
    public static final CircuitComponent PRIORITY_ENCODER_16_4 = register(new CircuitComponent(ElementsPlus.id("priority_encoder_16_4"), Component.translatable("circuit.elements-plus.component.priority_encoder_16_4"), null, ElementsPlus.id("textures/circuit/priority_encoder_16_4.png"), 1, 2) {{
        setPin(Direction.WEST, 0, PinType.INPUT, "A1", 8);
        setPin(Direction.WEST, 1, PinType.INPUT, "A0", 8);
        setPin(Direction.NORTH, 0, PinType.OUTPUT, "Y0");
        setPin(Direction.EAST, 0, PinType.OUTPUT, "Y1");
        setPin(Direction.EAST, 1, PinType.OUTPUT, "Y2");
        setPin(Direction.SOUTH, 0, PinType.OUTPUT, "Y3");
    }});
    // 加法器
    public static final CircuitComponent HALF_ADDER = register(new CircuitComponent(ElementsPlus.id("half_adder"), Component.translatable("circuit.elements-plus.component.half_adder"), null, ElementsPlus.id("textures/circuit/adder.png"), 1, 2) {{
        setPin(Direction.WEST, 0, PinType.INPUT, "A");
        setPin(Direction.WEST, 1, PinType.INPUT, "B");
        setPin(Direction.EAST, 0, PinType.OUTPUT, "S");
        setPin(Direction.SOUTH, 0, PinType.OUTPUT, "C");
    }});
    public static final CircuitComponent FULL_ADDER = register(new CircuitComponent(ElementsPlus.id("full_adder"), Component.translatable("circuit.elements-plus.component.full_adder"), null, ElementsPlus.id("textures/circuit/adder.png"), 1, 2) {{
        setPin(Direction.WEST, 0, PinType.INPUT, "A");
        setPin(Direction.WEST, 1, PinType.INPUT, "B");
        setPin(Direction.NORTH, 0, PinType.INPUT, "CI");
        setPin(Direction.EAST, 0, PinType.OUTPUT, "S");
        setPin(Direction.SOUTH, 0, PinType.OUTPUT, "CO");
    }});
    public static final CircuitComponent ADDER_8 = register(new CircuitComponent(ElementsPlus.id("adder_8"), Component.translatable("circuit.elements-plus.component.adder_8"), null, ElementsPlus.id("textures/circuit/adder_8.png"), 1, 2) {{
        setPin(Direction.WEST, 0, PinType.INPUT, "A", 8);
        setPin(Direction.WEST, 1, PinType.INPUT, "B", 8);
        setPin(Direction.NORTH, 0, PinType.INPUT, "CI");
        setPin(Direction.EAST, 0, PinType.OUTPUT, "S", 8);
        setPin(Direction.SOUTH, 0, PinType.OUTPUT, "CO");
    }});
    // 总线
    public static final CircuitComponent BUS_JOINER_8 = register(new CircuitComponent(ElementsPlus.id("bus_joiner"), Component.translatable("circuit.elements-plus.component.bus_joiner_8"), Component.translatable("circuit.elements-plus.component.bus_joiner_8.description"), ElementsPlus.id("textures/circuit/bus_joiner_8.png"), 1, 8) {{
        setPin(Direction.WEST, 0, PinType.INPUT, "D7");
        setPin(Direction.WEST, 1, PinType.INPUT, "D6");
        setPin(Direction.WEST, 2, PinType.INPUT, "D5");
        setPin(Direction.WEST, 3, PinType.INPUT, "D4");
        setPin(Direction.WEST, 4, PinType.INPUT, "D3");
        setPin(Direction.WEST, 5, PinType.INPUT, "D2");
        setPin(Direction.WEST, 6, PinType.INPUT, "D1");
        setPin(Direction.WEST, 7, PinType.INPUT, "D0");
        setPin(Direction.EAST, 0, PinType.OUTPUT, "Y", 8);
        clearIngredientSupplier();
    }});
    public static final CircuitComponent BUS_SPLITTER_8 = register(new CircuitComponent(ElementsPlus.id("bus_splitter"), Component.translatable("circuit.elements-plus.component.bus_splitter_8"), Component.translatable("circuit.elements-plus.component.bus_splitter_8.description"), ElementsPlus.id("textures/circuit/bus_splitter_8.png"), 1, 8) {{
        setPin(Direction.WEST, 7, PinType.INPUT, "D", 8);
        setPin(Direction.EAST, 0, PinType.OUTPUT, "Y0");
        setPin(Direction.EAST, 1, PinType.OUTPUT, "Y1");
        setPin(Direction.EAST, 2, PinType.OUTPUT, "Y2");
        setPin(Direction.EAST, 3, PinType.OUTPUT, "Y3");
        setPin(Direction.EAST, 4, PinType.OUTPUT, "Y4");
        setPin(Direction.EAST, 5, PinType.OUTPUT, "Y5");
        setPin(Direction.EAST, 6, PinType.OUTPUT, "Y6");
        setPin(Direction.EAST, 7, PinType.OUTPUT, "Y7");
        clearIngredientSupplier();
    }});
    // 模数转换
    public static final CircuitComponent DAC = register(new CircuitComponent(ElementsPlus.id("dac"), Component.translatable("circuit.elements-plus.component.dac"), null, ElementsPlus.id("textures/circuit/dac.png"), 1, 1) {{
        setPin(Direction.WEST, 0, PinType.INPUT, "D");
        setPin(Direction.EAST, 0, PinType.OUTPUT, "Y", 8);
    }});
    public static final CircuitComponent ADC = register(new CircuitComponent(ElementsPlus.id("adc"), Component.translatable("circuit.elements-plus.component.adc"), null, ElementsPlus.id("textures/circuit/adc.png"), 1, 1) {{
        setPin(Direction.WEST, 0, PinType.INPUT, "A", 8);
        setPin(Direction.EAST, 0, PinType.OUTPUT, "Y");
    }});
    // 位运算器
    public static final CircuitComponent BITWISE_AND = register(new CircuitComponent(ElementsPlus.id("bitwise_and"), Component.translatable("circuit.elements-plus.component.bitwise_and"), null, ElementsPlus.id("textures/circuit/bitwise_and.png"), 1, 2) {{
        setPin(Direction.WEST, 0, PinType.INPUT, "A", 8);
        setPin(Direction.WEST, 1, PinType.INPUT, "B", 8);
        setPin(Direction.EAST, 0, PinType.OUTPUT, "Y", 8);
    }});
    public static final CircuitComponent BITWISE_OR = register(new CircuitComponent(ElementsPlus.id("bitwise_or"), Component.translatable("circuit.elements-plus.component.bitwise_or"), null, ElementsPlus.id("textures/circuit/bitwise_or.png"), 1, 2) {{
        setPin(Direction.WEST, 0, PinType.INPUT, "A", 8);
        setPin(Direction.WEST, 1, PinType.INPUT, "B", 8);
        setPin(Direction.EAST, 0, PinType.OUTPUT, "Y", 8);
    }});
    public static final CircuitComponent BITWISE_XOR = register(new CircuitComponent(ElementsPlus.id("bitwise_xor"), Component.translatable("circuit.elements-plus.component.bitwise_xor"), null, ElementsPlus.id("textures/circuit/bitwise_xor.png"), 1, 2) {{
        setPin(Direction.WEST, 0, PinType.INPUT, "A", 8);
        setPin(Direction.WEST, 1, PinType.INPUT, "B", 8);
        setPin(Direction.EAST, 0, PinType.OUTPUT, "Y", 8);
    }});
    public static final CircuitComponent BITWISE_NOT = register(new CircuitComponent(ElementsPlus.id("bitwise_not"), Component.translatable("circuit.elements-plus.component.bitwise_not"), null, ElementsPlus.id("textures/circuit/bitwise_not.png"), 1, 1) {{
        setPin(Direction.WEST, 0, PinType.INPUT, "A", 8);
        setPin(Direction.EAST, 0, PinType.OUTPUT, "Y", 8);
    }});
    public static final CircuitComponent NEG = register(new CircuitComponent(ElementsPlus.id("neg"), Component.translatable("circuit.elements-plus.component.neg"), null, ElementsPlus.id("textures/circuit/neg.png"), 1, 1) {{
        setPin(Direction.WEST, 0, PinType.INPUT, "A", 8);
        setPin(Direction.EAST, 0, PinType.OUTPUT, "Y", 8);
    }});
    public static final CircuitComponent BITWISE_SHIFT = register(new CircuitComponent(ElementsPlus.id("bitwise_shift"), Component.translatable("circuit.elements-plus.component.bitwise_shift"), null, ElementsPlus.id("textures/circuit/bitwise_shift.png"), 1, 1) {{
        setPin(Direction.WEST, 0, PinType.INPUT, "A", 8);
        setPin(Direction.NORTH, 0, PinType.INPUT, "B", 8);
        setPin(Direction.EAST, 0, PinType.OUTPUT, "Y", 8);
    }});
    public static final CircuitComponent BITWISE_NAND = register(new CircuitComponent(ElementsPlus.id("bitwise_nand"), Component.translatable("circuit.elements-plus.component.bitwise_nand"), null, ElementsPlus.id("textures/circuit/bitwise_nand.png"), 1, 2) {{
        setPin(Direction.WEST, 0, PinType.INPUT, "A", 8);
        setPin(Direction.WEST, 1, PinType.INPUT, "B", 8);
        setPin(Direction.EAST, 0, PinType.OUTPUT, "Y", 8);
    }});
    public static final CircuitComponent BITWISE_NOR = register(new CircuitComponent(ElementsPlus.id("bitwise_nor"), Component.translatable("circuit.elements-plus.component.bitwise_nor"), null, ElementsPlus.id("textures/circuit/bitwise_nor.png"), 1, 2) {{
        setPin(Direction.WEST, 0, PinType.INPUT, "A", 8);
        setPin(Direction.WEST, 1, PinType.INPUT, "B", 8);
        setPin(Direction.EAST, 0, PinType.OUTPUT, "Y", 8);
    }});
    public static final CircuitComponent BITWISE_XNOR = register(new CircuitComponent(ElementsPlus.id("bitwise_xnor"), Component.translatable("circuit.elements-plus.component.bitwise_xnor"), null, ElementsPlus.id("textures/circuit/bitwise_xnor.png"), 1, 2) {{
        setPin(Direction.WEST, 0, PinType.INPUT, "A", 8);
        setPin(Direction.WEST, 1, PinType.INPUT, "B", 8);
        setPin(Direction.EAST, 0, PinType.OUTPUT, "Y", 8);
    }});
    // 8路逻辑门：8位输入 A，1位输出 Y
    public static final CircuitComponent OR_GATE_8 = register(new CircuitComponent(ElementsPlus.id("or_gate_8"), Component.translatable("circuit.elements-plus.component.or_gate_8"), null, ElementsPlus.id("textures/circuit/or_gate_8.png")) {{
        setPin(Direction.WEST, 0, PinType.INPUT, "A", 8);
        setPin(Direction.EAST, 0, PinType.OUTPUT, "Y");
    }});
    public static final CircuitComponent AND_GATE_8 = register(new CircuitComponent(ElementsPlus.id("and_gate_8"), Component.translatable("circuit.elements-plus.component.and_gate_8"), null, ElementsPlus.id("textures/circuit/and_gate_8.png")) {{
        setPin(Direction.WEST, 0, PinType.INPUT, "A", 8);
        setPin(Direction.EAST, 0, PinType.OUTPUT, "Y");
    }});
    public static final CircuitComponent XOR_GATE_8 = register(new CircuitComponent(ElementsPlus.id("xor_gate_8"), Component.translatable("circuit.elements-plus.component.xor_gate_8"), null, ElementsPlus.id("textures/circuit/xor_gate_8.png")) {{
        setPin(Direction.WEST, 0, PinType.INPUT, "A", 8);
        setPin(Direction.EAST, 0, PinType.OUTPUT, "Y");
    }});
    public static final CircuitComponent XNOR_GATE_8 = register(new CircuitComponent(ElementsPlus.id("xnor_gate_8"), Component.translatable("circuit.elements-plus.component.xnor_gate_8"), null, ElementsPlus.id("textures/circuit/xnor_gate_8.png")) {{
        setPin(Direction.WEST, 0, PinType.INPUT, "A", 8);
        setPin(Direction.EAST, 0, PinType.OUTPUT, "Y");
    }});
    public static final CircuitComponent BITWISE_ROTATE = register(new CircuitComponent(ElementsPlus.id("bitwise_rotate"), Component.translatable("circuit.elements-plus.component.bitwise_rotate"), null, ElementsPlus.id("textures/circuit/bitwise_rotate.png")) {{
        setPin(Direction.WEST, 0, PinType.INPUT, "A", 8);
        setPin(Direction.NORTH, 0, PinType.INPUT, "B", 8);
        setPin(Direction.EAST, 0, PinType.OUTPUT, "Y", 8);
    }});
    public static final CircuitComponent BITWISE_SHIFT_ANALOG = register(new CircuitComponent(ElementsPlus.id("bitwise_shift_analog"), Component.translatable("circuit.elements-plus.component.bitwise_shift_analog"), null, ElementsPlus.id("textures/circuit/bitwise_shift_analog.png")) {{
        setPin(Direction.WEST, 0, PinType.INPUT, "A", 8);
        setPin(Direction.NORTH, 0, PinType.INPUT, "B", 8);
        setPin(Direction.EAST, 0, PinType.OUTPUT, "Y", 8);
    }});
    public static final CircuitComponent BITWISE_ROTATE_ANALOG = register(new CircuitComponent(ElementsPlus.id("bitwise_rotate_analog"), Component.translatable("circuit.elements-plus.component.bitwise_rotate_analog"), null, ElementsPlus.id("textures/circuit/bitwise_rotate_analog.png")) {{
        setPin(Direction.WEST, 0, PinType.INPUT, "A", 8);
        setPin(Direction.NORTH, 0, PinType.INPUT, "B", 8);
        setPin(Direction.EAST, 0, PinType.OUTPUT, "Y", 8);
    }});
    // 高级元件
    public static final CircuitComponent ALU = register(new CircuitComponent(ElementsPlus.id("alu"), Component.translatable("circuit.elements-plus.component.alu"), null, ElementsPlus.id("textures/circuit/alu.png"), 1, 3) {{
        setPin(Direction.NORTH, 0, PinType.INPUT, "OP");
        setPin(Direction.WEST, 0, PinType.INPUT, "A", 8);
        setPin(Direction.WEST, 1, PinType.INPUT, "B", 8);
        setPin(Direction.WEST, 2, PinType.INPUT, "C", 8);
        setPin(Direction.EAST, 0, PinType.OUTPUT, "Y", 8);
        setPin(Direction.SOUTH, 0, PinType.OUTPUT, "CO");
    }});
    public static final CircuitComponent ALU_ADVANCED = register(new CircuitComponent(ElementsPlus.id("alu_advanced"), Component.translatable("circuit.elements-plus.component.alu_advanced"), null, ElementsPlus.id("textures/circuit/alu_advanced.png"), 1, 3) {{
        setPin(Direction.NORTH, 0, PinType.INPUT, "OP");
        setPin(Direction.WEST, 0, PinType.INPUT, "A", 8);
        setPin(Direction.WEST, 1, PinType.INPUT, "B", 8);
        setPin(Direction.WEST, 2, PinType.INPUT, "C", 8);
        setPin(Direction.EAST, 0, PinType.OUTPUT, "Y", 8);
        setPin(Direction.SOUTH, 0, PinType.OUTPUT, "CO");
    }});
    public static final CircuitComponent TFF = register(new CircuitComponent(ElementsPlus.id("tff"), Component.translatable("circuit.elements-plus.component.tff"), null, ElementsPlus.id("textures/circuit/tff.png"), 1, 1) {{
        setPin(Direction.WEST, 0, PinType.INPUT, "T");
        setPin(Direction.EAST, 0, PinType.OUTPUT, "Q");
    }});
    public static final CircuitComponent REGISTER = register(new CircuitComponent(ElementsPlus.id("register"), Component.translatable("circuit.elements-plus.component.register"), null, ElementsPlus.id("textures/circuit/register.png"), 1, 1) {{
        setPin(Direction.NORTH, 0, PinType.INPUT, "EN");
        setPin(Direction.WEST, 0, PinType.INPUT, "D");
        setPin(Direction.SOUTH, 0, PinType.INPUT, "RST");
        setPin(Direction.EAST, 0, PinType.OUTPUT, "Q");
    }});
    public static final CircuitComponent REGISTER_8 = register(new CircuitComponent(ElementsPlus.id("register_8"), Component.translatable("circuit.elements-plus.component.register_8"), null, ElementsPlus.id("textures/circuit/register_8.png"), 1, 1) {{
        setPin(Direction.NORTH, 0, PinType.INPUT, "EN");
        setPin(Direction.WEST, 0, PinType.INPUT, "D", 8);
        setPin(Direction.SOUTH, 0, PinType.INPUT, "RST");
        setPin(Direction.EAST, 0, PinType.OUTPUT, "Q", 8);
    }});
    public static final CircuitComponent RAM = register(new CircuitComponent(ElementsPlus.id("ram"), Component.translatable("circuit.elements-plus.component.ram"), Component.translatable("circuit.elements-plus.component.ram.description"), ElementsPlus.id("textures/circuit/ram.png"), 1, 2) {{
        setPin(Direction.NORTH, 0, PinType.INPUT, "WE");
        setPin(Direction.WEST, 0, PinType.INPUT, "D", 8);
        setPin(Direction.WEST, 1, PinType.INPUT, "A", 8);
        setPin(Direction.SOUTH, 0, PinType.INPUT, "CLK");
        setPin(Direction.EAST, 0, PinType.OUTPUT, "Q", 8);
    }});
    public static final CircuitComponent MULTIPLIER = register(new CircuitComponent(ElementsPlus.id("multiplier"), Component.translatable("circuit.elements-plus.component.multiplier"), null, ElementsPlus.id("textures/circuit/multiplier.png")));
    public static final CircuitComponent DIVIDER = register(new CircuitComponent(ElementsPlus.id("divider"), Component.translatable("circuit.elements-plus.component.divider"), null, ElementsPlus.id("textures/circuit/divider.png")));
    public static final CircuitComponent ANALOG_COUNTER = register(new CircuitComponent(ElementsPlus.id("analog_counter"), Component.translatable("circuit.elements-plus.component.analog_counter"), null, ElementsPlus.id("textures/circuit/analog_counter.png")));
    public static final CircuitComponent COUNTER_8 = register(new CircuitComponent(ElementsPlus.id("counter_8"), Component.translatable("circuit.elements-plus.component.counter_8"), null, ElementsPlus.id("textures/circuit/counter_8.png")));

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
                    .component(BITWISE_SHIFT)
                    .component(MULTIPLIER)
                    .component(REGISTER)
                    .component(COUNTER_8)
            );
}
