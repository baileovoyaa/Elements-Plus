package com.elementsplus.core.circuit;

import com.elementsplus.core.circuit.storage.CircuitStorage;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 元件查找入口：先查内置元件，再查已编译的自定义元件。
 */
public final class CircuitComponentRegistry {
    private CircuitComponentRegistry() {
    }

    @Nullable
    public static CircuitComponent byId(@Nullable ResourceLocation id) {
        CircuitComponent builtin = BuiltinCircuitComponents.byId(id);
        if (builtin != null) {
            return builtin;
        }
        return CircuitStorage.get().customComponent(id);
    }

    public static Collection<CircuitComponent> getAll() {
        List<CircuitComponent> result = new ArrayList<>(BuiltinCircuitComponents.getAll());
        result.addAll(CircuitStorage.get().customComponents());
        return result;
    }
}
