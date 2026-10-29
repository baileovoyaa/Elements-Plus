package com.elementsplus.core.circuit;

import com.elementsplus.ElementsPlus;
import com.elementsplus.core.circuit.component.Input8ComponentInstance;
import com.elementsplus.core.circuit.component.InputComponentInstance;
import com.elementsplus.core.circuit.component.Output8ComponentInstance;
import com.elementsplus.core.circuit.component.OutputComponentInstance;
import com.elementsplus.core.circuit.diagram.CircuitDiagram;
import com.elementsplus.core.circuit.storage.CircuitStorage;
import com.elementsplus.core.circuit.storage.StoredCircuit;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** 自定义元件的引脚分析、自动布局和编译校验。 */
public final class CustomComponentCompiler {
    private CustomComponentCompiler() {
    }

    public record PinCandidate(String label, CircuitComponent.PinType type, int bitWidth, int x, int y,
                               boolean duplicate) {
    }

    public record Layout(int width, int height, List<StoredCircuit.PinBinding> pins) {
    }

    public static List<PinCandidate> candidates(CircuitDiagram diagram) {
        List<PinCandidate> raw = new ArrayList<>();
        Map<String, Integer> counts = new HashMap<>();
        for (CircuitDiagram.Chunk chunk : diagram.chunks.values()) {
            for (CircuitDiagram.Component component : chunk.components) {
                String label = null;
                CircuitComponent.PinType type = null;
                int bitWidth = 1;
                if (component.component == BuiltinCircuitComponents.INPUT
                        && component.instance instanceof InputComponentInstance input) {
                    label = input.getLabel();
                    type = CircuitComponent.PinType.INPUT;
                } else if (component.component == BuiltinCircuitComponents.INPUT_8
                        && component.instance instanceof Input8ComponentInstance input8) {
                    label = input8.getLabel();
                    type = CircuitComponent.PinType.INPUT;
                    bitWidth = 8;
                } else if (component.component == BuiltinCircuitComponents.OUTPUT
                        && component.instance instanceof OutputComponentInstance output) {
                    label = output.getLabel();
                    type = CircuitComponent.PinType.OUTPUT;
                } else if (component.component == BuiltinCircuitComponents.OUTPUT_8
                        && component.instance instanceof Output8ComponentInstance output8) {
                    label = output8.getLabel();
                    type = CircuitComponent.PinType.OUTPUT;
                    bitWidth = 8;
                }
                if (label == null || label.isBlank() || "VCC".equals(label)) {
                    continue;
                }
                counts.merge(label, 1, Integer::sum);
                raw.add(new PinCandidate(label, type, bitWidth, component.x, component.y, false));
            }
        }
        List<PinCandidate> result = new ArrayList<>();
        for (PinCandidate pin : raw) {
            result.add(new PinCandidate(pin.label(), pin.type(), pin.bitWidth(), pin.x(), pin.y(),
                    counts.getOrDefault(pin.label(), 0) > 1));
        }
        return result;
    }

    public static Layout autoLayout(CircuitDiagram diagram) {
        List<PinCandidate> pins = candidates(diagram).stream().filter(pin -> !pin.duplicate())
                .sorted(Comparator.comparingInt(PinCandidate::y).thenComparingInt(PinCandidate::x))
                .toList();
        if (pins.isEmpty()) {
            return new Layout(1, 1, List.of());
        }
        int size = 1;
        while (size < 16 && size * 4 < pins.size()) {
            size++;
        }
        if (size * 4 < pins.size()) {
            return new Layout(1, 1, List.of());
        }

        List<List<PinCandidate>> sides = new ArrayList<>();
        for (int i = 0; i < 4; i++) sides.add(new ArrayList<>());
        int centerX2 = 0, centerY2 = 0;
        for (PinCandidate pin : pins) {
            centerX2 += pin.x();
            centerY2 += pin.y();
        }
        double cx = (double) centerX2 / pins.size();
        double cy = (double) centerY2 / pins.size();
        for (PinCandidate pin : pins) {
            double dx = pin.x() + 0.5 - cx;
            double dy = pin.y() + 0.5 - cy;
            int preferred = Math.abs(dx) >= Math.abs(dy)
                    ? (dx >= 0 ? 1 : 3) // EAST / WEST
                    : (dy >= 0 ? 2 : 0); // SOUTH / NORTH
            int side = preferred;
            for (int i = 0; i < 4; i++) {
                int candidate = (preferred + i) % 4;
                if (sides.get(candidate).size() < size) {
                    side = candidate;
                    break;
                }
            }
            sides.get(side).add(pin);
        }

        List<StoredCircuit.PinBinding> bindings = new ArrayList<>();
        for (int side = 0; side < 4; side++) {
            List<PinCandidate> list = sides.get(side);
            if (side == 0 || side == 2) {
                list.sort(Comparator.comparingInt(PinCandidate::x));
            } else {
                list.sort(Comparator.comparingInt(PinCandidate::y));
            }
            Direction direction = switch (side) {
                case 0 -> Direction.NORTH;
                case 1 -> Direction.EAST;
                case 2 -> Direction.SOUTH;
                default -> Direction.WEST;
            };
            for (int offset = 0; offset < list.size(); offset++) {
                PinCandidate pin = list.get(offset);
                bindings.add(new StoredCircuit.PinBinding(direction, offset, pin.type(), pin.bitWidth(), pin.label()));
            }
        }
        return new Layout(size, size, bindings);
    }

    public static List<Component> validate(CircuitDiagram diagram, String name, int width, int height,
                                           List<StoredCircuit.PinBinding> pins) {
        List<Component> reasons = new ArrayList<>();
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty()) {
            reasons.add(Component.translatable("gui.elements-plus.custom_component.error.name_empty"));
        } else {
            ResourceLocation candidateId = ResourceLocation.tryParse(ElementsPlus.MOD_ID + ":" + trimmed);
            if (candidateId != null && BuiltinCircuitComponents.byId(candidateId) != null) {
                reasons.add(Component.translatable("gui.elements-plus.custom_component.error.name_reserved"));
            }
        }
        if (width < 1 || width > 16 || height < 1 || height > 16) {
            reasons.add(Component.translatable("gui.elements-plus.custom_component.error.size"));
            return reasons;
        }
        if (diagram == null) {
            reasons.add(Component.translatable("experiment.elements-plus.precheck.not_diagram"));
            return reasons;
        }

        List<PinCandidate> candidates = candidates(diagram);
        Map<String, PinCandidate> candidateByLabel = new HashMap<>();
        for (PinCandidate pin : candidates) {
            if (!pin.duplicate()) candidateByLabel.put(pin.label(), pin);
        }
        Set<String> assignedLabels = new HashSet<>();
        Set<String> occupied = new HashSet<>();
        for (StoredCircuit.PinBinding pin : pins) {
            if (pin.label() == null || pin.label().isBlank()) {
                reasons.add(Component.translatable("gui.elements-plus.custom_component.error.empty_pin"));
                continue;
            }
            PinCandidate candidate = candidateByLabel.get(pin.label());
            if (candidate == null) {
                reasons.add(Component.translatable("gui.elements-plus.custom_component.error.duplicate_pin", pin.label()));
                continue;
            }
            if (candidate.type() != pin.type() || candidate.bitWidth() != pin.bitWidth()) {
                reasons.add(Component.translatable("gui.elements-plus.custom_component.error.pin_type", pin.label()));
                continue;
            }
            if (!validPosition(pin.side(), pin.offset(), width, height)) {
                reasons.add(Component.translatable("gui.elements-plus.custom_component.error.pin_position", pin.label()));
                continue;
            }
            if (!assignedLabels.add(pin.label()) || !occupied.add(pin.side().ordinal() + ":" + pin.offset())) {
                reasons.add(Component.translatable("gui.elements-plus.custom_component.error.pin_overlap", pin.label()));
            }
        }

        for (CircuitDiagram.Chunk chunk : diagram.chunks.values()) {
            for (CircuitDiagram.Component component : chunk.components) {
                if (!component.component.isCustom() || component.component.getCustomScale() == null) {
                    continue;
                }
                CircuitDiagram.Scale child = component.component.getCustomScale();
                if (!diagram.scale.isInfinite()
                        && (child.isInfinite() || child.chunks >= diagram.scale.chunks)) {
                    reasons.add(Component.translatable("gui.elements-plus.custom_component.error.nested_scale"));
                    break;
                }
            }
        }

        CircuitSimulator simulator = new CircuitSimulator();
        simulator.setDiagram(diagram.copy());
        if (simulator.hasCycle()) {
            reasons.add(Component.translatable("experiment.elements-plus.precheck.cycle"));
        }
        return reasons;
    }

    public static StoredCircuit compile(CircuitDiagram diagram, String name, String description,
                                        @Nullable ResourceLocation iconItem, @Nullable String iconChar,
                                        int color, int width, int height,
                                        List<StoredCircuit.PinBinding> pins, UUID author, String authorName) {
        UUID id = UUID.randomUUID();
        StoredCircuit.CustomInfo info = new StoredCircuit.CustomInfo(name.trim(),
                description == null ? "" : description, iconItem, iconChar,
                color, Math.clamp(width, 1, 16), Math.clamp(height, 1, 16), new ArrayList<>(pins));
        StoredCircuit record = new StoredCircuit(id, diagram.copy(), null, info, author,
                authorName == null || authorName.isBlank() ? author.toString() : authorName);
        CircuitStorage.get().put(record);
        return record;
    }

    private static boolean validPosition(Direction side, int offset, int width, int height) {
        return switch (side) {
            case NORTH, SOUTH -> offset >= 0 && offset < width;
            case EAST, WEST -> offset >= 0 && offset < height;
            default -> false;
        };
    }
}
