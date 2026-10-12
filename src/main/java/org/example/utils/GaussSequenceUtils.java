package org.example.utils;

import org.apache.commons.math3.distribution.NormalDistribution;

/**
 * Builds a byte sequence whose histogram follows a normal curve, for use as a seed in place of a
 * webcam frame - the bell-shaped counterpart of {@link IdealSequenceUtils}.
 *
 * <p>Where the equal sequence is the best case a randomness extractor can be handed, this one is a
 * known skewed input: whatever a CA generation does to it shows how much the rule actually flattens
 * a distribution, measured against a shape that is known exactly.</p>
 *
 * <p>The counts are computed from the curve rather than sampled with {@code nextGaussian}, so the
 * sequence is the same every run and carries no sampling noise.</p>
 */
public final class GaussSequenceUtils {

    private static final int VALUES = 256;

    public static final double DEFAULT_MEAN = 0;
    public static final double DEFAULT_SIGMA = 40;

    private GaussSequenceUtils() {
    }

    /**
     * Each value {@code v} gets the probability mass of {@code [v - 0.5, v + 0.5)}. The curve is
     * truncated to the byte range and renormalised rather than clamped, so the tails never pile up
     * on -128 and 127. Counts are rounded with the largest-remainder method, so they sum to exactly
     * {@code length}.
     *
     * @param mean  centre of the curve, as a signed byte value
     * @param sigma standard deviation, in byte values; must be positive
     * @param order {@code CYCLIC} walks -128..127 repeatedly, emitting each value that still has
     *              bytes left; {@code GROUPED} writes every -128 first, then every -127, and so on
     */
    public static byte[] generate(int length, double mean, double sigma, IdealSequenceUtils.Order order) {
        if (length < 0) {
            throw new IllegalArgumentException("Sequence length must not be negative: " + length);
        }
        if (!(sigma > 0)) {
            throw new IllegalArgumentException("Standard deviation must be positive: " + sigma);
        }

        int[] counts = counts(length, mean, sigma);

        return order == IdealSequenceUtils.Order.GROUPED
                ? grouped(counts, length)
                : cyclic(counts, length);
    }

    /**
     * Counts indexed by {@code value - Byte.MIN_VALUE}.
     */
    private static int[] counts(int length, double mean, double sigma) {
        NormalDistribution normal = new NormalDistribution(null, mean, sigma);

        double[] mass = new double[VALUES];
        double total = 0;
        for (int value = 0; value < VALUES; value++) {
            double centre = Byte.MIN_VALUE + value;
            mass[value] = normal.probability(centre - 0.5, centre + 0.5);
            total += mass[value];
        }

        if (total == 0) {
            // the whole curve sits outside the byte range; there is nothing to truncate to
            throw new IllegalArgumentException(
                    "Normal curve (mean %s, sigma %s) has no mass in the byte range".formatted(mean, sigma));
        }

        int[] counts = new int[VALUES];
        double[] remainders = new double[VALUES];
        int assigned = 0;
        for (int value = 0; value < VALUES; value++) {
            double exact = length * mass[value] / total;
            counts[value] = (int) Math.floor(exact);
            remainders[value] = exact - counts[value];
            assigned += counts[value];
        }

        // hand the bytes lost to flooring to the values that lost the most
        for (int left = length - assigned; left > 0; left--) {
            int best = 0;
            for (int value = 1; value < VALUES; value++) {
                if (remainders[value] > remainders[best]) {
                    best = value;
                }
            }
            counts[best]++;
            remainders[best] = -1;
        }

        return counts;
    }

    private static byte[] grouped(int[] counts, int length) {
        byte[] sequence = new byte[length];
        int index = 0;

        for (int value = 0; value < VALUES; value++) {
            byte current = (byte) (Byte.MIN_VALUE + value);
            for (int i = 0; i < counts[value]; i++) {
                sequence[index++] = current;
            }
        }

        return sequence;
    }

    private static byte[] cyclic(int[] counts, int length) {
        byte[] sequence = new byte[length];
        int index = 0;

        int[] left = counts.clone();
        int[] active = new int[VALUES];
        int activeCount = 0;
        for (int value = 0; value < VALUES; value++) {
            if (left[value] > 0) {
                active[activeCount++] = value;
            }
        }

        // each pass drops the values that have run out, so the walk never revisits an empty one
        while (activeCount > 0) {
            int kept = 0;
            for (int i = 0; i < activeCount; i++) {
                int value = active[i];
                sequence[index++] = (byte) (Byte.MIN_VALUE + value);
                if (--left[value] > 0) {
                    active[kept++] = value;
                }
            }
            activeCount = kept;
        }

        return sequence;
    }
}
