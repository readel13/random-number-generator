package org.example.ui.button;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamUtils;
import com.github.sarxos.webcam.util.ImageUtils;
import org.example.utils.BufferedImageUtils;
import org.example.utils.CellAutomataUtils;

import javax.swing.*;
import java.awt.event.ActionListener;

import static org.example.Main.FILE_FORMAT;

public class TakePhotoButton extends JButton {

    private static final String NAME = "Take photo";

    private final Webcam webcam;

    public TakePhotoButton(Webcam webcam) {
        this.webcam = webcam;
        setText(NAME);
        addActionListener(buildActionListener());
    }

    private ActionListener buildActionListener() {
        return e -> {
            WebcamUtils.capture(webcam, "test1", ImageUtils.FORMAT_BMP);
            byte[] imageBytes = WebcamUtils.getImageBytes(webcam, "bmp");

            var webcamSize = webcam.getViewSize();
            var newImage = CellAutomataUtils.evolveWithCAImage(imageBytes,  webcamSize.width, webcamSize.height, 20);
            BufferedImageUtils.saveImageToFile(newImage, "new", FILE_FORMAT);

            var modifiedImage = new JFrame("Modified image");
            modifiedImage.setResizable(true);
            modifiedImage.setSize((int) (webcamSize.getWidth() * 2), (int) (webcamSize.getHeight() * 2));
            modifiedImage.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            modifiedImage.setVisible(true);

            modifiedImage.add(new JLabel(new ImageIcon(newImage)));
        };
    }
}
