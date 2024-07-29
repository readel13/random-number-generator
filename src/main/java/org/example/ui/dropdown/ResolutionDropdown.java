package org.example.ui.dropdown;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamResolution;

import javax.swing.*;
import java.awt.*;

public class ResolutionDropdown extends JComboBox<String> {

    public static final Dimension[] CUSTOM_RESOLUTIONS = new Dimension[]{
            WebcamResolution.VGA.getSize(),
            WebcamResolution.HD.getSize(),
            WebcamResolution.HDP.getSize(),
            WebcamResolution.FHD.getSize(),
            WebcamResolution.WQHD.getSize()
    };

    public ResolutionDropdown(Webcam defaultWebcam) {
        refreshResolutions(defaultWebcam);

        setSelectedIndex(getSelectedIndexResolution(defaultWebcam.getViewSize()));
    }

    public void refreshResolutions(Webcam webcam) {
        removeAllItems();

        webcam.setCustomViewSizes(CUSTOM_RESOLUTIONS);

        for (var resolution : CUSTOM_RESOLUTIONS) {
            addItem((int) resolution.getWidth() + "x" + (int) resolution.getHeight());
        }
    }


    private int getSelectedIndexResolution(Dimension webcamResolution) {
        int resultIndex = 0;
        for (int i = 0; i < CUSTOM_RESOLUTIONS.length; i++) {
            if (CUSTOM_RESOLUTIONS[i].equals(webcamResolution)) {
                resultIndex = i;
            }
        }

        return resultIndex;
    }
}
