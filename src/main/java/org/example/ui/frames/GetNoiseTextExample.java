package org.example.ui.frames;

import org.example.ui.AsyncReport;
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

            textArea.setText("Generating...");
            AsyncReport.run(this, () -> generateNoiseText(selectedRule, iters), textArea::setText);
        }));


        add(buttonPanel, BorderLayout.NORTH);
        add(textArea, BorderLayout.CENTER);
    }

    private String generateNoiseText(Rule rule, int iterations) {
        boolean[] bits = CellAutomataUtils.evolveWithCABytesBits(session.captureBmp(), iterations, rule);

        StringBuilder bytesString = new StringBuilder(bits.length);
        for (boolean bit : bits) {
            bytesString.append(bit ? "1" : "0");
        }

        return bytesString.toString();
    }

    private void buildDefaultFrame() {
        textArea.setEditable(false);
        setTitle(NAME);
        setLayout(new BorderLayout());
        setSize(1600, 900);

        setVisible(true);
        setResizable(true);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }
}
