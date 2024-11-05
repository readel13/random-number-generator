package org.example.ui.frames;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamUtils;
import com.github.sarxos.webcam.util.ImageUtils;
import org.example.utils.BufferedImageUtils;
import org.example.utils.CellAutomataUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

import static org.example.Main.FILE_FORMAT;

public class ModifiedImageFrame extends JFrame {

    private static final String NAME = "Modified image";

    private final Webcam webcam;

    public ModifiedImageFrame(Webcam webcam) {
        this.webcam = webcam;

        buildDefaultFrame(webcam);
        BufferedImage bufferedImage = buildModifiedImage();

        add(new JLabel(new ImageIcon(bufferedImage)));
    }

    private BufferedImage buildModifiedImage() {
        WebcamUtils.capture(webcam, "test1", ImageUtils.FORMAT_BMP);
        byte[] imageBytes = WebcamUtils.getImageBytes(webcam, "bmp");

        var webcamSize = webcam.getViewSize();
        var newImage = CellAutomataUtils.evolveWithCAImage(imageBytes, webcamSize.width, webcamSize.height, 20);
        BufferedImageUtils.saveImageToFile(newImage, "new", FILE_FORMAT);

        return newImage;
    }

    private void buildDefaultFrame(Webcam webcam) {
        var webcamSize = webcam.getViewSize();

        setTitle(NAME);
        setLayout(new BorderLayout());
        setSize((int) (webcamSize.getWidth() * 2), (int) (webcamSize.getHeight() * 2));

        setVisible(true);
        setResizable(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
}
