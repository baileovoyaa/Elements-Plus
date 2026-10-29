package com.elementsplus.core.circuit;

import com.elementsplus.ModDataComponents;
import com.elementsplus.core.circuit.diagram.CircuitDiagram;
import com.elementsplus.core.circuit.storage.CircuitStorage;
import com.elementsplus.core.circuit.storage.StoredCircuit;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/** 电路图物品的数据访问：编辑中读组件，已编译读分离存储。 */
public final class CircuitItemData {
    private CircuitItemData() {
    }

    @Nullable
    public static UUID compiledId(ItemStack stack) {
        return stack == null || stack.isEmpty() ? null : stack.get(ModDataComponents.COMPILED_CIRCUIT);
    }

    @Nullable
    public static StoredCircuit stored(ItemStack stack) {
        return CircuitStorage.get().get(compiledId(stack));
    }

    @Nullable
    public static CircuitDiagram diagram(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        StoredCircuit record = stored(stack);
        if (record != null) {
            return record.diagram();
        }
        return stack.get(ModDataComponents.CIRCUIT_DIAGRAM);
    }

    public static boolean isCompiled(ItemStack stack) {
        return compiledId(stack) != null;
    }

    /** 可作为元件放置的等效元件 id：真实元件物品或已编译电路图。 */
    @Nullable
    public static ResourceLocation componentId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        ResourceLocation equivalent = stack.get(ModDataComponents.EQUIVALENT_COMPONENT);
        if (equivalent != null) {
            return equivalent;
        }
        StoredCircuit record = stored(stack);
        return record == null ? null : record.componentId();
    }

    /** 将已经保存到存储的电路写回物品，物品只保留 UUID 引用。 */
    public static void bindCompiled(ItemStack stack, StoredCircuit record) {
        stack.set(ModDataComponents.COMPILED_CIRCUIT, record.id());
        if (record.equivalentId() != null) {
            stack.set(ModDataComponents.EQUIVALENT_COMPONENT, record.equivalentId());
        } else {
            stack.remove(ModDataComponents.EQUIVALENT_COMPONENT);
        }
        stack.remove(ModDataComponents.CIRCUIT_DIAGRAM);
    }

    /** 复制编译状态：只复制引用。 */
    public static void copyCompiledReference(ItemStack source, ItemStack target) {
        UUID id = compiledId(source);
        if (id != null) {
            target.set(ModDataComponents.COMPILED_CIRCUIT, id);
            if (source.has(ModDataComponents.EQUIVALENT_COMPONENT)) {
                target.set(ModDataComponents.EQUIVALENT_COMPONENT, source.get(ModDataComponents.EQUIVALENT_COMPONENT));
            } else {
                target.remove(ModDataComponents.EQUIVALENT_COMPONENT);
            }
            target.remove(ModDataComponents.CIRCUIT_DIAGRAM);
        }
    }

    /** 将已编译电路复制成可编辑电路图。 */
    public static void copyToEditable(ItemStack source, ItemStack target) {
        CircuitDiagram diagram = diagram(source);
        target.remove(ModDataComponents.COMPILED_CIRCUIT);
        target.remove(ModDataComponents.EQUIVALENT_COMPONENT);
        if (diagram != null) {
            target.set(ModDataComponents.CIRCUIT_DIAGRAM, diagram.copy());
        }
    }
}
