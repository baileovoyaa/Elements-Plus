package com.elementsplus.client;

import com.elementsplus.ModBlocks;
import com.elementsplus.ModItems;
import com.elementsplus.blocks.entity.RedstoneInterfaceBlockEntity;
import com.elementsplus.core.circuit.CircuitComponent;
import com.elementsplus.network.WrenchInterfaceActionPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 红石接口扳手辅助操作。模式与剪贴板是纯客户端临时状态，
 * 操作执行时才通过 {@link WrenchInterfaceActionPayload} 发到服务端。
 */
public final class WrenchInterfaceClient {
    public enum Mode {
        ROTATE_PINS("wrench.elements-plus.mode.rotate"),
        ASSIGN_PINS("wrench.elements-plus.mode.assign"),
        COPY_CONFIG("wrench.elements-plus.mode.copy"),
        TOGGLE_STATE("wrench.elements-plus.mode.state");

        private final String translationKey;

        Mode(String translationKey) {
            this.translationKey = translationKey;
        }

        public Component displayName() {
            return Component.translatable(translationKey);
        }
    }

    private record Target(BlockPos pos, Direction face, RedstoneInterfaceBlockEntity entity) {
    }

    public record ClipPin(String label, CircuitComponent.PinType type, int bitWidth) {
    }

    private record HudLine(String prefix, String text, CircuitComponent.PinType type,
                           int bitWidth, boolean selected, boolean pinLine) {
    }

    private static Mode mode = Mode.ROTATE_PINS;
    private static int selectedPin = -1;
    private static BlockPos selectedPos;
    private static boolean hasClipboard;
    private static boolean attackHeld;
    private static final Map<Direction, ClipPin> clipboard = new EnumMap<>(Direction.class);

    private WrenchInterfaceClient() {
    }

    private static Target target(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.level == null || minecraft.screen != null
                || !minecraft.player.getMainHandItem().is(ModItems.WRENCH)
                || !(minecraft.hitResult instanceof BlockHitResult hit)
                || hit.getType() != HitResult.Type.BLOCK
                || !(minecraft.level.getBlockState(hit.getBlockPos()).getBlock()
                instanceof com.elementsplus.blocks.RedstoneInterfaceBlock)
                || !(minecraft.level.getBlockEntity(hit.getBlockPos()) instanceof RedstoneInterfaceBlockEntity entity)) {
            return null;
        }
        return new Target(hit.getBlockPos(), hit.getDirection(), entity);
    }

    public static void cycleMode(boolean reverse) {
        Mode[] values = Mode.values();
        mode = values[(mode.ordinal() + (reverse ? -1 : 1) + values.length) % values.length];
        selectedPin = -1;
        selectedPos = null;
    }

    /** 按住左键时只在第一次触发切换，松开后由 updateAttackState 允许下一次。 */
    public static boolean beginModeSelection(boolean reverse) {
        if (attackHeld) {
            return false;
        }
        attackHeld = true;
        cycleMode(reverse);
        return true;
    }

    public static void updateAttackState(boolean down) {
        if (!down) {
            attackHeld = false;
        }
    }

    public static boolean handleScroll(double amount) {
        if (Math.abs(amount) < 0.001) {
            return false;
        }
        Target target = target(Minecraft.getInstance());
        if (target == null) {
            return false;
        }
        if (mode == Mode.ROTATE_PINS) {
            send(target, amount > 0 ? WrenchInterfaceActionPayload.Action.ROTATE_CW
                    : WrenchInterfaceActionPayload.Action.ROTATE_CCW, -1, List.of());
            return true;
        }
        if (mode == Mode.ASSIGN_PINS) {
            cycleSelection(target, amount > 0 ? -1 : 1);
            return true;
        }
        return false;
    }

    public static boolean handleUse(BlockHitResult hit) {
        Target target = target(Minecraft.getInstance());
        if (target == null) {
            return false;
        }
        boolean shift = Screen.hasShiftDown();
        switch (mode) {
            case ROTATE_PINS -> send(target, shift
                    ? WrenchInterfaceActionPayload.Action.MIRROR
                    : WrenchInterfaceActionPayload.Action.SWAP_FACE, -1, List.of());
            case ASSIGN_PINS -> {
                if (shift) {
                    send(target, WrenchInterfaceActionPayload.Action.UNASSIGN, -1, List.of());
                } else {
                    ensureSelection(target);
                    if (selectedPin < 0) {
                        localMessage("wrench.elements-plus.no_pin");
                    } else {
                        send(target, WrenchInterfaceActionPayload.Action.ASSIGN, selectedPin, List.of());
                    }
                }
            }
            case COPY_CONFIG -> {
                if (shift) {
                    copyConfiguration(target);
                } else if (!hasClipboard) {
                    localMessage("wrench.elements-plus.no_clipboard");
                } else {
                    List<String> labels = new ArrayList<>(RedstoneInterfaceBlockEntity.FACES.length);
                    for (Direction face : RedstoneInterfaceBlockEntity.FACES) {
                        ClipPin pin = clipboard.get(face);
                        labels.add(pin == null ? "" : pin.label());
                    }
                    send(target, WrenchInterfaceActionPayload.Action.PASTE, -1, labels);
                }
            }
            case TOGGLE_STATE -> send(target, shift
                    ? WrenchInterfaceActionPayload.Action.RESET
                    : (target.entity.isRunning()
                    ? WrenchInterfaceActionPayload.Action.PAUSE
                    : WrenchInterfaceActionPayload.Action.RUN), -1, List.of());
        }
        return true;
    }

    public static void renderHud(GuiGraphics graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        Target target = target(minecraft);
        if (target == null) {
            return;
        }
        Font font = minecraft.font;
        ensureSelection(target);
        List<HudLine> lines = buildHudLines(target);
        int lineHeight = 12;
        int contentWidth = 0;
        for (HudLine line : lines) {
            contentWidth = Math.max(contentWidth, hudLineWidth(font, line));
        }
        int height = lines.size() * lineHeight;
        int guiWidth = minecraft.getWindow().getGuiScaledWidth();
        int guiHeight = minecraft.getWindow().getGuiScaledHeight();
        // 紧贴准星右侧，使用左上角作为固定锚点；内容变化时只扩展背景。
        int x = guiWidth / 2 + 24;
        int y = guiHeight / 2 - 12;
        int boxLeft = x - 4;
        int boxTop = y - 4;
        int boxRight = x + contentWidth + 4;
        int boxBottom = y + height;
        if (boxRight > guiWidth - 4) {
            boxLeft = guiWidth - 4 - (contentWidth + 8);
            boxRight = guiWidth - 4;
            x = boxLeft + 4;
        }
        if (boxBottom > guiHeight - 4) {
            boxTop = Math.max(4, guiHeight - 4 - (height + 8));
            boxBottom = boxTop + height + 8;
            y = boxTop + 4;
        }
        if (boxTop < 4) {
            boxTop = 4;
            boxBottom = boxTop + height + 8;
            y = boxTop + 4;
        }
        graphics.fill(boxLeft, boxTop, boxRight, boxBottom, 0x90000000);
        for (int i = 0; i < lines.size(); i++) {
            HudLine line = lines.get(i);
            int rowY = y + i * lineHeight;
            if (line.selected()) {
                graphics.fill(boxLeft + 1, rowY - 2, boxRight - 1, rowY + lineHeight - 2, 0x40000000);
            }
            drawHudLine(graphics, font, x, rowY, line);
        }
    }

    private static List<HudLine> buildHudLines(Target target) {
        List<HudLine> lines = new ArrayList<>();
        RedstoneInterfaceBlockEntity.PinInfo facePin = target.entity.getFacePin(target.face);
        lines.add(new HudLine(faceName(target.face) + "：",
                facePin == null ? "-" : pinLabel(facePin),
                facePin == null ? null : facePin.type(),
                facePin == null ? 0 : facePin.bitWidth(), false, facePin != null));
        lines.add(new HudLine("",
                Component.translatable("wrench.elements-plus.mode", mode.displayName()).getString(),
                null, 0, false, false));
        if (mode == Mode.ASSIGN_PINS) {
            for (RedstoneInterfaceBlockEntity.PinInfo pin : unusedPins(target.entity)) {
                boolean selected = pin.index() == selectedPin;
                lines.add(new HudLine(selected ? "-> " : "   ", pinLabel(pin),
                        pin.type(), pin.bitWidth(), selected, true));
            }
        } else if (mode == Mode.COPY_CONFIG) {
            lines.add(new HudLine("",
                    Component.translatable("wrench.elements-plus.clipboard").getString(),
                    null, 0, false, false));
            if (clipboard.isEmpty()) {
                lines.add(new HudLine("", "-", null, 0, false, false));
            } else {
                for (Direction face : RedstoneInterfaceBlockEntity.FACES) {
                    ClipPin pin = clipboard.get(face);
                    if (pin != null) {
                        lines.add(new HudLine(faceName(face) + "：",
                                pin.label() == null || pin.label().isBlank() ? "?" : pin.label(),
                                pin.type(), pin.bitWidth(), false, true));
                    }
                }
            }
        }
        return lines;
    }

    private static int hudLineWidth(Font font, HudLine line) {
        int width = font.width(line.prefix());
        if (line.pinLine()) {
            return width + 8 + font.width(line.text());
        }
        return width + font.width(line.text());
    }

    private static void drawHudLine(GuiGraphics graphics, Font font, int x, int y, HudLine line) {
        graphics.drawString(font, line.prefix(), x, y,
                line.selected() ? 0xFFFFFF00 : 0xFFFFFFFF, false);
        int px = x + font.width(line.prefix());
        if (!line.pinLine()) {
            graphics.drawString(font, line.text(), px, y,
                    line.selected() ? 0xFFFFFF00 : 0xFFFFFFFF, false);
            return;
        }
        int typeColor = line.type() == CircuitComponent.PinType.INPUT ? 0xFF4488FF : 0xFF44FF88;
        int widthColor = line.bitWidth() >= 8 ? 0xFFFF55FF : 0xFFAAAAAA;
        graphics.fill(px, y + 3, px + 5, y + 8, typeColor);
        graphics.drawString(font, line.text(), px + 8, y, widthColor, false);
    }

    private static String pinLabel(RedstoneInterfaceBlockEntity.PinInfo pin) {
        if (pin.label() != null && !pin.label().isBlank()) {
            return pin.label();
        }
        return Component.translatable(pin.type() == CircuitComponent.PinType.INPUT
                ? "gui.elements-plus.redstone_interface.pin.input"
                : "gui.elements-plus.redstone_interface.pin.output", pin.x(), pin.y()).getString();
    }

    private static void copyConfiguration(Target target) {
        clipboard.clear();
        for (Direction face : RedstoneInterfaceBlockEntity.FACES) {
            RedstoneInterfaceBlockEntity.PinInfo pin = target.entity.getFacePin(face);
            if (pin != null) {
                clipboard.put(face, new ClipPin(pin.label(), pin.type(), pin.bitWidth()));
            }
        }
        hasClipboard = true;
        localMessage("wrench.elements-plus.copied");
    }

    private static void cycleSelection(Target target, int delta) {
        List<RedstoneInterfaceBlockEntity.PinInfo> unused = unusedPins(target.entity);
        if (unused.isEmpty()) {
            selectedPin = -1;
            return;
        }
        if (selectedPos == null || !selectedPos.equals(target.pos)) {
            selectedPos = target.pos;
            selectedPin = unused.get(delta > 0 ? 0 : unused.size() - 1).index();
            return;
        }
        int current = -1;
        for (int i = 0; i < unused.size(); i++) {
            if (unused.get(i).index() == selectedPin) {
                current = i;
                break;
            }
        }
        if (current < 0) {
            selectedPin = unused.get(delta > 0 ? 0 : unused.size() - 1).index();
            return;
        }
        selectedPin = unused.get((current + delta + unused.size()) % unused.size()).index();
    }

    private static void ensureSelection(Target target) {
        List<RedstoneInterfaceBlockEntity.PinInfo> unused = unusedPins(target.entity);
        if (unused.isEmpty()) {
            selectedPin = -1;
            selectedPos = target.pos;
            return;
        }
        boolean valid = false;
        for (RedstoneInterfaceBlockEntity.PinInfo pin : unused) {
            if (pin.index() == selectedPin) {
                valid = true;
                break;
            }
        }
        if (!valid || selectedPos == null || !selectedPos.equals(target.pos)) {
            selectedPos = target.pos;
            selectedPin = unused.get(0).index();
        }
    }

    private static List<RedstoneInterfaceBlockEntity.PinInfo> unusedPins(RedstoneInterfaceBlockEntity entity) {
        List<RedstoneInterfaceBlockEntity.PinInfo> result = new ArrayList<>();
        Map<Direction, Integer> assignments = entity.getAssignments();
        for (RedstoneInterfaceBlockEntity.PinInfo pin : entity.getPins()) {
            if (!assignments.containsValue(pin.index())) {
                result.add(pin);
            }
        }
        return result;
    }

    private static void send(Target target, WrenchInterfaceActionPayload.Action action,
                             int pin, List<String> clipboardLabels) {
        ClientPlayNetworking.send(new WrenchInterfaceActionPayload(
                target.pos, target.face, action, pin, clipboardLabels));
    }

    private static void localMessage(String key) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.player.displayClientMessage(Component.translatable(key), true);
        }
    }

    private static String faceName(Direction face) {
        return Component.translatable("gui.elements-plus.direction." + face.getSerializedName()).getString();
    }

    public static void reset() {
        mode = Mode.ROTATE_PINS;
        selectedPin = -1;
        selectedPos = null;
        hasClipboard = false;
        attackHeld = false;
        clipboard.clear();
    }
}
