package com.elementsplus.core.circuit;

import net.minecraft.network.chat.Component;

public class BuiltinCircuitComponents {
    public static final CircuitComponent AND_GATE = new CircuitComponent(Component.nullToEmpty("与门"), Component.nullToEmpty("与门描述"), null, 1, 2);
    public static final CircuitComponent OR_GATE = new CircuitComponent("或门");
    public static final CircuitComponent NOT_GATE = new CircuitComponent("非门");
    public static final CircuitComponent ADD = new CircuitComponent("加法器");
    public static final CircuitComponent BITWISE_MOVE = new CircuitComponent("位移器");
    public static final CircuitComponent MULTIPLIER = new CircuitComponent("乘法器");
    public static final CircuitComponent REGISTER = new CircuitComponent("寄存器");
    public static final CircuitComponent COUNTER = new CircuitComponent("计数器");

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
