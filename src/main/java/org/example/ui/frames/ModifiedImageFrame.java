package org.example.ui.frames;

import org.example.ui.AsyncReport;
import org.example.webcam.WebcamSession;
import org.example.utils.BufferedImageUtils;
import org.example.utils.CellAutomataUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

import static org.example.Main.FILE_FORMAT;

public class ModifiedImageFrame extends JFrame {

    private static final String NAME = "Modified image";

    private final WebcamSession session;

    public ModifiedImageFrame(WebcamSession session) {
        this.session = session;

        buildDefaultFrame();

        AsyncReport.load(this, this::buildModifiedImage, image -> new JLabel(new ImageIcon(image)));
    }

    private BufferedImage buildModifiedImage() {
        session.captureToFile("test1");
        byte[] imageBytes = session.captureBmp();

        var webcamSize = session.getViewSize();
        var newImage = CellAutomataUtils.evolveWithCAImage(imageBytes, webcamSize.width, webcamSize.height, 20);
        BufferedImageUtils.saveImageToFile(newImage, "new", FILE_FORMAT);

        return newImage;
    }

    private void buildDefaultFrame() {
        var webcamSize = session.getViewSize();

        setTitle(NAME);
        setLayout(new BorderLayout());
        setSize((int) (webcamSize.getWidth() * 2), (int) (webcamSize.getHeight() * 2));

        setVisible(true);
        setResizable(true);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }
}
