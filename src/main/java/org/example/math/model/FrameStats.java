package org.example.math.model;

import lombok.Builder;
import lombok.Data;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.util.Map;

@Data
@Builder
public class FrameStats {

    private int size;

    private double neededDeltaPercentage;

    private double neededPercentageConsistent;

    private Map<Byte, Integer> byteCountMap;

    private int countBytesConsistentByRange;

    private double consistencyRateByItemCount;

    private double consistencyRateByItemAvgDiff;

    private double averageDeviationFromIdealDist;

    // stats of byteCount map
    private DescriptiveStatistics itemCountStats;

    // stats of differences between each item and average
    private DescriptiveStatistics itemAvgDiffStats;

    public boolean isConsistentFrame() {
        return consistencyRateByItemAvgDiff >= neededPercentageConsistent
                && consistencyRateByItemCount >= neededPercentageConsistent;
    }
}
