package org.example;

import org.example.utils.CellAutomataUtils;
import org.example.utils.BufferedImageUtils;

public class Main {

    public static final String FILE_FORMAT = "bmp";

    private static final int WIDTH = 640;
    private static final int HEIGHT = 480;


    public static void main(String[] args) {
        var image = BufferedImageUtils.generateNoiseImage(WIDTH, HEIGHT);
        var newImage = CellAutomataUtils.evolveWithCAImage(image, WIDTH, HEIGHT);

        BufferedImageUtils.saveImageToFile(image, "old", FILE_FORMAT);
        BufferedImageUtils.saveImageToFile(newImage, "new.bmp", FILE_FORMAT);
    }
}