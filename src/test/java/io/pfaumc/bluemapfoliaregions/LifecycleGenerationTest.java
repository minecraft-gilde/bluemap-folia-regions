package io.pfaumc.bluemapfoliaregions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LifecycleGenerationTest {
    @Test
    void invalidatesUpdatesFromAnEarlierLifecycle() {
        LifecycleGeneration generation = new LifecycleGeneration();
        long first = generation.advance();

        assertTrue(generation.isCurrent(first));

        long second = generation.advance();

        assertFalse(generation.isCurrent(first));
        assertTrue(generation.isCurrent(second));
    }
}
