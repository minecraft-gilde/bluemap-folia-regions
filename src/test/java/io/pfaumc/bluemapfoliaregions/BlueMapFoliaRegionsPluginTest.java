package io.pfaumc.bluemapfoliaregions;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BlueMapFoliaRegionsPluginTest {
    @Test
    void resolvesEachDimensionByItsFullWorldPath() {
        TestWorld overworld = new TestWorld(
                "overworld",
                Path.of("world", "dimensions", "minecraft", "overworld")
        );
        TestWorld nether = new TestWorld(
                "nether",
                Path.of("world", "dimensions", "minecraft", "the_nether")
        );
        TestWorld end = new TestWorld(
                "end",
                Path.of("world", "dimensions", "minecraft", "the_end")
        );
        List<TestWorld> worlds = List.of(overworld, nether, end);

        assertEquals(
                overworld,
                BlueMapFoliaRegionsPlugin.findByWorldPath(worlds, TestWorld::path, overworld.path())
                        .orElseThrow()
        );
        assertEquals(
                nether,
                BlueMapFoliaRegionsPlugin.findByWorldPath(worlds, TestWorld::path, nether.path())
                        .orElseThrow()
        );
        assertEquals(
                end,
                BlueMapFoliaRegionsPlugin.findByWorldPath(worlds, TestWorld::path, end.path())
                        .orElseThrow()
        );
    }

    @Test
    void normalizesPathsBeforeComparingThem() {
        TestWorld world = new TestWorld(
                "overworld",
                Path.of("world", "dimensions", "minecraft", "overworld")
        );

        TestWorld resolved = BlueMapFoliaRegionsPlugin.findByWorldPath(
                List.of(world),
                TestWorld::path,
                Path.of(".", "world", "dimensions", "minecraft", "overworld", "..", "overworld")
        ).orElseThrow();

        assertEquals(world, resolved);
    }

    private record TestWorld(String name, Path path) {}
}
