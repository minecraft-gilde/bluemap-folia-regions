package io.pfaumc.bluemapfoliaregions.region;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Immutable view of a Folia tick region at a specific point in time.
 */
public record RegionSnapshot(
        long regionId,
        String worldName,
        int centerChunkX,
        int centerChunkZ,
        List<Long> sections,
        int sectionBlockSize,
        int chunkCount,
        int entityCount,
        int playerCount,
        Instant capturedAt
) {
    public RegionSnapshot {
        worldName = Objects.requireNonNull(worldName, "worldName");
        sections = List.copyOf(sections);
        capturedAt = Objects.requireNonNull(capturedAt, "capturedAt");
        if (sectionBlockSize <= 0) {
            throw new IllegalArgumentException("sectionBlockSize must be positive");
        }
    }

    public int sectionCount() {
        return this.sections.size();
    }

    public int centerBlockX() {
        return this.centerChunkX * 16 + 8;
    }

    public int centerBlockZ() {
        return this.centerChunkZ * 16 + 8;
    }

    public long areaBlocks() {
        return (long) sectionCount() * this.sectionBlockSize * this.sectionBlockSize;
    }

    public double entitiesPerChunk() {
        return density(this.entityCount);
    }

    public double playersPerChunk() {
        return density(this.playerCount);
    }

    private double density(int count) {
        return this.chunkCount == 0 ? 0.0D : (double) count / this.chunkCount;
    }
}
