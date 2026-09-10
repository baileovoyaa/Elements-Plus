package com.elementsplus.client.gui;

import java.util.List;

public class ButtonGroup {
    private final List<GroupButton> buttons;
    private GroupButton selected;

    public ButtonGroup(GroupButton... buttons) {
        this.buttons = List.of(buttons);
        for (GroupButton button : buttons) {
            button.buttonGroup = this;
        }
        if (buttons.length > 0) setSelected(buttons[0]);
    }

    public List<GroupButton> getButtons() {
        return buttons;
    }

    public void onButtonClick(GroupButton button) {
        setSelected(button);
    }

    public GroupButton getSelected() {
        return selected;
    }

    public void setSelected(GroupButton button) {
        if (button != null && !buttons.contains(button)) {
            throw new IllegalArgumentException("button 不属于这个 TabGroup");
        }
        this.selected = button;
        for (GroupButton groupButton : this.getButtons()) {
            groupButton.active = (groupButton == button);
        }
    }

}
