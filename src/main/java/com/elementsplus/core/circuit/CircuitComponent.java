package com.elementsplus.core.circuit;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class CircuitComponent {
    private final ResourceLocation id;
    private Component name;
    private Component description;
    private ResourceLocation icon;
    private final int height;
    private final int width;

    public CircuitComponent(ResourceLocation id, Component name, Component description, ResourceLocation icon, int width, int height) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.icon = icon;
        this.width = width;
        this.height = height;
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
}