package io.fand.server.world;

import io.fand.api.world.ChunkBatchOptions;
import io.fand.api.world.ChunkOrder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

final class ChunkBatchPlanner {

    private ChunkBatchPlanner() {
    }

    static List<io.fand.api.world.ChunkPos> ordered(
            Iterable<io.fand.api.world.ChunkPos> chunks,
            ChunkBatchOptions options
    ) {
        Objects.requireNonNull(chunks, "chunks");
        Objects.requireNonNull(options, "options");
        var ordered = chunks instanceof Collection<?> collection
                ? new ArrayList<io.fand.api.world.ChunkPos>(collection.size())
                : new ArrayList<io.fand.api.world.ChunkPos>();
        HashSet<io.fand.api.world.ChunkPos> seen = options.deduplicate() ? new HashSet<>() : null;
        for (var chunk : chunks) {
            Objects.requireNonNull(chunk, "chunks contains null");
            if (seen == null || seen.add(chunk)) {
                ordered.add(chunk);
            }
            if (ordered.size() == Integer.MAX_VALUE) {
                throw new IllegalArgumentException("chunk batch contains too many chunks");
            }
        }
        var comparator = comparator(options);
        if (comparator != null) {
            ordered.sort(comparator);
        }
        return ordered;
    }

    private static @Nullable Comparator<io.fand.api.world.ChunkPos> comparator(ChunkBatchOptions options) {
        var center = options.priorityCenter();
        return switch (options.order()) {
            case SOURCE -> null;
            case NEAREST_FIRST -> center == null ? null : Comparator
                    .comparingLong((io.fand.api.world.ChunkPos pos) -> pos.distanceSquared(center))
                    .thenComparingInt(io.fand.api.world.ChunkPos::x)
                    .thenComparingInt(io.fand.api.world.ChunkPos::z);
            case FORWARD_FIRST -> forwardComparator(options);
        };
    }

    private static @Nullable Comparator<io.fand.api.world.ChunkPos> forwardComparator(ChunkBatchOptions options) {
        var center = options.priorityCenter();
        if (center == null) {
            return null;
        }
        var direction = options.priorityDirection();
        if (direction == null || direction.lengthSquared() == 0.0D) {
            return comparator(options.withOrder(ChunkOrder.NEAREST_FIRST));
        }
        double length = Math.hypot(direction.x(), direction.z());
        if (length == 0.0D) {
            return comparator(options.withOrder(ChunkOrder.NEAREST_FIRST));
        }
        double forwardX = direction.x() / length;
        double forwardZ = direction.z() / length;
        return Comparator
                .comparingInt((io.fand.api.world.ChunkPos pos) -> center.chebyshevDistance(pos))
                .thenComparingDouble(pos -> -forwardScore(center, pos, forwardX, forwardZ))
                .thenComparingLong(pos -> pos.distanceSquared(center))
                .thenComparingInt(io.fand.api.world.ChunkPos::x)
                .thenComparingInt(io.fand.api.world.ChunkPos::z);
    }

    private static double forwardScore(
            io.fand.api.world.ChunkPos center,
            io.fand.api.world.ChunkPos pos,
            double forwardX,
            double forwardZ
    ) {
        return ((double) pos.x() - center.x()) * forwardX + ((double) pos.z() - center.z()) * forwardZ;
    }
}
