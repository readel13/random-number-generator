package org.example.ui.dropdown;

import com.github.sarxos.webcam.Webcam;

import javax.swing.JComboBox;
import java.util.List;

/** Lists every webcam found on the system; picking one reloads the resolutions offered for it. */
public class WebcamDropdown extends JComboBox<String> {

    private final List<Webcam> webcams;

    public WebcamDropdown(List<Webcam> webcams, ResolutionDropdown resolutionDropdown) {
        this.webcams = webcams;

        webcams.forEach(webcam -> addItem(webcam.getName()));
        addActionListener(e -> {
            Webcam selected = getSelectedWebcam();
            if (selected != null) {
                resolutionDropdown.refreshResolutions(selected);
            }
        });
    }

    /** @return the selected device, or {@code null} when no webcam was found. */
    public Webcam getSelectedWebcam() {
        int index = getSelectedIndex();

        return index >= 0 && index < webcams.size() ? webcams.get(index) : null;
    }
}
