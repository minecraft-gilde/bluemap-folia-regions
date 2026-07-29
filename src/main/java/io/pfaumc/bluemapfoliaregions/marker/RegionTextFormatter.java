package io.pfaumc.bluemapfoliaregions.marker;

import io.pfaumc.bluemapfoliaregions.region.RegionSnapshot;

import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class RegionTextFormatter {
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{([a-z_]+)}");

    private RegionTextFormatter() {}

    static String formatLabel(String format, RegionSnapshot snapshot, DateTimeFormatter timestampFormatter) {
        return format(format, snapshot, timestampFormatter, Function.identity());
    }

    static String formatDetail(String format, RegionSnapshot snapshot, DateTimeFormatter timestampFormatter) {
        return format(format, snapshot, timestampFormatter, RegionTextFormatter::escapeHtml);
    }

    private static String format(
            String format,
            RegionSnapshot snapshot,
            DateTimeFormatter timestampFormatter,
            Function<String, String> valueSanitizer
    ) {
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
                Map.entry("chunks", Integer.toString(snapshot.chunkCount())),
                Map.entry("area_blocks", Long.toString(snapshot.areaBlocks())),
                Map.entry("entities", Integer.toString(snapshot.entityCount())),
                Map.entry("players", Integer.toString(snapshot.playerCount())),
                Map.entry("entities_per_chunk", formatDensity(snapshot.entitiesPerChunk())),
                Map.entry("players_per_chunk", formatDensity(snapshot.playersPerChunk())),
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
        return String.format(Locale.ROOT, "%.2f", density);
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
