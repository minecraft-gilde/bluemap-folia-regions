package io.pfaumc.bluemapfoliaregions.performance;

import ca.spottedleaf.moonrise.common.time.TickData.TickReportData;

import java.util.Arrays;

public record RegionPerformanceSnapshot(
        boolean available,
        ReportWindow reportWindow,
        int collectedTicks,
        double tps,
        double averageMspt,
        double worstFivePercentMspt,
        double worstOnePercentMspt,
        double utilization
) {
    private static final double NANOSECONDS_TO_MILLISECONDS = 1.0E-6D;

    public static RegionPerformanceSnapshot unavailable(ReportWindow reportWindow) {
        return new RegionPerformanceSnapshot(false, reportWindow, 0, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D);
    }

    public static RegionPerformanceSnapshot from(TickReportData report, ReportWindow reportWindow) {
        if (report == null || report.collectedTicks() == 0) {
            return unavailable(reportWindow);
        }

        // Calculate these from the raw values because Folia 26.1.2 exposes the
        // one- and five-percent segments through swapped record components.
        long[] tickTimes = report.timePerTickData().rawData().clone();
        Arrays.sort(tickTimes);
        return new RegionPerformanceSnapshot(
                true,
                reportWindow,
                report.collectedTicks(),
                report.tpsData().segmentAll().average(),
                report.timePerTickData().segmentAll().average() * NANOSECONDS_TO_MILLISECONDS,
                worstAverage(tickTimes, 0.05D) * NANOSECONDS_TO_MILLISECONDS,
                worstAverage(tickTimes, 0.01D) * NANOSECONDS_TO_MILLISECONDS,
                report.utilisation()
        );
    }

    private static double worstAverage(long[] sortedValues, double fraction) {
        if (sortedValues.length == 0) {
            return 0.0D;
        }

        int start = (int) Math.floor((1.0D - fraction) * sortedValues.length);
        start = Math.min(start, sortedValues.length - 1);
        double sum = 0.0D;
        for (int i = start; i < sortedValues.length; i++) {
            sum += sortedValues[i];
        }
        return sum / (sortedValues.length - start);
    }
}
