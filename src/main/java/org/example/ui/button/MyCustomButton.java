package org.example.ui.button;

import java.awt.*;
import java.awt.event.ActionListener;

public class MyCustomButton extends Button {

    public MyCustomButton(String label, ActionListener actionListener) throws HeadlessException {
        super(label);
        addActionListener(actionListener);
    }
}
