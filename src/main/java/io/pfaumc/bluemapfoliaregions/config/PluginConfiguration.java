package io.pfaumc.bluemapfoliaregions.config;

import de.bluecolored.bluemap.api.math.Color;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public record PluginConfiguration(
        String markerSetId,
        String markerSetLabel,
        boolean defaultHidden,
        boolean toggleable,
        String markerLabelFormat,
        int markerHeight,
        Color markerLineColor,
        Color markerFillColor,
        int markerLineWidth,
        long updateIntervalTicks
) {
    private static final String DEFAULT_MARKER_SET_ID = "folia-regions";
    private static final String DEFAULT_MARKER_SET_LABEL = "Folia Tick-Regionen";
    private static final String DEFAULT_LABEL_FORMAT = "Region[{center_x},{center_z}]";
    private static final Color DEFAULT_LINE_COLOR = new Color(155, 70, 255, 1.0f);
    private static final Color DEFAULT_FILL_COLOR = new Color(210, 170, 255, 0.35f);

    public static PluginConfiguration from(JavaPlugin plugin) {
        FileConfiguration config = plugin.getConfig();
        Logger logger = plugin.getLogger();

        return new PluginConfiguration(
                stringOrDefault(config.getString("marker-set.id"), DEFAULT_MARKER_SET_ID),
                stringOrDefault(config.getString("marker-set.label"), DEFAULT_MARKER_SET_LABEL),
                config.getBoolean("marker-set.default-hidden", true),
                config.getBoolean("marker-set.toggleable", true),
                stringOrDefault(config.getString("markers.label-format"), DEFAULT_LABEL_FORMAT),
                Math.max(1, config.getInt("markers.height", 80)),
                parseColor(config.getString("markers.line-color", "#9b46ffff"), DEFAULT_LINE_COLOR, logger),
                parseColor(config.getString("markers.fill-color", "#d2aaff59"), DEFAULT_FILL_COLOR, logger),
                Math.max(1, config.getInt("markers.line-width", 2)),
                Math.max(20L, config.getLong("update-interval-seconds", 5L) * 20L)
        );
    }

    private static String stringOrDefault(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
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
