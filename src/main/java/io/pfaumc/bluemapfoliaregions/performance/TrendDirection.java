package io.pfaumc.bluemapfoliaregions.performance;

public enum TrendDirection {
    IMPROVING("Besser"),
    STABLE("Stabil"),
    WORSENING("Schlechter"),
    UNAVAILABLE("Keine Vergleichsdaten");

    private final String displayName;

    TrendDirection(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return this.displayName;
    }
}
