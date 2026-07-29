package io.pfaumc.bluemapfoliaregions.marker;

import de.bluecolored.bluemap.api.math.Color;
import io.pfaumc.bluemapfoliaregions.config.VisualizationConfiguration;
import io.pfaumc.bluemapfoliaregions.performance.PerformanceThresholds;
import io.pfaumc.bluemapfoliaregions.performance.RegionPerformanceSnapshot;
import io.pfaumc.bluemapfoliaregions.performance.RegionStatus;
import io.pfaumc.bluemapfoliaregions.performance.RegionTrendSnapshot;
import io.pfaumc.bluemapfoliaregions.performance.TrendDirection;
import io.pfaumc.bluemapfoliaregions.region.RegionSnapshot;

import java.text.NumberFormat;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class RegionTextFormatter {
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{([a-z0-9_]+)}");

    private RegionTextFormatter() {}

    static String formatLabel(
            String format,
            RegionSnapshot snapshot,
            VisualizationConfiguration visualization,
            DateTimeFormatter timestampFormatter
    ) {
        return formatLabel(
                format,
                snapshot,
                noTrend(),
                visualization,
                timestampFormatter
        );
    }

    static String formatLabel(
            String format,
            RegionSnapshot snapshot,
            RegionTrendSnapshot trend,
            VisualizationConfiguration visualization,
            DateTimeFormatter timestampFormatter
    ) {
        return format(format, snapshot, trend, visualization, timestampFormatter, Function.identity());
    }

    static String formatDetail(
            String format,
            RegionSnapshot snapshot,
            VisualizationConfiguration visualization,
            DateTimeFormatter timestampFormatter
    ) {
        return formatDetail(
                format,
                snapshot,
                noTrend(),
                visualization,
                timestampFormatter
        );
    }

    static String formatDetail(
            String format,
            RegionSnapshot snapshot,
            RegionTrendSnapshot trend,
            VisualizationConfiguration visualization,
            DateTimeFormatter timestampFormatter
    ) {
        return format(
                format,
                snapshot,
                trend,
                visualization,
                timestampFormatter,
                RegionTextFormatter::escapeHtml
        );
    }

    private static String format(
            String format,
            RegionSnapshot snapshot,
            RegionTrendSnapshot trend,
            VisualizationConfiguration visualization,
            DateTimeFormatter timestampFormatter,
            Function<String, String> valueSanitizer
    ) {
        RegionPerformanceSnapshot performance = snapshot.performance();
        RegionStatus overallStatus = visualization.overallStatus(performance);
        RegionStatus visualizationStatus = visualization.visualizationStatus(performance);
        RegionStatus tpsStatus = visualization.tpsStatus(performance);
        RegionStatus msptStatus = visualization.msptStatus(performance);
        RegionStatus utilizationStatus = visualization.utilizationStatus(performance);
        String unavailable = "n/a";
        Map<String, String> values = Map.ofEntries(
                Map.entry("region_id", Long.toString(snapshot.regionId())),
                Map.entry("world", snapshot.worldName()),
                Map.entry("center_x", Integer.toString(snapshot.centerChunkX())),
                Map.entry("center_z", Integer.toString(snapshot.centerChunkZ())),
                Map.entry("center_chunk_x", Integer.toString(snapshot.centerChunkX())),
                Map.entry("center_chunk_z", Integer.toString(snapshot.centerChunkZ())),
                Map.entry("center_block_x", Integer.toString(snapshot.centerBlockX())),
                Map.entry("center_block_z", Integer.toString(snapshot.centerBlockZ())),
                Map.entry("sections", Integer.toString(snapshot.sectionCount())),
                Map.entry("sections_formatted", formatInteger(snapshot.sectionCount())),
                Map.entry("chunks", Integer.toString(snapshot.chunkCount())),
                Map.entry("chunks_formatted", formatInteger(snapshot.chunkCount())),
                Map.entry("area_blocks", Long.toString(snapshot.areaBlocks())),
                Map.entry("area_blocks_formatted", formatInteger(snapshot.areaBlocks())),
                Map.entry("entities", Integer.toString(snapshot.entityCount())),
                Map.entry("entities_formatted", formatInteger(snapshot.entityCount())),
                Map.entry("players", Integer.toString(snapshot.playerCount())),
                Map.entry("players_formatted", formatInteger(snapshot.playerCount())),
                Map.entry("entities_per_chunk", formatDensity(snapshot.entitiesPerChunk())),
                Map.entry("players_per_chunk", formatDensity(snapshot.playersPerChunk())),
                Map.entry("report_window", performance.reportWindow().configValue()),
                Map.entry("collected_ticks", Integer.toString(performance.collectedTicks())),
                Map.entry("collected_ticks_formatted", formatInteger(performance.collectedTicks())),
                Map.entry("tps", performance.available() ? formatMetric(performance.tps()) : unavailable),
                Map.entry("tps_status", tpsStatus.displayName()),
                Map.entry("tps_status_color", metricStatusColor(visualization, tpsStatus)),
                Map.entry("mspt", performance.available() ? formatMetric(performance.averageMspt()) : unavailable),
                Map.entry("mspt_status", msptStatus.displayName()),
                Map.entry("mspt_status_color", metricStatusColor(visualization, msptStatus)),
                Map.entry(
                        "mspt_worst_5",
                        performance.available() ? formatMetric(performance.worstFivePercentMspt()) : unavailable
                ),
                Map.entry(
                        "mspt_worst_1",
                        performance.available() ? formatMetric(performance.worstOnePercentMspt()) : unavailable
                ),
                Map.entry(
                        "utilization",
                        performance.available() ? formatMetric(performance.utilization() * 100.0D) : unavailable
                ),
                Map.entry("utilization_status", utilizationStatus.displayName()),
                Map.entry(
                        "utilization_status_color",
                        metricStatusColor(visualization, utilizationStatus)
                ),
                Map.entry("diagnosis", diagnosis(performance, visualization)),
                Map.entry("status", overallStatus.displayName()),
                Map.entry("status_color", cssColor(visualization.colorsForStatus(overallStatus).line())),
                Map.entry("visualization_status", visualizationStatus.displayName()),
                Map.entry(
                        "visualization_color",
                        cssColor(visualization.colorsForStatus(visualizationStatus).line())
                ),
                Map.entry("visualization_mode", visualization.mode().configValue()),
                Map.entry("trend_available", Boolean.toString(trend.comparisonAvailable())),
                Map.entry("trend_samples", Integer.toString(trend.sampleCount())),
                Map.entry("tps_trend", trendSymbol(trend.tps(), true)),
                Map.entry("tps_trend_status", trend.tps().displayName()),
                Map.entry("tps_trend_color", trendColor(trend.tps())),
                Map.entry("mspt_trend", trendSymbol(trend.mspt(), false)),
                Map.entry("mspt_trend_status", trend.mspt().displayName()),
                Map.entry("mspt_trend_color", trendColor(trend.mspt())),
                Map.entry("utilization_trend", trendSymbol(trend.utilization(), false)),
                Map.entry("utilization_trend_status", trend.utilization().displayName()),
                Map.entry("utilization_trend_color", trendColor(trend.utilization())),
                Map.entry("tick_spike", trend.tickSpike() ? "Ja" : "Nein"),
                Map.entry("spike_detail", trend.tickSpike() ? " · Tickspitze" : ""),
                Map.entry(
                        "warning_duration",
                        trend.warningActive() ? formatDuration(trend.warningDuration()) : unavailable
                ),
                Map.entry(
                        "warning_duration_detail",
                        trend.warningActive()
                                ? " · Warnung seit " + formatDuration(trend.warningDuration())
                                : ""
                ),
                Map.entry("updated_at", timestampFormatter.format(snapshot.capturedAt()))
        );

        Matcher matcher = PLACEHOLDER.matcher(format);
        StringBuilder result = new StringBuilder(format.length() + 64);
        while (matcher.find()) {
            String value = values.get(matcher.group(1));
            if (value == null) {
                matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group()));
            } else {
                matcher.appendReplacement(result, Matcher.quoteReplacement(valueSanitizer.apply(value)));
            }
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static String formatDensity(double density) {
        return formatMetric(density);
    }

    static String formatDuration(Duration duration) {
        long seconds = Math.max(0L, duration.toSeconds());
        if (seconds == 0L) {
            return "< 1 s";
        }
        if (seconds < 60L) {
            return seconds + " s";
        }
        long minutes = seconds / 60L;
        long remainingSeconds = seconds % 60L;
        if (minutes < 60L) {
            return remainingSeconds == 0L
                    ? minutes + " min"
                    : minutes + " min " + remainingSeconds + " s";
        }
        long hours = minutes / 60L;
        long remainingMinutes = minutes % 60L;
        return remainingMinutes == 0L
                ? hours + " h"
                : hours + " h " + remainingMinutes + " min";
    }

    private static String trendSymbol(TrendDirection direction, boolean higherIsBetter) {
        return switch (direction) {
            case IMPROVING -> higherIsBetter ? "↑" : "↓";
            case STABLE -> "→";
            case WORSENING -> higherIsBetter ? "↓" : "↑";
            case UNAVAILABLE -> "–";
        };
    }

    private static String trendColor(TrendDirection direction) {
        return switch (direction) {
            case IMPROVING -> "#51cf66";
            case STABLE -> "rgba(255,255,255,.55)";
            case WORSENING -> "#ff6b6b";
            case UNAVAILABLE -> "rgba(255,255,255,.35)";
        };
    }

    private static String metricStatusColor(
            VisualizationConfiguration visualization,
            RegionStatus status
    ) {
        return status == RegionStatus.UNAVAILABLE
                ? "rgba(255,255,255,.45)"
                : cssColor(visualization.colorsForStatus(status).line());
    }

    private static String diagnosis(
            RegionPerformanceSnapshot performance,
            VisualizationConfiguration visualization
    ) {
        if (!performance.available()) {
            return "Bewertung: Noch keine Leistungsdaten";
        }

        List<MetricCause> causes = new ArrayList<>(3);
        addCause(
                causes,
                "TPS",
                visualization.tpsStatus(performance),
                visualization.tpsThresholds(),
                " TPS",
                performance.tps()
        );
        addCause(
                causes,
                "Tickzeit",
                visualization.msptStatus(performance),
                visualization.msptThresholds(),
                " ms",
                performance.averageMspt()
        );
        addCause(
                causes,
                "Auslastung",
                visualization.utilizationStatus(performance),
                visualization.utilizationThresholds(),
                " %",
                performance.utilization(),
                100.0D
        );
        if (causes.isEmpty()) {
            return "Bewertung: Alle Leistungswerte normal";
        }

        causes.sort(Comparator.comparingInt((MetricCause cause) -> cause.status().ordinal()).reversed());
        String prefix = causes.size() == 1 ? "Ursache: " : "Ursachen: ";
        return prefix + String.join(
                " · ",
                causes.stream().map(RegionTextFormatter::formatCause).toList()
        );
    }

    private static void addCause(
            List<MetricCause> causes,
            String metric,
            RegionStatus status,
            PerformanceThresholds thresholds,
            String unit,
            double value
    ) {
        addCause(causes, metric, status, thresholds, unit, value, 1.0D);
    }

    private static void addCause(
            List<MetricCause> causes,
            String metric,
            RegionStatus status,
            PerformanceThresholds thresholds,
            String unit,
            double value,
            double scale
    ) {
        if (status == RegionStatus.WARNING
                || status == RegionStatus.HIGH
                || status == RegionStatus.CRITICAL) {
            causes.add(new MetricCause(metric, status, thresholds, unit, value, scale));
        }
    }

    private static String formatCause(MetricCause cause) {
        String comparator = cause.thresholds().lowerIsWorse() ? "≤" : "≥";
        double threshold = cause.thresholds().thresholdFor(cause.status()) * cause.scale();
        return cause.metric()
                + ": "
                + cause.status().displayName()
                + " ("
                + formatMetric(cause.value() * cause.scale())
                + " "
                + comparator
                + " "
                + formatMetric(threshold)
                + cause.unit()
                + ")";
    }

    private static RegionTrendSnapshot noTrend() {
        return RegionTrendSnapshot.unavailable(false, false, Duration.ZERO);
    }

    private record MetricCause(
            String metric,
            RegionStatus status,
            PerformanceThresholds thresholds,
            String unit,
            double value,
            double scale
    ) {}

    private static String formatMetric(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static String formatInteger(long value) {
        return NumberFormat.getIntegerInstance(Locale.GERMANY).format(value);
    }

    private static String cssColor(Color color) {
        return String.format(
                Locale.ROOT,
                "rgba(%d,%d,%d,%.3f)",
                color.getRed(),
                color.getGreen(),
                color.getBlue(),
                color.getAlpha()
        );
    }

    private static String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
