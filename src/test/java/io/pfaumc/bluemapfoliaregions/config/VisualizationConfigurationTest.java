package io.pfaumc.bluemapfoliaregions.config;

import de.bluecolored.bluemap.api.math.Color;
import io.pfaumc.bluemapfoliaregions.config.VisualizationConfiguration.MarkerColors;
import io.pfaumc.bluemapfoliaregions.performance.PerformanceThresholds;
import io.pfaumc.bluemapfoliaregions.performance.RegionPerformanceSnapshot;
import io.pfaumc.bluemapfoliaregions.performance.RegionStatus;
import io.pfaumc.bluemapfoliaregions.performance.ReportWindow;
import io.pfaumc.bluemapfoliaregions.performance.VisualizationMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VisualizationConfigurationTest {
    private static final MarkerColors COLORS = new MarkerColors(
            new Color(0, 0, 0),
            new Color(255, 255, 255)
    );

    @Test
    void separatesOverallHealthFromSelectedColorMetric() {
        RegionPerformanceSnapshot performance = performance(20.0D, 10.0D, 0.95D);
        VisualizationConfiguration visualization = visualization(VisualizationMode.MSPT);

        assertEquals(RegionStatus.CRITICAL, visualization.overallStatus(performance));
        assertEquals(RegionStatus.NORMAL, visualization.visualizationStatus(performance));
    }

    @Test
    void staticModeStillReportsOverallHealth() {
        RegionPerformanceSnapshot performance = performance(14.0D, 10.0D, 0.20D);
        VisualizationConfiguration visualization = visualization(VisualizationMode.STATIC);

        assertEquals(RegionStatus.CRITICAL, visualization.visualizationStatus(performance));
    }

    @Test
    void reportsUnavailableWhenFoliaHasNotCollectedDataYet() {
        VisualizationConfiguration visualization = visualization(VisualizationMode.UTILIZATION);

        assertEquals(
                RegionStatus.UNAVAILABLE,
                visualization.overallStatus(RegionPerformanceSnapshot.unavailable(ReportWindow.FIFTEEN_SECONDS))
        );
    }

    @Test
    void classifiesEachPerformanceMetricIndependently() {
        RegionPerformanceSnapshot performance = performance(17.0D, 52.0D, 0.80D);
        VisualizationConfiguration visualization = visualization(VisualizationMode.UTILIZATION);

        assertEquals(RegionStatus.HIGH, visualization.tpsStatus(performance));
        assertEquals(RegionStatus.CRITICAL, visualization.msptStatus(performance));
        assertEquals(RegionStatus.HIGH, visualization.utilizationStatus(performance));
        assertEquals(RegionStatus.CRITICAL, visualization.overallStatus(performance));
    }

    private static VisualizationConfiguration visualization(VisualizationMode mode) {
        return new VisualizationConfiguration(
                mode,
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
    }

    private static RegionPerformanceSnapshot performance(double tps, double mspt, double utilization) {
        return new RegionPerformanceSnapshot(
                true,
                ReportWindow.FIFTEEN_SECONDS,
                100,
                tps,
                mspt,
                mspt,
                mspt,
                utilization
        );
    }
}
