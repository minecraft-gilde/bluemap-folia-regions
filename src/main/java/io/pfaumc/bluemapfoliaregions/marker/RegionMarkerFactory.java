package io.pfaumc.bluemapfoliaregions.marker;

import com.flowpowered.math.vector.Vector2d;
import de.bluecolored.bluemap.api.markers.ShapeMarker;
import de.bluecolored.bluemap.api.math.Shape;
import io.pfaumc.bluemapfoliaregions.config.PluginConfiguration;
import io.pfaumc.bluemapfoliaregions.region.RegionSnapshot;
import io.papermc.paper.threadedregions.ThreadedRegionizer;
import io.papermc.paper.threadedregions.ThreadedRegionizer.ThreadedRegion;
import io.papermc.paper.threadedregions.TickRegions;
import io.papermc.paper.threadedregions.TickRegions.TickRegionData;
import io.papermc.paper.threadedregions.TickRegions.TickRegionSectionData;
import net.minecraft.world.level.ChunkPos;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
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

    public MarkerBuildResult createMarkers(
            ThreadedRegionizer<TickRegionData, TickRegionSectionData> regioniser,
            PluginConfiguration configuration,
            String worldName,
            Instant capturedAt
    ) {
        List<RegionSnapshot> snapshots = Collections.synchronizedList(new ArrayList<>());
        regioniser.computeForAllRegions((region) -> {
            RegionSnapshot snapshot = toSnapshot(region, worldName, capturedAt);
            if (snapshot != null) {
                snapshots.add(snapshot);
            }
        });

        Map<String, ShapeMarker> markers = new HashMap<>(snapshots.size());
        for (RegionSnapshot snapshot : snapshots) {
            if (snapshot.sections().isEmpty()) {
                continue;
            }

            List<RegionPolygon> polygons = createRegionPolygons(snapshot.sections(), this.sectionBlockSize);
            if (polygons.isEmpty()) {
                continue;
            }

            String baseMarkerId = "region-" + snapshot.regionId();
            String label = RegionTextFormatter.formatLabel(
                    configuration.markerLabelFormat(),
                    snapshot,
                    configuration.markerTimestampFormatter()
            );
            String detail = RegionTextFormatter.formatDetail(
                    configuration.markerDetailFormat(),
                    snapshot,
                    configuration.markerTimestampFormatter()
            );

            for (int i = 0; i < polygons.size(); i++) {
                RegionPolygon polygon = polygons.get(i);
                String markerId = polygons.size() == 1 ? baseMarkerId : baseMarkerId + "-" + (i + 1);

                ShapeMarker marker = ShapeMarker.builder()
                        .shape(new Shape(polygon.outline()), configuration.markerHeight())
                        .holes(polygon.holes().stream().map(Shape::new).toArray(Shape[]::new))
                        .label(label)
                        .lineColor(configuration.markerLineColor())
                        .fillColor(configuration.markerFillColor())
                        .lineWidth(configuration.markerLineWidth())
                        .depthTestEnabled(false)
                        .build();
                marker.setDetail(detail);

                markers.put(markerId, marker);
            }
        }
        return new MarkerBuildResult(Map.copyOf(markers), List.copyOf(snapshots));
    }

    private RegionSnapshot toSnapshot(
            ThreadedRegion<TickRegionData, TickRegionSectionData> region,
            String worldName,
            Instant capturedAt
    ) {
        ChunkPos centerChunk = region.getCenterChunk();
        if (centerChunk == null) {
            return null;
        }
        List<Long> sections = List.copyOf(region.getOwnedSections());
        TickRegions.RegionStats stats = region.getData().getRegionStats();
        return new RegionSnapshot(
                region.getData().id,
                worldName,
                centerChunk.x(),
                centerChunk.z(),
                sections,
                this.sectionBlockSize,
                stats.getChunkCount(),
                stats.getEntityCount(),
                stats.getPlayerCount(),
                capturedAt
        );
    }

    static List<RegionPolygon> createRegionPolygons(Collection<Long> sections, int sectionBlockSize) {
        Set<SectionCoord> sectionCoords = new HashSet<>(sections.size());
        for (long sectionKey : sections) {
            sectionCoords.add(new SectionCoord(getChunkX(sectionKey), getChunkZ(sectionKey)));
        }

        List<GridLoop> loops = extractOutlines(sectionCoords);
        if (loops.isEmpty()) {
            return List.of();
        }

        loops.sort(
                Comparator.comparingDouble(GridLoop::areaAbs).reversed()
                        .thenComparingInt(GridLoop::minX)
                        .thenComparingInt(GridLoop::minZ)
        );

        List<GridLoop> outlines = loops.stream()
                .filter((loop) -> loop.signedArea2() < 0L)
                .toList();
        if (outlines.isEmpty()) {
            outlines = loops;
        }

        Map<GridLoop, List<GridLoop>> holesByOutline = new HashMap<>();
        for (GridLoop outline : outlines) {
            holesByOutline.put(outline, new ArrayList<>());
        }

        for (GridLoop loop : loops) {
            if (loop.signedArea2() <= 0L) {
                continue;
            }

            GridLoop outline = findSmallestContainingOutline(loop, outlines);
            if (outline != null) {
                holesByOutline.get(outline).add(loop);
            }
        }

        List<RegionPolygon> polygons = new ArrayList<>(outlines.size());
        for (GridLoop outline : outlines) {
            List<List<Vector2d>> holes = holesByOutline.get(outline).stream()
                    .sorted(Comparator.comparingDouble(GridLoop::areaAbs).reversed())
                    .map((hole) -> toVectors(hole.points(), sectionBlockSize))
                    .toList();

            polygons.add(new RegionPolygon(toVectors(outline.points(), sectionBlockSize), holes));
        }
        return polygons;
    }

    private static List<Vector2d> toVectors(List<GridPoint> outline, int sectionBlockSize) {
        List<Vector2d> points = new ArrayList<>(outline.size());
        for (GridPoint point : outline) {
            points.add(Vector2d.from(
                    (double) point.x() * sectionBlockSize,
                    (double) point.z() * sectionBlockSize
            ));
        }
        return points;
    }

    private static List<GridLoop> extractOutlines(Set<SectionCoord> sections) {
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
        List<GridLoop> loops = new ArrayList<>();
        for (Map.Entry<GridPoint, List<GridPoint>> entry : outgoing.entrySet()) {
            for (GridPoint to : entry.getValue()) {
                Edge start = new Edge(entry.getKey(), to);
                if (visited.contains(start)) {
                    continue;
                }

                List<GridPoint> loop = traceLoop(outgoing, visited, start);
                if (loop.size() >= 3) {
                    List<GridPoint> simplified = simplifyLoop(loop);
                    long signedArea2 = polygonSignedArea2(simplified);
                    if (signedArea2 != 0L) {
                        loops.add(new GridLoop(simplified, signedArea2));
                    }
                }
            }
        }

        return loops;
    }

    private static GridLoop findSmallestContainingOutline(GridLoop loop, List<GridLoop> outlines) {
        Vector2d samplePoint = polygonCentroid(loop.points());
        GridLoop best = null;
        for (GridLoop outline : outlines) {
            if (outline == loop || outline.areaAbs() <= loop.areaAbs()) {
                continue;
            }
            if (!containsPoint(outline.points(), samplePoint.getX(), samplePoint.getY())) {
                continue;
            }
            if (best == null || outline.areaAbs() < best.areaAbs()) {
                best = outline;
            }
        }
        return best;
    }

    private static Vector2d polygonCentroid(List<GridPoint> points) {
        double x = 0D;
        double z = 0D;
        for (GridPoint point : points) {
            x += point.x();
            z += point.z();
        }
        return Vector2d.from(x / points.size(), z / points.size());
    }

    private static boolean containsPoint(List<GridPoint> polygon, double x, double z) {
        boolean inside = false;
        for (int i = 0, j = polygon.size() - 1; i < polygon.size(); j = i++) {
            GridPoint current = polygon.get(i);
            GridPoint previous = polygon.get(j);
            if ((current.z() > z) != (previous.z() > z)
                    && x < (double) (previous.x() - current.x()) * (z - current.z())
                    / (previous.z() - current.z()) + current.x()) {
                inside = !inside;
            }
        }
        return inside;
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
                case 3 -> 0;
                case 0 -> 1;
                case 1 -> 2;
                default -> 3;
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

    private static long polygonSignedArea2(List<GridPoint> points) {
        if (points.size() < 3) {
            return 0L;
        }

        long area2 = 0L;
        for (int i = 0; i < points.size(); i++) {
            GridPoint current = points.get(i);
            GridPoint next = points.get((i + 1) % points.size());
            area2 += (long) current.x() * next.z() - (long) next.x() * current.z();
        }
        return area2;
    }

    private static int getChunkX(long chunkKey) {
        return (int) chunkKey;
    }

    private static int getChunkZ(long chunkKey) {
        return (int) (chunkKey >> 32);
    }

    public record MarkerBuildResult(Map<String, ShapeMarker> markers, List<RegionSnapshot> snapshots) {}
    record RegionPolygon(List<Vector2d> outline, List<List<Vector2d>> holes) {}

    private record SectionCoord(int x, int z) {}

    private record GridPoint(int x, int z) {}

    private record Edge(GridPoint from, GridPoint to) {}

    private record GridLoop(List<GridPoint> points, long signedArea2) {
        double areaAbs() {
            return Math.abs(this.signedArea2) / 2.0D;
        }

        int minX() {
            return this.points.stream().mapToInt(GridPoint::x).min().orElse(0);
        }

        int minZ() {
            return this.points.stream().mapToInt(GridPoint::z).min().orElse(0);
        }
    }
}
