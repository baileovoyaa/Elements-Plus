package com.elementsplus.core.circuit;

import com.elementsplus.ElementsPlus;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BuiltinCircuitComponents {
    public static final CircuitComponent AND_GATE = new CircuitComponent(ElementsPlus.id("and_gate"), Component.nullToEmpty("与门"), Component.nullToEmpty("与门描述"), null, 1, 2);
    public static final CircuitComponent OR_GATE = new CircuitComponent(ElementsPlus.id("or_gate"), "或门");
    public static final CircuitComponent NOT_GATE = new CircuitComponent(ElementsPlus.id("not_gate"), "非门");
    public static final CircuitComponent ADD = new CircuitComponent(ElementsPlus.id("adder"), "加法器");
    public static final CircuitComponent BITWISE_MOVE = new CircuitComponent(ElementsPlus.id("bitwise_move"), "位移器");
    public static final CircuitComponent MULTIPLIER = new CircuitComponent(ElementsPlus.id("multiplier"), "乘法器");
    public static final CircuitComponent REGISTER = new CircuitComponent(ElementsPlus.id("register"), "寄存器");
    public static final CircuitComponent COUNTER = new CircuitComponent(ElementsPlus.id("counter"), "计数器");

    private static final Map<ResourceLocation, CircuitComponent> BY_ID = new HashMap<>();

    static {
        for (CircuitComponent component : List.of(AND_GATE, OR_GATE, NOT_GATE, ADD, BITWISE_MOVE, MULTIPLIER, REGISTER, COUNTER)) {
            BY_ID.put(component.getId(), component);
        }
    }

    public static CircuitComponent byId(ResourceLocation id) {
        return id == null ? null : BY_ID.get(id);
    }

    public static final CircuitComponentToolbox EXAMPLE_TOOLBOX = CircuitComponentToolbox.create()
            .category(CircuitComponentToolbox.Category.create("基本元件")
                    .component(AND_GATE)
                    .component(OR_GATE)
                    .component(NOT_GATE)
            )
            .category(CircuitComponentToolbox.Category.create("高级元件")
                    .component(ADD)
                    .component(BITWISE_MOVE)
                    .component(MULTIPLIER)
                    .component(REGISTER)
                    .component(COUNTER)
            );
}