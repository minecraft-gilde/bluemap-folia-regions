package io.pfaumc.bluemapfoliaregions.performance;

import de.bluecolored.bluemap.api.math.Color;
import io.pfaumc.bluemapfoliaregions.config.TrendConfiguration;
import io.pfaumc.bluemapfoliaregions.config.VisualizationConfiguration;
import io.pfaumc.bluemapfoliaregions.config.VisualizationConfiguration.MarkerColors;
import io.pfaumc.bluemapfoliaregions.region.RegionSnapshot;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegionTrendTrackerTest {
    private static final TrendConfiguration CONFIGURATION =
            new TrendConfiguration(true, Duration.ofSeconds(30), 0.10D, 1.0D, 0.02D);
    private static final MarkerColors COLORS =
            new MarkerColors(new Color(0, 0, 0), new Color(255, 255, 255));
    private static final VisualizationConfiguration VISUALIZATION = new VisualizationConfiguration(
            VisualizationMode.UTILIZATION,
            ReportWindow.FIFTEEN_SECONDS,
            new PerformanceThresholds(0.60D, 0.75D, 0.90D, false),
            new PerformanceThresholds(25.0D, 40.0D, 50.0D, false),
            new PerformanceThresholds(19.5D, 18.0D, 15.0D, true),
            COLORS,
            COLORS,
            COLORS,
            COLORS,
            COLORS
    );

    @Test
    void comparesConsecutiveSamplesAndTracksWarningDuration() {
        RegionTrendTracker tracker = new RegionTrendTracker(CONFIGURATION);
        Instant firstCapture = Instant.parse("2026-07-29T10:00:00Z");

        RegionTrendSnapshot first = tracker.update(
                List.of(snapshot(firstCapture, List.of(1L, 2L), 19.0D, 30.0D, 0.70D, 45.0D)),
                VISUALIZATION
        ).get(42L);
        RegionTrendSnapshot second = tracker.update(
                List.of(snapshot(firstCapture.plusSeconds(10), List.of(1L, 2L), 18.5D, 27.0D, 0.80D, 55.0D)),
                VISUALIZATION
        ).get(42L);

        assertFalse(first.comparisonAvailable());
        assertTrue(second.comparisonAvailable());
        assertEquals(TrendDirection.WORSENING, second.tps());
        assertEquals(TrendDirection.IMPROVING, second.mspt());
        assertEquals(TrendDirection.WORSENING, second.utilization());
        assertTrue(second.tickSpike());
        assertTrue(second.warningActive());
        assertEquals(Duration.ofSeconds(10), second.warningDuration());
        assertEquals(2, second.sampleCount());
    }

    @Test
    void resetsComparisonWhenRegionSectionsChange() {
        RegionTrendTracker tracker = new RegionTrendTracker(CONFIGURATION);
        Instant firstCapture = Instant.parse("2026-07-29T10:00:00Z");
        tracker.update(
                List.of(snapshot(firstCapture, List.of(1L, 2L), 20.0D, 10.0D, 0.20D, 20.0D)),
                VISUALIZATION
        );

        RegionTrendSnapshot trend = tracker.update(
                List.of(snapshot(firstCapture.plusSeconds(5), List.of(1L, 3L), 18.0D, 40.0D, 0.80D, 55.0D)),
                VISUALIZATION
        ).get(42L);

        assertFalse(trend.comparisonAvailable());
        assertEquals(1, trend.sampleCount());
        assertEquals(Duration.ZERO, trend.warningDuration());
    }

    @Test
    void resetsComparisonAfterConfiguredGap() {
        RegionTrendTracker tracker = new RegionTrendTracker(CONFIGURATION);
        Instant firstCapture = Instant.parse("2026-07-29T10:00:00Z");
        tracker.update(
                List.of(snapshot(firstCapture, List.of(1L), 20.0D, 10.0D, 0.20D, 20.0D)),
                VISUALIZATION
        );

        RegionTrendSnapshot trend = tracker.update(
                List.of(snapshot(firstCapture.plusSeconds(31), List.of(1L), 18.0D, 40.0D, 0.80D, 55.0D)),
                VISUALIZATION
        ).get(42L);

        assertFalse(trend.comparisonAvailable());
        assertEquals(1, trend.sampleCount());
    }

    @Test
    void treatsChangesInsideSensitivityAsStable() {
        assertEquals(TrendDirection.STABLE, RegionTrendTracker.classify(20.0D, 19.95D, 0.10D, true));
        assertEquals(TrendDirection.STABLE, RegionTrendTracker.classify(10.0D, 10.5D, 1.0D, false));
        assertEquals(TrendDirection.STABLE, RegionTrendTracker.classify(10.0D, 10.0D, 0.0D, false));
    }

    private static RegionSnapshot snapshot(
            Instant capturedAt,
            List<Long> sections,
            double tps,
            double mspt,
            double utilization,
            double worstOnePercentMspt
    ) {
        return new RegionSnapshot(
                42L,
                "world",
                10,
                -20,
                sections,
                128,
                8,
                12,
                2,
                new RegionPerformanceSnapshot(
                        true,
                        ReportWindow.FIFTEEN_SECONDS,
                        200,
                        tps,
                        mspt,
                        30.0D,
                        worstOnePercentMspt,
                        utilization
                ),
                capturedAt
        );
    }
}
