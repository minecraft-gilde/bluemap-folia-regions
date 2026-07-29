package io.pfaumc.bluemapfoliaregions.performance;

public enum RegionStatus {
    UNAVAILABLE(-1, "Keine Daten"),
    NORMAL(0, "Normal"),
    WARNING(1, "Erh\u00f6ht"),
    HIGH(2, "Hoch"),
    CRITICAL(3, "Kritisch");

    private final int severity;
    private final String displayName;

    RegionStatus(int severity, String displayName) {
        this.severity = severity;
        this.displayName = displayName;
    }

    public String displayName() {
        return this.displayName;
    }

    public static RegionStatus worst(RegionStatus... statuses) {
        RegionStatus worst = UNAVAILABLE;
        for (RegionStatus status : statuses) {
            if (status.severity > worst.severity) {
                worst = status;
            }
        }
        return worst;
    }
}
