package io.pfaumc.bluemapfoliaregions.config;

import java.time.Duration;

public record TrendConfiguration(
        boolean enabled,
        Duration resetAfter,
        double minimumTpsChange,
        double minimumMsptChange,
        double minimumUtilizationChange,
        int minimumEntityChange,
        int minimumPlayerChange
) {
    public TrendConfiguration {
        if (resetAfter.isNegative() || resetAfter.isZero()) {
            throw new IllegalArgumentException("resetAfter must be positive");
        }
        if (!Double.isFinite(minimumTpsChange)
                || !Double.isFinite(minimumMsptChange)
                || !Double.isFinite(minimumUtilizationChange)
                || minimumTpsChange < 0.0D
                || minimumMsptChange < 0.0D
                || minimumUtilizationChange < 0.0D
                || minimumEntityChange < 0
                || minimumPlayerChange < 0) {
            throw new IllegalArgumentException("Trend sensitivities must not be negative");
        }
    }
}
