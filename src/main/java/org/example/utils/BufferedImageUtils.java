package org.example.utils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Random;

import static java.awt.image.BufferedImage.TYPE_3BYTE_BGR;

public class BufferedImageUtils {

    public static void saveImageToFile(BufferedImage image, String fileName, String format) {
        File ImageFile = new File(fileName + "." + format);
        try {
            ImageIO.write(image, format, ImageFile);
        } catch (IOException e) {
            throw new RuntimeException("Cannot save image", e);
        }
    }

    public static BufferedImage generateNoiseImage(int width, int height) {
        var image = new BufferedImage(width, height, TYPE_3BYTE_BGR);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                image.setRGB(x, y, new Random().nextInt(0, 255 * 255 * 255));
            }
        }
        return image;
    }

    public static BufferedImage boolToImg(boolean[] newGenBool, int width, int height) {
        var newGenByte = BitsUtils.toByteArray(newGenBool);
        return imgFromBytes(newGenByte, width, height);
    }

    public static boolean[] imgToBool(BufferedImage image) {
        if (image.getData().getDataBuffer() instanceof DataBufferByte bufferByte) {
            return BitsUtils.toBool(bufferByte.getData());
        } else {
            throw new RuntimeException("Unexpected data buffer type");
        }
    }

    private static BufferedImage imgFromBytes(byte[] imgBytes, int width, int height) {
        var buffer = new DataBufferByte(imgBytes, imgBytes.length);
        var image = new BufferedImage(width, height, TYPE_3BYTE_BGR);
        image.setData(
                Raster.createInterleavedRaster(
                        buffer, width, height,
                        3 * width, 3,
                        new int[]{2, 1, 0}, null));
        return image;
    }

    // use for geting start image index without metadata
    public static int getImageOffsetBmp(byte[] image) {
        return ByteBuffer.wrap((Arrays.copyOfRange(image, 10, 14))).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }
}
