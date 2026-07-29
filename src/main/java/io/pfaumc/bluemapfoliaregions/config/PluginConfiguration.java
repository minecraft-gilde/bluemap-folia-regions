package io.pfaumc.bluemapfoliaregions.config;

import de.bluecolored.bluemap.api.math.Color;
import io.pfaumc.bluemapfoliaregions.config.VisualizationConfiguration.MarkerColors;
import io.pfaumc.bluemapfoliaregions.performance.PerformanceThresholds;
import io.pfaumc.bluemapfoliaregions.performance.ReportWindow;
import io.pfaumc.bluemapfoliaregions.performance.VisualizationMode;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;

public record PluginConfiguration(
        String markerSetId,
        String markerSetLabel,
        boolean defaultHidden,
        boolean toggleable,
        String markerLabelFormat,
        String markerDetailFormat,
        DateTimeFormatter markerTimestampFormatter,
        int markerHeight,
        Color markerLineColor,
        Color markerFillColor,
        int markerLineWidth,
        VisualizationConfiguration visualization,
        TrendConfiguration trends,
        LoadContextConfiguration loadContext,
        long updateIntervalTicks
) {
    private static final String DEFAULT_MARKER_SET_ID = "folia-regions";
    private static final String DEFAULT_MARKER_SET_LABEL = "Folia Tick-Regionen";
    private static final String DEFAULT_LABEL_FORMAT = "Region[{center_x},{center_z}]";
    private static final String DEFAULT_DETAIL_FORMAT = """
            <div style="width:100%;max-width:100%;box-sizing:border-box;overflow:hidden;font-size:14px;line-height:1.25">
              <div style="display:flex;align-items:center;justify-content:space-between;gap:12px">
                <strong style="font-size:18px">Folia-Region {region_id}</strong>
                <span style="color:{status_color};border:1px solid {status_color};border-radius:999px;padding:2px 8px;font-size:12px;font-weight:700">{status}</span>
              </div>
              <div style="margin-top:3px;opacity:.65;font-size:12px">{world} &middot; Chunk {center_chunk_x}, {center_chunk_z} &middot; Block {center_block_x}, {center_block_z}</div>
              <div style="margin-top:11px;padding-top:9px;border-top:1px solid rgba(255,255,255,.14)">
                <div style="margin-bottom:6px;opacity:.55;font-size:10px;font-weight:700;letter-spacing:.08em">REGION</div>
                <div style="display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:12px">
                  <div><strong>{sections_formatted}</strong><div style="opacity:.6;font-size:11px">Sektionen</div></div>
                  <div><strong>{chunks_formatted}</strong><div style="opacity:.6;font-size:11px">Chunks</div></div>
                  <div><strong>{area_blocks_formatted}</strong><div style="opacity:.6;font-size:11px">Bl&ouml;cke&sup2;</div></div>
                </div>
              </div>
              <div style="margin-top:11px;padding-top:9px;border-top:1px solid rgba(255,255,255,.14)">
                <div style="margin-bottom:6px;opacity:.55;font-size:10px;font-weight:700;letter-spacing:.08em">AKTIVIT&Auml;T</div>
                <div style="display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:12px">
                  <div><strong>{entities_formatted} <span style="color:{entities_trend_color};font-size:11px">{entities_trend}</span></strong><div style="opacity:.65;font-size:11px">Entit&auml;ten</div><div style="margin-top:1px;opacity:.45;font-size:10px">{entities_per_chunk} / Chunk</div></div>
                  <div><strong>{players_formatted} <span style="color:{players_trend_color};font-size:11px">{players_trend}</span></strong><div style="opacity:.65;font-size:11px">Spieler</div><div style="margin-top:1px;opacity:.45;font-size:10px">{players_per_chunk} / Chunk</div></div>
                </div>
                <div style="display:{load_context_display};margin-top:6px;opacity:.65;font-size:11px;line-height:1.35;overflow-wrap:anywhere">{load_context}</div>
              </div>
              <div style="margin-top:11px;padding-top:9px;border-top:1px solid rgba(255,255,255,.14)">
                <div style="margin-bottom:6px;opacity:.55;font-size:10px;font-weight:700;letter-spacing:.08em">LEISTUNG &middot; {report_window}</div>
                <div style="display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:12px">
                  <div><strong style="color:{tps_status_color}">{tps}</strong><div style="opacity:.6;font-size:11px">TPS</div></div>
                  <div><strong style="color:{mspt_status_color}">{mspt} ms</strong><div style="opacity:.6;font-size:11px">&Oslash; Tickzeit</div></div>
                  <div><strong style="color:{utilization_status_color}">{utilization} %</strong><div style="opacity:.6;font-size:11px">Auslastung</div></div>
                </div>
                <div style="margin-top:7px;opacity:.65;font-size:11px;line-height:1.35">Spitzen: 5 % {mspt_worst_5} ms &middot; 1 % {mspt_worst_1} ms &middot; {collected_ticks_formatted} Ticks</div>
                <div style="margin-top:5px;opacity:.65;font-size:11px;line-height:1.35">Trend: TPS <span style="color:{tps_trend_color};font-weight:700">{tps_trend}</span> &middot; Tickzeit <span style="color:{mspt_trend_color};font-weight:700">{mspt_trend}</span> &middot; Auslastung <span style="color:{utilization_trend_color};font-weight:700">{utilization_trend}</span>{spike_detail}{warning_duration_detail}</div>
                <div style="margin-top:5px;opacity:.75;font-size:11px;line-height:1.35;overflow-wrap:anywhere">{diagnosis}</div>
              </div>
              <div style="margin-top:10px;text-align:right;opacity:.45;font-size:10px">Stand: {updated_at}</div>
            </div>""";
    private static final String LEGACY_DETAIL_FORMAT = """
            <b>Folia-Region {region_id}</b><br>
            Welt: {world}<br>
            Zentrum: Chunk {center_chunk_x}, {center_chunk_z} / Block {center_block_x}, {center_block_z}<br>
            Sektionen: {sections}<br>
            Chunks: {chunks}<br>
            Regionsfl&auml;che: {area_blocks} Bl&ouml;cke&sup2;<br>
            Entit&auml;ten: {entities} ({entities_per_chunk}/Chunk)<br>
            Spieler: {players} ({players_per_chunk}/Chunk)<br>
            Status ({report_window}): {status}<br>
            TPS: {tps}<br>
            Tickzeit: &Oslash; {mspt} ms / schlechteste 5 %: {mspt_worst_5} ms / 1 %: {mspt_worst_1} ms<br>
            Auslastung: {utilization} % ({collected_ticks} Ticks)<br>
            Aktualisiert: {updated_at}""";
    private static final String DEFAULT_TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm:ss z";
    private static final Color DEFAULT_LINE_COLOR = new Color(155, 70, 255, 1.0f);
    private static final Color DEFAULT_FILL_COLOR = new Color(210, 170, 255, 0.35f);
    private static final VisualizationMode DEFAULT_VISUALIZATION_MODE = VisualizationMode.UTILIZATION;
    private static final ReportWindow DEFAULT_REPORT_WINDOW = ReportWindow.FIFTEEN_SECONDS;
    private static final PerformanceThresholds DEFAULT_UTILIZATION_THRESHOLDS =
            new PerformanceThresholds(0.60D, 0.75D, 0.90D, false);
    private static final PerformanceThresholds DEFAULT_MSPT_THRESHOLDS =
            new PerformanceThresholds(25.0D, 40.0D, 50.0D, false);
    private static final PerformanceThresholds DEFAULT_TPS_THRESHOLDS =
            new PerformanceThresholds(19.5D, 18.0D, 15.0D, true);
    private static final PerformanceThresholds DEFAULT_ENTITY_DENSITY_THRESHOLDS =
            new PerformanceThresholds(8.0D, 16.0D, 32.0D, false);
    private static final PerformanceThresholds DEFAULT_REGION_CHUNK_THRESHOLDS =
            new PerformanceThresholds(1_500.0D, 3_000.0D, 5_000.0D, false);

    public static PluginConfiguration from(JavaPlugin plugin) {
        FileConfiguration config = plugin.getConfig();
        Logger logger = plugin.getLogger();
        TrendConfiguration trends = parseTrends(config);

        return new PluginConfiguration(
                stringOrDefault(config.getString("marker-set.id"), DEFAULT_MARKER_SET_ID),
                stringOrDefault(config.getString("marker-set.label"), DEFAULT_MARKER_SET_LABEL),
                config.getBoolean("marker-set.default-hidden", true),
                config.getBoolean("marker-set.toggleable", true),
                stringOrDefault(config.getString("markers.label-format"), DEFAULT_LABEL_FORMAT),
                trendDetailFormat(
                        detailFormatOrDefault(config.getString("markers.detail-format")),
                        trends.enabled()
                ),
                parseTimestampFormatter(
                        config.getString("markers.timestamp-format", DEFAULT_TIMESTAMP_FORMAT),
                        logger
                ),
                Math.max(1, config.getInt("markers.height", 80)),
                parseColor(config.getString("markers.line-color", "#9b46ffff"), DEFAULT_LINE_COLOR, logger),
                parseColor(config.getString("markers.fill-color", "#d2aaff59"), DEFAULT_FILL_COLOR, logger),
                Math.max(1, config.getInt("markers.line-width", 2)),
                parseVisualization(config, logger),
                trends,
                parseLoadContext(config, logger),
                Math.max(20L, config.getLong("update-interval-seconds", 5L) * 20L)
        );
    }

    static TrendConfiguration parseTrends(FileConfiguration config) {
        return new TrendConfiguration(
                config.getBoolean("trends.enabled", true),
                Duration.ofSeconds(Math.max(1L, config.getLong("trends.reset-after-seconds", 30L))),
                Math.max(0.0D, config.getDouble("trends.sensitivity.tps", 0.10D)),
                Math.max(0.0D, config.getDouble("trends.sensitivity.mspt", 1.0D)),
                Math.max(
                        0.0D,
                        config.getDouble("trends.sensitivity.utilization-percentage-points", 2.0D) / 100.0D
                ),
                Math.max(0, config.getInt("trends.sensitivity.entities", 5)),
                Math.max(0, config.getInt("trends.sensitivity.players", 1))
        );
    }

    static LoadContextConfiguration parseLoadContext(FileConfiguration config, Logger logger) {
        return new LoadContextConfiguration(
                config.getBoolean("load-context.enabled", true),
                parseThresholds(
                        config,
                        "load-context.thresholds.entities-per-chunk",
                        DEFAULT_ENTITY_DENSITY_THRESHOLDS,
                        logger
                ),
                parseThresholds(
                        config,
                        "load-context.thresholds.region-chunks",
                        DEFAULT_REGION_CHUNK_THRESHOLDS,
                        logger
                )
        );
    }

    private static VisualizationConfiguration parseVisualization(FileConfiguration config, Logger logger) {
        VisualizationMode mode = VisualizationMode.fromConfig(config.getString("visualization.mode"))
                .orElseGet(() -> {
                    warnInvalidOption(
                            config.getString("visualization.mode"),
                            "visualization.mode",
                            DEFAULT_VISUALIZATION_MODE.configValue(),
                            logger
                    );
                    return DEFAULT_VISUALIZATION_MODE;
                });
        ReportWindow reportWindow = ReportWindow.fromConfig(config.getString("visualization.report-window"))
                .orElseGet(() -> {
                    warnInvalidOption(
                            config.getString("visualization.report-window"),
                            "visualization.report-window",
                            DEFAULT_REPORT_WINDOW.configValue(),
                            logger
                    );
                    return DEFAULT_REPORT_WINDOW;
                });

        PerformanceThresholds utilization = parseThresholds(
                config,
                "visualization.thresholds.utilization",
                DEFAULT_UTILIZATION_THRESHOLDS,
                logger
        );
        PerformanceThresholds mspt = parseThresholds(
                config,
                "visualization.thresholds.mspt",
                DEFAULT_MSPT_THRESHOLDS,
                logger
        );
        PerformanceThresholds tps = parseThresholds(
                config,
                "visualization.thresholds.tps",
                DEFAULT_TPS_THRESHOLDS,
                logger
        );

        return new VisualizationConfiguration(
                mode,
                reportWindow,
                utilization,
                mspt,
                tps,
                parseMarkerColors(config, "normal", "#37b24dff", "#51cf6666", logger),
                parseMarkerColors(config, "warning", "#f08c00ff", "#ffd43b73", logger),
                parseMarkerColors(config, "high", "#e8590cff", "#ff922b80", logger),
                parseMarkerColors(config, "critical", "#c92a2aff", "#fa525299", logger),
                parseMarkerColors(config, "unavailable", "#9b46ffff", "#d2aaff59", logger)
        );
    }

    private static PerformanceThresholds parseThresholds(
            FileConfiguration config,
            String path,
            PerformanceThresholds fallback,
            Logger logger
    ) {
        PerformanceThresholds parsed = new PerformanceThresholds(
                config.getDouble(path + ".warning", fallback.warning()),
                config.getDouble(path + ".high", fallback.high()),
                config.getDouble(path + ".critical", fallback.critical()),
                fallback.lowerIsWorse()
        );
        if (parsed.isOrdered()) {
            return parsed;
        }

        logger.warning("Invalid threshold order at " + path + " in config.yml; using defaults.");
        return fallback;
    }

    private static MarkerColors parseMarkerColors(
            FileConfiguration config,
            String status,
            String defaultLine,
            String defaultFill,
            Logger logger
    ) {
        String path = "visualization.colors." + status;
        Color fallbackLine = parseColor(defaultLine, DEFAULT_LINE_COLOR, logger);
        Color fallbackFill = parseColor(defaultFill, DEFAULT_FILL_COLOR, logger);
        return new MarkerColors(
                parseColor(config.getString(path + ".line-color", defaultLine), fallbackLine, logger),
                parseColor(config.getString(path + ".fill-color", defaultFill), fallbackFill, logger)
        );
    }

    private static void warnInvalidOption(
            String configuredValue,
            String path,
            String fallback,
            Logger logger
    ) {
        if (configuredValue != null && !configuredValue.isBlank()) {
            logger.warning("Invalid " + path + " in config.yml: " + configuredValue + "; using " + fallback + ".");
        }
    }

    private static DateTimeFormatter parseTimestampFormatter(String pattern, Logger logger) {
        String normalized = stringOrDefault(pattern, DEFAULT_TIMESTAMP_FORMAT);
        try {
            return DateTimeFormatter.ofPattern(normalized).withZone(ZoneId.systemDefault());
        } catch (IllegalArgumentException exception) {
            logger.warning("Invalid markers.timestamp-format in config.yml: " + normalized);
            return DateTimeFormatter.ofPattern(DEFAULT_TIMESTAMP_FORMAT).withZone(ZoneId.systemDefault());
        }
    }

    private static String stringOrDefault(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }

    static String detailFormatOrDefault(String value) {
        String configured = stringOrDefault(value, DEFAULT_DETAIL_FORMAT);
        if (configured.strip().equals(LEGACY_DETAIL_FORMAT.strip())) {
            return DEFAULT_DETAIL_FORMAT;
        }
        String responsive = configured
                .replace(
                        "width:310px;font-size:13px;line-height:1.2",
                        "width:100%;max-width:100%;box-sizing:border-box;overflow:hidden;font-size:13px;line-height:1.2"
                )
                .replace("repeat(3,1fr)", "repeat(3,minmax(0,1fr))")
                .replace("repeat(2,1fr)", "repeat(2,minmax(0,1fr))");
        if (responsive.contains(
                "width:100%;max-width:100%;box-sizing:border-box;overflow:hidden;font-size:13px;line-height:1.2"
        ) && responsive.contains("Entit&auml;ten &middot; {entities_per_chunk}/Chunk")
                && responsive.contains("margin-top:8px;text-align:right;opacity:.45;font-size:9px")) {
            return DEFAULT_DETAIL_FORMAT;
        }
        if (responsive.contains(
                "width:100%;max-width:100%;box-sizing:border-box;overflow:hidden;font-size:14px;line-height:1.25"
        ) && responsive.contains(
                "Spitzen: 5 % {mspt_worst_5} ms &middot; 1 % {mspt_worst_1} ms"
        ) && !responsive.contains("{tps_trend}")) {
            return DEFAULT_DETAIL_FORMAT;
        }
        if (responsive.contains(
                "width:100%;max-width:100%;box-sizing:border-box;overflow:hidden;font-size:14px;line-height:1.25"
        ) && responsive.contains("Trend: TPS ")
                && responsive.contains("{tps_trend_color}")
                && !responsive.contains("{diagnosis}")) {
            return DEFAULT_DETAIL_FORMAT;
        }
        if (responsive.contains(
                "width:100%;max-width:100%;box-sizing:border-box;overflow:hidden;font-size:14px;line-height:1.25"
        ) && responsive.contains("{diagnosis}")
                && !responsive.contains("{load_context}")) {
            return DEFAULT_DETAIL_FORMAT;
        }
        return responsive;
    }

    static String trendDetailFormat(String detailFormat, boolean trendsEnabled) {
        if (trendsEnabled) {
            return detailFormat;
        }
        String withoutPerformanceTrend = detailFormat.lines()
                .filter((line) -> !line.contains(">Trend: TPS ")
                        || !line.contains("{tps_trend}")
                        || !line.contains("{mspt_trend}")
                        || !line.contains("{utilization_trend}"))
                .collect(java.util.stream.Collectors.joining("\n"));
        return withoutPerformanceTrend
                .replace(
                        " <span style=\"color:{entities_trend_color};font-size:11px\">"
                                + "{entities_trend}</span>",
                        ""
                )
                .replace(
                        " <span style=\"color:{players_trend_color};font-size:11px\">"
                                + "{players_trend}</span>",
                        ""
                );
    }

    private static Color parseColor(String value, Color fallback, Logger logger) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.startsWith("#")) {
            normalized = normalized.substring(1);
        }

        try {
            return switch (normalized.length()) {
                case 6 -> new Color(
                        Integer.parseInt(normalized.substring(0, 2), 16),
                        Integer.parseInt(normalized.substring(2, 4), 16),
                        Integer.parseInt(normalized.substring(4, 6), 16)
                );
                case 8 -> new Color(
                        Integer.parseInt(normalized.substring(0, 2), 16),
                        Integer.parseInt(normalized.substring(2, 4), 16),
                        Integer.parseInt(normalized.substring(4, 6), 16),
                        Integer.parseInt(normalized.substring(6, 8), 16) / 255.0f
                );
                default -> {
                    logger.warning("Invalid color value in config.yml: " + value);
                    yield fallback;
                }
            };
        } catch (NumberFormatException exception) {
            logger.warning("Invalid color value in config.yml: " + value);
            return fallback;
        }
    }
}
