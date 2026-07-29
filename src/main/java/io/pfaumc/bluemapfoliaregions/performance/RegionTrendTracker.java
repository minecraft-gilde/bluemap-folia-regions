package io.pfaumc.bluemapfoliaregions.performance;

import io.pfaumc.bluemapfoliaregions.config.TrendConfiguration;
import io.pfaumc.bluemapfoliaregions.config.VisualizationConfiguration;
import io.pfaumc.bluemapfoliaregions.region.RegionSnapshot;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class RegionTrendTracker {
    private final TrendConfiguration configuration;
    private Map<Long, TrackedRegion> regions = Map.of();

    public RegionTrendTracker(TrendConfiguration configuration) {
        this.configuration = configuration;
    }

    public synchronized Map<Long, RegionTrendSnapshot> update(
            Collection<RegionSnapshot> snapshots,
            VisualizationConfiguration visualization
    ) {
        if (!this.configuration.enabled()) {
            this.regions = Map.of();
            return Map.of();
        }

        Map<Long, TrackedRegion> nextRegions = new HashMap<>(snapshots.size());
        Map<Long, RegionTrendSnapshot> trends = new HashMap<>(snapshots.size());
        for (RegionSnapshot snapshot : snapshots) {
            TrackedRegion previous = this.regions.get(snapshot.regionId());
            Set<Long> sections = Set.copyOf(snapshot.sections());
            boolean topologyComparable = isTopologyComparable(previous, snapshot, sections);
            boolean performanceComparable = topologyComparable
                    && previous.performance().available()
                    && snapshot.performance().available();
            RegionStatus status = visualization.overallStatus(snapshot.performance());
            Instant warningSince = warningSince(previous, snapshot.capturedAt(), status, topologyComparable);
            int sampleCount = topologyComparable ? previous.sampleCount() + 1 : 1;
            boolean tickSpike = snapshot.performance().available()
                    && snapshot.performance().worstOnePercentMspt()
                    >= visualization.msptThresholds().critical();

            RegionTrendSnapshot trend = topologyComparable
                    ? compare(
                            previous,
                            snapshot,
                            warningSince,
                            tickSpike,
                            sampleCount,
                            performanceComparable
                    )
                    : RegionTrendSnapshot.unavailable(
                            tickSpike,
                            isWarning(status),
                            warningDuration(warningSince, snapshot.capturedAt())
                    );
            trends.put(snapshot.regionId(), trend);
            nextRegions.put(snapshot.regionId(), new TrackedRegion(
                    sections,
                    snapshot.performance(),
                    snapshot.capturedAt(),
                    status,
                    warningSince,
                    snapshot.entityCount(),
                    snapshot.playerCount(),
                    sampleCount
            ));
        }
        this.regions = Map.copyOf(nextRegions);
        return Map.copyOf(trends);
    }

    synchronized int trackedRegionCount() {
        return this.regions.size();
    }

    private boolean isTopologyComparable(TrackedRegion previous, RegionSnapshot current, Set<Long> sections) {
        if (previous == null
                || !previous.sections().equals(sections)) {
            return false;
        }
        Duration gap = Duration.between(previous.capturedAt(), current.capturedAt());
        return !gap.isNegative() && gap.compareTo(this.configuration.resetAfter()) <= 0;
    }

    private RegionTrendSnapshot compare(
            TrackedRegion previous,
            RegionSnapshot current,
            Instant warningSince,
            boolean tickSpike,
            int sampleCount,
            boolean performanceComparable
    ) {
        RegionPerformanceSnapshot oldPerformance = previous.performance();
        RegionPerformanceSnapshot newPerformance = current.performance();
        return new RegionTrendSnapshot(
                performanceComparable,
                performanceComparable
                        ? classify(
                                oldPerformance.tps(),
                                newPerformance.tps(),
                                this.configuration.minimumTpsChange(),
                                true
                        )
                        : TrendDirection.UNAVAILABLE,
                performanceComparable
                        ? classify(
                                oldPerformance.averageMspt(),
                                newPerformance.averageMspt(),
                                this.configuration.minimumMsptChange(),
                                false
                        )
                        : TrendDirection.UNAVAILABLE,
                performanceComparable
                        ? classify(
                                oldPerformance.utilization(),
                                newPerformance.utilization(),
                                this.configuration.minimumUtilizationChange(),
                                false
                        )
                        : TrendDirection.UNAVAILABLE,
                classifyValue(
                        previous.entityCount(),
                        current.entityCount(),
                        this.configuration.minimumEntityChange()
                ),
                classifyValue(
                        previous.playerCount(),
                        current.playerCount(),
                        this.configuration.minimumPlayerChange()
                ),
                tickSpike,
                warningSince != null,
                warningDuration(warningSince, current.capturedAt()),
                sampleCount
        );
    }

    static TrendDirection classify(
            double previous,
            double current,
            double minimumChange,
            boolean higherIsBetter
    ) {
        double difference = current - previous;
        if (Math.abs(difference) <= minimumChange) {
            return TrendDirection.STABLE;
        }
        boolean improving = higherIsBetter ? difference > 0.0D : difference < 0.0D;
        return improving ? TrendDirection.IMPROVING : TrendDirection.WORSENING;
    }

    static ValueTrend classifyValue(int previous, int current, int minimumChange) {
        int difference = current - previous;
        if (difference == 0 || Math.abs((long) difference) < minimumChange) {
            return ValueTrend.STABLE;
        }
        return difference > 0 ? ValueTrend.INCREASING : ValueTrend.DECREASING;
    }

    private static Instant warningSince(
            TrackedRegion previous,
            Instant capturedAt,
            RegionStatus status,
            boolean comparable
    ) {
        if (!isWarning(status)) {
            return null;
        }
        if (comparable && isWarning(previous.status()) && previous.warningSince() != null) {
            return previous.warningSince();
        }
        return capturedAt;
    }

    private static boolean isWarning(RegionStatus status) {
        return status == RegionStatus.WARNING
                || status == RegionStatus.HIGH
                || status == RegionStatus.CRITICAL;
    }

    private static Duration warningDuration(Instant warningSince, Instant capturedAt) {
        return warningSince == null ? Duration.ZERO : Duration.between(warningSince, capturedAt);
    }

    private record TrackedRegion(
            Set<Long> sections,
            RegionPerformanceSnapshot performance,
            Instant capturedAt,
            RegionStatus status,
            Instant warningSince,
            int entityCount,
            int playerCount,
            int sampleCount
    ) {}
}
