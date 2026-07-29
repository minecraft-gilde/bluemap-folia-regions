package io.pfaumc.bluemapfoliaregions.marker;

import io.pfaumc.bluemapfoliaregions.region.RegionSnapshot;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RegionTextFormatterTest {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss z")
            .withZone(ZoneOffset.UTC);

    @Test
    void replacesAllSupportedPlaceholders() {
        String format = "{region_id}|{world}|{center_x}|{center_z}|{center_chunk_x}|{center_chunk_z}|"
                + "{center_block_x}|{center_block_z}|{sections}|{chunks}|{area_blocks}|{entities}|{players}|"
                + "{entities_per_chunk}|{players_per_chunk}|{updated_at}";

        String result = RegionTextFormatter.formatLabel(format, snapshot("world"), TIME_FORMATTER);

        assertEquals(
                "42|world|10|-20|10|-20|168|-312|3|8|49152|12|2|1.50|0.25|2026-07-29 10:15:30 Z",
                result
        );
    }

    @Test
    void keepsUnknownPlaceholdersUnchanged() {
        String result = RegionTextFormatter.formatLabel(
                "{region_id} {future_metric}",
                snapshot("world"),
                TIME_FORMATTER
        );

        assertEquals("42 {future_metric}", result);
    }

    @Test
    void doesNotInterpretPlaceholdersInsideReplacementValues() {
        String result = RegionTextFormatter.formatLabel("{world}", snapshot("{chunks}"), TIME_FORMATTER);

        assertEquals("{chunks}", result);
    }

    @Test
    void escapesDynamicValuesInHtmlDetails() {
        String result = RegionTextFormatter.formatDetail(
                "<b>{world}</b>",
                snapshot("<world & 'friends'>"),
                TIME_FORMATTER
        );

        assertEquals("<b>&lt;world &amp; &#39;friends&#39;&gt;</b>", result);
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
                Instant.parse("2026-07-29T10:15:30Z")
        );
    }
}
