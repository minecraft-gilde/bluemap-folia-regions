package io.pfaumc.bluemapfoliaregions.marker;

import com.flowpowered.math.vector.Vector2d;
import de.bluecolored.bluemap.api.markers.ShapeMarker;
import de.bluecolored.bluemap.api.math.Shape;
import io.pfaumc.bluemapfoliaregions.config.PluginConfiguration;
import io.papermc.paper.threadedregions.ThreadedRegionizer;
import io.papermc.paper.threadedregions.ThreadedRegionizer.ThreadedRegion;
import io.papermc.paper.threadedregions.TickRegions;
import io.papermc.paper.threadedregions.TickRegions.TickRegionData;
import io.papermc.paper.threadedregions.TickRegions.TickRegionSectionData;
import net.minecraft.world.level.ChunkPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RegionMarkerFactory {
    private final int sectionBlockSize;

    public RegionMarkerFactory(int sectionChunkSize) {
        this.sectionBlockSize = sectionChunkSize * 16;
    }

    public Map<String, ShapeMarker> createMarkers(
            ThreadedRegionizer<TickRegionData, TickRegionSectionData> regioniser,
            PluginConfiguration configuration
    ) {
        List<RegionSnapshot> snapshots = Collections.synchronizedList(new ArrayList<>());
        regioniser.computeForAllRegions((region) -> snapshots.add(toSnapshot(region)));

        Map<String, ShapeMarker> markers = new HashMap<>(snapshots.size());
        for (RegionSnapshot snapshot : snapshots) {
            if (snapshot.centerChunk() == null || snapshot.sections().isEmpty()) {
                continue;
            }

            List<Vector2d> points = getSectionPoints(snapshot.sections());
            if (points.size() < 3) {
                continue;
            }

            ChunkPos centerChunk = snapshot.centerChunk();
            String markerId = "region-" + centerChunk.x + "-" + centerChunk.z;
            String label = formatLabel(configuration.markerLabelFormat(), snapshot);
            Shape shape = new Shape(points);

            String detail =
                    "Sektionen: " + snapshot.sections().size() + "<br>" +
                    "Chunks: " + snapshot.chunkCount() + "<br>" +
                    "Entitäten: " + snapshot.entityCount() + "<br>" +
                    "Spieler: " + snapshot.playerCount();

            ShapeMarker marker = ShapeMarker.builder()
                    .shape(shape, configuration.markerHeight())
                    .label(label)
                    .lineColor(configuration.markerLineColor())
                    .fillColor(configuration.markerFillColor())
                    .depthTestEnabled(false)
                    .build();
            marker.setLineWidth(configuration.markerLineWidth());
            marker.setDetail(detail);

            markers.put(markerId, marker);
        }
        return markers;
    }

    private static RegionSnapshot toSnapshot(ThreadedRegion<TickRegionData, TickRegionSectionData> region) {
        ChunkPos centerChunk = region.getCenterChunk();
        List<Long> sections = List.copyOf(region.getOwnedSections());
        TickRegions.RegionStats stats = region.getData().getRegionStats();
        String worldName = region.getData().world.getTypeKey().identifier().getPath();
        return new RegionSnapshot(
                worldName,
                centerChunk,
                sections,
                stats.getChunkCount(),
                stats.getEntityCount(),
                stats.getPlayerCount()
        );
    }

    private static String formatLabel(String format, RegionSnapshot snapshot) {
        ChunkPos centerChunk = snapshot.centerChunk();
        return format
                .replace("{world}", snapshot.worldName())
                .replace("{center_x}", Integer.toString(centerChunk.x))
                .replace("{center_z}", Integer.toString(centerChunk.z))
                .replace("{sections}", Integer.toString(snapshot.sections().size()))
                .replace("{chunks}", Integer.toString(snapshot.chunkCount()))
                .replace("{entities}", Integer.toString(snapshot.entityCount()))
                .replace("{players}", Integer.toString(snapshot.playerCount()));
    }

    private List<Vector2d> getSectionPoints(List<Long> sections) {
        Set<SectionCoord> sectionCoords = new HashSet<>(sections.size());
        for (long sectionKey : sections) {
            sectionCoords.add(new SectionCoord(getChunkX(sectionKey), getChunkZ(sectionKey)));
        }

        List<GridPoint> outline = extractLargestOutline(sectionCoords);
        List<Vector2d> points = new ArrayList<>(outline.size());
        for (GridPoint point : outline) {
            points.add(Vector2d.from(
                    (double) point.x() * this.sectionBlockSize,
                    (double) point.z() * this.sectionBlockSize
            ));
        }
        return points;
    }

    private static List<GridPoint> extractLargestOutline(Set<SectionCoord> sections) {
        if (sections.isEmpty()) {
            return List.of();
        }

        Map<GridPoint, List<GridPoint>> outgoing = new HashMap<>();
        for (SectionCoord section : sections) {
            int x = section.x();
            int z = section.z();

            if (!sections.contains(new SectionCoord(x, z - 1))) {
                addEdge(outgoing, new GridPoint(x + 1, z), new GridPoint(x, z));
            }
            if (!sections.contains(new SectionCoord(x + 1, z))) {
                addEdge(outgoing, new GridPoint(x + 1, z + 1), new GridPoint(x + 1, z));
            }
            if (!sections.contains(new SectionCoord(x, z + 1))) {
                addEdge(outgoing, new GridPoint(x, z + 1), new GridPoint(x + 1, z + 1));
            }
            if (!sections.contains(new SectionCoord(x - 1, z))) {
                addEdge(outgoing, new GridPoint(x, z), new GridPoint(x, z + 1));
            }
        }

        Set<Edge> visited = new HashSet<>();
        List<List<GridPoint>> loops = new ArrayList<>();
        for (Map.Entry<GridPoint, List<GridPoint>> entry : outgoing.entrySet()) {
            for (GridPoint to : entry.getValue()) {
                Edge start = new Edge(entry.getKey(), to);
                if (visited.contains(start)) {
                    continue;
                }

                List<GridPoint> loop = traceLoop(outgoing, visited, start);
                if (loop.size() >= 3) {
                    loops.add(simplifyLoop(loop));
                }
            }
        }

        return loops.stream()
                .max(Comparator.comparingDouble(RegionMarkerFactory::polygonAreaAbs))
                .orElse(List.of());
    }

    private static List<GridPoint> traceLoop(Map<GridPoint, List<GridPoint>> outgoing, Set<Edge> visited, Edge start) {
        List<GridPoint> loop = new ArrayList<>();
        GridPoint from = start.from();
        GridPoint to = start.to();

        while (true) {
            Edge current = new Edge(from, to);
            if (!visited.add(current)) {
                break;
            }
            loop.add(from);

            List<GridPoint> candidates = outgoing.getOrDefault(to, List.of());
            GridPoint next = selectNext(to, from, candidates, visited);
            if (next == null) {
                break;
            }

            from = to;
            to = next;
            if (from.equals(start.from()) && to.equals(start.to())) {
                break;
            }
        }
        return loop;
    }

    private static GridPoint selectNext(
            GridPoint current,
            GridPoint previous,
            List<GridPoint> candidates,
            Set<Edge> visited
    ) {
        GridPoint best = null;
        int bestPriority = Integer.MAX_VALUE;
        int inDirection = directionIndex(current.x() - previous.x(), current.z() - previous.z());

        for (GridPoint candidate : candidates) {
            Edge edge = new Edge(current, candidate);
            if (visited.contains(edge)) {
                continue;
            }

            int outDirection = directionIndex(candidate.x() - current.x(), candidate.z() - current.z());
            int turn = (outDirection - inDirection + 4) % 4;
            int priority = switch (turn) {
                case 3 -> 0; // prefer left turn
                case 0 -> 1; // then straight
                case 1 -> 2; // then right
                default -> 3; // avoid going back
            };

            if (priority < bestPriority) {
                bestPriority = priority;
                best = candidate;
            }
        }
        return best;
    }

    private static int directionIndex(int dx, int dz) {
        if (dx == 1 && dz == 0) {
            return 0;
        }
        if (dx == 0 && dz == 1) {
            return 1;
        }
        if (dx == -1 && dz == 0) {
            return 2;
        }
        if (dx == 0 && dz == -1) {
            return 3;
        }
        throw new IllegalArgumentException("Edge is not axis-aligned: (" + dx + ", " + dz + ")");
    }

    private static List<GridPoint> simplifyLoop(List<GridPoint> loop) {
        if (loop.size() < 3) {
            return loop;
        }

        List<GridPoint> simplified = new ArrayList<>(loop.size());
        for (int i = 0; i < loop.size(); i++) {
            GridPoint previous = loop.get((i - 1 + loop.size()) % loop.size());
            GridPoint current = loop.get(i);
            GridPoint next = loop.get((i + 1) % loop.size());

            int dx1 = current.x() - previous.x();
            int dz1 = current.z() - previous.z();
            int dx2 = next.x() - current.x();
            int dz2 = next.z() - current.z();

            if (dx1 == dx2 && dz1 == dz2) {
                continue;
            }
            simplified.add(current);
        }

        return simplified.size() >= 3 ? simplified : loop;
    }

    private static void addEdge(Map<GridPoint, List<GridPoint>> outgoing, GridPoint from, GridPoint to) {
        outgoing.computeIfAbsent(from, ignored -> new ArrayList<>()).add(to);
    }

    private static double polygonAreaAbs(List<GridPoint> points) {
        if (points.size() < 3) {
            return 0D;
        }

        long area2 = 0L;
        for (int i = 0; i < points.size(); i++) {
            GridPoint current = points.get(i);
            GridPoint next = points.get((i + 1) % points.size());
            area2 += (long) current.x() * next.z() - (long) next.x() * current.z();
        }
        return Math.abs(area2) / 2.0D;
    }

    private static int getChunkX(long chunkKey) {
        return (int) chunkKey;
    }

    private static int getChunkZ(long chunkKey) {
        return (int) (chunkKey >> 32);
    }

    private record RegionSnapshot(
            String worldName,
            ChunkPos centerChunk,
            List<Long> sections,
            int chunkCount,
            int entityCount,
            int playerCount
    ) {}

    private record SectionCoord(int x, int z) {}

    private record GridPoint(int x, int z) {}

    private record Edge(GridPoint from, GridPoint to) {}
}
