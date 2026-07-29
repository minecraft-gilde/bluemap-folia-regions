package io.pfaumc.bluemapfoliaregions.performance;

import java.time.Duration;

public record RegionTrendSnapshot(
        boolean comparisonAvailable,
        TrendDirection tps,
        TrendDirection mspt,
        TrendDirection utilization,
        boolean tickSpike,
        boolean warningActive,
        Duration warningDuration,
        int sampleCount
) {
    public static RegionTrendSnapshot unavailable(
            boolean tickSpike,
            boolean warningActive,
            Duration warningDuration
    ) {
        return new RegionTrendSnapshot(
                false,
                TrendDirection.UNAVAILABLE,
                TrendDirection.UNAVAILABLE,
                TrendDirection.UNAVAILABLE,
                tickSpike,
                warningActive,
                warningDuration,
                1
        );
    }
}
