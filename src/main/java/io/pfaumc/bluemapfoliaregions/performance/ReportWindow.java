package io.pfaumc.bluemapfoliaregions.performance;

import java.util.Arrays;
import java.util.Optional;

public enum ReportWindow {
    FIVE_SECONDS("5s"),
    FIFTEEN_SECONDS("15s"),
    ONE_MINUTE("1m"),
    FIVE_MINUTES("5m"),
    FIFTEEN_MINUTES("15m");

    private final String configValue;

    ReportWindow(String configValue) {
        this.configValue = configValue;
    }

    public String configValue() {
        return this.configValue;
    }

    public static Optional<ReportWindow> fromConfig(String value) {
        if (value == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter((window) -> window.configValue.equalsIgnoreCase(value.trim()))
                .findFirst();
    }
}
