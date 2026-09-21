package org.example.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * {@link Collections#shuffle(List)} used as an alternative randomness extractor, so it can be put
 * next to the CA rules in the reports.
 * <p>
 * The shuffle runs over the same bit representation the CA generations run over, which makes one
 * shuffle round the counterpart of one CA generation.
 */
public class ShuffleUtils {

    private static final BitsUtils bitsUtils = new BitsUtils();

    public static byte[] shuffleBytes(byte[] image, int rounds) {
        return BitsUtils.toByteArray(shuffleBytesBits(image, rounds));
    }

    public static boolean[] shuffleBytesBits(byte[] image, int rounds) {
        boolean[] bits = bitsUtils.toBoolCached(image);

        List<Boolean> bitList = new ArrayList<>(bits.length);
        for (boolean bit : bits) {
            bitList.add(bit);
        }

        for (int i = 0; i < rounds; i++) {
            Collections.shuffle(bitList);
        }

        boolean[] shuffled = new boolean[bits.length];
        for (int i = 0; i < shuffled.length; i++) {
            shuffled[i] = bitList.get(i);
        }

        return shuffled;
    }
}
