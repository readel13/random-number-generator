package org.example.math.model;


public record Analytics(byte min, byte max, byte average) {

    public static Analytics analyse(byte[] bytes) {
        byte min = bytes[0];
        byte max = bytes[0];
        long sum = 0;

        for (int i = 0; i < bytes.length; i++) {
            if (bytes[i] < min) {
                min = bytes[i];
            }

            if (bytes[i] > max) {
                max = bytes[i];
            }

            sum += bytes[i];
        }


        return new Analytics(min, max, (byte) (sum / bytes.length));
    }
}