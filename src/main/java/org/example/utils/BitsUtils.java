package org.example.utils;

import java.util.Arrays;
import java.util.BitSet;

public class BitsUtils {

    private final boolean[][] cachedToBoolList1 = new boolean[256][8];

    public BitsUtils() {
        for (int i = Byte.MIN_VALUE; i <= Byte.MAX_VALUE; ++i) {
            boolean[] booles = toBool((byte) i);
            System.arraycopy(booles, 0, cachedToBoolList1[i + 128], 0, booles.length - 1 + 1);
        }
    }

    public boolean[] toBoolCached(byte[] bytes) {
        boolean[] booleans = new boolean[bytes.length * 8];
        for (int i = 0; i < bytes.length; i++) {
            System.arraycopy(cachedToBoolList1[bytes[i] + 128], 0, booleans, i * 8, 8);
        }

        return booleans;
    }

    public static boolean[] toBool(byte bytes) {
        BitSet bits = BitSet.valueOf(new byte[]{bytes});
        boolean[] bools = new boolean[8];
        for (int i = bits.nextSetBit(0); i != -1; i = bits.nextSetBit(i + 1)) {
            bools[i] = true;
        }
        return bools;
    }


    public static boolean[] toBool(byte[] bytes) {
        BitSet bits = BitSet.valueOf(bytes);
        boolean[] bools = new boolean[bytes.length * 8];
        for (int i = bits.nextSetBit(0); i != -1; i = bits.nextSetBit(i + 1)) {
            bools[i] = true;
        }
        return bools;
    }

    public static byte[] toByteArray(boolean[] bools) {
        BitSet bits = new BitSet(bools.length);
        for (int i = 0; i < bools.length; i++) {
            if (bools[i]) {
                bits.set(i);
            }
        }

        byte[] bytes = bits.toByteArray();
        if (bytes.length * 8 >= bools.length) {
            return bytes;
        } else {
            return Arrays.copyOf(bytes, bools.length / 8 + (bools.length % 8 == 0 ? 0 : 1));
        }
    }
}