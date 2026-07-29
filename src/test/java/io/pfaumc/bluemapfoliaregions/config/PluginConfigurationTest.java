package io.pfaumc.bluemapfoliaregions.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.logging.Logger;

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

    @Test
    void hidesDefaultTrendRowWhenTrendsAreDisabled() {
        String detail = """
                <div>Leistung</div>
                <div>Trend: TPS {tps_trend} · Tickzeit {mspt_trend} · Auslastung {utilization_trend}</div>
                <div>{entities_formatted} <span style="color:{entities_trend_color};font-size:11px">{entities_trend}</span></div>
                <div>{players_formatted} <span style="color:{players_trend_color};font-size:11px">{players_trend}</span></div>
                <div>Stand</div>""";

        String result = PluginConfiguration.trendDetailFormat(detail, false);

        assertEquals(
                "<div>Leistung</div>\n<div>{entities_formatted}</div>\n"
                        + "<div>{players_formatted}</div>\n<div>Stand</div>",
                result
        );
    }

    @Test
    void parsesTrendSensitivityAndPercentagePoints() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("trends.enabled", true);
        config.set("trends.reset-after-seconds", 45);
        config.set("trends.sensitivity.tps", 0.2D);
        config.set("trends.sensitivity.mspt", 2.5D);
        config.set("trends.sensitivity.utilization-percentage-points", 3.0D);
        config.set("trends.sensitivity.entities", 7);
        config.set("trends.sensitivity.players", 2);

        TrendConfiguration trends = PluginConfiguration.parseTrends(config);

        assertTrue(trends.enabled());
        assertEquals(Duration.ofSeconds(45), trends.resetAfter());
        assertEquals(0.2D, trends.minimumTpsChange());
        assertEquals(2.5D, trends.minimumMsptChange());
        assertEquals(0.03D, trends.minimumUtilizationChange());
        assertEquals(7, trends.minimumEntityChange());
        assertEquals(2, trends.minimumPlayerChange());
    }

    @Test
    void upgradesTrendLayoutToDiagnosticLayout() {
        String previous = """
                <div style="width:100%;max-width:100%;box-sizing:border-box;overflow:hidden;font-size:14px;line-height:1.25">
                  <div>Spitzen: 5 % {mspt_worst_5} ms &middot; 1 % {mspt_worst_1} ms</div>
                  <div>Trend: TPS <span style="color:{tps_trend_color}">{tps_trend}</span></div>
                </div>""";

        String migrated = PluginConfiguration.detailFormatOrDefault(previous);

        assertTrue(migrated.contains("color:{tps_status_color}"));
        assertTrue(migrated.contains("{diagnosis}"));
    }

    @Test
    void upgradesDiagnosticLayoutToLoadContextLayout() {
        String previous = """
                <div style="width:100%;max-width:100%;box-sizing:border-box;overflow:hidden;font-size:14px;line-height:1.25">
                  <div>{diagnosis}</div>
                </div>""";

        String migrated = PluginConfiguration.detailFormatOrDefault(previous);

        assertTrue(migrated.contains("{entities_trend}"));
        assertTrue(migrated.contains("{load_context}"));
    }

    @Test
    void parsesLoadContextThresholds() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("load-context.enabled", true);
        config.set("load-context.thresholds.entities-per-chunk.warning", 4.0D);
        config.set("load-context.thresholds.entities-per-chunk.high", 8.0D);
        config.set("load-context.thresholds.entities-per-chunk.critical", 12.0D);
        config.set("load-context.thresholds.region-chunks.warning", 1000.0D);
        config.set("load-context.thresholds.region-chunks.high", 2000.0D);
        config.set("load-context.thresholds.region-chunks.critical", 3000.0D);

        LoadContextConfiguration context =
                PluginConfiguration.parseLoadContext(config, Logger.getAnonymousLogger());

        assertTrue(context.enabled());
        assertEquals(8.0D, context.entityDensityThresholds().high());
        assertEquals(3_000.0D, context.regionChunkThresholds().critical());
    }
}
