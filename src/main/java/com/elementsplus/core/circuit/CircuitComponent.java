package com.elementsplus.core.circuit;

import com.elementsplus.core.circuit.component.CircuitComponentInstance;
import com.elementsplus.core.circuit.component.SimpleComponentInstance;
import com.elementsplus.core.circuit.diagram.CircuitDiagram;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.UUID;
import java.util.function.Supplier;

public class CircuitComponent {
    private final ResourceLocation id;
    private Component name;
    private Component description;
    private ResourceLocation icon;
    private final int height;
    private final int width;
    private final PinType[] pins;
    private final int[] pinBitWidths;
    private final String[] pinNames;
    private Supplier<CircuitComponentInstance> instanceFactory = () -> SimpleComponentInstance.INSTANCE;
    private IngredientSupplier ingredientSupplier;

    // 自定义元件元数据；内置元件保持默认值。
    private boolean custom;
    private int color = 0xFF808080;
    @Nullable private UUID circuitId;
    @Nullable private UUID author;
    @Nullable private String customAuthorName;
    @Nullable private ResourceLocation customIconItem;
    @Nullable private String customIconText;
    @Nullable private CircuitDiagram customCircuit;
    @Nullable private CircuitDiagram.Scale customScale;


    public CircuitComponent(ResourceLocation id, Component name, Component description, ResourceLocation icon, int width, int height) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.icon = icon;
        this.width = width;
        this.height = height;

        this.pins = new PinType[2 * (width + height)];
        Arrays.fill(this.pins, PinType.NONE);
        this.pinBitWidths = new int[2 * (width + height)];
        this.pinNames = new String[2 * (width + height)];

        setSelfAsIngredientSupplier();
    }

    public CircuitComponent(ResourceLocation id, Component name, Component description, ResourceLocation icon) {
        this(id, name, description, icon, 1, 1);
    }

    public CircuitComponent(ResourceLocation id, Component name) {
        this(id, name, null, null, 1, 1);
    }

    public CircuitComponent(ResourceLocation id, String name) {
        this(id, Component.nullToEmpty(name));
    }

    public ResourceLocation getId() {
        return id;
    }

    public Component getName() {
        return name;
    }

    public ResourceLocation getIcon() {
        return icon;
    }

    public void setName(Component name) {
        this.name = name;
    }

    public void setIcon(ResourceLocation icon) {
        this.icon = icon;
    }

    public Component getDescription() {
        return description;
    }

    public void setDescription(Component description) {
        this.description = description;
    }

    public int getHeight() {
        return height;
    }

    public int getWidth() {
        return width;
    }

    public enum PinType {
        NONE,          // 没有引脚
        INPUT,         // 输入
        OUTPUT,        // 输出
    }

    private Integer index(Direction side, int offset) {
        return switch (side) {
            case DOWN, UP -> null;
            case NORTH -> offset; // 上边：从左到右
            case EAST -> width + offset; // 右边：从上到下
            case SOUTH -> width + height + offset; // 下边：从右到左
            case WEST -> 2 * width + height + offset; // 左边：从下到上
        };
    }

    public PinType getPin(Direction side, int offset) {
        Integer i = index(side, offset);
        return i != null ? pins[i] : null;
    }

    public void setPin(Direction side, int offset, PinType type) {
        Integer i = index(side, offset);
        if (i == null) return;
        pins[i] = type;
    }

    /**
     * 一次性设置引脚类型与名称，位宽默认为 1。
     */
    public void setPin(Direction side, int offset, PinType type, String name) {
        setPin(side, offset, type, name, 1);
    }

    /**
     * 一次性设置引脚类型、名称与位宽。位宽 {@code <= 0} 时按 1 处理。
     */
    public void setPin(Direction side, int offset, PinType type, String name, int bitWidth) {
        Integer i = index(side, offset);
        if (i == null) return;
        pins[i] = type;
        pinNames[i] = name;
        pinBitWidths[i] = bitWidth > 0 ? bitWidth : 1;
    }

    /**
     * 引脚名称（用于检查功能的 tooltip），未命名时返回 {@code null}。
     */
    public String getPinName(Direction side, int offset) {
        Integer i = index(side, offset);
        return i != null ? pinNames[i] : null;
    }

    /**
     * 计算引脚相对元件（旋转后）左上角的位置，单位：格。
     * <p>
     * 引脚先在元件本地坐标系中按 {@code rot} 顺时针旋转（{@code rot == NORTH} 表示原始方向），
     * 若 {@code mirrored} 为 {@code true} 再在屏幕上做水平镜像（左右翻转，绕竖直轴）。
     * <p>
     * 只需要一个镜像布尔量：垂直镜像（上下翻转）恒等于“水平镜像 + 旋转 180 度”，
     * 因此 {@code (rot, mirrored)} 的 8 种组合恰好对应该元件的 8 种朝向（二面体群 D4）。
     * 调用方通过 {@link #verticalMirrorDirection} 与翻转 {@code mirrored} 来实现垂直镜像。
     * <p>
     * 返回值为 {@code {x, y}}，取值范围为 {@code x∈[0,wRot], y∈[0,hRot]}
     * （wRot/hRot 为旋转后的宽高）。镜像只影响引脚位置，不影响元件图标，
     * 也不改变元件占用的格子尺寸。
     */
    public static double[] rotatedPinOffset(int w0, int h0, Direction rot, boolean mirrored,
                                            Direction side, int offset) {
        double nx, ny;
        switch (side) {
            case NORTH -> {
                nx = offset + 0.5;
                ny = 0;
            }
            case EAST -> {
                nx = w0;
                ny = offset + 0.5;
            }
            case SOUTH -> {
                nx = w0 - 0.5 - offset;
                ny = h0;
            }
            default -> { // WEST
                nx = 0;
                ny = h0 - 0.5 - offset;
            }
        }
        double rx, ry;
        switch (rot) {
            case EAST -> {
                rx = h0 - ny;
                ry = nx;
            }
            case SOUTH -> {
                rx = w0 - nx;
                ry = h0 - ny;
            }
            case WEST -> {
                rx = ny;
                ry = w0 - nx;
            }
            default -> { // NORTH
                rx = nx;
                ry = ny;
            }
        }
        if (mirrored) {
            rx = ((rot == Direction.EAST || rot == Direction.WEST) ? h0 : w0) - rx;
        }
        return new double[]{rx, ry};
    }

    /**
     * 垂直镜像（上下翻转）对应的旋转方向：在原有 {@code rot} 基础上再转 180 度。
     * 垂直镜像效果等价于“水平镜像 + 旋转 180 度”，故实现垂直镜像时
     * 应把方向换成该值，并同时翻转镜像布尔量。
     */
    public static Direction verticalMirrorDirection(Direction rot) {
        return rot.getOpposite();
    }

    /**
     * 引脚在应用旋转与镜像后所处的那条边（即检查 tooltip 中显示的“实际方向”）。
     * 判定优先级与 {@link CircuitSimulator} 一致：先上下、再左右。
     */
    public static Direction rotatedPinSide(int w0, int h0, Direction rot, boolean mirrored,
                                           Direction side, int offset) {
        double[] p = rotatedPinOffset(w0, h0, rot, mirrored, side, offset);
        double rx = p[0];
        double ry = p[1];
        double wRot = (rot == Direction.EAST || rot == Direction.WEST) ? h0 : w0;
        double hRot = (rot == Direction.EAST || rot == Direction.WEST) ? w0 : h0;
        if (ry <= 1e-6) return Direction.NORTH;
        if (ry >= hRot - 1e-6) return Direction.SOUTH;
        if (rx <= 1e-6) return Direction.WEST;
        return Direction.EAST;
    }

    /**
     * 引脚位宽。未显式设置的引脚默认 1 位。
     */
    public int getPinBitWidth(Direction side, int offset) {
        Integer i = index(side, offset);
        if (i == null) return 1;
        int w = pinBitWidths[i];
        return w > 0 ? w : 1;
    }

    public void setInstanceFactory(Supplier<CircuitComponentInstance> instanceFactory) {
        this.instanceFactory = instanceFactory;
    }

    public CircuitComponentInstance createInstance() {
        return instanceFactory.get();
    }

    public void setIngredientSupplier(Item item) {
        setIngredientSupplier(item.getDefaultInstance());
    }

    public void setIngredientSupplier(ItemStack itemStack) {
        setIngredientSupplier(new ItemStack[]{itemStack});
    }

    public void setIngredientSupplier(ItemStack[] itemStacks) {
        this.ingredientSupplier = new ItemIngredientSupplier(itemStacks);
    }

    public void setIngredientSupplier(CircuitComponent component) {
        this.ingredientSupplier = new ComponentIngredientSupplier(component);
    }

    public void setSelfAsIngredientSupplier() {
        setIngredientSupplier(this);
    }

    public void clearIngredientSupplier() {
        this.ingredientSupplier = null;
    }

    public IngredientSupplier getIngredientSupplier() {
        return ingredientSupplier;
    }

    public boolean isCustom() {
        return custom;
    }

    public int getColor() {
        return color;
    }

    @Nullable
    public UUID getCircuitId() {
        return circuitId;
    }

    @Nullable
    public UUID getAuthor() {
        return author;
    }

    @Nullable
    public String getCustomAuthorName() {
        return customAuthorName;
    }

    @Nullable
    public ResourceLocation getCustomIconItem() {
        return customIconItem;
    }

    @Nullable
    public String getCustomIconText() {
        return customIconText;
    }

    @Nullable
    public CircuitDiagram getCustomCircuit() {
        return customCircuit;
    }

    @Nullable
    public CircuitDiagram.Scale getCustomScale() {
        return customScale;
    }

    public void setCustomMetadata(@Nullable UUID circuitId, @Nullable UUID author, @Nullable String authorName,
                                  @Nullable CircuitDiagram circuit, @Nullable CircuitDiagram.Scale scale,
                                  int color, @Nullable ResourceLocation iconItem, @Nullable String iconText) {
        this.custom = true;
        this.circuitId = circuitId;
        this.author = author;
        this.customAuthorName = authorName;
        this.customCircuit = circuit;
        this.customScale = scale;
        this.color = color;
        this.customIconItem = iconItem;
        this.customIconText = iconText;
    }

    public sealed interface IngredientSupplier permits ItemIngredientSupplier, ComponentIngredientSupplier {
    }

    public record ItemIngredientSupplier(ItemStack[] itemStacks) implements IngredientSupplier {
    }

    public record ComponentIngredientSupplier(CircuitComponent component) implements IngredientSupplier {
    }
}