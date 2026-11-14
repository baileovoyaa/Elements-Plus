package com.elementsplus.client.screen;

import com.elementsplus.ElementsPlus;
import com.elementsplus.blocks.entity.RedstoneInterfaceBlockEntity;
import com.elementsplus.client.gui.GuiUtil;
import com.elementsplus.client.gui.IconButton;
import com.elementsplus.core.circuit.CircuitComponent;
import com.elementsplus.menu.RedstoneInterfaceMenu;
import com.elementsplus.network.RedstoneInterfaceActionPayload;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import java.util.ArrayList;
import java.util.List;

@Environment(EnvType.CLIENT)
public class RedstoneInterfaceScreen extends AbstractContainerScreen<RedstoneInterfaceMenu> {
    private static final int LIST_X = 8;
    private static final int LIST_Y = 52;
    private static final int LIST_W = 105;
    private static final int ROW_H = 12;
    private static final int VISIBLE_ROWS = 6;
    private static final int ASSIGNED_X = 120;
    private static final int ASSIGNED_Y = 50;
    private static final int ASSIGNED_W = 85;
    private static final ResourceLocation TEXTURE_PLAY = ElementsPlus.id("textures/gui/play.png");
    private static final ResourceLocation TEXTURE_PAUSE = ElementsPlus.id("textures/gui/pause.png");
    private static final ResourceLocation TEXTURE_STEP = ElementsPlus.id("textures/gui/step.png");
    private static final ResourceLocation TEXTURE_STOP = ElementsPlus.id("textures/gui/stop.png");

    private IconButton playPauseButton;
    private int unassignedScroll;
    private int draggingPin = -1;
    private boolean draggingAssigned;
    private Direction draggingFace;

    public RedstoneInterfaceScreen(RedstoneInterfaceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 300;
        this.imageHeight = 226;
        this.inventoryLabelY = 131;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;

        int buttonX = leftPos + 52;
        playPauseButton = this.addRenderableWidget(new IconButton(buttonX, topPos + 20, 16, 16,
                TEXTURE_PLAY, button -> {
            boolean running = menu.isRunning();
            send(running ? RedstoneInterfaceActionPayload.Action.PAUSE
                    : RedstoneInterfaceActionPayload.Action.RUN, -1, null);
            ((IconButton) button).icon = running ? TEXTURE_PLAY : TEXTURE_PAUSE;
        }));
        this.addRenderableWidget(new IconButton(buttonX + 18, topPos + 20, 16, 16,
                TEXTURE_STEP, button -> send(RedstoneInterfaceActionPayload.Action.STEP, -1, null)));
        this.addRenderableWidget(new IconButton(buttonX + 36, topPos + 20, 16, 16,
                TEXTURE_STOP, button -> send(RedstoneInterfaceActionPayload.Action.RESET, -1, null)));
        updatePlayButtonIcon();
    }

    private void updatePlayButtonIcon() {
        if (playPauseButton != null) {
            playPauseButton.icon = menu.isRunning() ? TEXTURE_PAUSE : TEXTURE_PLAY;
        }
    }

    private RedstoneInterfaceBlockEntity entity() {
        if (minecraft == null || minecraft.level == null) {
            return null;
        }
        return minecraft.level.getBlockEntity(menu.getBlockPos()) instanceof RedstoneInterfaceBlockEntity e ? e : null;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        GuiUtil.drawMainPanel(g, leftPos, topPos, leftPos + imageWidth, topPos + imageHeight);
        for (Slot slot : menu.slots) {
            GuiUtil.drawSlot(g, leftPos + slot.x - 1, topPos + slot.y - 1);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        updatePlayButtonIcon();
        super.render(g, mouseX, mouseY, partialTick);
        RedstoneInterfaceBlockEntity be = entity();
        int x = leftPos;
        int y = topPos;

        g.drawString(font, Component.translatable("gui.elements-plus.redstone_interface.circuit"),
                x + 8, y + 9, 0xFFA0A0A0, false);
        g.drawString(font, Component.translatable("gui.elements-plus.redstone_interface.battery"),
                x + 30, y + 9, 0xFFA0A0A0, false);

        drawPinLists(g, be, x, y, mouseX, mouseY);
        drawDisks(g, x, y);
        drawStats(g, x + imageWidth - 8, y + 8);
        if (menu.isHibernating()) {
            g.drawString(font, Component.translatable("gui.elements-plus.redstone_interface.hibernating"),
                    x + 110, y + 24, 0xFFFF5555, true);
        }
        if (draggingPin >= 0 && be != null) {
            drawDragGhost(g, be, mouseX, mouseY, x, y);
        } else {
            // AbstractContainerScreen 的 render 不会自动绘制槽位 tooltip；
            // 先绘制自定义面板，再在最后绘制物品悬停提示，避免被列表覆盖。
            this.renderTooltip(g, mouseX, mouseY);
        }
    }

    private void drawPinLists(GuiGraphics g, RedstoneInterfaceBlockEntity be, int x, int y,
                              int mouseX, int mouseY) {
        g.drawString(font, Component.translatable("gui.elements-plus.redstone_interface.unassigned"),
                x + LIST_X, y + 40, 0xFF3F3F3F, false);
        g.drawString(font, Component.translatable("gui.elements-plus.redstone_interface.assigned"),
                x + ASSIGNED_X, y + 40, 0xFF3F3F3F, false);
        g.fill(x + LIST_X, y + LIST_Y - 2, x + LIST_X + LIST_W, y + LIST_Y + VISIBLE_ROWS * ROW_H,
                0x30000000);

        if (be == null) {
            return;
        }
        int localMouseX = (int) Math.floor(mouseX - leftPos);
        int localMouseY = (int) Math.floor(mouseY - topPos);
        List<RedstoneInterfaceBlockEntity.PinInfo> unused = unusedPins(be);
        unassignedScroll = Math.max(0, Math.min(unassignedScroll,
                Math.max(0, unused.size() - VISIBLE_ROWS)));
        for (int i = 0; i < VISIBLE_ROWS; i++) {
            int index = unassignedScroll + i;
            if (index >= unused.size()) {
                continue;
            }
            int rowY = y + LIST_Y + i * ROW_H;
            boolean hovered = inRect(mouseX, mouseY, x + LIST_X, rowY, LIST_W, ROW_H);
            if (hovered) {
                g.fill(x + LIST_X, rowY, x + LIST_X + LIST_W, rowY + ROW_H - 1, 0x55000000);
            }
            RedstoneInterfaceBlockEntity.PinInfo pin = unused.get(index);
            drawPinLabel(g, pinText(pin), pin, x + LIST_X + 3, rowY + 2);
        }

        Direction draggedTarget = draggingPin >= 0 && localMouseX >= ASSIGNED_X
                && localMouseX < ASSIGNED_X + ASSIGNED_W
                ? assignedFaceAt(be, localMouseY) : null;
        for (int i = 0; i < RedstoneInterfaceBlockEntity.FACES.length; i++) {
            Direction face = RedstoneInterfaceBlockEntity.FACES[i];
            int rowY = y + ASSIGNED_Y + i * ROW_H;
            boolean hovered = inRect(mouseX, mouseY, x + ASSIGNED_X, rowY, ASSIGNED_W, ROW_H);
            if (hovered) {
                g.fill(x + ASSIGNED_X, rowY, x + ASSIGNED_X + ASSIGNED_W, rowY + ROW_H - 1, 0x55000000);
            }
            if (face == draggedTarget) {
                g.fill(x + ASSIGNED_X, rowY, x + ASSIGNED_X + ASSIGNED_W, rowY + ROW_H - 1, 0x55FFFFFF);
                g.fill(x + ASSIGNED_X, rowY, x + ASSIGNED_X + ASSIGNED_W, rowY + 1, 0xFFFFFFFF);
                g.fill(x + ASSIGNED_X, rowY + ROW_H - 2, x + ASSIGNED_X + ASSIGNED_W,
                        rowY + ROW_H - 1, 0xFFFFFFFF);
            }
            Integer pinIndex = be.getAssignments().get(face);
            String faceName = Component.translatable("gui.elements-plus.direction." + face.getSerializedName()).getString();
            int faceX = x + ASSIGNED_X + 3;
            if (pinIndex == null) {
                g.drawString(font, faceName + ":", faceX, rowY + 2, 0xFF808080, false);
                continue;
            }
            RedstoneInterfaceBlockEntity.PinInfo pin = pinAt(be, pinIndex);
            if (pin == null) {
                g.drawString(font, faceName + ":", faceX, rowY + 2, 0xFF808080, false);
                continue;
            }
            g.drawString(font, faceName + ":", faceX, rowY + 2, 0xFF808080, false);
            int pinX = faceX + font.width(faceName + ":") + 4;
            drawPinLabel(g, pinText(pin), pin, pinX, rowY + 2);
        }
    }

    private void drawPinLabel(GuiGraphics g, String label, RedstoneInterfaceBlockEntity.PinInfo pin,
                              int x, int y) {
        g.fill(x, y + 1, x + 5, y + 6, pinTypeColor(pin.type()));
        g.drawString(font, label, x + 8, y, bitWidthColor(pin.bitWidth()), false);
    }

    private void drawDragGhost(GuiGraphics g, RedstoneInterfaceBlockEntity be,
                               int mouseX, int mouseY, int x, int y) {
        RedstoneInterfaceBlockEntity.PinInfo pin = pinAt(be, draggingPin);
        if (pin == null) {
            return;
        }
        String label = pinText(pin);
        int tx = mouseX + 10;
        int ty = mouseY - 6;
        int width = font.width(label) + 10;
        g.fill(tx - 2, ty - 2, tx + width + 2, ty + 12, 0xB0000000);
        drawPinLabel(g, label, pin, tx, ty);
    }

    private static int pinTypeColor(CircuitComponent.PinType type) {
        return type == CircuitComponent.PinType.INPUT ? 0xFF4488FF : 0xFF44FF88;
    }

    private static int bitWidthColor(int bitWidth) {
        return bitWidth >= 8 ? 0xFFFF55FF : 0xFFAAAAAA;
    }

    private static RedstoneInterfaceBlockEntity.PinInfo pinAt(RedstoneInterfaceBlockEntity be, int index) {
        List<RedstoneInterfaceBlockEntity.PinInfo> pins = be.getPins();
        return index >= 0 && index < pins.size() ? pins.get(index) : null;
    }

    private void drawDisks(GuiGraphics g, int x, int y) {
        g.drawString(font, Component.translatable("gui.elements-plus.redstone_interface.disks"),
                x + 210, y + 40, 0xFF3F3F3F, false);
        for (int i = 0; i < 6; i++) {
            g.drawString(font, Component.translatable("gui.elements-plus.redstone_interface.disk_label"),
                    x + 232, y + 56 + i * 18, 0xFF808080, false);
        }
    }

    private void drawStats(GuiGraphics g, int rightX, int y) {
        double avg = menu.getAverageMs();
        double last = menu.getLastStepMs();
        int limit = menu.getAvgLimit();
        Component avgText = Component.translatable("gui.elements-plus.redstone_interface.avg_mspt", format(avg));
        g.drawString(font, avgText, rightX - font.width(avgText), y, statsColor(avg, limit), false);
        if (last > 0) {
            Component lastText = Component.translatable("gui.elements-plus.redstone_interface.step_ms", format(last));
            g.drawString(font, lastText, rightX - font.width(lastText), y + 12,
                    statsColor(last, limit), false);
        }
    }

    private static int statsColor(double value, int limit) {
        if (value <= 0) {
            return 0xFF000000;
        }
        if (limit <= 0) {
            return 0xFFFF3333;
        }
        if (value >= limit * 0.75) {
            return 0xFFFF3333;
        }
        if (value >= limit * 0.5) {
            return 0xFFFFA500;
        }
        return 0xFF33CC33;
    }

    private static String format(double value) {
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        RedstoneInterfaceBlockEntity be = entity();
        if (be == null) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        int localX = (int) Math.floor(mouseX - leftPos);
        int localY = (int) Math.floor(mouseY - topPos);
        if (button == 0) {
            PinHit hit = pinHit(be, localX, localY);
            if (hit != null) {
                if (Screen.hasShiftDown()) {
                    if (hit.face() == null) {
                        Direction free = firstFreeFace(be);
                        if (free != null) {
                            send(RedstoneInterfaceActionPayload.Action.ASSIGN, hit.pinIndex(), free);
                        }
                    } else {
                        send(RedstoneInterfaceActionPayload.Action.UNASSIGN, -1, hit.face());
                    }
                } else {
                    draggingPin = hit.pinIndex();
                    draggingAssigned = hit.face() != null;
                    draggingFace = hit.face();
                }
                return true;
            }
        }
        if (button == 0 || button == 1 || button == 2) {
            // 物品栏槽位仍然使用原版处理。
            return super.mouseClicked(mouseX, mouseY, button);
        }
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingPin >= 0) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (draggingPin >= 0) {
            RedstoneInterfaceBlockEntity be = entity();
            int localX = (int) Math.floor(mouseX - leftPos);
            int localY = (int) Math.floor(mouseY - topPos);
            if (be != null) {
                if (draggingAssigned) {
                    if (isInUnassigned(localX, localY) && draggingFace != null) {
                        send(RedstoneInterfaceActionPayload.Action.UNASSIGN, -1, draggingFace);
                    } else if (localX >= ASSIGNED_X && localX < ASSIGNED_X + ASSIGNED_W) {
                        Direction target = assignedFaceAt(be, localY);
                        if (target != null && target != draggingFace) {
                            // 同一个 ASSIGN 操作会把引脚从原面移动到目标面。
                            send(RedstoneInterfaceActionPayload.Action.ASSIGN, draggingPin, target);
                        }
                    }
                } else if (localX >= ASSIGNED_X && localX < ASSIGNED_X + ASSIGNED_W) {
                    Direction face = assignedFaceAt(be, localY);
                    if (face != null) {
                        send(RedstoneInterfaceActionPayload.Action.ASSIGN, draggingPin, face);
                    }
                }
            }
            draggingPin = -1;
            draggingAssigned = false;
            draggingFace = null;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (inRect(mouseX, mouseY, leftPos + LIST_X, topPos + LIST_Y,
                LIST_W, VISIBLE_ROWS * ROW_H)) {
            unassignedScroll = Math.max(0, unassignedScroll - (int) Math.signum(verticalAmount));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    private void send(RedstoneInterfaceActionPayload.Action action, int pin, Direction face) {
        ClientPlayNetworking.send(new RedstoneInterfaceActionPayload(menu.getBlockPos(), action, pin, face));
    }

    private static String pinText(RedstoneInterfaceBlockEntity.PinInfo pin) {
        String text = pin.label();
        if (text == null || text.isBlank()) {
            String key = pin.type() == CircuitComponent.PinType.INPUT
                    ? "gui.elements-plus.redstone_interface.pin.input"
                    : "gui.elements-plus.redstone_interface.pin.output";
            text = Component.translatable(key, pin.x(), pin.y()).getString();
        }
        return text;
    }

    private static List<RedstoneInterfaceBlockEntity.PinInfo> unusedPins(RedstoneInterfaceBlockEntity be) {
        List<RedstoneInterfaceBlockEntity.PinInfo> result = new ArrayList<>();
        for (RedstoneInterfaceBlockEntity.PinInfo pin : be.getPins()) {
            if (!be.getAssignments().containsValue(pin.index())) {
                result.add(pin);
            }
        }
        return result;
    }

    private static Direction firstFreeFace(RedstoneInterfaceBlockEntity be) {
        for (Direction face : RedstoneInterfaceBlockEntity.FACES) {
            if (!be.getAssignments().containsKey(face)) {
                return face;
            }
        }
        return null;
    }

    private PinHit pinHit(RedstoneInterfaceBlockEntity be, int x, int y) {
        if (x >= LIST_X && x < LIST_X + LIST_W && y >= LIST_Y && y < LIST_Y + VISIBLE_ROWS * ROW_H) {
            int row = (y - LIST_Y) / ROW_H;
            List<RedstoneInterfaceBlockEntity.PinInfo> unused = unusedPins(be);
            int index = unassignedScroll + row;
            if (index >= 0 && index < unused.size()) {
                return new PinHit(unused.get(index).index(), null);
            }
        }
        Direction face = assignedFaceAt(be, y);
        if (face != null && x >= ASSIGNED_X && x < ASSIGNED_X + ASSIGNED_W) {
            Integer index = be.getAssignments().get(face);
            return index == null ? null : new PinHit(index, face);
        }
        return null;
    }

    private static Direction assignedFaceAt(RedstoneInterfaceBlockEntity be, int y) {
        if (y < ASSIGNED_Y || y >= ASSIGNED_Y + RedstoneInterfaceBlockEntity.FACES.length * ROW_H) {
            return null;
        }
        int row = (y - ASSIGNED_Y) / ROW_H;
        return row >= 0 && row < RedstoneInterfaceBlockEntity.FACES.length
                ? RedstoneInterfaceBlockEntity.FACES[row] : null;
    }

    private static boolean isInUnassigned(int x, int y) {
        return x >= LIST_X && x < LIST_X + LIST_W && y >= LIST_Y && y < LIST_Y + VISIBLE_ROWS * ROW_H;
    }

    private record PinHit(int pinIndex, Direction face) {
    }

    private static boolean inRect(double x, double y, int rx, int ry, int rw, int rh) {
        return x >= rx && x < rx + rw && y >= ry && y < ry + rh;
    }
}
