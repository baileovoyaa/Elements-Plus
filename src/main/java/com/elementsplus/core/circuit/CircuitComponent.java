package com.elementsplus.core.circuit;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class CircuitComponent {
    private Component name;
    private Component description;
    private ResourceLocation icon;

    public CircuitComponent(Component name, Component description, ResourceLocation icon) {
        this.name = name;
        this.description = description;
        this.icon = icon;
    }

    public CircuitComponent(Component name) {
        this(name, Component.empty(), null);
    }

    public CircuitComponent(String name) {
        this(Component.nullToEmpty(name));
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
}
