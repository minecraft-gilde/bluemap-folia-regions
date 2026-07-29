package io.pfaumc.bluemapfoliaregions.performance;

import java.time.Duration;

public record RegionTrendSnapshot(
        boolean comparisonAvailable,
        TrendDirection tps,
        TrendDirection mspt,
        TrendDirection utilization,
        ValueTrend entities,
        ValueTrend players,
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
                ValueTrend.UNAVAILABLE,
                ValueTrend.UNAVAILABLE,
                tickSpike,
                warningActive,
                warningDuration,
                1
        );
    }
}
