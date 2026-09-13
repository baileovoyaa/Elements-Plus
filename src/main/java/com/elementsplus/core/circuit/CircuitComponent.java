package com.elementsplus.core.circuit;

import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Arrays;

public class CircuitComponent {
    private final ResourceLocation id;
    private Component name;
    private Component description;
    private ResourceLocation icon;
    private final int height;
    private final int width;
    private final PinType[] pins;


    public CircuitComponent(ResourceLocation id, Component name, Component description, ResourceLocation icon, int width, int height) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.icon = icon;
        this.width = width;
        this.height = height;

        this.pins = new PinType[2 * (width + height)];
        Arrays.fill(this.pins, PinType.NONE);
    }

    public CircuitComponent(ResourceLocation id, Component name, Component description, ResourceLocation icon) {
        this(id, name, description, icon, 1, 1);
    }

    public CircuitComponent(ResourceLocation id, Component name) {
        this(id, name, Component.empty(), null, 1, 1);
    }

    public CircuitComponent(ResourceLocation id, String name) {
        this(id, Component.nullToEmpty(name));
    }

    public CircuitComponent(Component name, Component description, ResourceLocation icon, int width, int height) {
        this(null, name, description, icon, width, height);
    }

    public CircuitComponent(Component name, Component description, ResourceLocation icon) {
        this(null, name, description, icon);
    }

    public CircuitComponent(Component name) {
        this(null, name);
    }

    public CircuitComponent(String name) {
        this(null, Component.nullToEmpty(name));
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
}