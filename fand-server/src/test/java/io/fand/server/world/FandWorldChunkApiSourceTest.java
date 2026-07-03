package io.fand.server.world;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class FandWorldChunkApiSourceTest {

    @Test
    void chunkBatchApiIsBackedByCancellableRunner() throws IOException {
        var source = Files.readString(
                Path.of("src/main/java/io/fand/server/world/FandWorld.java"),
                StandardCharsets.UTF_8);
        var planner = Files.readString(
                Path.of("src/main/java/io/fand/server/world/ChunkBatchPlanner.java"),
                StandardCharsets.UTF_8);

        assertThat(source).contains("public ChunkBatchOperation loadChunks");
        assertThat(source).contains("public ChunkBatchOperation setChunksForceLoaded");
        assertThat(source).contains("private final class ChunkBatchRunner implements Runnable, ChunkBatchOperation");
        assertThat(source).contains("private final AtomicBoolean cancellationRequested = new AtomicBoolean()");
        assertThat(source).contains("var orderedChunks = ChunkBatchPlanner.ordered(chunks, options)");
        assertThat(source).contains("private final CopyOnWriteArrayList<ChunkBatchListener> listeners = new CopyOnWriteArrayList<>()");
        assertThat(source).contains("public ChunkBatchOperation onProgress(ChunkBatchListener listener)");
        assertThat(source).contains("listener.onProgress(progress)");
        assertThat(source).contains("catch (RuntimeException ignored)");
        assertThat(source).contains("scheduler.runMainAfterTicks(this, 0L)");
        assertThat(source).contains("handle.getChunk(pos.x(), pos.z(), ChunkStatus.FULL, true)");
        assertThat(source).contains("handle.setChunkForced(pos.x(), pos.z(), options.forceLoaded())");
        assertThat(planner).contains(
                "final class ChunkBatchPlanner",
                "HashSet<io.fand.api.world.ChunkPos> seen = options.deduplicate() ? new HashSet<>() : null",
                "ordered.sort(comparator)",
                "case FORWARD_FIRST -> forwardComparator(options)");
    }
}
