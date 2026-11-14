package com.elementsplus.core.circuit;

import com.elementsplus.core.circuit.component.Input8ComponentInstance;
import com.elementsplus.core.circuit.component.InputComponentInstance;
import com.elementsplus.core.circuit.component.Output8ComponentInstance;
import com.elementsplus.core.circuit.component.OutputComponentInstance;
import com.elementsplus.core.circuit.diagram.CircuitDiagram;
import net.minecraft.core.Direction;

/**
 * 把单个等效元件包装成一张只含该元件和外部引脚的电路图。
 * <p>
 * 创造模式元件、内置元件物品和紫水晶元件本身没有编译后的电路图，
 * 但它们带有一个等效元件 id。这里把该元件放在电路图中央，并把元件的
 * 每个输入/输出引脚通过一小段导线接到一个 INPUT/OUTPUT 元件上，
 * 这样红石接口就可以复用现有的电路模拟器和引脚分配 UI。
 * 元件的配置全部使用实例默认值。
 */
public final class CreativeChipCircuit {
    private static final int TARGET_X = 8;
    private static final int TARGET_Y = 8;

    private CreativeChipCircuit() {
    }

    public static CircuitDiagram wrap(CircuitComponent component) {
        if (component == null) {
            return null;
        }
        int width = component.getWidth();
        int height = component.getHeight();
        // 给外部引脚留出 2 格空间，同时避免越过小型电路图的范围。
        if (width <= 0 || height <= 0 || width > 8 || height > 8) {
            return null;
        }

        CircuitDiagram diagram = new CircuitDiagram();
        CircuitDiagram.Component target = new CircuitDiagram.Component(component, Direction.NORTH);
        if (!diagram.setBlock(TARGET_X, TARGET_Y, target)) {
            return null;
        }

        for (Direction side : new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
            int count = side == Direction.NORTH || side == Direction.SOUTH ? width : height;
            for (int offset = 0; offset < count; offset++) {
                CircuitComponent.PinType type = component.getPin(side, offset);
                if (type == null || type == CircuitComponent.PinType.NONE) {
                    continue;
                }
                int pinWidth = component.getPinBitWidth(side, offset) >= 8 ? 8 : 1;
                int ex = externalX(side, offset, width, height);
                int ey = externalY(side, offset, width, height);

                // 导线只向元件和外侧包装元件两个方向延伸，避免连接到无关格子。
                CircuitDiagram.Wire wire = switch (side) {
                    case NORTH, SOUTH -> new CircuitDiagram.Wire(
                            CircuitDiagram.Wire.WireMaterial.COPPER, null,
                            CircuitDiagram.Wire.WireMaterial.COPPER, null, pinWidth);
                    default -> new CircuitDiagram.Wire(
                            null, CircuitDiagram.Wire.WireMaterial.COPPER,
                            null, CircuitDiagram.Wire.WireMaterial.COPPER, pinWidth);
                };
                if (!diagram.setBlock(ex, ey, wire)) {
                    return null;
                }

                int wx = ex + side.getStepX();
                int wy = ey + side.getStepZ();
                boolean input = type == CircuitComponent.PinType.INPUT;
                CircuitComponent wrapperComponent = input
                        ? (pinWidth == 8 ? BuiltinCircuitComponents.INPUT_8 : BuiltinCircuitComponents.INPUT)
                        : (pinWidth == 8 ? BuiltinCircuitComponents.OUTPUT_8 : BuiltinCircuitComponents.OUTPUT);
                Direction wrapperDirection = input
                        ? directionForOutputPin(side.getOpposite())
                        : directionForInputPin(side.getOpposite());
                CircuitDiagram.Component wrapper = new CircuitDiagram.Component(wrapperComponent, wrapperDirection);
                String name = component.getPinName(side, offset);
                name = name == null ? "" : name;
                if (wrapper.instance instanceof InputComponentInstance inputInstance) {
                    inputInstance.setLabel(name);
                } else if (wrapper.instance instanceof Input8ComponentInstance inputInstance) {
                    inputInstance.setLabel(name);
                } else if (wrapper.instance instanceof OutputComponentInstance outputInstance) {
                    outputInstance.setLabel(name);
                } else if (wrapper.instance instanceof Output8ComponentInstance outputInstance) {
                    outputInstance.setLabel(name);
                }
                if (!diagram.setBlock(wx, wy, wrapper)) {
                    return null;
                }
            }
        }
        return diagram;
    }

    private static int externalX(Direction side, int offset, int width, int height) {
        return switch (side) {
            case NORTH, SOUTH -> TARGET_X + (side == Direction.SOUTH ? width - 1 - offset : offset);
            case WEST -> TARGET_X - 1;
            default -> TARGET_X + width;
        };
    }

    private static int externalY(Direction side, int offset, int width, int height) {
        return switch (side) {
            case NORTH -> TARGET_Y - 1;
            case SOUTH -> TARGET_Y + height;
            case WEST -> TARGET_Y + height - 1 - offset;
            default -> TARGET_Y + offset;
        };
    }

    private static Direction directionForOutputPin(Direction desiredSide) {
        // 输入元件默认输出引脚在 EAST。
        for (Direction rotation : new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
            if (CircuitComponent.rotatedPinSide(1, 1, rotation, false, Direction.EAST, 0) == desiredSide) {
                return rotation;
            }
        }
        return Direction.NORTH;
    }

    private static Direction directionForInputPin(Direction desiredSide) {
        // 输出元件默认输入引脚在 WEST。
        for (Direction rotation : new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
            if (CircuitComponent.rotatedPinSide(1, 1, rotation, false, Direction.WEST, 0) == desiredSide) {
                return rotation;
            }
        }
        return Direction.NORTH;
    }
}
