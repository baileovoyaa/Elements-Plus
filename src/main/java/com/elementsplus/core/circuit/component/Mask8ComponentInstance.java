package com.elementsplus.core.circuit.component;

/**
 * 8 位掩码的位配置，语义与 8 位常量相同；输出时把通道强度替换为输入 A 的强度。
 */
public class Mask8ComponentInstance extends Const8ComponentInstance {

    public Mask8ComponentInstance() {
        super();
    }

    public Mask8ComponentInstance(Mask8ComponentInstance source) {
        super(source);
    }

    @Override
    protected CircuitComponentInstance createEmptyCopy() {
        return new Mask8ComponentInstance();
    }
}
