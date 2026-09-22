package com.elementsplus.core.circuit;

import com.elementsplus.ElementsPlus;
import com.elementsplus.ModItems;
import com.elementsplus.core.circuit.component.*;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BuiltinCircuitComponents {
    private static final Map<ResourceLocation, CircuitComponent> BY_ID = new HashMap<>();

    // 基本元件
    public static final CircuitComponent TRANSISTOR = register(new CircuitComponent(ElementsPlus.id("transistor"), Component.translatable("circuit.elements-plus.component.transistor"), Component.translatable("circuit.elements-plus.component.transistor.description"), ElementsPlus.id("textures/item/amethyst_transistor.png"), 1, 1) {{
        setPin(Direction.WEST, 0, PinType.INPUT);
        setPin(Direction.SOUTH, 0, PinType.INPUT);
        setPin(Direction.EAST, 0, PinType.OUTPUT);
    }});
    public static final CircuitComponent DIODE = register(new CircuitComponent(ElementsPlus.id("diode"), Component.translatable("circuit.elements-plus.component.diode"), Component.translatable("circuit.elements-plus.component.diode.description"), ElementsPlus.id("textures/item/amethyst_diode.png"), 1, 1) {{
        setPin(Direction.WEST, 0, PinType.INPUT);
        setPin(Direction.EAST, 0, PinType.OUTPUT);
    }});
    public static final CircuitComponent CAPACITOR = register(new CircuitComponent(ElementsPlus.id("capacitor"), Component.translatable("circuit.elements-plus.component.capacitor"), Component.translatable("circuit.elements-plus.component.capacitor.description"), ElementsPlus.id("textures/item/amethyst_capacitor.png"), 1, 1) {{
        setPin(Direction.WEST, 0, PinType.INPUT);
        setPin(Direction.EAST, 0, PinType.OUTPUT);
        setInstanceFactory(CapacitorComponentInstance::new);
    }});
    public static final CircuitComponent RESISTOR = register(new CircuitComponent(ElementsPlus.id("resistor"), Component.translatable("circuit.elements-plus.component.resistor"), Component.translatable("circuit.elements-plus.component.resistor.description"), ElementsPlus.id("textures/item/amethyst_resistor.png"), 1, 1) {{
        setPin(Direction.WEST, 0, PinType.INPUT);
        setPin(Direction.EAST, 0, PinType.OUTPUT);
        setInstanceFactory(ResistorComponentInstance::new);
    }});
    public static final CircuitComponent RESONATOR = register(new CircuitComponent(ElementsPlus.id("resonator"), Component.translatable("circuit.elements-plus.component.resonator"), Component.translatable("circuit.elements-plus.component.resonator.description"), ElementsPlus.id("textures/item/amethyst_resonator.png"), 1, 1) {{
        setPin(Direction.WEST, 0, PinType.INPUT);
        setPin(Direction.EAST, 0, PinType.OUTPUT);
        setInstanceFactory(ResonatorComponentInstance::new);
    }});
    public static final CircuitComponent BATTERY = register(new CircuitComponent(ElementsPlus.id("battery"), Component.translatable("circuit.elements-plus.component.battery"), Component.translatable("circuit.elements-plus.component.battery.description"), ElementsPlus.id("textures/item/amethyst_battery.png"), 1, 1) {{
        setPin(Direction.NORTH, 0, PinType.OUTPUT);
    }});
    // 模拟
    public static final CircuitComponent AMPLIFIER = register(new CircuitComponent(ElementsPlus.id("amplifier"), Component.translatable("circuit.elements-plus.component.amplifier"), Component.translatable("circuit.elements-plus.component.amplifier.description"), null, 1, 1) {{
        setPin(Direction.WEST, 0, PinType.INPUT);
        setPin(Direction.EAST, 0, PinType.OUTPUT);
    }});
    // 输入输出
    public static final CircuitComponent INPUT = register(new CircuitComponent(ElementsPlus.id("input"), Component.translatable("circuit.elements-plus.component.input"), Component.translatable("circuit.elements-plus.component.input.description"), ElementsPlus.id("textures/circuit/input.png"), 1, 1) {{
        setPin(Direction.EAST, 0, PinType.OUTPUT);
        setInstanceFactory(InputComponentInstance::new);
        clearIngredientSupplier();
    }});
    public static final CircuitComponent OUTPUT = register(new CircuitComponent(ElementsPlus.id("output"), Component.translatable("circuit.elements-plus.component.output"), Component.translatable("circuit.elements-plus.component.output.description"), ElementsPlus.id("textures/circuit/output.png"), 1, 1) {{
        setPin(Direction.WEST, 0, PinType.INPUT);
        setInstanceFactory(OutputComponentInstance::new);
        clearIngredientSupplier();
    }});
    // 逻辑门
    public static final CircuitComponent AND_GATE = register(new CircuitComponent(ElementsPlus.id("and_gate"), Component.nullToEmpty("与门"), Component.nullToEmpty("与门描述"), ElementsPlus.id("textures/circuit/and_gate.png"), 1, 2) {{
        setPin(Direction.WEST, 0, PinType.INPUT);
        setPin(Direction.WEST, 1, PinType.INPUT);
        setPin(Direction.EAST, 0, PinType.OUTPUT);
    }});
    public static final CircuitComponent OR_GATE = register(new CircuitComponent(ElementsPlus.id("or_gate"), "或门"));
    public static final CircuitComponent NOT_GATE = register(new CircuitComponent(ElementsPlus.id("not_gate"), "非门"));
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

    // 高级元件
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
    public static final CircuitComponent BITWISE_MOVE = register(new CircuitComponent(ElementsPlus.id("bitwise_move"), "位移器"));
    public static final CircuitComponent MULTIPLIER = register(new CircuitComponent(ElementsPlus.id("multiplier"), "乘法器"));
    public static final CircuitComponent REGISTER = register(new CircuitComponent(ElementsPlus.id("register"), "寄存器"));
    public static final CircuitComponent COUNTER = register(new CircuitComponent(ElementsPlus.id("counter"), "计数器"));

    public static CircuitComponent register(CircuitComponent circuitComponent) {
        BY_ID.put(circuitComponent.getId(), circuitComponent);
        return circuitComponent;
    }

    public static CircuitComponent byId(ResourceLocation id) {
        return id == null ? null : BY_ID.get(id);
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
                    .component(COUNTER)
            );
}