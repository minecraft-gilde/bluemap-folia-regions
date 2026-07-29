package io.pfaumc.bluemapfoliaregions.performance;

import java.util.Arrays;
import java.util.Optional;

public enum VisualizationMode {
    STATIC("static"),
    UTILIZATION("utilization"),
    MSPT("mspt"),
    TPS("tps");

    private final String configValue;

    VisualizationMode(String configValue) {
        this.configValue = configValue;
    }

    public String configValue() {
        return this.configValue;
    }

    public static Optional<VisualizationMode> fromConfig(String value) {
        if (value == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter((mode) -> mode.configValue.equalsIgnoreCase(value.trim()))
                .findFirst();
    }
}
