package io.pfaumc.bluemapfoliaregions.marker;

import de.bluecolored.bluemap.api.math.Color;
import io.pfaumc.bluemapfoliaregions.config.VisualizationConfiguration;
import io.pfaumc.bluemapfoliaregions.config.VisualizationConfiguration.MarkerColors;
import io.pfaumc.bluemapfoliaregions.performance.PerformanceThresholds;
import io.pfaumc.bluemapfoliaregions.performance.RegionPerformanceSnapshot;
import io.pfaumc.bluemapfoliaregions.performance.RegionTrendSnapshot;
import io.pfaumc.bluemapfoliaregions.performance.ReportWindow;
import io.pfaumc.bluemapfoliaregions.performance.TrendDirection;
import io.pfaumc.bluemapfoliaregions.performance.VisualizationMode;
import io.pfaumc.bluemapfoliaregions.region.RegionSnapshot;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RegionTextFormatterTest {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss z")
            .withZone(ZoneOffset.UTC);
    private static final MarkerColors COLORS = new MarkerColors(
            new Color(0, 0, 0),
            new Color(255, 255, 255)
    );
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
    void replacesAllSupportedRegionPlaceholders() {
        String format = "{region_id}|{world}|{center_x}|{center_z}|{center_chunk_x}|{center_chunk_z}|"
                + "{center_block_x}|{center_block_z}|{sections}|{chunks}|{area_blocks}|{entities}|{players}|"
                + "{entities_per_chunk}|{players_per_chunk}|{updated_at}";

        String result = RegionTextFormatter.formatLabel(format, snapshot("world"), VISUALIZATION, TIME_FORMATTER);

        assertEquals(
                "42|world|10|-20|10|-20|168|-312|3|8|49152|12|2|1.50|0.25|2026-07-29 10:15:30 Z",
                result
        );
    }

    @Test
    void formatsPerformancePlaceholders() {
        String format = "{report_window}|{collected_ticks}|{tps}|{mspt}|{mspt_worst_5}|{mspt_worst_1}|"
                + "{utilization}|{status}|{visualization_status}|{visualization_mode}";

        String result = RegionTextFormatter.formatLabel(format, snapshot("world"), VISUALIZATION, TIME_FORMATTER);

        assertEquals("15s|200|19.75|12.50|30.00|45.00|42.50|Normal|Normal|utilization", result);
    }

    @Test
    void formatsCompactDisplayValuesAndStatusColors() {
        String format = "{sections_formatted}|{chunks_formatted}|{area_blocks_formatted}|"
                + "{entities_formatted}|{players_formatted}|{collected_ticks_formatted}|{status_color}";

        String result = RegionTextFormatter.formatLabel(format, snapshot("world"), VISUALIZATION, TIME_FORMATTER);

        assertEquals("3|8|49.152|12|2|200|rgba(0,0,0,1.000)", result);
    }

    @Test
    void keepsUnknownPlaceholdersUnchanged() {
        String result = RegionTextFormatter.formatLabel(
                "{region_id} {future_metric}",
                snapshot("world"),
                VISUALIZATION,
                TIME_FORMATTER
        );

        assertEquals("42 {future_metric}", result);
    }

    @Test
    void doesNotInterpretPlaceholdersInsideReplacementValues() {
        String result = RegionTextFormatter.formatLabel(
                "{world}",
                snapshot("{chunks}"),
                VISUALIZATION,
                TIME_FORMATTER
        );

        assertEquals("{chunks}", result);
    }

    @Test
    void escapesDynamicValuesInHtmlDetails() {
        String result = RegionTextFormatter.formatDetail(
                "<b>{world}</b>",
                snapshot("<world & 'friends'>"),
                VISUALIZATION,
                TIME_FORMATTER
        );

        assertEquals("<b>&lt;world &amp; &#39;friends&#39;&gt;</b>", result);
    }

    @Test
    void formatsCompactPerformanceTrends() {
        RegionTrendSnapshot trend = new RegionTrendSnapshot(
                true,
                TrendDirection.IMPROVING,
                TrendDirection.STABLE,
                TrendDirection.WORSENING,
                true,
                true,
                Duration.ofSeconds(65),
                4
        );
        String format = "{tps_trend}|{mspt_trend}|{utilization_trend}|{tps_trend_status}|"
                + "{tick_spike}|{warning_duration}|{trend_samples}{spike_detail}{warning_duration_detail}";

        String result = RegionTextFormatter.formatLabel(
                format,
                snapshot("world"),
                trend,
                VISUALIZATION,
                TIME_FORMATTER
        );

        assertEquals("↑|→|↑|Besser|Ja|1 min 5 s|4 · Tickspitze · Warnung seit 1 min 5 s", result);
    }

    @Test
    void escapesInitialWarningDurationInHtml() {
        RegionTrendSnapshot trend = RegionTrendSnapshot.unavailable(true, true, Duration.ZERO);

        String result = RegionTextFormatter.formatDetail(
                "{warning_duration}",
                snapshot("world"),
                trend,
                VISUALIZATION,
                TIME_FORMATTER
        );

        assertEquals("&lt; 1 s", result);
    }

    @Test
    void formatsLongTrendDurationsCompactly() {
        assertEquals("2 h 5 min", RegionTextFormatter.formatDuration(Duration.ofMinutes(125)));
    }

    private static RegionSnapshot snapshot(String worldName) {
        return new RegionSnapshot(
                42L,
                worldName,
                10,
                -20,
                List.of(1L, 2L, 3L),
                128,
                8,
                12,
                2,
                new RegionPerformanceSnapshot(
                        true,
                        ReportWindow.FIFTEEN_SECONDS,
                        200,
                        19.75D,
                        12.5D,
                        30.0D,
                        45.0D,
                        0.425D
                ),
                Instant.parse("2026-07-29T10:15:30Z")
        );
    }
}
