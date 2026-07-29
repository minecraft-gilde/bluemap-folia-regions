package io.pfaumc.bluemapfoliaregions.performance;

public record PerformanceThresholds(
        double warning,
        double high,
        double critical,
        boolean lowerIsWorse
) {
    public boolean isOrdered() {
        if (!Double.isFinite(this.warning) || !Double.isFinite(this.high) || !Double.isFinite(this.critical)) {
            return false;
        }
        return this.lowerIsWorse
                ? this.warning >= this.high && this.high >= this.critical
                : this.warning <= this.high && this.high <= this.critical;
    }

    public RegionStatus classify(double value) {
        if (!Double.isFinite(value)) {
            return RegionStatus.UNAVAILABLE;
        }

        if (this.lowerIsWorse) {
            if (value <= this.critical) {
                return RegionStatus.CRITICAL;
            }
            if (value <= this.high) {
                return RegionStatus.HIGH;
            }
            if (value <= this.warning) {
                return RegionStatus.WARNING;
            }
        } else {
            if (value >= this.critical) {
                return RegionStatus.CRITICAL;
            }
            if (value >= this.high) {
                return RegionStatus.HIGH;
            }
            if (value >= this.warning) {
                return RegionStatus.WARNING;
            }
        }
        return RegionStatus.NORMAL;
    }
}
