package io.pfaumc.bluemapfoliaregions;

import java.util.concurrent.atomic.AtomicLong;

final class LifecycleGeneration {
    private final AtomicLong value = new AtomicLong();

    long advance() {
        return this.value.incrementAndGet();
    }

    long current() {
        return this.value.get();
    }

    boolean isCurrent(long generation) {
        return generation == this.value.get();
    }
}
