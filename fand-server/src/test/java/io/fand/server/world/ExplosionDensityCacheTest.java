package io.fand.server.world;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

final class ExplosionDensityCacheTest {

    private static final AABB BOX = new AABB(0.0, 0.0, 0.0, 1.0, 2.0, 1.0);

    @Test
    void sameCenterBlockAndBoxComputesOnce() {
        var cache = new ExplosionDensityCache();
        var calls = new AtomicInteger();

        float first = cache.seenPercent(new Vec3(10.2, 64.5, -3.7), BOX, () -> {
            calls.incrementAndGet();
            return 0.75F;
        });
        float second = cache.seenPercent(new Vec3(10.9, 64.1, -3.1), BOX, () -> {
            calls.incrementAndGet();
            return 0.0F;
        });

        assertThat(first).isEqualTo(0.75F);
        assertThat(second).isEqualTo(0.75F);
        assertThat(calls).hasValue(1);
    }

    @Test
    void distinctCenterBlocksComputeSeparately() {
        var cache = new ExplosionDensityCache();
        var calls = new AtomicInteger();

        cache.seenPercent(new Vec3(10.0, 64.0, 0.0), BOX, () -> {
            calls.incrementAndGet();
            return 0.5F;
        });
        cache.seenPercent(new Vec3(11.0, 64.0, 0.0), BOX, () -> {
            calls.incrementAndGet();
            return 0.25F;
        });

        assertThat(calls).hasValue(2);
        assertThat(cache.size()).isEqualTo(2);
    }

    @Test
    void distinctBoundingBoxesComputeSeparately() {
        var cache = new ExplosionDensityCache();
        var center = new Vec3(0.5, 0.5, 0.5);
        var calls = new AtomicInteger();

        cache.seenPercent(center, BOX, () -> {
            calls.incrementAndGet();
            return 1.0F;
        });
        cache.seenPercent(center, BOX.move(0.0, 1.0, 0.0), () -> {
            calls.incrementAndGet();
            return 0.0F;
        });

        assertThat(calls).hasValue(2);
    }

    @Test
    void zeroExposureIsCached() {
        var cache = new ExplosionDensityCache();
        var center = new Vec3(0.5, 0.5, 0.5);
        var calls = new AtomicInteger();

        cache.seenPercent(center, BOX, () -> {
            calls.incrementAndGet();
            return 0.0F;
        });
        float again = cache.seenPercent(center, BOX, () -> {
            calls.incrementAndGet();
            return 1.0F;
        });

        assertThat(again).isEqualTo(0.0F);
        assertThat(calls).hasValue(1);
    }

    @Test
    void clearDropsAllEntries() {
        var cache = new ExplosionDensityCache();
        cache.seenPercent(new Vec3(0.5, 0.5, 0.5), BOX, () -> 0.5F);

        cache.clear();

        assertThat(cache.size()).isZero();
        var calls = new AtomicInteger();
        cache.seenPercent(new Vec3(0.5, 0.5, 0.5), BOX, () -> {
            calls.incrementAndGet();
            return 0.5F;
        });
        assertThat(calls).hasValue(1);
    }
}
