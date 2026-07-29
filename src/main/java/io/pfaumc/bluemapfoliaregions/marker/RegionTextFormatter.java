package io.pfaumc.bluemapfoliaregions.marker;

import de.bluecolored.bluemap.api.math.Color;
import io.pfaumc.bluemapfoliaregions.config.VisualizationConfiguration;
import io.pfaumc.bluemapfoliaregions.performance.RegionPerformanceSnapshot;
import io.pfaumc.bluemapfoliaregions.performance.RegionStatus;
import io.pfaumc.bluemapfoliaregions.region.RegionSnapshot;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
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
        return format(format, snapshot, visualization, timestampFormatter, Function.identity());
    }

    static String formatDetail(
            String format,
            RegionSnapshot snapshot,
            VisualizationConfiguration visualization,
            DateTimeFormatter timestampFormatter
    ) {
        return format(format, snapshot, visualization, timestampFormatter, RegionTextFormatter::escapeHtml);
    }

    private static String format(
            String format,
            RegionSnapshot snapshot,
            VisualizationConfiguration visualization,
            DateTimeFormatter timestampFormatter,
            Function<String, String> valueSanitizer
    ) {
        RegionPerformanceSnapshot performance = snapshot.performance();
        RegionStatus overallStatus = visualization.overallStatus(performance);
        RegionStatus visualizationStatus = visualization.visualizationStatus(performance);
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
                Map.entry("mspt", performance.available() ? formatMetric(performance.averageMspt()) : unavailable),
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
                Map.entry("status", overallStatus.displayName()),
                Map.entry("status_color", cssColor(visualization.colorsForStatus(overallStatus).line())),
                Map.entry("visualization_status", visualizationStatus.displayName()),
                Map.entry(
                        "visualization_color",
                        cssColor(visualization.colorsForStatus(visualizationStatus).line())
                ),
                Map.entry("visualization_mode", visualization.mode().configValue()),
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
