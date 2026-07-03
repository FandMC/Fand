package io.fand.api.world;

import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Scheduling policy for batch chunk operations.
 */
public record ChunkBatchOptions(
        int maxChunksPerTick,
        boolean skipAlreadyLoaded,
        boolean forceLoaded,
        boolean deduplicate,
        ChunkOrder order,
        @Nullable ChunkPos priorityCenter,
        @Nullable Vector3 priorityDirection
) {

    public static final int DEFAULT_MAX_CHUNKS_PER_TICK = 64;
    public static final ChunkBatchOptions DEFAULTS =
            new ChunkBatchOptions(DEFAULT_MAX_CHUNKS_PER_TICK, true, false);

    public ChunkBatchOptions(int maxChunksPerTick, boolean skipAlreadyLoaded, boolean forceLoaded) {
        this(maxChunksPerTick, skipAlreadyLoaded, forceLoaded, true, ChunkOrder.SOURCE, null, null);
    }

    public ChunkBatchOptions {
        if (maxChunksPerTick <= 0) {
            throw new IllegalArgumentException("maxChunksPerTick must be positive");
        }
        Objects.requireNonNull(order, "order");
        if (priorityDirection != null && !finite(priorityDirection)) {
            throw new IllegalArgumentException("priorityDirection must be finite");
        }
    }

    public static ChunkBatchOptions defaults() {
        return DEFAULTS;
    }

    public static ChunkBatchOptions immediate() {
        return new ChunkBatchOptions(Integer.MAX_VALUE, true, false);
    }

    public ChunkBatchOptions withMaxChunksPerTick(int maxChunksPerTick) {
        return copy(maxChunksPerTick, skipAlreadyLoaded, forceLoaded, deduplicate, order, priorityCenter, priorityDirection);
    }

    public ChunkBatchOptions withSkipAlreadyLoaded(boolean skipAlreadyLoaded) {
        return copy(maxChunksPerTick, skipAlreadyLoaded, forceLoaded, deduplicate, order, priorityCenter, priorityDirection);
    }

    public ChunkBatchOptions withForceLoaded(boolean forceLoaded) {
        return copy(maxChunksPerTick, skipAlreadyLoaded, forceLoaded, deduplicate, order, priorityCenter, priorityDirection);
    }

    public ChunkBatchOptions withDeduplicate(boolean deduplicate) {
        return copy(maxChunksPerTick, skipAlreadyLoaded, forceLoaded, deduplicate, order, priorityCenter, priorityDirection);
    }

    public ChunkBatchOptions withOrder(ChunkOrder order) {
        return copy(maxChunksPerTick, skipAlreadyLoaded, forceLoaded, deduplicate, order, priorityCenter, priorityDirection);
    }

    public ChunkBatchOptions withPriorityCenter(ChunkPos priorityCenter) {
        Objects.requireNonNull(priorityCenter, "priorityCenter");
        return copy(maxChunksPerTick, skipAlreadyLoaded, forceLoaded, deduplicate, order, priorityCenter, priorityDirection);
    }

    public ChunkBatchOptions withoutPriorityCenter() {
        return copy(maxChunksPerTick, skipAlreadyLoaded, forceLoaded, deduplicate, order, null, priorityDirection);
    }

    public ChunkBatchOptions withPriorityDirection(Vector3 priorityDirection) {
        Objects.requireNonNull(priorityDirection, "priorityDirection");
        return copy(maxChunksPerTick, skipAlreadyLoaded, forceLoaded, deduplicate, order, priorityCenter, priorityDirection);
    }

    public ChunkBatchOptions withoutPriorityDirection() {
        return copy(maxChunksPerTick, skipAlreadyLoaded, forceLoaded, deduplicate, order, priorityCenter, null);
    }

    public ChunkBatchOptions prioritize(ChunkPos center) {
        return withPriorityCenter(center).withOrder(ChunkOrder.NEAREST_FIRST);
    }

    public ChunkBatchOptions prioritize(ChunkPos center, Vector3 direction) {
        return withPriorityCenter(center).withPriorityDirection(direction).withOrder(ChunkOrder.FORWARD_FIRST);
    }

    public ChunkBatchOptions prioritize(Location location) {
        Objects.requireNonNull(location, "location");
        return prioritize(ChunkPos.containing(location), horizontalDirection(location));
    }

    private static ChunkBatchOptions copy(
            int maxChunksPerTick,
            boolean skipAlreadyLoaded,
            boolean forceLoaded,
            boolean deduplicate,
            ChunkOrder order,
            @Nullable ChunkPos priorityCenter,
            @Nullable Vector3 priorityDirection
    ) {
        return new ChunkBatchOptions(
                maxChunksPerTick,
                skipAlreadyLoaded,
                forceLoaded,
                deduplicate,
                order,
                priorityCenter,
                priorityDirection);
    }

    private static boolean finite(Vector3 vector) {
        return Double.isFinite(vector.x()) && Double.isFinite(vector.y()) && Double.isFinite(vector.z());
    }

    private static Vector3 horizontalDirection(Location location) {
        double radians = Math.toRadians(location.yaw());
        return new Vector3(-Math.sin(radians), 0.0D, Math.cos(radians));
    }
}
