package org.example.ui.dropdown;

import com.github.sarxos.webcam.Webcam;
import org.example.webcam.WebcamService;

import javax.swing.JComboBox;
import java.awt.Dimension;
import java.util.List;

/** Resolutions available on the webcam currently picked in the {@link WebcamDropdown}. */
public class ResolutionDropdown extends JComboBox<String> {

    private List<Dimension> resolutions = List.of();

    public ResolutionDropdown(Webcam webcam) {
        if (webcam != null) {
            refreshResolutions(webcam);
        }
    }

    /**
     * Reloads the list for the given device, keeping the current choice when that device also
     * supports it and falling back to {@link WebcamService#DEFAULT_RESOLUTION} otherwise. Never
     * falls back to the first entry alone - that is the smallest resolution the driver reports,
     * which some devices refuse to deliver.
     */
    public void refreshResolutions(Webcam webcam) {
        Dimension previous = getSelectedResolution();

        resolutions = WebcamService.resolutionsFor(webcam);

        removeAllItems();
        resolutions.forEach(resolution -> addItem(WebcamService.describe(resolution)));

        int index = indexOf(previous);
        if (index < 0) {
            index = indexOf(WebcamService.DEFAULT_RESOLUTION);
        }
        setSelectedIndex(Math.max(index, 0));
    }

    // List.of() is null-hostile, and there is no previous choice on the first refresh
    private int indexOf(Dimension resolution) {
        return resolution == null ? -1 : resolutions.indexOf(resolution);
    }

    /** @return the selected resolution, or {@code null} while the list is empty. */
    public Dimension getSelectedResolution() {
        int index = getSelectedIndex();

        return index >= 0 && index < resolutions.size() ? resolutions.get(index) : null;
    }
}
