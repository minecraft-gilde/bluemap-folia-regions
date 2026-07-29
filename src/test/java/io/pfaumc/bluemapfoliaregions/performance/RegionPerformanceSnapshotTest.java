package io.pfaumc.bluemapfoliaregions.performance;

import ca.spottedleaf.moonrise.common.time.TickData.SegmentData;
import ca.spottedleaf.moonrise.common.time.TickData.SegmentedAverage;
import ca.spottedleaf.moonrise.common.time.TickData.TickReportData;
import org.junit.jupiter.api.Test;

import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegionPerformanceSnapshotTest {
    @Test
    void convertsFoliaReportAndCalculatesWorstSegmentsFromRawData() {
        long[] tickTimes = LongStream.rangeClosed(1L, 100L)
                .map((milliseconds) -> milliseconds * 1_000_000L)
                .toArray();
        TickReportData report = report(tickTimes, 19.75D, 50_500_000.0D, 0.425D);

        RegionPerformanceSnapshot snapshot =
                RegionPerformanceSnapshot.from(report, ReportWindow.FIFTEEN_SECONDS);

        assertTrue(snapshot.available());
        assertEquals(100, snapshot.collectedTicks());
        assertEquals(19.75D, snapshot.tps());
        assertEquals(50.5D, snapshot.averageMspt());
        assertEquals(98.0D, snapshot.worstFivePercentMspt());
        assertEquals(100.0D, snapshot.worstOnePercentMspt());
        assertEquals(0.425D, snapshot.utilization());
    }

    @Test
    void representsMissingTickReportAsUnavailable() {
        RegionPerformanceSnapshot snapshot =
                RegionPerformanceSnapshot.from(null, ReportWindow.FIVE_SECONDS);

        assertFalse(snapshot.available());
        assertEquals(ReportWindow.FIVE_SECONDS, snapshot.reportWindow());
        assertEquals(0, snapshot.collectedTicks());
    }

    private static TickReportData report(
            long[] tickTimes,
            double tps,
            double averageTickNanos,
            double utilization
    ) {
        SegmentData tpsSegment = new SegmentData(tickTimes.length, tps, tps, tps, tps);
        SegmentData tickSegment = new SegmentData(
                tickTimes.length,
                averageTickNanos,
                averageTickNanos,
                tickTimes[0],
                tickTimes[tickTimes.length - 1]
        );
        SegmentedAverage tpsData = segmentedAverage(tpsSegment, new long[tickTimes.length]);
        SegmentedAverage tickData = segmentedAverage(tickSegment, tickTimes);
        return new TickReportData(
                tickTimes.length,
                0L,
                1L,
                0L,
                utilization,
                tpsData,
                tickData,
                tickData
        );
    }

    private static SegmentedAverage segmentedAverage(SegmentData segment, long[] rawData) {
        return new SegmentedAverage(segment, segment, segment, segment, segment, rawData);
    }
}
