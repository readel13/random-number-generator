package org.example.ui.dropdown;

import org.example.webcam.WebcamService;

import javax.swing.JComboBox;
import java.awt.Dimension;
import java.util.List;

/**
 * Picks the size of a generated image, offering the same resolutions as the webcam dropdown. Unlike
 * {@link ResolutionDropdown} it is not tied to a device, so it stays usable when the seed comes from
 * {@link org.example.utils.IdealSequenceUtils} rather than from a camera.
 */
public class SizeDropdown extends JComboBox<String> {

    private final List<Dimension> resolutions = WebcamService.PREFERRED_RESOLUTIONS;

    public SizeDropdown(Dimension initial) {
        resolutions.forEach(resolution -> addItem(WebcamService.describe(resolution)));

        setSelectedIndex(Math.max(resolutions.indexOf(initial), 0));
    }

    public Dimension getSelectedResolution() {
        int index = getSelectedIndex();

        return resolutions.get(index < 0 ? 0 : index);
    }
}
