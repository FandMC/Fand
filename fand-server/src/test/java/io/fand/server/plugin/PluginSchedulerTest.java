package io.fand.server.plugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.server.MinecraftServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import io.fand.api.entity.Entity;
import io.fand.api.event.EventSubscription;
import io.fand.api.world.Location;
import io.fand.api.world.World;
import io.fand.server.scheduler.TaskScheduler;

final class PluginSchedulerTest {

    private final PluginResourceTracker resources = new PluginResourceTracker();
    private TaskScheduler scheduler;
    private PluginScheduler plugin;

    @BeforeEach
    void setUp() {
        var server = mock(MinecraftServer.class);
        when(server.isSameThread()).thenReturn(true);
        scheduler = new TaskScheduler(1, 1, () -> server);
        plugin = new PluginScheduler(scheduler, resources);
    }

    @AfterEach
    void tearDown() {
        resources.close();
        scheduler.close();
    }

    @Test
    void disablingPluginCancelsAllOwnedTargetsBeforeExecution() {
        var calls = new AtomicInteger();
        var pending = List.of(
                plugin.global().call(calls::incrementAndGet),
                plugin.at(new Location(mock(World.class), 0, 0, 0, 0, 0)).call(calls::incrementAndGet),
                plugin.forEntity(mock(Entity.class)).call(calls::incrementAndGet));

        resources.close();
        scheduler.tick();

        assertThat(pending).allMatch(CompletableFuture::isCancelled);
        assertThat(calls).hasValue(0);
    }

    @Test
    void retainedTargetRejectsSubmissionAfterPluginDisable() {
        var target = plugin.global();
        resources.close();

        assertThatThrownBy(() -> target.call(() -> 42).join())
                .hasCauseInstanceOf(RejectedExecutionException.class)
                .hasRootCauseMessage("Plugin is disabled");
        assertThat(scheduler.tick()).isZero();
    }

    @Test
    void pendingCallCannotEnterWhileEarlierResourcesAreBeingClosed() {
        var subscription = mock(EventSubscription.class);
        doAnswer(invocation -> scheduler.tick()).when(subscription).unregister();
        resources.track(subscription);
        var calls = new AtomicInteger();
        var pending = plugin.global().call(calls::incrementAndGet);

        resources.close();

        assertThat(pending).isCancelled();
        assertThat(calls).hasValue(0);
    }

    @Test
    void cancellationFromCallerStillPreventsExecution() {
        var calls = new AtomicInteger();
        var pending = plugin.global().run(calls::incrementAndGet);

        pending.cancel(false);
        scheduler.tick();

        assertThat(calls).hasValue(0);
    }

    @Test
    void completedAndFailedCallsAreReleasedBeforePluginDisable() {
        var completed = new CountingCancellationFuture();
        var failed = new CountingCancellationFuture();
        resources.trackCall(() -> completed);
        resources.trackCall(() -> failed);

        completed.complete(42);
        failed.completeExceptionally(new IllegalStateException("failed"));
        resources.close();

        assertThat(completed.cancellations).hasValue(0);
        assertThat(failed.cancellations).hasValue(0);
    }

    @Test
    void alreadyCompletedCallsAreNotRetained() {
        var completed = new CountingCancellationFuture();
        completed.complete(42);

        resources.trackCall(() -> completed);
        resources.close();

        assertThat(completed.cancellations).hasValue(0);
    }

    @Test
    void successfulPluginCallPreservesItsResultAfterDisable() {
        var result = plugin.global().call(() -> 42);
        scheduler.tick();

        resources.close();

        assertThat(result).isCompletedWithValue(42);
    }

    @Test
    void disablingPluginInvokesCompletionCallbacksOutsideTheResourceLock() throws Exception {
        try (var worker = Executors.newSingleThreadExecutor()) {
            var pending = plugin.global().call(() -> 42);
            var callback = pending.handle((value, failure) -> {
                try {
                    return worker.submit(() -> plugin.global().call(() -> 43).isCompletedExceptionally())
                            .get(5, TimeUnit.SECONDS);
                } catch (Exception unexpected) {
                    throw new AssertionError(unexpected);
                }
            });

            resources.close();

            assertThat(callback.join()).isTrue();
        }
    }

    private static final class CountingCancellationFuture extends CompletableFuture<Integer> {

        private final AtomicInteger cancellations = new AtomicInteger();

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            cancellations.incrementAndGet();
            return super.cancel(mayInterruptIfRunning);
        }
    }
}
