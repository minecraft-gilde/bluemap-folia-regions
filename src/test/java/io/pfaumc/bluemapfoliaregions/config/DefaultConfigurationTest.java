package io.pfaumc.bluemapfoliaregions.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultConfigurationTest {
    @Test
    void packagesAValidVersionTwoConfiguration() {
        YamlConfiguration config = loadDefaultConfiguration();
        Logger logger = Logger.getAnonymousLogger();

        TrendConfiguration trends = PluginConfiguration.parseTrends(config, logger);
        LoadContextConfiguration loadContext = PluginConfiguration.parseLoadContext(config, logger);

        assertEquals(2, config.getInt("config-version"));
        assertEquals(100L, PluginConfiguration.parseUpdateIntervalTicks(config, logger));
        assertTrue(trends.enabled());
        assertTrue(loadContext.enabled());
        assertTrue(loadContext.entityDensityThresholds().isOrdered());
        assertTrue(loadContext.regionChunkThresholds().isOrdered());
        assertTrue(config.getString("markers.detail-format", "").contains("{load_context}"));
    }

    private static YamlConfiguration loadDefaultConfiguration() {
        InputStream input = DefaultConfigurationTest.class
                .getClassLoader()
                .getResourceAsStream("config.yml");
        if (input == null) {
            throw new IllegalStateException("Packaged config.yml is missing");
        }
        return YamlConfiguration.loadConfiguration(
                new InputStreamReader(input, StandardCharsets.UTF_8)
        );
    }
}
