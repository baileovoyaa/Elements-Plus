package com.elementsplus.core.circuit;

import com.elementsplus.ModDataComponents;
import com.elementsplus.ModItems;
import com.elementsplus.core.circuit.diagram.CircuitDiagram;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 芯片制造共用逻辑（服务端制造校验/扣料与客户端原料统计共用）。
 */
public final class ChipManufacture {

    private ChipManufacture() {
    }

    /**
     * 单条原料需求：要么是某类物品，要么是某元件对应的芯片。
     */
    public sealed interface MaterialRequirement permits ItemRequirement, ComponentRequirement {
        int count();
    }

    public record ItemRequirement(ItemStack itemStack, int count) implements MaterialRequirement {
    }

    public record ComponentRequirement(CircuitComponent component, int count) implements MaterialRequirement {
    }

    /** 空芯片对应的电路图规模；不是空芯片时返回 null。 */
    public static CircuitDiagram.Scale emptyChipScale(ItemStack stack) {
        if (stack.isEmpty()) return null;
        Item item = stack.getItem();
        if (item == ModItems.SMALL_EMPTY_CHIP) return CircuitDiagram.Scale.SMALL;
        if (item == ModItems.MEDIUM_EMPTY_CHIP) return CircuitDiagram.Scale.MEDIUM;
        if (item == ModItems.LARGE_EMPTY_CHIP) return CircuitDiagram.Scale.LARGE;
        if (item == ModItems.HUGE_EMPTY_CHIP) return CircuitDiagram.Scale.HUGE;
        return null;
    }

    /** 规模对应的成品芯片；不在 SMALL..HUGE 时返回 null。 */
    public static Item chipItemForScale(CircuitDiagram.Scale scale) {
        return switch (scale) {
            case SMALL -> ModItems.SMALL_CHIP;
            case MEDIUM -> ModItems.MEDIUM_CHIP;
            case LARGE -> ModItems.LARGE_CHIP;
            case HUGE -> ModItems.HUGE_CHIP;
            default -> null;
        };
    }

    /** 是否是四种成品芯片之一。 */
    public static boolean isChipItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        Item item = stack.getItem();
        return item == ModItems.SMALL_CHIP || item == ModItems.MEDIUM_CHIP
                || item == ModItems.LARGE_CHIP || item == ModItems.HUGE_CHIP;
    }

    /** 空芯片规模能否容纳电路图规模；无限规模电路图不可制造。 */
    public static boolean canHoldScale(CircuitDiagram.Scale chipScale, CircuitDiagram.Scale diagramScale) {
        if (diagramScale == null || diagramScale.isInfinite()) return false;
        return chipScale.chunks >= diagramScale.chunks;
    }

    /**
     * 统计电路图的原料需求：
     * 元件读取 ingredientSupplier（ItemIngredientSupplier→物品、ComponentIngredientSupplier→元件、null→无），
     * 导线按有无对应侧向材料计 1 份，位宽为倍率。
     */
    public static List<MaterialRequirement> computeRequirements(CircuitDiagram diagram) {
        Map<Item, Integer> itemCounts = new LinkedHashMap<>();
        Map<ResourceLocation, Integer> componentCounts = new LinkedHashMap<>();

        for (CircuitDiagram.Chunk chunk : diagram.chunks.values()) {
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    CircuitDiagram.Wire wire = chunk.wires[x][y];
                    if (wire == null) continue;
                    int multiplier = Math.max(1, wire.bitWidth);
                    if (hasSide(wire, CircuitDiagram.Wire.WireMaterial.COPPER)) {
                        itemCounts.merge(ModItems.COPPER_WIRE, multiplier, Integer::sum);
                    }
                    if (hasSide(wire, CircuitDiagram.Wire.WireMaterial.GOLD)) {
                        itemCounts.merge(ModItems.GOLD_WIRE, multiplier, Integer::sum);
                    }
                }
            }
            for (CircuitDiagram.Component component : chunk.components) {
                CircuitComponent cc = component.component;
                if (cc == null) continue;
                CircuitComponent.IngredientSupplier supplier = cc.getIngredientSupplier();
                if (supplier instanceof CircuitComponent.ItemIngredientSupplier itemSupplier) {
                    for (ItemStack stack : itemSupplier.itemStacks()) {
                        if (stack.isEmpty()) continue;
                        itemCounts.merge(stack.getItem(), stack.getCount(), Integer::sum);
                    }
                } else if (supplier instanceof CircuitComponent.ComponentIngredientSupplier componentSupplier) {
                    ResourceLocation id = componentSupplier.component().getId();
                    if (id != null) componentCounts.merge(id, 1, Integer::sum);
                }
            }
        }

        List<MaterialRequirement> result = new ArrayList<>();
        itemCounts.forEach((item, count) -> result.add(new ItemRequirement(item.getDefaultInstance(), count)));
        componentCounts.forEach((id, count) -> {
            CircuitComponent cc = BuiltinCircuitComponents.byId(id);
            if (cc != null) result.add(new ComponentRequirement(cc, count));
        });
        return result;
    }

    private static boolean hasSide(CircuitDiagram.Wire wire, CircuitDiagram.Wire.WireMaterial material) {
        return wire.north == material || wire.east == material || wire.south == material || wire.west == material;
    }

    /** 统计物品栏中满足需求的数目。 */
    public static int countOwned(List<ItemStack> inventory, MaterialRequirement requirement) {
        int owned = 0;
        if (requirement instanceof ItemRequirement itemReq) {
            Item item = itemReq.itemStack().getItem();
            for (ItemStack stack : inventory) {
                if (!stack.isEmpty() && stack.is(item)) owned += stack.getCount();
            }
        } else if (requirement instanceof ComponentRequirement componentReq) {
            ResourceLocation id = componentReq.component().getId();
            for (ItemStack stack : inventory) {
                if (!isChipItem(stack)) continue;
                ResourceLocation equiv = stack.get(ModDataComponents.EQUIVALENT_COMPONENT);
                if (id != null && id.equals(equiv)) owned += stack.getCount();
            }
        }
        return owned;
    }

    /** 所有需求是否都满足。 */
    public static boolean allSufficient(List<ItemStack> inventory, List<MaterialRequirement> requirements) {
        for (MaterialRequirement requirement : requirements) {
            if (countOwned(inventory, requirement) < requirement.count()) return false;
        }
        return true;
    }

    /** 从容器中按需求扣料（尽力而为，数量不足时扣完为止）。 */
    public static void consume(Container container, MaterialRequirement requirement) {
        int remaining = requirement.count();
        if (remaining <= 0) return;
        if (requirement instanceof ItemRequirement itemReq) {
            Item item = itemReq.itemStack().getItem();
            for (int i = 0; i < container.getContainerSize() && remaining > 0; i++) {
                ItemStack stack = container.getItem(i);
                if (stack.isEmpty() || !stack.is(item)) continue;
                int take = Math.min(remaining, stack.getCount());
                stack.shrink(take);
                remaining -= take;
            }
        } else if (requirement instanceof ComponentRequirement componentReq) {
            ResourceLocation id = componentReq.component().getId();
            for (int i = 0; i < container.getContainerSize() && remaining > 0; i++) {
                ItemStack stack = container.getItem(i);
                if (!isChipItem(stack)) continue;
                ResourceLocation equiv = stack.get(ModDataComponents.EQUIVALENT_COMPONENT);
                if (id != null && id.equals(equiv)) {
                    stack.shrink(1);
                    remaining--;
                }
            }
        }
    }

    /** 生成成品芯片。 */
    public static ItemStack makeChip(CircuitDiagram.Scale chipScale, ResourceLocation equivalentComponent) {
        Item chipItem = chipItemForScale(chipScale);
        if (chipItem == null || equivalentComponent == null) return ItemStack.EMPTY;
        ItemStack stack = new ItemStack(chipItem);
        stack.set(ModDataComponents.EQUIVALENT_COMPONENT, equivalentComponent);
        return stack;
    }
}