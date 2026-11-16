package com.elementsplus.core.circuit;

import com.elementsplus.ModItems;
import com.elementsplus.core.circuit.diagram.CircuitDiagram;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 电路板编辑/制造共用的材料差异计算。正向数量表示需要从玩家那里扣除，
 * 负向数量表示需要返还给玩家。
 */
public final class CircuitBoardCrafting {
    private CircuitBoardCrafting() {
    }

    public record Change(ChipManufacture.MaterialRequirement requirement, int delta) {
        public boolean added() {
            return delta > 0;
        }
    }

    public static List<ChipManufacture.MaterialRequirement> requirements(CircuitDiagram diagram) {
        return diagram == null ? List.of() : ChipManufacture.computeRequirements(diagram);
    }

    public static List<Change> diff(CircuitDiagram oldDiagram, CircuitDiagram newDiagram) {
        Map<String, Entry> old = counts(oldDiagram);
        Map<String, Entry> now = counts(newDiagram);
        List<Change> result = new ArrayList<>();
        for (String key : union(old, now)) {
            Entry before = old.get(key);
            Entry after = now.get(key);
            int delta = (after == null ? 0 : after.count) - (before == null ? 0 : before.count);
            if (delta == 0) continue;
            ChipManufacture.MaterialRequirement requirement = delta > 0
                    ? after.requirement : before.requirement;
            result.add(new Change(requirement, delta));
        }
        return result;
    }

    public static boolean canApplyAdditions(Player player, List<Change> changes) {
        List<ItemStack> inventory = player.getInventory().items;
        ItemStack carried = player.containerMenu == null ? ItemStack.EMPTY : player.containerMenu.getCarried();
        for (Change change : changes) {
            if (change.delta > 0) {
                int owned = ChipManufacture.countOwned(inventory, change.requirement)
                        + countInStack(carried, change.requirement);
                if (owned < change.delta) return false;
            }
        }
        return true;
    }

    public static boolean canApply(Player player, List<Change> changes) {
        List<ItemStack> inventory = player.getInventory().items;
        ItemStack carried = player.containerMenu == null ? ItemStack.EMPTY : player.containerMenu.getCarried();
        for (Change change : changes) {
            if (change.delta > 0) {
                int owned = ChipManufacture.countOwned(inventory, change.requirement)
                        + countInStack(carried, change.requirement);
                if (owned < change.delta) return false;
            }
        }
        List<ItemStack> simulated = new ArrayList<>(inventory.size());
        for (ItemStack stack : inventory) {
            simulated.add(stack.copy());
        }
        ItemStack simulatedCarried = carried.copy();
        // 先模拟扣除新增材料，释放出的空间才能用于返还被移除的材料。
        for (Change change : changes) {
            if (change.delta > 0) {
                int remaining = consumeFromList(simulated, change.requirement, change.delta);
                if (remaining > 0) consumeFromStack(simulatedCarried, change.requirement, remaining);
            }
        }
        for (Change change : changes) {
            if (change.delta < 0) {
                for (ItemStack stack : refundStacks(change.requirement, -change.delta)) {
                    if (!simulateInsert(simulated, stack)) return false;
                }
            }
        }
        return true;
    }

    public static boolean apply(Player player, List<Change> changes) {
        if (!canApply(player, changes)) return false;
        Inventory inventory = player.getInventory();
        ItemStack carried = player.containerMenu == null ? ItemStack.EMPTY : player.containerMenu.getCarried();
        for (Change change : changes) {
            if (change.delta > 0) {
                int remaining = change.delta;
                remaining = consumeFromList(inventory.items, change.requirement, remaining);
                if (remaining > 0) {
                    consumeFromStack(carried, change.requirement, remaining);
                }
            }
        }
        for (Change change : changes) {
            if (change.delta < 0) {
                for (ItemStack stack : refundStacks(change.requirement, -change.delta)) {
                    if (!inventory.add(stack)) {
                        player.drop(stack, false);
                    }
                }
            }
        }
        if (player.containerMenu != null) {
            player.containerMenu.setCarried(carried);
        }
        return true;
    }

    /** 制造差异：先尝试返还到物品栏，放不下的部分丢到地上。 */
    public static boolean applyWithOverflowRefunds(Player player, List<Change> changes) {
        Inventory inventory = player.getInventory();
        ItemStack carried = player.containerMenu == null ? ItemStack.EMPTY : player.containerMenu.getCarried();
        for (Change change : changes) {
            if (change.delta > 0) {
                int owned = ChipManufacture.countOwned(inventory.items, change.requirement)
                        + countInStack(carried, change.requirement);
                if (owned < change.delta) return false;
            }
        }
        for (Change change : changes) {
            if (change.delta > 0) {
                int remaining = change.delta;
                remaining = consumeFromList(inventory.items, change.requirement, remaining);
                if (remaining > 0) consumeFromStack(carried, change.requirement, remaining);
            } else {
                for (ItemStack stack : refundStacks(change.requirement, -change.delta)) {
                    if (!inventory.add(stack)) player.drop(stack, false);
                }
            }
        }
        if (player.containerMenu != null) player.containerMenu.setCarried(carried);
        return true;
    }

    public static boolean applyDroppingRefunds(Player player, List<Change> changes) {
        Inventory inventory = player.getInventory();
        ItemStack carried = player.containerMenu == null ? ItemStack.EMPTY : player.containerMenu.getCarried();
        for (Change change : changes) {
            if (change.delta > 0) {
                int owned = ChipManufacture.countOwned(inventory.items, change.requirement)
                        + countInStack(carried, change.requirement);
                if (owned < change.delta) return false;
            }
        }
        for (Change change : changes) {
            if (change.delta > 0) {
                int remaining = change.delta;
                remaining = consumeFromList(inventory.items, change.requirement, remaining);
                if (remaining > 0) consumeFromStack(carried, change.requirement, remaining);
            } else {
                for (ItemStack stack : refundStacks(change.requirement, -change.delta)) {
                    player.drop(stack, false);
                }
            }
        }
        if (player.containerMenu != null) player.containerMenu.setCarried(carried);
        return true;
    }

    public static ItemStack boardOutput(CircuitDiagram diagram) {
        ItemStack stack = new ItemStack(ModItems.CIRCUIT_BOARD);
        stack.set(com.elementsplus.ModDataComponents.CIRCUIT_DIAGRAM,
                diagram == null ? CircuitDiagram.EMPTY.copy() : diagram.copy());
        return stack;
    }

    private record Entry(ChipManufacture.MaterialRequirement requirement, int count) {
    }

    private static Map<String, Entry> counts(CircuitDiagram diagram) {
        Map<String, Entry> result = new LinkedHashMap<>();
        for (ChipManufacture.MaterialRequirement requirement : requirements(diagram)) {
            String key = key(requirement);
            Entry entry = result.get(key);
            if (entry == null) {
                result.put(key, new Entry(requirement, requirement.count()));
            } else {
                result.put(key, new Entry(entry.requirement, entry.count + requirement.count()));
            }
        }
        return result;
    }

    private static List<String> union(Map<String, Entry> a, Map<String, Entry> b) {
        List<String> keys = new ArrayList<>(a.keySet());
        for (String key : b.keySet()) {
            if (!keys.contains(key)) keys.add(key);
        }
        return keys;
    }

    private static String key(ChipManufacture.MaterialRequirement requirement) {
        if (requirement instanceof ChipManufacture.ItemRequirement itemRequirement) {
            return "item:" + BuiltInRegistries.ITEM.getKey(itemRequirement.itemStack().getItem());
        }
        if (requirement instanceof ChipManufacture.ComponentRequirement componentRequirement) {
            return "component:" + componentRequirement.component().getId()
                    + ":" + componentRequirement.circuitId();
        }
        return requirement.toString();
    }

    private static int countInStack(ItemStack stack, ChipManufacture.MaterialRequirement requirement) {
        if (stack.isEmpty()) return 0;
        if (requirement instanceof ChipManufacture.ItemRequirement itemRequirement) {
            return stack.is(itemRequirement.itemStack().getItem()) ? stack.getCount() : 0;
        }
        if (requirement instanceof ChipManufacture.ComponentRequirement componentRequirement) {
            if (!ChipManufacture.isChipItem(stack)) return 0;
            if (componentRequirement.circuitId() != null) {
                return componentRequirement.circuitId().equals(stack.get(com.elementsplus.ModDataComponents.COMPILED_CIRCUIT))
                        ? stack.getCount() : 0;
            }
            return componentRequirement.component().getId() != null
                    && componentRequirement.component().getId().equals(
                    stack.get(com.elementsplus.ModDataComponents.EQUIVALENT_COMPONENT))
                    ? stack.getCount() : 0;
        }
        return 0;
    }

    private static int consumeFromList(List<ItemStack> stacks, ChipManufacture.MaterialRequirement requirement,
                                       int remaining) {
        for (ItemStack stack : stacks) {
            if (remaining <= 0) break;
            int count = countInStack(stack, requirement);
            if (count <= 0) continue;
            int take = Math.min(remaining, count);
            stack.shrink(take);
            remaining -= take;
        }
        return remaining;
    }

    private static void consumeFromStack(ItemStack stack, ChipManufacture.MaterialRequirement requirement,
                                         int remaining) {
        int count = countInStack(stack, requirement);
        if (count > 0) {
            stack.shrink(Math.min(remaining, count));
        }
    }

    private static List<ItemStack> refundStacks(ChipManufacture.MaterialRequirement requirement, int count) {
        List<ItemStack> result = new ArrayList<>();
        if (requirement instanceof ChipManufacture.ItemRequirement itemRequirement) {
            int remaining = count;
            int max = Math.max(1, itemRequirement.itemStack().getMaxStackSize());
            while (remaining > 0) {
                int take = Math.min(max, remaining);
                ItemStack stack = itemRequirement.itemStack().copy();
                stack.setCount(take);
                result.add(stack);
                remaining -= take;
            }
        } else if (requirement instanceof ChipManufacture.ComponentRequirement componentRequirement) {
            CircuitDiagram.Scale chipScale = componentRequirement.component().getCustomScale() == null
                    ? CircuitDiagram.Scale.SMALL
                    : componentRequirement.component().getCustomScale();
            ItemStack stack = ChipManufacture.makeChip(chipScale,
                    componentRequirement.component().getId(), componentRequirement.circuitId());
            if (!stack.isEmpty()) {
                for (int i = 0; i < count; i++) result.add(stack.copy());
            }
        }
        return result;
    }

    private static boolean simulateInsert(List<ItemStack> stacks, ItemStack stack) {
        ItemStack remaining = stack.copy();
        int max = Math.max(1, remaining.getMaxStackSize());
        for (int i = 0; i < stacks.size() && !remaining.isEmpty(); i++) {
            ItemStack existing = stacks.get(i);
            if (!existing.isEmpty() && ItemStack.isSameItemSameComponents(existing, remaining)
                    && existing.getCount() < max) {
                int move = Math.min(max - existing.getCount(), remaining.getCount());
                existing.grow(move);
                remaining.shrink(move);
            }
        }
        for (int i = 0; i < stacks.size() && !remaining.isEmpty(); i++) {
            if (stacks.get(i).isEmpty()) {
                int move = Math.min(max, remaining.getCount());
                ItemStack copy = remaining.copy();
                copy.setCount(move);
                stacks.set(i, copy);
                remaining.shrink(move);
            }
        }
        return remaining.isEmpty();
    }
}
