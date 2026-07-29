package io.pfaumc.bluemapfoliaregions.performance;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PerformanceThresholdsTest {
    @Test
    void classifiesHigherValuesAsWorse() {
        PerformanceThresholds thresholds = new PerformanceThresholds(0.60D, 0.75D, 0.90D, false);

        assertEquals(RegionStatus.NORMAL, thresholds.classify(0.59D));
        assertEquals(RegionStatus.WARNING, thresholds.classify(0.60D));
        assertEquals(RegionStatus.HIGH, thresholds.classify(0.75D));
        assertEquals(RegionStatus.CRITICAL, thresholds.classify(0.90D));
        assertTrue(thresholds.isOrdered());
    }

    @Test
    void classifiesLowerValuesAsWorse() {
        PerformanceThresholds thresholds = new PerformanceThresholds(19.5D, 18.0D, 15.0D, true);

        assertEquals(RegionStatus.NORMAL, thresholds.classify(19.51D));
        assertEquals(RegionStatus.WARNING, thresholds.classify(19.5D));
        assertEquals(RegionStatus.HIGH, thresholds.classify(18.0D));
        assertEquals(RegionStatus.CRITICAL, thresholds.classify(15.0D));
        assertTrue(thresholds.isOrdered());
    }

    @Test
    void rejectsIncorrectThresholdOrder() {
        assertFalse(new PerformanceThresholds(0.75D, 0.60D, 0.90D, false).isOrdered());
        assertFalse(new PerformanceThresholds(18.0D, 19.5D, 15.0D, true).isOrdered());
    }

    @Test
    void returnsThresholdForEachProblemStatus() {
        PerformanceThresholds thresholds = new PerformanceThresholds(25.0D, 40.0D, 50.0D, false);

        assertEquals(25.0D, thresholds.thresholdFor(RegionStatus.WARNING));
        assertEquals(40.0D, thresholds.thresholdFor(RegionStatus.HIGH));
        assertEquals(50.0D, thresholds.thresholdFor(RegionStatus.CRITICAL));
        assertTrue(Double.isNaN(thresholds.thresholdFor(RegionStatus.NORMAL)));
    }
}
