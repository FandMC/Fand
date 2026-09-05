package io.fand.server.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import io.fand.api.world.Location;
import io.fand.server.entity.FandEntity;
import io.fand.server.entity.FandPlayer;
import io.fand.server.world.FandWorld;
import io.fand.server.world.WorldRegistry;

final class OwnedSchedulerTest {

    private MinecraftServer server;
    private TaskScheduler scheduler;

    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @BeforeEach
    void setUp() {
        server = mock(MinecraftServer.class);
        var tickThread = Thread.currentThread();
        when(server.isSameThread()).thenAnswer(invocation -> Thread.currentThread() == tickThread);
        scheduler = new TaskScheduler(1, 1, () -> server);
    }

    @AfterEach
    void tearDown() {
        scheduler.close();
    }

    @Test
    void backgroundSubmissionRunsOnlyOnTheNextServerTick() throws Exception {
        var caller = Thread.currentThread();
        try (var worker = Executors.newSingleThreadExecutor()) {
            var result = worker.submit(() -> scheduler.global().call(Thread::currentThread)).get(5, TimeUnit.SECONDS);

            assertThat(result).isNotDone();
            scheduler.tick();

            assertThat(result.join()).isSameAs(caller);
        }
    }

    @Test
    void interleavesWithExistingMainTasksInSubmissionOrder() {
        List<String> calls = new ArrayList<>();
        scheduler.runMain(() -> calls.add("main"));
        var result = scheduler.global().run(() -> calls.add("owned"));
        scheduler.runMain(() -> calls.add("last"));

        assertThat(result).isNotDone();
        assertThat(scheduler.tick()).isEqualTo(3);
        assertThat(calls).containsExactly("main", "owned", "last");
        assertThat(result).isCompletedWithValue(null);
    }

    @Test
    void nestedSubmissionWaitsForAnotherTick() {
        var outer = scheduler.global().call(() -> scheduler.global().call(() -> 42));

        scheduler.tick();
        var inner = outer.join();
        assertThat(inner).isNotDone();
        scheduler.tick();

        assertThat(inner).isCompletedWithValue(42);
    }

    @Test
    void reportsActionFailureWithoutSkippingOtherTasks() {
        var failure = new IllegalArgumentException("invalid action");
        var failed = scheduler.global().call(() -> { throw failure; });
        var next = scheduler.global().call(() -> 42);

        scheduler.tick();

        assertThatThrownBy(failed::join).hasCause(failure);
        assertThat(next).isCompletedWithValue(42);
    }

    @Test
    void cancellationPreventsPendingAction() {
        var calls = new AtomicInteger();
        var result = scheduler.global().call(calls::incrementAndGet);

        result.cancel(true);
        scheduler.tick();

        assertThat(result).isCancelled();
        assertThat(calls).hasValue(0);
    }

    @Test
    void closeCancelsQueuedCallsAndRejectsNewSubmissions() {
        var pending = scheduler.global().call(() -> 42);

        scheduler.close();

        assertThat(pending).isCancelled();
        assertThatThrownBy(() -> scheduler.global().call(() -> 43).join())
                .hasCauseInstanceOf(RejectedExecutionException.class);
    }

    @Test
    void closeDuringTickCancelsCallsAlreadyTakenFromTheQueue() {
        var calls = new AtomicInteger();
        scheduler.runMain(scheduler::close);
        var pending = scheduler.global().call(calls::incrementAndGet);
        var legacy = scheduler.runMain(calls::incrementAndGet);

        assertThat(scheduler.tick()).isEqualTo(1);

        assertThat(pending).isCancelled();
        assertThat(legacy.cancelled()).isTrue();
        assertThat(calls).hasValue(0);
    }

    @Test
    void closeCompletesCallbacksOutsideTheQueueLock() throws Exception {
        try (var worker = Executors.newSingleThreadExecutor()) {
            var pending = scheduler.global().call(() -> 42);
            var callback = pending.handle((value, failure) -> {
                try {
                    return worker.submit(() -> scheduler.global().call(() -> 43).isCompletedExceptionally())
                            .get(5, TimeUnit.SECONDS);
                } catch (Exception unexpected) {
                    throw new AssertionError(unexpected);
                }
            });

            scheduler.close();

            assertThat(callback.join()).isTrue();
        }
    }

    @Test
    void concurrentCloseAndSubmissionsLeaveNoUnfinishedFuture() throws Exception {
        try (var workers = Executors.newFixedThreadPool(2)) {
            var start = new CountDownLatch(1);
            var submissions = workers.submit(() -> {
                start.await();
                var results = new ArrayList<CompletableFuture<Integer>>();
                for (int index = 0; index < 500; index++) {
                    results.add(scheduler.global().call(() -> 42));
                }
                return results;
            });
            var closing = workers.submit(() -> {
                start.await();
                scheduler.close();
                return null;
            });

            start.countDown();
            closing.get(5, TimeUnit.SECONDS);

            assertThat(submissions.get(5, TimeUnit.SECONDS)).allMatch(CompletableFuture::isDone);
        }
    }

    @Test
    void unattachedSchedulerFailsInsteadOfExecutingOnTheCaller() {
        try (var unattached = new TaskScheduler(1, 1)) {
            var calls = new AtomicInteger();
            var result = unattached.global().call(calls::incrementAndGet);

            unattached.tick();

            assertThatThrownBy(result::join).hasRootCauseMessage("Minecraft server is not attached");
            assertThat(calls).hasValue(0);
        }
    }

    @Test
    void rejectsWrongExecutionThreadBeforeReadingTheWorld() {
        var world = mock(FandWorld.class);
        var result = scheduler.at(new Location(world, 0, 64, 0, 0, 0)).call(() -> 42);
        when(server.isSameThread()).thenReturn(false);

        scheduler.tick();

        assertThatThrownBy(result::join).hasRootCauseMessage("Owned tasks must execute on the server tick thread");
        verify(world, never()).handle();
    }

    @Test
    void locationIsValidatedAtExecutionAndRemainsBoundToItsWorldInstance() {
        var level = loadedWorld();
        var world = mock(FandWorld.class);
        when(world.handle()).thenReturn(level);
        var target = scheduler.at(new Location(world, -17, 64, -1, 0, 0));
        var valid = target.call(() -> 42);
        verify(world, never()).handle();

        scheduler.tick();
        assertThat(valid).isCompletedWithValue(42);

        var stale = target.call(() -> 43);
        when(server.getLevel(Level.OVERWORLD)).thenReturn(mock(ServerLevel.class));
        scheduler.tick();

        assertThatThrownBy(stale::join).hasRootCauseMessage("Scheduled world is no longer loaded");
    }

    @Test
    void removedEntityNeverExecutesItsPendingAction() {
        var handle = activeEntity(loadedWorld());
        var entity = new FandEntity(handle, mock(WorldRegistry.class));
        var result = scheduler.forEntity(entity).call(() -> 42);
        when(handle.isRemoved()).thenReturn(true);

        scheduler.tick();

        assertThatThrownBy(result::join).hasRootCauseMessage("Scheduled entity is no longer active");
    }

    @Test
    void entityTargetUsesItsCurrentWorldInsteadOfTheSubmissionWorld() {
        var source = loadedWorld();
        var handle = activeEntity(source);
        var entity = new FandEntity(handle, mock(WorldRegistry.class));
        var result = scheduler.forEntity(entity).call(handle::level);
        var destination = mock(ServerLevel.class);
        when(destination.dimension()).thenReturn(Level.NETHER);
        when(destination.getServer()).thenReturn(server);
        when(server.getLevel(Level.NETHER)).thenReturn(destination);
        when(destination.getEntity(handle.getUUID())).thenReturn(handle);
        when(handle.level()).thenReturn(destination);
        when(server.getLevel(Level.OVERWORLD)).thenReturn(null);

        scheduler.tick();

        assertThat(result.join()).isSameAs(destination);
    }

    @Test
    void entityWithReusedUuidDoesNotReviveAnOldHandle() {
        var level = loadedWorld();
        var handle = activeEntity(level);
        var result = scheduler.forEntity(new FandEntity(handle, mock(WorldRegistry.class))).call(() -> 42);
        when(level.getEntity(handle.getUUID())).thenReturn(mock(net.minecraft.world.entity.Entity.class));

        scheduler.tick();

        assertThatThrownBy(result::join).hasRootCauseMessage("Scheduled entity is no longer active");
    }

    @Test
    void playerTargetResolvesTheCurrentPlayerHandleAtExecution() {
        var level = loadedWorld();
        var player = mock(FandPlayer.class);
        var previous = mock(ServerPlayer.class);
        when(player.handle()).thenReturn(previous);
        var result = scheduler.forEntity(player).call(player::handle);
        var current = mock(ServerPlayer.class);
        var id = UUID.randomUUID();
        when(current.getUUID()).thenReturn(id);
        when(current.level()).thenReturn(level);
        when(level.getEntity(id)).thenReturn(current);
        when(player.handle()).thenReturn(current);

        scheduler.tick();

        assertThat(result.join()).isSameAs(current);
        verify(previous, never()).level();
    }

    private ServerLevel loadedWorld() {
        var level = mock(ServerLevel.class);
        when(level.dimension()).thenReturn(Level.OVERWORLD);
        when(level.getServer()).thenReturn(server);
        when(server.getLevel(Level.OVERWORLD)).thenReturn(level);
        return level;
    }

    private net.minecraft.world.entity.Entity activeEntity(ServerLevel level) {
        var handle = mock(net.minecraft.world.entity.Entity.class);
        var id = UUID.randomUUID();
        when(handle.getUUID()).thenReturn(id);
        when(handle.level()).thenReturn(level);
        when(level.getEntity(id)).thenReturn(handle);
        return handle;
    }
}
