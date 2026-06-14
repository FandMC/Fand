package net.minecraft.server.level;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

final class ChunkGenerationTaskTest {

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void waitsForAllIncompleteFuturesInScheduledLayer() {
        CompletableFuture<ChunkResult<ChunkAccess>> first = new CompletableFuture<>();
        CompletableFuture<ChunkResult<ChunkAccess>> second = new CompletableFuture<>();
        List<CompletableFuture<ChunkResult<ChunkAccess>>> layer = new ArrayList<>(List.of(first, second));

        CompletableFuture<?> waitFor = ChunkGenerationTask.waitForScheduledLayer(layer, () -> {
        });

        assertThat(waitFor).isNotNull();
        assertThat(waitFor).isNotDone();

        first.complete(ChunkResult.of(null));
        assertThat(waitFor).isNotDone();

        second.complete(ChunkResult.of(null));
        assertThat(waitFor).isDone();
    }

    @Test
    void removesCompletedFuturesAndMarksFailure() {
        CompletableFuture<ChunkResult<ChunkAccess>> success = CompletableFuture.completedFuture(ChunkResult.of(null));
        CompletableFuture<ChunkResult<ChunkAccess>> failure = CompletableFuture.completedFuture(ChunkResult.error("failed"));
        List<CompletableFuture<ChunkResult<ChunkAccess>>> layer = new ArrayList<>(List.of(success, failure));
        AtomicBoolean cancelled = new AtomicBoolean(false);

        CompletableFuture<?> waitFor = ChunkGenerationTask.waitForScheduledLayer(layer, () -> cancelled.set(true));

        assertThat(waitFor).isNull();
        assertThat(layer).isEmpty();
        assertThat(cancelled).isTrue();
    }
}
