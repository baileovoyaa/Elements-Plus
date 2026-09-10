package com.elementsplus.client.gui;

public record Point(int x, int y) {
    public static final Point ORIGIN = new Point(0, 0);

    public static Point of(int x, int y) {
        return new Point(x, y);
    }
}
