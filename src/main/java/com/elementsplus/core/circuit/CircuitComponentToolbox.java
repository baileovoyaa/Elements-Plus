package com.elementsplus.core.circuit;

import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class CircuitComponentToolbox {
    public static class Category {
        public Component name;
        public List<CircuitComponent> components;

        public Category(Component name) {
            this.name = name;
            this.components = new ArrayList<>();
        }

        public static Category create(Component name) {
            return new Category(name);
        }

        public static Category create(String name) {
            return new Category(Component.nullToEmpty(name));
        }

        public Category component(CircuitComponent component) {
            this.components.add(component);
            return this;
        }
    }

    public List<Category> categories = new ArrayList<>();

    public static CircuitComponentToolbox create() {
        return new CircuitComponentToolbox();
    }

    public CircuitComponentToolbox category(Category category) {
        this.categories.add(category);
        return this;
    }
}
