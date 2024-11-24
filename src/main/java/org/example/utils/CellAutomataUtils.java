package org.example.utils;

import org.example.rule.Rule;
import org.example.rule.RulesSet;

import java.awt.image.BufferedImage;

public class CellAutomataUtils {

    private static final BitsUtils bitsUtils = new BitsUtils();

    public static BufferedImage evolveWithCAImage(BufferedImage image, int width, int height) {
        var booleanBytes = BufferedImageUtils.imgToBool(image);
        var newGenBool = CellAutomataUtils.makeGeneration(booleanBytes, RulesSet::rule30);
        return BufferedImageUtils.boolToImg(newGenBool, width, height);
    }

    public static BufferedImage evolveWithCAImage(byte[] image, int width, int height, int generations) {
        boolean[] generation = BitsUtils.toBool(image);

        for (int i = 0; i < generations; i++) {
            generation = CellAutomataUtils.makeGeneration(generation, RulesSet::rule30);
        }
        return BufferedImageUtils.boolToImg(generation, width, height);
    }

    public static byte[] evolveWithCABytes(byte[] image, int generations) {
        return evolveWithCABytes(image, generations, RulesSet::rule30);
    }

    public static byte[] evolveWithCABytes(byte[] image, int generations, Rule rule) {
        boolean[] generation = bitsUtils.toBoolCached(image);

        for (int i = 0; i < generations; i++) {
            generation = CellAutomataUtils.makeGeneration(generation, rule);
        }

        return BitsUtils.toByteArray(generation);
    }

    public static boolean[] evolveWithCABytesBits(byte[] image, int generations, Rule rule) {
        boolean[] generation = bitsUtils.toBoolCached(image);

        for (int i = 0; i < generations; i++) {
            generation = CellAutomataUtils.makeGeneration(generation, rule);
        }

        return generation;
    }

    private static boolean[] makeGeneration(boolean[] boolStart, Rule rule) {
        boolean[] boolResult = new boolean[boolStart.length];
        for (int i = 0; i < boolStart.length; i++) {
            switch ((Integer) i) {
                case 0 -> boolResult[i] = rule.step(boolStart[boolResult.length - 1], boolStart[i], boolStart[i + 1]);
                case Integer last when last == boolResult.length - 1 ->
                        boolResult[i] = rule.step(boolStart[i - 1], boolStart[i], boolStart[0]);
                default -> boolResult[i] = rule.step(boolStart[i - 1], boolStart[i], boolStart[i + 1]);
            }
        }
        return boolResult;
    }
}
