package org.example.utils;

import java.awt.*;

/**
 * Builds a perfectly uniform byte sequence - every value in {@code Byte.MIN_VALUE..Byte.MAX_VALUE}
 * appearing the same number of times - for use as a seed in place of a webcam frame.
 *
 * <p>It is the best case a randomness extractor can be handed: the histogram is already flat, so
 * whatever a CA generation does to it shows up against a known baseline rather than against the
 * lumpy distribution of a real frame.</p>
 */
public final class IdealSequenceUtils {

    private static final int VALUES = 256;

    /**
     * How the values are laid out across the sequence. Both orders contain the same byte counts.
     */
    public enum Order {

        CYCLIC("Cyclic"),
        GROUPED("Grouped");

        private final String label;

        Order(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private IdealSequenceUtils() {
    }

    /**
     * Bytes in an image of this size, three per pixel - the layout {@code TYPE_3BYTE_BGR} uses.
     */
    public static int byteCount(Dimension resolution) {
        return resolution.width * resolution.height * 3;
    }

    /**
     * @param length how many bytes to produce. When it is not a multiple of 256 the first
     *               {@code length % 256} values appear once more than the rest, so the counts still
     *               differ by at most one - as even as the requested length allows.
     */
    public static byte[] generate(int length, Order order) {
        if (length < 0) {
            throw new IllegalArgumentException("Sequence length must not be negative: " + length);
        }

        byte[] sequence = new byte[length];

        if (order == Order.CYCLIC) {
            for (int i = 0; i < length; i++) {
                sequence[i] = (byte) (Byte.MIN_VALUE + i % VALUES);
            }

            return sequence;
        }

        int base = length / VALUES;
        int remainder = length % VALUES;
        int index = 0;

        for (int value = 0; value < VALUES; value++) {
            int repeats = base + (value < remainder ? 1 : 0);
            byte current = (byte) (Byte.MIN_VALUE + value);

            for (int i = 0; i < repeats; i++) {
                sequence[index++] = current;
            }
        }

        return sequence;
    }
}
