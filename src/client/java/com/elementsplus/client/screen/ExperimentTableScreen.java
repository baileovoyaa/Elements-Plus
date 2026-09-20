package com.elementsplus.client.screen;

import com.elementsplus.ElementsPlus;
import com.elementsplus.blocks.entity.ExperimentStatus;
import com.elementsplus.client.gui.*;
import com.elementsplus.core.experiment.*;
import com.elementsplus.menu.ExperimentTableMenu;
import com.elementsplus.network.ExperimentTableControlPayload;
import com.elementsplus.network.ExperimentTableDataRequestPayload;
import com.elementsplus.network.ExperimentTableSelectionUpdatePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
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
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class ExperimentTableScreen extends AbstractContainerScreen<ExperimentTableMenu> implements SlotPositionProvider {
    public TabButton tabButtonExperiment;
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
    public BaseExperiment selectedExperiment;
    public AbstractWidget currentExperimentWidget;

    public BlockPos tablePos;
    private final Set<String> unlockedChapters = new HashSet<>();
    private final Set<String> completedExperiments = new HashSet<>();
    private List<ChapterEntryButton> chapterButtons = new ArrayList<>();

    private ExperimentStatus status = ExperimentStatus.IDLE;
    private boolean tablePaused = false;
    private float currentProgress = 0f;
    private final List<Integer> currentTestResults = new ArrayList<>();
    private final List<Component> statusTooltipLines = new ArrayList<>();
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
        GuiUtil.drawMainPanel(guiGraphics, leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF404040);
        GuiUtil.drawMainPanel(guiGraphics, leftPos, topPos + 18, leftPos + imageWidth, topPos + imageHeight, 0xFFFFFFFF);
        if (inventoryActive) {
            GuiUtil.drawSubPanel(guiGraphics, leftPos + 5, topPos + imageHeight - 188, leftPos + 5 + 79, topPos + imageHeight - 5, 0xFFA0A0A0);
        }

        if (collapseButtonInventory.active) {
            for (Slot slot : this.menu.slots) {
                Point point = getSlotPosition(slot);
                if (point != null) {
                    GuiUtil.drawSlot(guiGraphics, this.leftPos + point.x() - 1, this.topPos + point.y() - 1);
                }
            }
        }

        Point point = getSlotPosition(this.menu.extraSlot);
        GuiUtil.drawSlot(guiGraphics, this.leftPos + point.x() - 1, this.topPos + point.y() - 1);

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
        updateScreenSize();
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
        this.addRenderableWidget(tabButtonExperiment = new TabButton(this.leftPos + this.font.width(this.title) + 10, this.topPos, 50, 22, Component.translatable("gui.elements-plus.experiment_table.tab")));
        tabButtonExperiment.active = true;

        this.addRenderableWidget(collapseButtonInventory = new CollapseButton(this.leftPos + 5, inventoryActive ? this.topPos + this.imageHeight - 188 : this.topPos + this.imageHeight - 23, 79, 18, Component.nullToEmpty("物品栏")) {
            @Override
            public void onClick(double d, double e) {
                super.onClick(d, e);
                inventoryActive = active;
                this.setY(inventoryActive ? topPos + imageHeight - 188 : topPos + imageHeight - 23);
                experimentWidget.setHeight(inventoryActive ? imageHeight - 218 : imageHeight - 53);
            }
        });
        collapseButtonInventory.active = inventoryActive;

        this.addRenderableWidget(experimentWidget = new ScrollPanelWidget(this.leftPos + 5, this.topPos + 25, 79, imageHeight - 218, GuiUtil.SubPanelType.BORDERED, 0xFFA0A0A0));
        experimentWidget.setHeight(inventoryActive ? imageHeight - 218 : imageHeight - 53);
        experimentWidget.overflowBehaviorX = ScrollPanelWidget.OverflowBehavior.CLIP;

        this.addRenderableWidget(experimentChapterWidget = new ScrollPanelWidget(leftPos + 9 + 80, topPos + 48, Math.max(1, imageWidth - 95), Math.max(1, imageHeight - 195), GuiUtil.SubPanelType.BORDERED, 0xFFA0A0A0));
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
            setSelectedChapter(chapterButtons.get(0).chapter);
        }

        ClientPlayNetworking.send(new ExperimentTableDataRequestPayload());

        this.addRenderableWidget(testCasePanelWidget = new ScrollPanelWidget(leftPos + 9 + 80, topPos + imageHeight - 125, Math.max(1, imageWidth - 95), 120, GuiUtil.SubPanelType.BORDERED, 0xFFA0A0A0));
        this.addRenderableWidget(collapseButtonTestCases = new CollapseButton(this.leftPos + 89, testCasesActive ? this.topPos + this.imageHeight - 142 : this.topPos + this.imageHeight - 23, imageWidth - 95, 18, Component.nullToEmpty("测例信息")) {
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
        this.addRenderableWidget(currentExperimentWidget = new AbstractWidget(leftPos + 110, topPos + 26, 0, 16, Component.empty()) {

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
            } else if (selectedExperiment != null) {
                ClientPlayNetworking.send(new ExperimentTableControlPayload(tablePos, ExperimentTableControlPayload.Action.START, selectedExperiment.getName()));
            }
        }));

        // 进度条（动态x，宽度可变）
        this.addRenderableWidget(progressBar = new ProgressBar(0, topPos + 26, 0, 16));

        // 实验状态
        this.addRenderableWidget(statusButton = new IconButton(leftPos + imageWidth - 22, topPos + 26, 16, 16, ElementsPlus.id("textures/gui/experiment_table/idle.png"), (button) -> {
        }));

        updateCurrentExperimentDisplay();
        updateStatusButton();
        updateProgressBar();
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
            if (cornerText != null) {
                guiGraphics.drawString(font, cornerText, this.getX() + 4, this.getY() + 4, 0xFFFFFFFF);
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {

        }
    }

    public void updateCurrentExperimentDisplay() {
        if (selectedExperiment == null) {
            Component text = Component.translatable("gui.elements-plus.experiment_table.no_experiment");
            currentExperimentWidget.setWidth(10 + font.width(text));
        } else {
            currentExperimentWidget.setWidth(12 + (selectedExperiment.getIcon() != null ? 16 : 0) + font.width(selectedExperiment.getDisplayName()));
        }
        startExperimentButton.setX(leftPos + currentExperimentWidget.getWidth() + 114);
        progressBar.setX(leftPos + currentExperimentWidget.getWidth() + 114 + 16 + 5);
        progressBar.setWidth(imageWidth - currentExperimentWidget.getWidth() - 162);
        testCasePanelWidget.clearChildren();
        testCaseWidgets.clear();
        if (selectedExperiment instanceof CircuitExperiment circuitExperiment) {
            int x = 0;
            int y = 0;
            int i = 1;
            for (TestCase testCase : circuitExperiment.testCases) {
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
            currentProgress = 0f;
        }
        applyTestResultsToWidgets();
    }

    public void setCurrentExperiment(BaseExperiment experiment) {
        setCurrentExperiment(experiment, true);
    }

    private void setCurrentExperiment(BaseExperiment experiment, boolean announce) {
        selectedExperiment = experiment;
        if (announce && tablePos != null && experiment != null) {
            ClientPlayNetworking.send(new ExperimentTableSelectionUpdatePayload(tablePos, null, experiment.getName()));
        }
        updateCurrentExperimentDisplay();
    }

    public void onStatusUpdate(BlockPos pos, ExperimentStatus status, boolean paused, String experimentName, float progress, List<Integer> testResults, List<String> tooltipJson) {
        if (tablePos == null || !pos.equals(tablePos)) {
            return;
        }
        this.status = status;
        this.tablePaused = paused;
        this.currentProgress = progress;
        this.resultsExperimentName = experimentName;
        this.currentTestResults.clear();
        this.currentTestResults.addAll(testResults);
        this.statusTooltipLines.clear();
        for (String json : tooltipJson) {
            statusTooltipLines.add(decodeTooltip(json));
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
        if (status == ExperimentStatus.ERROR && !statusTooltipLines.isEmpty()) {
            MutableComponent lines = Component.literal("");
            for (int i = 0; i < statusTooltipLines.size(); i++) {
                if (i > 0) {
                    lines.append("\n");
                }
                lines.append(statusTooltipLines.get(i).copy());
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
            if (result > 0) {
                widget.color = 0xFF00A000;
                widget.centerText = Component.translatable("gui.elements-plus.experiment_table.test_correct");
            } else if (result == 0) {
                widget.color = 0xFFA00000;
                widget.centerText = Component.translatable("gui.elements-plus.experiment_table.test_wrong");
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
            if (section instanceof ExperimentChapter.TextSection textSection) {
                TextSectionWidget textSectionWidget = new TextSectionWidget(5, y, experimentChapterWidget.getWidth() - 10, textSection.text);
                experimentChapterWidget.addChild(textSectionWidget);
                y += textSectionWidget.getHeight();
            } else if (section instanceof ExperimentChapter.ImageSection imageSection) {
                TranslatedImage imageSectionWidget = new TranslatedImage(5, y, experimentChapterWidget.getWidth() - 15, imageSection.image, imageSection.width, imageSection.height);
                experimentChapterWidget.addChild(imageSectionWidget);
                y += imageSectionWidget.getHeight();
            } else if (section instanceof ExperimentChapter.ExperimentSection experimentSection) {
                ScrollPanelWidget experimentSectionWidget = new ScrollPanelWidget(5, y, experimentChapterWidget.getWidth() - 10, experimentSection.experiments.size() * 25 + 2, GuiUtil.SubPanelType.NONE, 0x00000000) {{
                    int y = 0;
                    for (BaseExperiment experiment : experimentSection.experiments) {
                        AbstractWidget experimentWidget = new AbstractWidget(0, y, experimentChapterWidget.getWidth() - 11, 20, Component.empty()) {
                            @Override
                            protected void renderWidget(GuiGraphics guiGraphics, int i, int j, float f) {
                                guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), isHovered ? 0xFFFFFFFF : 0xFF000000);
                                boolean selected = experiment == selectedExperiment;
                                GuiUtil.drawSubPanel(guiGraphics, this.getX() + 1, this.getY() + 1, this.getX() + this.getWidth() - 1, this.getY() + this.getHeight() - 1, selected ? 0xFF808080 : 0xFFA0A0A0, selected ? GuiUtil.SubPanelType.CONCAVE : GuiUtil.SubPanelType.CONVEX);
                                if (experiment.getIcon() != null) {
                                    guiGraphics.blit(experiment.getIcon(), 5 + this.getX(), this.getY() + 2, 0, 0, 16, 16, 16, 16);
                                }
                                guiGraphics.drawString(font, experiment.getDisplayName(), 5 + this.getX() + 16 + 2, this.getY() + this.getHeight() / 2 - font.lineHeight / 2, 0xFFFFFFFF, false);
                                if (hasCompletedExperiment(experiment)) {
                                    guiGraphics.blit(ElementsPlus.id("textures/gui/experiment_table/success.png"), 5 + this.getX() + this.getWidth() - 24, this.getY() + this.getHeight() / 2 - 8, 0, 0, 16, 16, 16, 16);
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
                        y += 25;
                    }
                }};
                experimentChapterWidget.addChild(experimentSectionWidget);
                y += experimentSectionWidget.getHeight();
            }
            y += 10;
        }
        experimentChapterWidget.addChild(new EmptyWidget(0, y));
    }

    public void onServerData(BlockPos pos, String selectedName, String selectedExperimentName, Set<String> unlocked, Set<String> completed) {
        this.tablePos = pos;
        unlockedChapters.clear();
        unlockedChapters.addAll(unlocked);
        completedExperiments.clear();
        completedExperiments.addAll(completed);
        for (ChapterEntryButton button : chapterButtons) {
            button.locked = !unlockedChapters.contains(button.chapter.name);
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
            if (collapseButtonInventory.active) {
                return slotPosition.getOrDefault(slot.getContainerSlot(), Point.of(slot.x, slot.y));
            } else {
                return null;
            }
        } else {
            return new Point(slot.x, slot.y);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int i, int j) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFFFFFFFF, false);
    }

    private void updateScreenSize() {
        this.imageWidth = Math.clamp(this.width - 80, 220, 300);
        this.imageHeight = Math.max(230, this.height - 40);
        this.topPos = this.height / 2 - this.imageHeight / 2;
        this.leftPos = this.width / 2 - this.imageWidth / 2;
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
            int left = this.getX() + 3;
            int right = this.getX() + this.getWidth() - 3;
            int color = locked ? 0xFF808080 : i;
            guiGraphics.enableScissor(left, this.getY(), right, this.getY() + this.getHeight());
            guiGraphics.drawString(font, this.getMessage(), left, this.getY() + (this.getHeight() - 9) / 2, color);
            guiGraphics.disableScissor();
        }
    }
}