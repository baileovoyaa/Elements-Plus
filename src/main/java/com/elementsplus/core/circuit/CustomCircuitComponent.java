package com.elementsplus.core.circuit;

import com.elementsplus.ElementsPlus;
import com.elementsplus.core.circuit.diagram.CircuitDiagram;
import com.elementsplus.core.circuit.storage.StoredCircuit;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * 从已编译存档电路恢复出来的自定义元件。元件本身不含电路副本的引用语义，
 * 但为了求值方便携带一个只读电路快照。
 */
public class CustomCircuitComponent extends CircuitComponent {

    private final StoredCircuit record;

    public CustomCircuitComponent(StoredCircuit record) {
        super(ElementsPlus.id("custom/" + record.id()),
                Component.literal(record.custom().name()).withStyle(ChatFormatting.ITALIC),
                record.custom().description() == null || record.custom().description().isBlank()
                        ? null : Component.literal(record.custom().description()),
                null,
                Math.clamp(record.custom().width(), 1, 16),
                Math.clamp(record.custom().height(), 1, 16));
        this.record = record;
        StoredCircuit.CustomInfo info = record.custom();
        for (StoredCircuit.PinBinding pin : info.pins()) {
            setPin(pin.side(), pin.offset(), pin.type(), pin.label(), pin.bitWidth());
        }
        setCustomMetadata(record.id(), record.author(), record.displayAuthor(),
                record.diagram().copy(), record.diagram().scale,
                info.color(), info.iconItem(), info.iconChar());
        setSelfAsIngredientSupplier();
    }

    public StoredCircuit getRecord() {
        return record;
    }

    public ResourceLocation getCircuitComponentId() {
        return record.componentId();
    }
}
