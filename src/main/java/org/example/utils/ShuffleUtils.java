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
 * <p>
 * <b>It permutes bits, not bytes.</b> Only the number of one-bits survives; because the bits move
 * across byte boundaries the bytes that get re-packed afterwards are different values, so the byte
 * histogram changes completely. That is why the reports show it flattening a lumpy frame - once the
 * one-bit density is near 0.5 a bit permutation makes every byte value roughly equally likely,
 * almost by construction, without adding any entropy from the frame.
 */
public class ShuffleUtils {

    private static final BitsUtils bitsUtils = new BitsUtils();

    /** Shuffles the bits of {@code image} and re-packs them into bytes. */
    public static byte[] shuffleBits(byte[] image, int rounds) {
        return BitsUtils.toByteArray(shuffleBitsRaw(image, rounds));
    }

    /** As {@link #shuffleBits(byte[], int)}, but returns the raw bits without re-packing. */
    public static boolean[] shuffleBitsRaw(byte[] image, int rounds) {
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
