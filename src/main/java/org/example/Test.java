package org.example;

import org.example.utils.MeasureTimeUtil;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class Test {
    public static void main(String[] args) throws InterruptedException {


        for (int i = 0; i < 10000; i++) {

            byte[] bytes1 = MeasureTimeUtil.measureAndPrintTime(() -> generateBytes(), "Gen1");

//            System.out.println("Similiraty: " + ComparingUtils.compare(bytes1, bytes2));
        }
    }

    private static byte[] generateBytes() {
        byte[] bytes = new byte[1_000_000];
        SecureRandom secureRandom;
        try {
            secureRandom = SecureRandom.getInstance("SHA1PRNG");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        secureRandom.nextBytes(bytes);
        return bytes;
    }
}
