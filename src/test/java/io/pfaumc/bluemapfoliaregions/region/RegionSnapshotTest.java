package io.pfaumc.bluemapfoliaregions.region;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RegionSnapshotTest {
    @Test
    void calculatesDerivedRegionInformation() {
        RegionSnapshot snapshot = snapshot(List.of(1L, 2L, 3L), 8, 12, 2);

        assertEquals(3, snapshot.sectionCount());
        assertEquals(168, snapshot.centerBlockX());
        assertEquals(-312, snapshot.centerBlockZ());
        assertEquals(49_152L, snapshot.areaBlocks());
        assertEquals(1.5D, snapshot.entitiesPerChunk());
        assertEquals(0.25D, snapshot.playersPerChunk());
    }

    @Test
    void returnsZeroDensityForRegionWithoutChunks() {
        RegionSnapshot snapshot = snapshot(List.of(1L), 0, 5, 2);

        assertEquals(0.0D, snapshot.entitiesPerChunk());
        assertEquals(0.0D, snapshot.playersPerChunk());
    }

    @Test
    void makesDefensiveCopyOfSections() {
        List<Long> sections = new ArrayList<>(List.of(1L));
        RegionSnapshot snapshot = snapshot(sections, 1, 0, 0);

        sections.add(2L);

        assertEquals(List.of(1L), snapshot.sections());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.sections().add(3L));
    }

    private static RegionSnapshot snapshot(
            List<Long> sections,
            int chunks,
            int entities,
            int players
    ) {
        return new RegionSnapshot(
                42L,
                "world",
                10,
                -20,
                sections,
                128,
                chunks,
                entities,
                players,
                Instant.parse("2026-07-29T10:15:30Z")
        );
    }
}
