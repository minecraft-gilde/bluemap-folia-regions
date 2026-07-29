package io.pfaumc.bluemapfoliaregions.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PluginConfigurationTest {
    @Test
    void upgradesPreviousDefaultDetailLayoutInMemory() {
        String legacy = """
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

        String migrated = PluginConfiguration.detailFormatOrDefault(legacy);

        assertNotEquals(legacy, migrated);
        assertTrue(migrated.contains("display:grid"));
        assertTrue(migrated.contains("{status_color}"));
    }

    @Test
    void preservesCustomDetailLayout() {
        String custom = "<strong>{world}</strong>";

        assertEquals(custom, PluginConfiguration.detailFormatOrDefault(custom));
    }

    @Test
    void makesPreviousCompactLayoutResponsive() {
        String previous = """
                <div style="width:310px;font-size:13px;line-height:1.2">
                  <div style="display:grid;grid-template-columns:repeat(3,1fr);gap:8px">Region</div>
                  <div style="display:grid;grid-template-columns:repeat(2,1fr);gap:8px">Activity</div>
                </div>""";

        String migrated = PluginConfiguration.detailFormatOrDefault(previous);

        assertTrue(migrated.contains("width:100%;max-width:100%;box-sizing:border-box;overflow:hidden"));
        assertTrue(migrated.contains("repeat(3,minmax(0,1fr))"));
        assertTrue(migrated.contains("repeat(2,minmax(0,1fr))"));
    }

    @Test
    void upgradesPreviousResponsiveDefaultToAirierLayout() {
        String previous = """
                <div style="width:100%;max-width:100%;box-sizing:border-box;overflow:hidden;font-size:13px;line-height:1.2">
                  <div>Entit&auml;ten &middot; {entities_per_chunk}/Chunk</div>
                  <div style="margin-top:8px;text-align:right;opacity:.45;font-size:9px">Stand</div>
                </div>""";

        String migrated = PluginConfiguration.detailFormatOrDefault(previous);

        assertTrue(migrated.contains("font-size:14px;line-height:1.25"));
        assertTrue(migrated.contains("{entities_per_chunk} / Chunk"));
        assertTrue(migrated.contains("margin-top:10px;text-align:right"));
    }
}
