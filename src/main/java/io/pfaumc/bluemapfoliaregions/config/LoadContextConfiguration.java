package io.pfaumc.bluemapfoliaregions.config;

import io.pfaumc.bluemapfoliaregions.performance.PerformanceThresholds;

import java.util.Objects;

public record LoadContextConfiguration(
        boolean enabled,
        PerformanceThresholds entityDensityThresholds,
        PerformanceThresholds regionChunkThresholds
) {
    public LoadContextConfiguration {
        entityDensityThresholds = Objects.requireNonNull(entityDensityThresholds, "entityDensityThresholds");
        regionChunkThresholds = Objects.requireNonNull(regionChunkThresholds, "regionChunkThresholds");
    }

    public static LoadContextConfiguration disabled() {
        PerformanceThresholds unreachable =
                new PerformanceThresholds(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE, false);
        return new LoadContextConfiguration(false, unreachable, unreachable);
    }
}
