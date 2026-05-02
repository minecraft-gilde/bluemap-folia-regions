package io.pfaumc.bluemapfoliaregions.marker;

import com.flowpowered.math.vector.Vector2d;
import io.pfaumc.bluemapfoliaregions.marker.RegionMarkerFactory.RegionPolygon;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegionMarkerFactoryTest {
    @Test
    void createsSingleSectionOutline() {
        List<RegionPolygon> polygons = polygons(section(0, 0));

        assertEquals(1, polygons.size());
        assertEquals(4, polygons.getFirst().outline().size());
        assertTrue(polygons.getFirst().holes().isEmpty());
        assertEquals(1.0D, area(polygons.getFirst().outline()));
        assertBounds(polygons.getFirst().outline(), 0D, 0D, 1D, 1D);
    }

    @Test
    void simplifiesRectangleOutline() {
        List<RegionPolygon> polygons = polygons(
                section(0, 0),
                section(1, 0),
                section(0, 1),
                section(1, 1)
        );

        assertEquals(1, polygons.size());
        assertEquals(4, polygons.getFirst().outline().size());
        assertEquals(4.0D, area(polygons.getFirst().outline()));
        assertBounds(polygons.getFirst().outline(), 0D, 0D, 2D, 2D);
    }

    @Test
    void keepsConcaveLShapeOutline() {
        List<RegionPolygon> polygons = polygons(
                section(0, 0),
                section(0, 1),
                section(1, 1)
        );

        assertEquals(1, polygons.size());
        assertEquals(6, polygons.getFirst().outline().size());
        assertEquals(3.0D, area(polygons.getFirst().outline()));
        assertBounds(polygons.getFirst().outline(), 0D, 0D, 2D, 2D);
    }

    @Test
    void keepsDisconnectedComponentsAsSeparatePolygons() {
        List<RegionPolygon> polygons = polygons(
                section(0, 0),
                section(3, 0)
        );

        assertEquals(2, polygons.size());
        assertTrue(polygons.stream().allMatch((polygon) -> polygon.outline().size() == 4));
        assertEquals(2.0D, polygons.stream().mapToDouble((polygon) -> area(polygon.outline())).sum());
    }

    @Test
    void keepsInteriorHoles() {
        List<RegionPolygon> polygons = polygons(
                section(0, 0),
                section(1, 0),
                section(2, 0),
                section(0, 1),
                section(2, 1),
                section(0, 2),
                section(1, 2),
                section(2, 2)
        );

        assertEquals(1, polygons.size());
        RegionPolygon polygon = polygons.getFirst();

        assertEquals(4, polygon.outline().size());
        assertEquals(1, polygon.holes().size());
        assertEquals(9.0D, area(polygon.outline()));
        assertEquals(1.0D, area(polygon.holes().getFirst()));
        assertBounds(polygon.outline(), 0D, 0D, 3D, 3D);
        assertBounds(polygon.holes().getFirst(), 1D, 1D, 2D, 2D);
    }

    private static List<RegionPolygon> polygons(long... sections) {
        return RegionMarkerFactory.createRegionPolygons(
                java.util.Arrays.stream(sections).boxed().toList(),
                1
        );
    }

    private static long section(int x, int z) {
        return ((long) z << 32) | (x & 0xffffffffL);
    }

    private static double area(List<Vector2d> points) {
        double area2 = 0D;
        for (int i = 0; i < points.size(); i++) {
            Vector2d current = points.get(i);
            Vector2d next = points.get((i + 1) % points.size());
            area2 += current.getX() * next.getY() - next.getX() * current.getY();
        }
        return Math.abs(area2) / 2.0D;
    }

    private static void assertBounds(
            List<Vector2d> points,
            double expectedMinX,
            double expectedMinZ,
            double expectedMaxX,
            double expectedMaxZ
    ) {
        double minX = points.stream().mapToDouble(Vector2d::getX).min().orElseThrow();
        double minZ = points.stream().mapToDouble(Vector2d::getY).min().orElseThrow();
        double maxX = points.stream().mapToDouble(Vector2d::getX).max().orElseThrow();
        double maxZ = points.stream().mapToDouble(Vector2d::getY).max().orElseThrow();

        assertEquals(expectedMinX, minX);
        assertEquals(expectedMinZ, minZ);
        assertEquals(expectedMaxX, maxX);
        assertEquals(expectedMaxZ, maxZ);
    }
}
