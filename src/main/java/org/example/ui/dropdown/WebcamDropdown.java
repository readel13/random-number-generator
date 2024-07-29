package org.example.ui.dropdown;

import com.github.sarxos.webcam.Webcam;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.util.List;

public class WebcamDropdown extends JComboBox<String> {

    private final List<Webcam> webcams;

    private final ResolutionDropdown resolutionDropdown;

    public WebcamDropdown(List<Webcam> webcams, ResolutionDropdown resolutionDropdown) {
        this.webcams = webcams;
        this.resolutionDropdown = resolutionDropdown;

        webcams.forEach(w -> addItem(w.getName()));
        addActionListener(buildActionListener());
    }


    private ActionListener buildActionListener() {
        return e -> {
            Webcam selectedWebcam = webcams.get(getSelectedIndex());

            resolutionDropdown.refreshResolutions(selectedWebcam);
        };
    }
}
