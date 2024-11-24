package org.example.math;


import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.example.math.model.Analytics;
import org.example.math.model.FrameStats;

import java.util.HashMap;
import java.util.Map;

public class MathUtil {

    public static double compare(boolean[] first, boolean[] second) {
        int count = 0;

        for (int i = 0; i < first.length; i++) {
            if (first[i] == second[i]) {
                count++;
            }
        }
        return ((double) count / first.length) * 100;
    }


    public static double compare(byte[] first, byte[] second) {
        int count = 0;

        for (int i = 0; i < first.length; i++) {
            if (first[i] == second[i]) {
                count++;
            }
        }
        return ((double) count / first.length) * 100;
    }


    public static FrameStats analyseFrame(byte[] bytes, double deltaPercentage, double percentageConsistent) {
        var analytic = Analytics.analyse(bytes);
        Map<Byte, Integer> byteCount = buildDefaultByteCountMap();

        int bytesConsistentByRange = 0;

        for (int i = 0; i < bytes.length; i++) {
            if (percentageDiff(bytes[i], analytic.average(), analytic.max(), analytic.min()) <= deltaPercentage) {
                bytesConsistentByRange++;
            }

            byteCount.computeIfPresent(bytes[i], (k, v) -> v + 1);
        }

//        DescriptiveStatistics itemCountStats = new DescriptiveStatistics();
//        for (Integer count : byteCount.values()) {
//            itemCountStats.addValue((count * 100.0) / bytes.length);
//        }

        var statsCount = byteCount.values().stream().mapToInt(i -> i).summaryStatistics();
        long countConsistent = byteCount.values().stream()
                .filter(count -> percentageDiff(count, (int) statsCount.getAverage(), statsCount.getMax(), statsCount.getMin()) <= deltaPercentage)
                .count();

        double consistencyRateByItemCount = (double) (100 * countConsistent) / byteCount.size();
        double consistencyRateByItemAvgDiff = 100 * (double) bytesConsistentByRange / bytes.length;

        System.out.printf("Percentage by rate: %.2f, percentage by value: %.2f\n", consistencyRateByItemAvgDiff, consistencyRateByItemCount);

        return FrameStats.builder()
                .size(bytes.length)
                .neededDeltaPercentage(deltaPercentage)
                .neededPercentageConsistent(percentageConsistent)
                .byteCountMap(byteCount)
                .countBytesConsistentByRange(bytesConsistentByRange)
                .consistencyRateByItemCount(consistencyRateByItemCount)
                .consistencyRateByItemAvgDiff(consistencyRateByItemAvgDiff)
                //.itemCountStats(itemCountStats)
                .averageDeviationFromIdealDist(averageDeviationFromIdealDist(byteCount, bytes.length))
                .itemAvgDiffStats(null) // TODO: implement
                .build();
    }


    private static Map<Byte, Integer> buildDefaultByteCountMap() {
        Map<Byte, Integer> byteCount = new HashMap<>();

        for (int i = Byte.MIN_VALUE; i <= Byte.MAX_VALUE; ++i) {
            byteCount.put((byte) i, 0);
        }

        return byteCount;
    }

    private static double averageDeviationFromIdealDist(Map<Byte, Integer> byteCountMap, int imageSize) {
        int idealDistbByteCount = imageSize / byteCountMap.size();

        return byteCountMap.values().stream()
                .map(Math::abs)
                .mapToDouble(currentCount -> (double) Math.abs(currentCount - idealDistbByteCount) / 100.0d)
                .average()
                .orElse(0.0d);
    }

    //
    public static double percentageDiff(byte first, byte second, int range) {
        return (double) 100 * (double) (Math.abs(first - second)) / range;
    }

    public static double percentageDiff(int first, int second, int rangeMax, int rangeMin) {
        return (double) 100 * (double) (Math.abs(first - second)) / (rangeMax - rangeMin);
    }
}
