package io.pfaumc.bluemapfoliaregions.config;

import de.bluecolored.bluemap.api.math.Color;
import io.pfaumc.bluemapfoliaregions.performance.PerformanceThresholds;
import io.pfaumc.bluemapfoliaregions.performance.RegionPerformanceSnapshot;
import io.pfaumc.bluemapfoliaregions.performance.RegionStatus;
import io.pfaumc.bluemapfoliaregions.performance.ReportWindow;
import io.pfaumc.bluemapfoliaregions.performance.VisualizationMode;

public record VisualizationConfiguration(
        VisualizationMode mode,
        ReportWindow reportWindow,
        PerformanceThresholds utilizationThresholds,
        PerformanceThresholds msptThresholds,
        PerformanceThresholds tpsThresholds,
        MarkerColors normalColors,
        MarkerColors warningColors,
        MarkerColors highColors,
        MarkerColors criticalColors,
        MarkerColors unavailableColors
) {
    public RegionStatus overallStatus(RegionPerformanceSnapshot performance) {
        if (!performance.available()) {
            return RegionStatus.UNAVAILABLE;
        }
        return RegionStatus.worst(
                this.utilizationThresholds.classify(performance.utilization()),
                this.msptThresholds.classify(performance.averageMspt()),
                this.tpsThresholds.classify(performance.tps())
        );
    }

    public RegionStatus visualizationStatus(RegionPerformanceSnapshot performance) {
        if (!performance.available()) {
            return RegionStatus.UNAVAILABLE;
        }
        return switch (this.mode) {
            case STATIC -> overallStatus(performance);
            case UTILIZATION -> this.utilizationThresholds.classify(performance.utilization());
            case MSPT -> this.msptThresholds.classify(performance.averageMspt());
            case TPS -> this.tpsThresholds.classify(performance.tps());
        };
    }

    public MarkerColors colorsFor(RegionPerformanceSnapshot performance, MarkerColors staticColors) {
        if (this.mode == VisualizationMode.STATIC) {
            return staticColors;
        }
        return colorsForStatus(visualizationStatus(performance));
    }

    public MarkerColors colorsForStatus(RegionStatus status) {
        return switch (status) {
            case NORMAL -> this.normalColors;
            case WARNING -> this.warningColors;
            case HIGH -> this.highColors;
            case CRITICAL -> this.criticalColors;
            case UNAVAILABLE -> this.unavailableColors;
        };
    }

    public record MarkerColors(Color line, Color fill) {}
}
