package com.elementsplus.client.screen;

import com.elementsplus.ElementsPlus;
import com.elementsplus.ModItems;
import com.elementsplus.blocks.entity.ExperimentStatus;
import com.elementsplus.client.config.ClientConfig;
import com.elementsplus.client.config.NumberFormat;
import com.elementsplus.client.experiment.ExperimentMarkdownParser;
import com.elementsplus.client.gui.*;
import com.elementsplus.client.gui.program.ProgrammingPage;
import com.elementsplus.core.circuit.CircuitItemData;
import com.elementsplus.core.circuit.diagram.CircuitDiagram;
import com.elementsplus.core.experiment.*;
import com.elementsplus.menu.ExperimentTableMenu;
import com.elementsplus.network.ExperimentTableControlPayload;
import com.elementsplus.network.ExperimentTableDataRequestPayload;
import com.elementsplus.network.ExperimentTableSelectionUpdatePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class ExperimentTableScreen extends AbstractContainerScreen<ExperimentTableMenu> implements SlotPositionProvider {
    /** 实验桌的两个标签页。 */
    public enum Tab {EXPERIMENT, PROGRAMMING}

    public Tab activeTab = Tab.EXPERIMENT;
    public TabButton tabButtonExperiment;
    public TabButton tabButtonProgramming;
    public IconButton programmingStatusButton;
    public ProgrammingPage programmingPage;
    private boolean programmingStatusOk = true;
    private List<Component> programmingStatusLines = List.of();

    public CollapseButton collapseButtonInventory;
    public CollapseButton collapseButtonTestCases;
    public boolean inventoryActive = true;
    public boolean testCasesActive = true;
    public Map<Integer, Point> slotPosition;

    public ScrollPanelWidget experimentWidget;
    public ScrollPanelWidget experimentChapterWidget;
    public ScrollPanelWidget testCasePanelWidget;
    public ButtonGroup experimentGroup;

    public ExperimentChapter selectedChapter;
    private long worldSeed;
    public BaseExperiment selectedExperiment;
    public AbstractWidget currentExperimentWidget;

    public BlockPos tablePos;
    /** 实验桌自身布局尺寸；layoutLeftPos/layoutImageWidth 由 AbstractContainerScreen 对外暴露给 JEI 等模组。 */
    private int layoutLeftPos;
    private int layoutImageWidth;
    private final Set<String> unlockedChapters = new HashSet<>();
    private final Set<String> forcedChapters = new HashSet<>();
    private final Set<String> completedExperiments = new HashSet<>();
    private final Map<String, Integer> experimentScores = new HashMap<>();
    private final List<Integer> currentTestTicks = new ArrayList<>();
    private List<ChapterEntryButton> chapterButtons = new ArrayList<>();

    private ExperimentStatus status = ExperimentStatus.IDLE;
    private boolean tablePaused = false;
    private float currentProgress = 0f;
    private final List<Integer> currentTestResults = new ArrayList<>();
    private final List<Component> statusTooltipLines = new ArrayList<>();
    private ExperimentFailureInfo failureInfo = null;
    private SequentialFailureInfo sequentialFailure = null;
    private int currentTestIndex = -1;
    private int currentFinalScore = -1;
    private SequentialTestDataDialog sequentialDialog = null;
    private ProgrammingTestDataDialog programmingDialog = null;
    private CustomComponentConfigDialog customComponentDialog = null;
    private String resultsExperimentName = null;
    private final List<TestCaseWidget> testCaseWidgets = new ArrayList<>();

    public IconButton startExperimentButton;
    public ProgressBar progressBar;
    public IconButton statusButton;

    public ExperimentTableScreen(ExperimentTableMenu experimentTableMenu, Inventory inventory, Component component) {
        super(experimentTableMenu, inventory, component);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float f, int i, int j) {
        GuiUtil.drawMainPanel(guiGraphics, layoutLeftPos, topPos, layoutLeftPos + layoutImageWidth, topPos + imageHeight, 0xFF404040);
        GuiUtil.drawMainPanel(guiGraphics, layoutLeftPos, topPos + 18, layoutLeftPos + layoutImageWidth, topPos + imageHeight, 0xFFFFFFFF);
        if (activeTab == Tab.PROGRAMMING) {
            if (inventoryActive) {
                GuiUtil.drawSubPanel(guiGraphics, layoutLeftPos + 5, topPos + imageHeight - 188,
                        layoutLeftPos + 5 + 79, topPos + imageHeight - 5, 0xFFA0A0A0);
            }
            if (collapseButtonInventory != null && collapseButtonInventory.active) {
                for (Slot slot : this.menu.slots) {
                    Point point = getSlotPosition(slot);
                    if (point != null) {
                        GuiUtil.drawSlot(guiGraphics, this.layoutLeftPos + point.x() - 1,
                                this.topPos + point.y() - 1);
                    }
                }
            }
            Point extra = getSlotPosition(this.menu.extraSlot);
            if (extra != null) {
                GuiUtil.drawSlot(guiGraphics, this.layoutLeftPos + extra.x() - 1, this.topPos + extra.y() - 1);
            }
            return;
        }
        if (inventoryActive) {
            GuiUtil.drawSubPanel(guiGraphics, layoutLeftPos + 5, topPos + imageHeight - 188, layoutLeftPos + 5 + 79, topPos + imageHeight - 5, 0xFFA0A0A0);
        }

        if (collapseButtonInventory.active) {
            for (Slot slot : this.menu.slots) {
                Point point = getSlotPosition(slot);
                if (point != null) {
                    GuiUtil.drawSlot(guiGraphics, this.layoutLeftPos + point.x() - 1, this.topPos + point.y() - 1);
                }
            }
        }

        Point point = getSlotPosition(this.menu.extraSlot);
        GuiUtil.drawSlot(guiGraphics, this.layoutLeftPos + point.x() - 1, this.topPos + point.y() - 1);

        // 实验进行中：运行中显示 pause，暂停显示 play，按住 Shift 显示 stop
        if (status == ExperimentStatus.BUSY) {
            startExperimentButton.icon = Screen.hasShiftDown()
                    ? ElementsPlus.id("textures/gui/stop.png")
                    : ElementsPlus.id("textures/gui/" + (tablePaused ? "play" : "pause") + ".png");
        } else {
            startExperimentButton.icon = ElementsPlus.id("textures/gui/play.png");
        }
    }

    @Override
    protected void init() {
        if (Minecraft.getInstance().level != null) {
            BuiltinExperimentChapters.ensureLoaded(Minecraft.getInstance().level.registryAccess());
        }
        updateScreenSize();
        sequentialDialog = null;
        programmingDialog = null;
        customComponentDialog = null;
        super.init();

        slotPosition = new HashMap<>();

        for (int l = 0; l < 3; l++) {
            for (int k = 0; k < 9; k++) {
                slotPosition.put(l * 9 + k + 9, Point.of(8 + l * 18, this.imageHeight - 6 - 9 * 18 + k * 18));
            }
        }

        for (int l = 0; l < 9; l++) {
            slotPosition.put(l, Point.of(11 + 3 * 18, this.imageHeight - 6 - 9 * 18 + l * 18));
        }

        this.titleLabelX = 6;
        int tabX = this.layoutLeftPos + this.font.width(this.title) + 10;
        this.addRenderableWidget(tabButtonExperiment = new TabButton(tabX, this.topPos, 50, 22,
                Component.translatable("gui.elements-plus.experiment_table.tab")) {
            @Override
            public void onClick(double d, double e) {
                switchTab(Tab.EXPERIMENT);
            }
        });
        this.addRenderableWidget(tabButtonProgramming = new TabButton(tabX + 50, this.topPos, 50, 22,
                Component.translatable("gui.elements-plus.experiment_table.tab_programming")) {
            @Override
            public void onClick(double d, double e) {
                switchTab(Tab.PROGRAMMING);
            }
        });
        tabButtonExperiment.active = activeTab == Tab.EXPERIMENT;
        tabButtonProgramming.active = activeTab == Tab.PROGRAMMING;

        if (activeTab == Tab.PROGRAMMING) {
            initProgrammingTab();
        } else {
            initExperimentTab();
        }
    }

    /** 切换标签页；两个标签页各自保留自己的选中状态和状态图标。 */
    public void switchTab(Tab tab) {
        if (activeTab == tab) {
            return;
        }
        if (activeTab == Tab.PROGRAMMING && programmingPage != null) {
            programmingPage.onTabHidden();
        }
        activeTab = tab;
        // 标签按钮的点击发生在控件遍历中，延后到下一帧重建，避免并发修改控件列表。
        Minecraft.getInstance().execute(this::rebuildWidgets);
    }

    private void initExperimentTab() {
        this.addRenderableWidget(collapseButtonInventory = new CollapseButton(this.layoutLeftPos + 5, inventoryActive ? this.topPos + this.imageHeight - 188 : this.topPos + this.imageHeight - 23, 79, 18, Component.translatable("container.inventory")) {
            @Override
            public void onClick(double d, double e) {
                super.onClick(d, e);
                inventoryActive = active;
                this.setY(inventoryActive ? topPos + imageHeight - 188 : topPos + imageHeight - 23);
                experimentWidget.setHeight(inventoryActive ? imageHeight - 218 : imageHeight - 53);
            }
        });
        collapseButtonInventory.active = inventoryActive;

        this.addRenderableWidget(experimentWidget = new ScrollPanelWidget(this.layoutLeftPos + 5, this.topPos + 25, 79, imageHeight - 218, GuiUtil.SubPanelType.BORDERED, 0xFFA0A0A0));
        experimentWidget.setHeight(inventoryActive ? imageHeight - 218 : imageHeight - 53);
        experimentWidget.overflowBehaviorX = ScrollPanelWidget.OverflowBehavior.CLIP;

        this.addRenderableWidget(experimentChapterWidget = new ScrollPanelWidget(layoutLeftPos + 9 + 80, topPos + 48, Math.max(1, layoutImageWidth - 95), Math.max(1, imageHeight - 195), GuiUtil.SubPanelType.BORDERED, 0xFFA0A0A0));
        experimentChapterWidget.overflowBehaviorX = ScrollPanelWidget.OverflowBehavior.CLIP;

        List<ChapterEntryButton> chapterButtons = new ArrayList<>();
        int entryY = 2;
        for (ExperimentChapter chapter : BuiltinExperimentChapters.BUILTIN_EXPERIMENT_CHAPTERS) {
            ChapterEntryButton button = experimentWidget.addChild(new ChapterEntryButton(1, entryY, experimentWidget.getWidth() - 2, 18, chapter));
            chapterButtons.add(button);
            entryY += 18;
        }
        this.chapterButtons = chapterButtons;
        if (!chapterButtons.isEmpty()) {
            experimentGroup = new ButtonGroup(chapterButtons.toArray(new GroupButton[0]));
            // 切换标签页会重建控件，保留原来选中的章节。
            ChapterEntryButton initial = chapterButtons.get(0);
            if (selectedChapter != null) {
                for (ChapterEntryButton button : chapterButtons) {
                    if (button.chapter == selectedChapter) {
                        initial = button;
                        break;
                    }
                }
            }
            experimentGroup.setSelected(initial);
            setSelectedChapter(initial.chapter);
        }

        ClientPlayNetworking.send(new ExperimentTableDataRequestPayload());

        this.addRenderableWidget(testCasePanelWidget = new ScrollPanelWidget(layoutLeftPos + 9 + 80, topPos + imageHeight - 125, Math.max(1, layoutImageWidth - 95), 120, GuiUtil.SubPanelType.BORDERED, 0xFFA0A0A0));
        this.addRenderableWidget(collapseButtonTestCases = new CollapseButton(this.layoutLeftPos + 89, testCasesActive ? this.topPos + this.imageHeight - 142 : this.topPos + this.imageHeight - 23, layoutImageWidth - 95, 18, Component.translatable("gui.elements-plus.experiment_table.test_cases")) {
            @Override
            public void onClick(double d, double e) {
                super.onClick(d, e);
                testCasesActive = active;
                this.setY(testCasesActive ? topPos + imageHeight - 142 : topPos + imageHeight - 23);
                experimentChapterWidget.setHeight(testCasesActive ? imageHeight - 195 : imageHeight - 75);
                testCasePanelWidget.visible = testCasesActive;
            }
        });
        collapseButtonTestCases.active = testCasesActive;

        // 当前实验（宽度可变）
        this.addRenderableWidget(currentExperimentWidget = new AbstractWidget(layoutLeftPos + 110, topPos + 26, 0, 16, Component.empty()) {

            @Override
            protected void renderWidget(GuiGraphics guiGraphics, int i, int j, float f) {
                guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), 0x80000000);
                if (selectedExperiment != null) {
                    if (selectedExperiment.getIcon() != null) {
                        guiGraphics.blit(selectedExperiment.getIcon(), 5 + this.getX(), this.getY(), 0, 0, 16, 16, 16, 16);
                        guiGraphics.drawString(font, selectedExperiment.getDisplayName(), 5 + this.getX() + 16 + 2, this.getY() + this.getHeight() / 2 - font.lineHeight / 2, 0xFFFFFFFF, false);
                    } else {
                        guiGraphics.drawString(font, selectedExperiment.getDisplayName(), 5 + this.getX(), this.getY() + this.getHeight() / 2 - font.lineHeight / 2, 0xFFFFFFFF, false);
                    }
                } else {
                    guiGraphics.drawString(font, Component.translatable("gui.elements-plus.experiment_table.no_experiment"), 5 + this.getX(), this.getY() + this.getHeight() / 2 - font.lineHeight / 2, 0xFFFFFFFF, false);
                }
            }

            @Override
            protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {

            }
        });

        // 开始实验（动态x坐标）
        this.addRenderableWidget(startExperimentButton = new IconButton(0, topPos + 26, 16, 16, ElementsPlus.id("textures/gui/play.png"), (button) -> {
            if (tablePos == null) {
                return;
            }
            if (status == ExperimentStatus.BUSY) {
                ExperimentTableControlPayload.Action action = Screen.hasShiftDown()
                        ? ExperimentTableControlPayload.Action.STOP
                        : (tablePaused ? ExperimentTableControlPayload.Action.RESUME : ExperimentTableControlPayload.Action.PAUSE);
                ClientPlayNetworking.send(new ExperimentTableControlPayload(tablePos, action, null));
            } else if (selectedExperiment == BuiltinExperiments.CUSTOM_COMPONENT) {
                openCustomComponentDialog();
            } else if (selectedExperiment != null) {
                ClientPlayNetworking.send(new ExperimentTableControlPayload(tablePos, ExperimentTableControlPayload.Action.START, selectedExperiment.getName()));
            }
        }));

        // 进度条（动态x，宽度可变）
        this.addRenderableWidget(progressBar = new ProgressBar(0, topPos + 26, 0, 16));

        // 实验状态
        this.addRenderableWidget(statusButton = new IconButton(layoutLeftPos + layoutImageWidth - 22, topPos + 26, 16, 16, ElementsPlus.id("textures/gui/experiment_table/idle.png"), (button) -> {
            if ((status == ExperimentStatus.ERROR || status == ExperimentStatus.SUCCESS)
                    && sequentialFailure != null) {
                if (sequentialFailure.programming() != null) {
                    ProgrammingTestDataDialog dialog = new ProgrammingTestDataDialog(
                            sequentialFailure.programming(), font, width, height, () -> {
                        programmingDialog = null;
                        restoreLayoutBounds();
                    });
                    programmingDialog = dialog;
                    applyModalBounds(dialog.getWindowX(), dialog.getWindowWidth());
                } else {
                    SequentialTestDataDialog dialog = new SequentialTestDataDialog(sequentialFailure, font, width, height,
                            () -> {
                                sequentialDialog = null;
                                restoreLayoutBounds();
                            });
                    sequentialDialog = dialog;
                    applyModalBounds(dialog.getWindowX(), dialog.getWindowWidth());
                }
            }
        }));

        // 数字格式按钮
        this.addRenderableWidget(new IconButton(layoutLeftPos + layoutImageWidth - 22 - 18, topPos + 2, 16, 16,
                LithographyMachineScreen.numberFormatIcon(true, ClientConfig.get().digitalNumberFormat),
                button -> cycleNumberFormat((IconButton) button, true)));
        this.addRenderableWidget(new IconButton(layoutLeftPos + layoutImageWidth - 22, topPos + 2, 16, 16,
                LithographyMachineScreen.numberFormatIcon(false, ClientConfig.get().analogNumberFormat),
                button -> cycleNumberFormat((IconButton) button, false)));

        menu.setInventorySlotsActive(true);
        updateCurrentExperimentDisplay();
        updateStatusButton();
        updateProgressBar();
    }

    private void initProgrammingTab() {
        if (programmingPage == null) {
            programmingPage = new ProgrammingPage(this);
        }
        // 页面整体保持全高，只有左侧根列表为物品栏让出空间。
        programmingPage.layout(layoutLeftPos, topPos, layoutImageWidth, imageHeight);
        programmingPage.visible = true;
        programmingPage.active = true;
        this.addRenderableWidget(programmingPage);

        // 物品栏按钮在页面之后添加，保证显示在页面背景之上。
        this.addRenderableWidget(collapseButtonInventory = new CollapseButton(this.layoutLeftPos + 5,
                inventoryActive ? this.topPos + this.imageHeight - 188 : this.topPos + this.imageHeight - 23,
                79, 18, Component.translatable("container.inventory")) {
            @Override
            public void onClick(double d, double e) {
                super.onClick(d, e);
                inventoryActive = active;
                this.setY(inventoryActive ? topPos + imageHeight - 188 : topPos + imageHeight - 23);
                menu.setInventorySlotsActive(inventoryActive);
            }
        });
        collapseButtonInventory.active = inventoryActive;

        this.addRenderableWidget(programmingStatusButton = new IconButton(programmingPage.statusIconX(),
                programmingPage.statusIconY(), 16, 16,
                ElementsPlus.id("textures/gui/experiment_table/idle.png"), button -> {
            if (programmingPage != null) {
                programmingPage.clearStatus();
            }
        }));
        menu.setInventorySlotsActive(inventoryActive);
        updateProgrammingStatus();
        if (tablePos == null) {
            ClientPlayNetworking.send(new ExperimentTableDataRequestPayload());
        }
        programmingPage.onOpen();
    }

    /** 编程页物品栏是否正在展开（只压缩左侧根列表）。 */
    public boolean isProgrammingInventoryExpanded() {
        return activeTab == Tab.PROGRAMMING && inventoryActive
                && collapseButtonInventory != null && collapseButtonInventory.active;
    }

    /** 展开的物品栏面板顶部（页面坐标系）。 */
    public int programmingInventoryTop() {
        return topPos + imageHeight - 188;
    }

    /** 重绘编程页左下物品栏的面板与槽位背景（页面背景之后调用）。 */
    public void drawProgrammingInventoryBackground(GuiGraphics guiGraphics) {
        if (!isProgrammingInventoryExpanded()) {
            return;
        }
        GuiUtil.drawSubPanel(guiGraphics, layoutLeftPos + 5, programmingInventoryTop(),
                layoutLeftPos + 5 + 79, topPos + imageHeight - 5, 0xFFA0A0A0);
        for (Slot slot : this.menu.slots) {
            Point point = getSlotPosition(slot);
            if (point != null) {
                GuiUtil.drawSlot(guiGraphics, this.layoutLeftPos + point.x() - 1,
                        this.topPos + point.y() - 1);
            }
        }
    }

    /** 鼠标是否在编程页的左下物品栏/折叠按钮区域（该区域交给容器界面处理）。 */
    public boolean isOverProgrammingInventory(double mouseX, double mouseY) {
        if (activeTab != Tab.PROGRAMMING) {
            return false;
        }
        int x0 = layoutLeftPos + 5;
        int x1 = x0 + 79;
        int y0 = inventoryActive ? programmingInventoryTop() : topPos + imageHeight - 23;
        int y1 = topPos + imageHeight - 5;
        return mouseX >= x0 && mouseX < x1 && mouseY >= y0 && mouseY < y1;
    }

    public BlockPos getTablePos() {
        return tablePos;
    }

    public ExperimentTableMenu experimentMenu() {
        return menu;
    }

    public void onFileSystemData(com.elementsplus.network.FileSystemDataPayload payload) {
        if (programmingPage != null) {
            programmingPage.onFileSystemData(payload);
        }
    }

    public void onFileContent(com.elementsplus.network.FileContentPayload payload) {
        if (programmingPage != null) {
            programmingPage.onFileContent(payload);
        }
    }

    public void onFileFolderArchive(com.elementsplus.network.FileFolderArchivePayload payload) {
        if (programmingPage != null) {
            programmingPage.onFolderArchive(payload);
        }
    }

    public void onEditorState(com.elementsplus.network.FileEditorStatePayload payload) {
        if (programmingPage != null) {
            programmingPage.onEditorState(payload);
        }
    }

    public void onFileActionResult(com.elementsplus.network.FileActionResultPayload payload) {
        if (programmingPage != null) {
            programmingPage.onActionResult(payload);
        }
    }

    /** 编程页右上角状态图标：显示最近一次操作的失败原因。 */
    public void setProgrammingStatus(boolean ok, List<Component> lines) {
        this.programmingStatusOk = ok;
        this.programmingStatusLines = lines == null ? List.of() : new ArrayList<>(lines);
        updateProgrammingStatus();
    }

    /** 失败闪烁期间控制状态按钮显隐。 */
    public void setProgrammingStatusBlinkVisible(boolean visible) {
        if (programmingStatusButton != null) {
            programmingStatusButton.visible = visible;
        }
    }

    private void updateProgrammingStatus() {
        if (programmingStatusButton == null) {
            return;
        }
        programmingStatusButton.icon = ElementsPlus.id(!programmingStatusOk
                ? "textures/gui/experiment_table/error.png"
                : (!programmingStatusLines.isEmpty()
                ? "textures/gui/experiment_table/success.png"
                : "textures/gui/experiment_table/idle.png"));
        programmingStatusButton.visible = true;
        if (!programmingStatusLines.isEmpty()) {
            MutableComponent joined = Component.empty();
            for (int i = 0; i < programmingStatusLines.size(); i++) {
                if (i > 0) {
                    // Tooltip 只接受一个 Component，不能用 \n 换行（会显示为字面 LF）。
                    joined.append(Component.literal("；"));
                }
                joined.append(programmingStatusLines.get(i).copy());
            }
            programmingStatusButton.setTooltip(Tooltip.create(joined));
        } else {
            programmingStatusButton.setTooltip(null);
        }
    }

    private void openCustomComponentDialog() {
        if (tablePos == null) {
            return;
        }
        ItemStack stack = menu.slots.get(0).getItem();
        if (stack.isEmpty() || !stack.is(ModItems.CIRCUIT_DIAGRAM) || CircuitItemData.isCompiled(stack)
                || stack.has(com.elementsplus.ModDataComponents.EQUIVALENT_COMPONENT)) {
            return;
        }
        CircuitDiagram diagram = stack.get(com.elementsplus.ModDataComponents.CIRCUIT_DIAGRAM);
        if (diagram == null) {
            return;
        }
        CustomComponentConfigDialog dialog = new CustomComponentConfigDialog(tablePos, diagram, font, width, height,
                payload -> ClientPlayNetworking.send(payload),
                () -> {
                    customComponentDialog = null;
                    restoreLayoutBounds();
                });
        customComponentDialog = dialog;
        applyModalBounds(dialog.getWindowX(), dialog.getWindowWidth());
    }

    public static class ProgressBar extends AbstractWidget {

        public float progress = 0.0f;
        public int color = 0xFF00A000;

        public ProgressBar(int x, int y, int width, int height) {
            super(x, y, width, height, Component.empty());
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int i, int j, float f) {
            GuiUtil.drawSubPanel(guiGraphics, this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), 0xFF808080, GuiUtil.SubPanelType.CONCAVE);
            guiGraphics.fill(this.getX() + 1, this.getY() + 1, (int) (this.getX() + 1 + (this.getWidth() - 2) * this.progress), this.getY() + this.getHeight() - 1, this.color);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        }
    }

    public static class TestCaseWidget extends AbstractWidget {

        public int color;
        public Component centerText;
        public Component cornerText;
        public Component bottomText;
        public TestCase testCase;
        public int testIndex = -1;

        public TestCaseWidget(int x, int y, int width, int height) {
            super(x, y, width, height, Component.empty());
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int i, int j, float f) {
            GuiUtil.drawSubPanel(guiGraphics, this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), this.color, GuiUtil.SubPanelType.CONVEX);
            Font font = Minecraft.getInstance().font;
            if (centerText != null) {
                guiGraphics.drawCenteredString(font, centerText, this.getX() + this.getWidth() / 2, this.getY() + this.getHeight() / 2 - font.lineHeight / 2, 0xFFFFFFFF);
            }
            if (bottomText != null) {
                guiGraphics.drawCenteredString(font, bottomText, this.getX() + this.getWidth() / 2, this.getY() + this.getHeight() / 2 - font.lineHeight / 2 + font.lineHeight, 0xFFFFFFFF);
            }
            if (cornerText != null) {
                guiGraphics.drawString(font, cornerText, this.getX() + 4, this.getY() + 4, 0xFFFFFFFF);
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {

        }
    }

    public void updateCurrentExperimentDisplay() {
        final int CURRENT_EXPERIMENT_MAX_WIDTH = 100;
        if (selectedExperiment == null) {
            Component text = Component.translatable("gui.elements-plus.experiment_table.no_experiment");
            currentExperimentWidget.setWidth(Math.min(10 + font.width(text), CURRENT_EXPERIMENT_MAX_WIDTH));
        } else {
            currentExperimentWidget.setWidth(Math.min(12 + (selectedExperiment.getIcon() != null ? 16 : 0) + font.width(selectedExperiment.getDisplayName()), CURRENT_EXPERIMENT_MAX_WIDTH));
        }
        startExperimentButton.setX(layoutLeftPos + currentExperimentWidget.getWidth() + 114);
        progressBar.setX(layoutLeftPos + currentExperimentWidget.getWidth() + 114 + 16 + 5);
        progressBar.setWidth(layoutImageWidth - currentExperimentWidget.getWidth() - 162);
        testCasePanelWidget.clearChildren();
        testCaseWidgets.clear();
        if (selectedExperiment != null) {
            int x = 0;
            int y = 0;
            int i = 1;
            for (TestCase testCase : selectedExperiment.getTestCases()) {
                TestCaseWidget testCaseWidget = new TestCaseWidget(x, y, 50, 50);
                testCaseWidget.color = 0xFF808080;
                testCaseWidget.centerText = Component.translatable("gui.elements-plus.experiment_table.test_waiting");
                testCaseWidget.cornerText = Component.translatable("#%s", i);
                testCaseWidget.testCase = testCase;
                testCaseWidget.testIndex = i - 1;
                testCasePanelWidget.addChild(testCaseWidget);
                testCaseWidgets.add(testCaseWidget);
                x += 50;
                if (x + 50 > testCasePanelWidget.getWidth()) {
                    x = 0;
                    y += 50;
                }
                i++;
            }
        }
        // 当前测例结果属于另一个实验时视为失效
        if (!Objects.equals(resultsExperimentName, selectedExperiment == null ? null : selectedExperiment.getName())) {
            currentTestResults.clear();
            currentTestTicks.clear();
            currentProgress = 0f;
            currentTestIndex = -1;
        }
        applyTestResultsToWidgets();
    }

    public void setCurrentExperiment(BaseExperiment experiment) {
        setCurrentExperiment(experiment, true);
    }

    private void setCurrentExperiment(BaseExperiment experiment, boolean announce) {
        BaseExperiment previous = selectedExperiment;
        selectedExperiment = experiment;
        if (previous != experiment && status != ExperimentStatus.BUSY) {
            currentFinalScore = -1;
        }
        if (announce && tablePos != null && experiment != null) {
            ClientPlayNetworking.send(new ExperimentTableSelectionUpdatePayload(tablePos, null, experiment.getName()));
        }
        updateCurrentExperimentDisplay();
    }

    public void onStatusUpdate(BlockPos pos, ExperimentStatus status, boolean paused, String experimentName, float progress,
                               List<Integer> testResults, List<Integer> testTicks, List<String> tooltipJson,
                               ExperimentFailureInfo failureInfo, int currentTestIndex,
                               SequentialFailureInfo sequentialFailure, int finalScore) {
        if (tablePos == null || !pos.equals(tablePos)) {
            return;
        }
        this.status = status;
        this.tablePaused = paused;
        this.currentProgress = progress;
        this.resultsExperimentName = experimentName;
        this.currentTestResults.clear();
        this.currentTestResults.addAll(testResults);
        this.currentTestTicks.clear();
        this.currentTestTicks.addAll(testTicks);
        this.failureInfo = failureInfo;
        this.currentTestIndex = currentTestIndex;
        this.currentFinalScore = finalScore;
        ProgrammingFailureInfo oldProgramming = this.sequentialFailure == null
                ? null : this.sequentialFailure.programming();
        if ((this.sequentialFailure != sequentialFailure) && this.sequentialDialog != null) {
            this.sequentialDialog = null;
            restoreLayoutBounds();
        }
        this.sequentialFailure = sequentialFailure;
        if ((status != ExperimentStatus.ERROR && status != ExperimentStatus.SUCCESS
                || sequentialFailure == null) && this.sequentialDialog != null) {
            this.sequentialDialog = null;
            restoreLayoutBounds();
        }
        ProgrammingFailureInfo newProgramming = sequentialFailure == null
                ? null : sequentialFailure.programming();
        if ((oldProgramming != newProgramming
                || status != ExperimentStatus.ERROR && status != ExperimentStatus.SUCCESS
                || newProgramming == null) && this.programmingDialog != null) {
            this.programmingDialog = null;
            restoreLayoutBounds();
        }
        this.statusTooltipLines.clear();
        if (sequentialFailure != null && sequentialFailure.programming() != null) {
            ProgrammingFailureInfo programming = sequentialFailure.programming();
            statusTooltipLines.add(Component.translatable(
                    "experiment.elements-plus.programming.wrong_byte", programming.wrongIndex() + 1));
            statusTooltipLines.add(Component.translatable(
                    "experiment.elements-plus.testfail.expected",
                    hexAt(programming.expectedOutput(), programming.wrongIndex())));
            statusTooltipLines.add(Component.translatable(
                    "experiment.elements-plus.testfail.actual",
                    hexAt(programming.actualOutput(), programming.wrongIndex())));
            statusTooltipLines.add(Component.translatable(
                    "experiment.elements-plus.testfail.click_for_data"));
        } else if (failureInfo != null) {
            statusTooltipLines.addAll(buildFailureTooltipLines(failureInfo));
        } else {
            for (String json : tooltipJson) {
                statusTooltipLines.add(decodeTooltip(json));
            }
        }
        if (status == ExperimentStatus.BUSY && experimentName != null) {
            BaseExperiment running = BuiltinExperiments.byId(experimentName);
            if (running != null && running != selectedExperiment) {
                setCurrentExperiment(running, false);
            }
        }
        updateStatusButton();
        updateProgressBar();
        applyTestResultsToWidgets();
    }

    private Component decodeTooltip(String json) {
        try {
            if (Minecraft.getInstance().getConnection() != null) {
                return Component.Serializer.fromJson(json, Minecraft.getInstance().getConnection().registryAccess());
            }
        } catch (Exception e) {
            // 忽略，回退为原始文本
        }
        return Component.literal(json);
    }

    private void cycleNumberFormat(IconButton button, boolean digital) {
        NumberFormat current = digital
                ? ClientConfig.get().digitalNumberFormat
                : ClientConfig.get().analogNumberFormat;
        NumberFormat next = Screen.hasShiftDown() ? current.previous() : current.next();
        if (digital) {
            ClientConfig.get().digitalNumberFormat = next;
        } else {
            ClientConfig.get().analogNumberFormat = next;
        }
        ClientConfig.get().save();
        button.icon = LithographyMachineScreen.numberFormatIcon(digital, next);
        refreshFailureTooltip();
    }

    /** 数字格式切换后，按当前格式重建失败测例信息。 */
    private void refreshFailureTooltip() {
        if (failureInfo != null) {
            statusTooltipLines.clear();
            statusTooltipLines.addAll(buildFailureTooltipLines(failureInfo));
        }
        updateStatusButton();
    }

    private List<Component> buildFailureTooltipLines(ExperimentFailureInfo failure) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("experiment.elements-plus.testfail.header"));
        lines.add(Component.translatable("experiment.elements-plus.testfail.input", formatPins(failure.inputs())));
        lines.add(Component.translatable("experiment.elements-plus.testfail.expected", formatPins(failure.expected())));
        lines.add(Component.translatable("experiment.elements-plus.testfail.actual", formatPins(failure.actual())));
        if (sequentialFailure != null) {
            lines.add(Component.translatable("experiment.elements-plus.testfail.click_for_data"));
        }
        return lines;
    }

    private static String hexAt(byte[] data, int index) {
        if (data == null || index < 0 || index >= data.length) {
            return "--";
        }
        return String.format("%02X", data[index] & 0xFF);
    }

    private static Component formatPins(List<ExperimentFailureInfo.Pin> pins) {
        if (pins == null) {
            return Component.empty();
        }
        MutableComponent result = Component.empty();
        boolean first = true;
        for (ExperimentFailureInfo.Pin pin : pins) {
            if (pin == null) {
                continue;
            }
            if (!first) {
                result.append(Component.literal(", "));
            }
            first = false;
            String label = pin.label() == null ? "" : pin.label();
            result.append(Component.literal(label + "="));
            byte[] values = pin.values();
            int[] lanes = new int[values == null ? 0 : values.length];
            for (int i = 0; i < lanes.length; i++) {
                lanes[i] = values[i] & 0xF;
            }
            result.append(NumberFormat.formatLanesComponent(lanes));
        }
        return result;
    }

    private void updateStatusButton() {
        if (statusButton == null) {
            return;
        }
        statusButton.icon = switch (status) {
            case BUSY -> ElementsPlus.id("textures/gui/experiment_table/busy.png");
            case SUCCESS -> ElementsPlus.id("textures/gui/experiment_table/success.png");
            case ERROR -> ElementsPlus.id("textures/gui/experiment_table/error.png");
            case IDLE -> ElementsPlus.id("textures/gui/experiment_table/idle.png");
        };
        if ((status == ExperimentStatus.ERROR || status == ExperimentStatus.SUCCESS)
                && (!statusTooltipLines.isEmpty() || currentFinalScore >= 0)) {
            MutableComponent lines = Component.literal("");
            for (int i = 0; i < statusTooltipLines.size(); i++) {
                if (i > 0) {
                    lines.append("\n");
                }
                lines.append(statusTooltipLines.get(i).copy());
            }
            if (currentFinalScore >= 0) {
                if (!statusTooltipLines.isEmpty()) {
                    lines.append("\n");
                }
                lines.append(Component.translatable("gui.elements-plus.experiment_table.score",
                        currentFinalScore));
            }
            statusButton.setTooltip(Tooltip.create(lines));
        } else {
            statusButton.setTooltip(null);
        }
    }

    private void updateProgressBar() {
        if (progressBar == null) {
            return;
        }
        progressBar.progress = Math.max(0f, Math.min(1f, currentProgress));
        progressBar.color = status == ExperimentStatus.ERROR ? 0xFFC00000 : 0xFF00A000;
    }

    private void applyTestResultsToWidgets() {
        for (TestCaseWidget widget : testCaseWidgets) {
            int result = widget.testIndex >= 0 && widget.testIndex < currentTestResults.size() ? currentTestResults.get(widget.testIndex) : -1;
            int ticks = widget.testIndex >= 0 && widget.testIndex < currentTestTicks.size()
                    ? currentTestTicks.get(widget.testIndex) : -1;
            boolean sequential = widget.testCase instanceof CircuitExperiment.SequentialTestCase;
            boolean architecture = widget.testCase instanceof CircuitExperiment.ArchitectureTestCase;
            boolean programming = widget.testCase instanceof ProgrammingExperiment.Case;
            boolean timed = sequential || architecture || programming;
            boolean interactive = widget.testCase instanceof CircuitExperiment.InteractiveTestCase;
            widget.bottomText = null;
            if (status == ExperimentStatus.BUSY && (timed || interactive)
                    && result < 0 && widget.testIndex == currentTestIndex) {
                widget.color = 0xFF3A6EC5;
                widget.centerText = Component.translatable("gui.elements-plus.experiment_table.test_running");
            } else if (result == 1) {
                widget.color = 0xFF00A000;
                widget.centerText = Component.translatable("gui.elements-plus.experiment_table.test_correct");
                if (interactive) {
                    if (ticks >= 0) {
                        widget.bottomText = Component.translatable("gui.elements-plus.experiment_table.test_ticks", ticks);
                    }
                } else if (programming) {
                    if (ticks >= 0) {
                        widget.bottomText = Component.translatable("gui.elements-plus.experiment_table.test_ticks", ticks);
                    }
                } else if (architecture) {
                    widget.bottomText = Component.translatable("gui.elements-plus.experiment_table.test_ticks",
                            ((CircuitExperiment.ArchitectureTestCase) widget.testCase).getTickCount());
                } else if (sequential) {
                    widget.bottomText = Component.translatable("gui.elements-plus.experiment_table.test_ticks",
                            ((CircuitExperiment.SequentialTestCase) widget.testCase).getTickCount());
                }
            } else if (result == 2) {
                widget.color = 0xFF8000FF;
                widget.centerText = Component.translatable("gui.elements-plus.experiment_table.test_tle");
            } else if (result == 3) {
                widget.color = 0xFF00008B;
                widget.centerText = Component.translatable("gui.elements-plus.experiment_table.test_re");
            } else if (result == 0) {
                widget.color = 0xFFA00000;
                widget.centerText = Component.translatable("gui.elements-plus.experiment_table.test_wrong");
                if (programming && ticks >= 0) {
                    widget.bottomText = Component.translatable(
                            "gui.elements-plus.experiment_table.test_ticks", ticks);
                } else if (timed && sequentialFailure != null && sequentialFailure.testIndex() == widget.testIndex) {
                    int totalTicks = architecture
                            ? ((CircuitExperiment.ArchitectureTestCase) widget.testCase).getTickCount()
                            : ((CircuitExperiment.SequentialTestCase) widget.testCase).getTickCount();
                    widget.bottomText = Component.translatable("gui.elements-plus.experiment_table.test_ticks_fraction",
                            sequentialFailure.failedTick(), totalTicks);
                }
            } else {
                widget.color = 0xFF808080;
                widget.centerText = Component.translatable("gui.elements-plus.experiment_table.test_waiting");
            }
        }
    }

    public boolean hasCompletedExperiment(BaseExperiment experiment) {
        return experiment != null && experiment.getName() != null && completedExperiments.contains(experiment.getName());
    }

    public void setSelectedChapter(ExperimentChapter chapter) {
        this.selectedChapter = chapter;
        // 不改变选中的实验，允许在实验过程中浏览其他章节
        experimentChapterWidget.clearChildren();
        experimentChapterWidget.scrollToTop();
        int y = 10;
        for (ExperimentChapter.Section section : chapter.sections) {
            if (section instanceof ExperimentChapter.MarkdownSection markdownSection) {
                y = addMarkdownChapter(markdownSection, y);
            } else if (section instanceof ExperimentChapter.TextSection textSection) {
                RichTextSectionWidget textSectionWidget = new RichTextSectionWidget(5, y, experimentChapterWidget.getWidth() - 10, textSection.text);
                experimentChapterWidget.addChild(textSectionWidget);
                y += textSectionWidget.getHeight();
            } else if (section instanceof ExperimentChapter.DynamicTextSection dynamicTextSection) {
                RichTextSectionWidget textSectionWidget = new RichTextSectionWidget(5, y,
                        experimentChapterWidget.getWidth() - 10, dynamicTextSection.text.apply(worldSeed));
                experimentChapterWidget.addChild(textSectionWidget);
                y += textSectionWidget.getHeight();
            } else if (section instanceof ExperimentChapter.ImageSection imageSection) {
                TranslatedImage imageSectionWidget = new TranslatedImage(5, y, experimentChapterWidget.getWidth() - 15, imageSection.image, imageSection.width, imageSection.height);
                experimentChapterWidget.addChild(imageSectionWidget);
                y += imageSectionWidget.getHeight();
            } else if (section instanceof ExperimentChapter.ExperimentSection experimentSection) {
                y = addExperimentList(y, experimentSection.experiments, experimentSection.optional);
            }
            y += 10;
        }
        experimentChapterWidget.addChild(new EmptyWidget(0, y));
    }

    /**
     * 按 Markdown 中出现的顺序添加正文和实验块；data 中定义但 Markdown
     * 没有提到的实验会在最后统一追加。
     */
    private int addMarkdownChapter(ExperimentChapter.MarkdownSection section, int y) {
        Map<String, BaseExperiment> allowed = new HashMap<>();
        for (BaseExperiment experiment : section.experiments) {
            if (experiment.getName() != null) {
                allowed.put(experiment.getName(), experiment);
                allowed.put("elements-plus:" + experiment.getName(), experiment);
            }
        }

        Set<BaseExperiment> seen = new HashSet<>();
        for (ExperimentMarkdownParser.Part part : ExperimentMarkdownParser.load(section.markdown)) {
            if (part instanceof ExperimentMarkdownParser.TextPart textPart) {
                RichTextSectionWidget textWidget = new RichTextSectionWidget(
                        5, y, experimentChapterWidget.getWidth() - 10, textPart.lines(), true);
                experimentChapterWidget.addChild(textWidget);
                y += textWidget.getHeight();
            } else if (part instanceof ExperimentMarkdownParser.ImagePart imagePart) {
                MarkdownImageWidget imageWidget = new MarkdownImageWidget(
                        5, y, experimentChapterWidget.getWidth() - 10,
                        imagePart.image(), imagePart.description(), imagePart.rawPath());
                experimentChapterWidget.addChild(imageWidget);
                y += imageWidget.getHeight();
            } else if (part instanceof ExperimentMarkdownParser.HorizontalRulePart) {
                AbstractWidget ruleWidget = new AbstractWidget(
                        5, y, experimentChapterWidget.getWidth() - 10, 1, Component.empty()) {
                    @Override
                    protected void renderWidget(GuiGraphics guiGraphics, int i, int j, float f) {
                        guiGraphics.fill(this.getX(), this.getY(),
                                this.getX() + this.getWidth(), this.getY() + 1, 0xFF808080);
                    }

                    @Override
                    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
                    }
                };
                experimentChapterWidget.addChild(ruleWidget);
                y += ruleWidget.getHeight();
            } else if (part instanceof ExperimentMarkdownParser.ExperimentPart experimentPart) {
                List<BaseExperiment> experiments = new ArrayList<>();
                for (String id : experimentPart.experimentIds()) {
                    BaseExperiment experiment = allowed.get(normalizeExperimentId(id));
                    if (experiment != null && seen.add(experiment)) {
                        experiments.add(experiment);
                    }
                }
                if (!experiments.isEmpty()) {
                    y = addExperimentList(y, experiments, section.optional);
                }
            } else if (part instanceof ExperimentMarkdownParser.SpecialTextPart specialPart) {
                for (ResourceLocation provider : specialPart.providers()) {
                    if (!section.specialText.contains(provider)) {
                        ElementsPlus.LOGGER.warn("Markdown chapter {} references undeclared text provider {}",
                                section.markdown, provider);
                        continue;
                    }
                    RichTextSectionWidget textWidget = new RichTextSectionWidget(
                            5, y, experimentChapterWidget.getWidth() - 10,
                            BuiltinExperimentChapters.dynamicText(provider, worldSeed));
                    experimentChapterWidget.addChild(textWidget);
                    y += textWidget.getHeight();
                }
            }
            y += 10;
        }

        List<BaseExperiment> remaining = new ArrayList<>();
        for (BaseExperiment experiment : section.experiments) {
            if (seen.add(experiment)) {
                remaining.add(experiment);
            }
        }
        if (!remaining.isEmpty()) {
            y = addExperimentList(y, remaining, section.optional);
            y += 10;
        }
        return y;
    }

    private static String normalizeExperimentId(String id) {
        ResourceLocation location = ResourceLocation.tryParse(id);
        if (location != null && location.getNamespace().equals(ElementsPlus.MOD_ID)) {
            return location.getPath();
        }
        return id;
    }

    private int addExperimentList(int y, List<BaseExperiment> experiments, Set<BaseExperiment> optional) {
        if (experiments.isEmpty()) {
            return y;
        }
        ScrollPanelWidget experimentSectionWidget = new ScrollPanelWidget(5, y,
                experimentChapterWidget.getWidth() - 10, experiments.size() * 25 + 2,
                GuiUtil.SubPanelType.NONE, 0x00000000) {{
            int childY = 0;
            for (BaseExperiment experiment : experiments) {
                AbstractWidget experimentWidget = new AbstractWidget(0, childY,
                        experimentChapterWidget.getWidth() - 11, 20, Component.empty()) {
                    @Override
                    protected void renderWidget(GuiGraphics guiGraphics, int i, int j, float f) {
                        guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), isHovered ? 0xFFFFFFFF : 0xFF000000);
                        boolean selected = experiment == selectedExperiment;
                        GuiUtil.drawSubPanel(guiGraphics, this.getX() + 1, this.getY() + 1,
                                this.getX() + this.getWidth() - 1, this.getY() + this.getHeight() - 1,
                                selected ? 0xFF808080 : 0xFFA0A0A0,
                                selected ? GuiUtil.SubPanelType.CONCAVE : GuiUtil.SubPanelType.CONVEX);
                        if (experiment.getIcon() != null) {
                            guiGraphics.blit(experiment.getIcon(), 5 + this.getX(), this.getY() + 2, 0, 0, 16, 16, 16, 16);
                        }
                        Component displayName = experiment.getDisplayName();
                        if (optional != null && optional.contains(experiment)) {
                            displayName = Component.translatable("gui.elements-plus.experiment_table.optional",
                                    displayName.copy().withStyle(ChatFormatting.WHITE)).withStyle(ChatFormatting.DARK_GRAY);
                        }
                        guiGraphics.drawString(font, displayName, 5 + this.getX() + 16 + 2,
                                this.getY() + this.getHeight() / 2 - font.lineHeight / 2,
                                0xFFFFFFFF, false);
                        boolean completed = hasCompletedExperiment(experiment);
                        Integer score = experiment.getName() == null ? null
                                : experimentScores.get(experiment.getName());
                        if (score != null && experiment.usesScoreSystem()) {
                            Component scoreText = Component.nullToEmpty(String.valueOf(score));
                            int scoreRight = this.getX() + this.getWidth() - (completed ? 24 : 4);
                            guiGraphics.drawString(font, scoreText,
                                    scoreRight - font.width(scoreText),
                                    this.getY() + this.getHeight() / 2 - font.lineHeight / 2,
                                    completed ? 0xFF00A000 : 0xFFA00000, false);
                        }
                        if (completed) {
                            guiGraphics.blit(ElementsPlus.id("textures/gui/experiment_table/success.png"),
                                    5 + this.getX() + this.getWidth() - 24,
                                    this.getY() + this.getHeight() / 2 - 8,
                                    0, 0, 16, 16, 16, 16);
                        }
                    }

                    @Override
                    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
                    }

                    @Override
                    public void onClick(double d, double e) {
                        if (unlockedChapters.contains(selectedChapter.name)) {
                            setCurrentExperiment(experiment);
                        }
                    }
                };
                addChild(experimentWidget);
                childY += 25;
            }
        }};
        experimentChapterWidget.addChild(experimentSectionWidget);
        return y + experimentSectionWidget.getHeight();
    }

    public void onServerData(BlockPos pos, String selectedName, String selectedExperimentName,
                             Set<String> unlocked, Set<String> forced, Set<String> completed,
                             Map<String, Integer> scores, long worldSeed) {
        if (tablePos != null && !pos.equals(tablePos)) {
            return;
        }
        this.tablePos = pos;
        this.worldSeed = worldSeed;
        if (programmingPage != null) {
            programmingPage.onOpen();
        }
        unlockedChapters.clear();
        unlockedChapters.addAll(unlocked);
        forcedChapters.clear();
        forcedChapters.addAll(forced);
        completedExperiments.clear();
        completedExperiments.addAll(completed);
        experimentScores.clear();
        if (scores != null) {
            experimentScores.putAll(scores);
        }
        if (selectedChapter != null) {
            setSelectedChapter(selectedChapter);
        }
        for (ChapterEntryButton button : chapterButtons) {
            boolean locked = !unlockedChapters.contains(button.chapter.name);
            button.locked = locked;
            button.setTooltip(Tooltip.create(buildChapterTooltip(button.chapter, locked)));
        }
        ChapterEntryButton target = null;
        if (selectedName != null) {
            for (ChapterEntryButton button : chapterButtons) {
                if (button.chapter.name.equals(selectedName)) {
                    target = button;
                    break;
                }
            }
        }
        if (target == null && !chapterButtons.isEmpty()) {
            target = chapterButtons.get(0);
        }
        if (target != null && target.chapter != selectedChapter) {
            experimentGroup.setSelected(target);
            setSelectedChapter(target.chapter);
        }
        if (selectedExperimentName != null) {
            BaseExperiment experiment = BuiltinExperiments.byId(selectedExperimentName);
            if (experiment != null) {
                setCurrentExperiment(experiment, false);
            }
        } else {
            setCurrentExperiment(null, false);
        }
    }

    /**
     * 章节的全部（非可选）实验是否都已通过；与服务端解锁判定一致。
     */
    private boolean isChapterComplete(ExperimentChapter chapter) {
        for (BaseExperiment experiment : chapter.getExperiments()) {
            if (!completedExperiments.contains(experiment.getName())) {
                return false;
            }
        }
        return true;
    }

    /**
     * 章节条目的悬停提示：第一行显示完整名称；未解锁时再追加原有前置章节提示。
     */
    private Component buildChapterTooltip(ExperimentChapter chapter, boolean locked) {
        MutableComponent tooltip = chapter.getDisplayName().copy();
        if (locked) {
            tooltip.append(Component.literal("\n")).append(buildLockedChapterTooltip(chapter));
        }
        return tooltip;
    }

    /**
     * 未解锁章节的悬停提示：列出尚未完成的前置章节。
     */
    private Component buildLockedChapterTooltip(ExperimentChapter chapter) {
        MutableComponent tooltip = Component.translatable("gui.elements-plus.experiment_table.unfinished_prerequisites");
        boolean any = false;
        for (ExperimentChapter dependency : chapter.dependencies) {
            if (!isChapterComplete(dependency)) {
                tooltip.append(Component.literal("\n").append(dependency.getDisplayName().copy().withStyle(ChatFormatting.GRAY)));
                any = true;
            }
        }
        return !any ? Component.translatable("gui.elements-plus.experiment_table.unfinished_prerequisites") : tooltip;
    }

    public void onSelectionChanged(BlockPos pos, String chapterName, String experimentName) {
        if (tablePos == null || !pos.equals(tablePos)) {
            return;
        }
        if (chapterName != null && !chapterName.equals(selectedChapter.name)) {
            for (ChapterEntryButton button : chapterButtons) {
                if (button.chapter.name.equals(chapterName)) {
                    experimentGroup.setSelected(button);
                    setSelectedChapter(button.chapter);
                    break;
                }
            }
        }
        if (experimentName != null) {
            BaseExperiment experiment = BuiltinExperiments.byId(experimentName);
            if (experiment != null) {
                setCurrentExperiment(experiment, false);
            }
        } else {
            setCurrentExperiment(null, false);
        }
    }

    @Override
    public Point getSlotPosition(Slot slot) {
        if (slot.container instanceof Inventory && slot.getContainerSlot() >= 0 && slot.getContainerSlot() <= 35) {
            if (collapseButtonInventory != null && collapseButtonInventory.active) {
                return slotPosition.getOrDefault(slot.getContainerSlot(), Point.of(slot.x, slot.y));
            }
            return null;
        }
        if (slot == this.menu.extraSlot) {
            return new Point(slot.x, slot.y);
        }
        return null;
    }

    @Override
    protected void slotClicked(Slot slot, int slotId, int mouseButton, ClickType type) {
        // 编程页收起物品栏时，物品栏槽位不可交互；展开时与实验页一致。
        if (activeTab == Tab.PROGRAMMING && !inventoryActive && slot != this.menu.extraSlot) {
            return;
        }
        super.slotClicked(slot, slotId, mouseButton, type);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int i, int j) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFFFFFFFF, false);
    }

    private void updateScreenSize() {
        if (activeTab == Tab.PROGRAMMING) {
            int desired = Math.clamp(this.width - 60, 260, 620);
            this.layoutImageWidth = Math.min(desired, Math.max(200, this.width - 20));
        } else {
            this.layoutImageWidth = Math.clamp(this.width - 80, 220, 300);
        }
        // 小窗口下也要保证面板不超出屏幕。
        this.imageHeight = Math.min(Math.max(230, this.height - 40),
                Math.max(120, this.height - 10));
        this.topPos = this.height / 2 - this.imageHeight / 2;
        this.layoutLeftPos = this.width / 2 - this.layoutImageWidth / 2;
        // 仅供外部模组（JEI 等）读取；实验桌内部布局使用 layout* 字段。
        this.imageWidth = this.layoutImageWidth;
        this.leftPos = this.layoutLeftPos;
    }

    private void applyModalBounds(int x, int width) {
        // 保持 leftPos 与实验桌布局一致，避免标题和槽位因 JEI 避让而整体左移。
        // 只扩大对外 imageWidth，让 JEI 把侧边栏放到弹窗右侧。
        this.leftPos = this.layoutLeftPos;
        int right = Math.max(this.layoutLeftPos + this.layoutImageWidth, x + width);
        this.imageWidth = Math.max(this.layoutImageWidth, right - this.layoutLeftPos);
    }

    private void restoreLayoutBounds() {
        this.leftPos = this.layoutLeftPos;
        this.imageWidth = this.layoutImageWidth;
    }

    @Override
    protected void renderSlot(GuiGraphics guiGraphics, Slot slot) {
        Point point = getSlotPosition(slot);
        if (point != null) {
            super.renderSlot(guiGraphics, new Slot(slot.container, slot.getContainerSlot(), point.x(), point.y()));
        }
    }

    private class ChapterEntryButton extends ListEntryButton {
        private final ExperimentChapter chapter;
        private boolean locked = true;

        ChapterEntryButton(int x, int y, int width, int height, ExperimentChapter chapter) {
            super(x, y, width, height, chapter.getDisplayName());
            this.chapter = chapter;
            this.setTooltip(Tooltip.create(chapter.getDisplayName()));
        }

        private boolean hasCompletionMark() {
            return !locked && unlockedChapters.contains(chapter.name) && isChapterComplete(chapter);
        }

        @Override
        public void onClick(double d, double e) {
            if (locked) {
                return;
            }
            super.onClick(d, e);
            if (tablePos != null) {
                ClientPlayNetworking.send(new ExperimentTableSelectionUpdatePayload(tablePos, chapter.name, null));
            }
            setSelectedChapter(chapter);
        }

        @Override
        public void renderString(GuiGraphics guiGraphics, Font font, int i) {
            boolean completionMark = hasCompletionMark();
            int left = this.getX() + 3;
            int right = this.getX() + this.getWidth() - (completionMark ? 20 : 3);
            int color = locked ? 0xFF909090 : (forcedChapters.contains(chapter.name) ? 0xFFFF55FF : i);
            guiGraphics.enableScissor(left, this.getY(), right, this.getY() + this.getHeight());
            GuiUtil.drawTruncatedText(guiGraphics, font, this.getMessage(), left,
                    this.getY() + (this.getHeight() - 9) / 2, Math.max(1, right - left), color, true);
            guiGraphics.disableScissor();
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int i, int j, float f) {
            super.renderWidget(guiGraphics, i, j, f);
            if (hasCompletionMark()) {
                guiGraphics.blit(ElementsPlus.id("textures/gui/experiment_table/success.png"), this.getX() + this.getWidth() - 18, this.getY() + this.getHeight() / 2 - 8, 0, 0, 16, 16, 16, 16);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (customComponentDialog != null) {
            return customComponentDialog.mouseClicked(mouseX, mouseY, button);
        }
        if (programmingDialog != null) {
            return programmingDialog.mouseClicked(mouseX, mouseY, button);
        }
        if (sequentialDialog != null) {
            return sequentialDialog.mouseClicked(mouseX, mouseY, button);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (activeTab == Tab.PROGRAMMING && programmingPage != null
                && programmingPage.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
            return true;
        }
        if (customComponentDialog != null) {
            return customComponentDialog.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }
        if (programmingDialog != null || sequentialDialog != null) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (activeTab == Tab.PROGRAMMING && programmingPage != null
                && programmingPage.mouseReleased(mouseX, mouseY, button)) {
            return true;
        }
        if (customComponentDialog != null) {
            return customComponentDialog.mouseReleased(mouseX, mouseY, button);
        }
        if (programmingDialog != null || sequentialDialog != null) {
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (activeTab == Tab.PROGRAMMING && programmingPage != null
                && programmingPage.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) {
            return true;
        }
        if (customComponentDialog != null) {
            return customComponentDialog.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
        if (programmingDialog != null) {
            return programmingDialog.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
        if (sequentialDialog != null) {
            return sequentialDialog.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (activeTab == Tab.PROGRAMMING && programmingPage != null
                && programmingPage.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (customComponentDialog != null) {
            return customComponentDialog.keyPressed(keyCode, scanCode, modifiers);
        }
        if (programmingDialog != null) {
            return programmingDialog.keyPressed(keyCode, scanCode, modifiers);
        }
        if (sequentialDialog != null) {
            return sequentialDialog.keyPressed(keyCode, scanCode, modifiers);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char c, int modifiers) {
        if (activeTab == Tab.PROGRAMMING && programmingPage != null
                && programmingPage.charTyped(c, modifiers)) {
            return true;
        }
        if (customComponentDialog != null) {
            return customComponentDialog.charTyped(c, modifiers);
        }
        if (programmingDialog != null) {
            return programmingDialog.charTyped(c, modifiers);
        }
        if (sequentialDialog != null) {
            return sequentialDialog.charTyped(c, modifiers);
        }
        return super.charTyped(c, modifiers);
    }

    @Override
    protected void containerTick() {
        if (activeTab == Tab.PROGRAMMING && programmingPage != null) {
            programmingPage.tick();
        }
    }

    @Override
    public void onClose() {
        if (programmingPage != null) {
            programmingPage.onScreenClosed();
        }
        super.onClose();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float f) {
        super.render(guiGraphics, mouseX, mouseY, f);
        if (activeTab == Tab.EXPERIMENT && statusButton != null && currentFinalScore >= 0) {
            Component score = Component.nullToEmpty(String.valueOf(currentFinalScore));
            guiGraphics.drawCenteredString(font, score,
                    statusButton.getX() + 8,
                    statusButton.getY() + 4,
                    status == ExperimentStatus.ERROR ? 0xFFFF5555 : 0xFF55FF55);
        }
        this.renderTooltip(guiGraphics, mouseX, mouseY);
        if (programmingDialog != null) {
            programmingDialog.render(guiGraphics, mouseX, mouseY, f);
        }
        if (sequentialDialog != null) {
            sequentialDialog.render(guiGraphics, mouseX, mouseY, f);
        }
        if (customComponentDialog != null) {
            customComponentDialog.render(guiGraphics, mouseX, mouseY, f);
        }
        if (activeTab == Tab.PROGRAMMING && programmingPage != null) {
            programmingPage.renderDialogs(guiGraphics, mouseX, mouseY, f);
        }
    }
}