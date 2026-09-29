package org.example.ui.frames;

import org.example.webcam.WebcamSession;
import org.example.rule.Rule;
import org.example.ui.button.MyCustomButton;
import org.example.ui.dropdown.CARuleDropdown;
import org.example.utils.CellAutomataUtils;

import javax.swing.*;
import java.awt.*;

public class GetNoiseTextExample extends JFrame {

    private static final String NAME = "Get Noise Text";

    private final TextArea textArea = new TextArea();
    private final JTextField iterations = new JTextField(10);
    private final CARuleDropdown ruleDropdown = new CARuleDropdown();

    private final WebcamSession session;

    public GetNoiseTextExample(WebcamSession session) {
        this.session = session;

        buildDefaultFrame();
        var buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 15));

        buttonPanel.add(new JLabel("Rule:"));
        buttonPanel.add(ruleDropdown);
        buttonPanel.add(new JLabel("Iterations:"));
        buttonPanel.add(iterations);
        buttonPanel.add(new MyCustomButton("Generate Text", e -> {
            Rule selectedRule = ruleDropdown.getSelectedRule();
            int iters = Integer.parseInt(iterations.getText());

            byte[] imageBytes = session.captureBmp();

            boolean[] bits = CellAutomataUtils.evolveWithCABytesBits(imageBytes, iters, selectedRule);

            StringBuilder bytesString = new StringBuilder();
            for (boolean b : bits) {
                bytesString.append(b ? "1" : "0");
            }

            textArea.setText(bytesString.toString());
        }));


        add(buttonPanel, BorderLayout.NORTH);
        add(textArea, BorderLayout.CENTER);
    }

    private void buildDefaultFrame() {
        textArea.setEditable(false);
        setTitle(NAME);
        setLayout(new BorderLayout());
        setSize(1600, 900);

        setVisible(true);
        setResizable(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
}
