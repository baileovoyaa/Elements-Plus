package com.elementsplus.client.gui;

import java.util.List;

public class TabGroup {
    private final List<TabButton> buttons;
    private TabButton selected;

    public TabGroup(TabButton... buttons) {
        this.buttons = List.of(buttons);
        for (TabButton button : buttons) {
            button.tabGroup = this;
        }
        if (buttons.length > 0) setSelected(buttons[0]);
    }

    public List<TabButton> getButtons() {
        return buttons;
    }

    public void onButtonClick(TabButton button) {
        setSelected(button);
    }

    public TabButton getSelected() {
        return selected;
    }

    public void setSelected(TabButton button) {
        if (button != null && !buttons.contains(button)) {
            throw new IllegalArgumentException("button 不属于这个 TabGroup");
        }
        this.selected = button;
        for (TabButton tabButton : this.getButtons()) {
            tabButton.active = (tabButton == button);
        }
    }

}
