package io.fand.server.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

final class TaskSchedulerTest {

    private final AtomicLong now = new AtomicLong();
    private TaskScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new TaskScheduler(now::get, Executors.newSingleThreadScheduledExecutor());
    }

    @AfterEach
    void tearDown() {
        scheduler.close();
    }

    @Test
    void runsMainTasksOnTickInSubmissionOrder() {
        List<String> calls = new ArrayList<>();

        scheduler.runMain(() -> calls.add("first"));
        scheduler.runMain(() -> calls.add("second"));

        assertThat(calls).isEmpty();
        assertThat(scheduler.tick()).isEqualTo(2);
        assertThat(calls).containsExactly("first", "second");
    }

    @Test
    void keepsTasksSubmittedDuringTickUntilNextTick() {
        List<String> calls = new ArrayList<>();

        scheduler.runMain(() -> {
            calls.add("first");
            scheduler.runMain(() -> calls.add("second"));
        });

        assertThat(scheduler.tick()).isEqualTo(1);
        assertThat(calls).containsExactly("first");

        assertThat(scheduler.tick()).isEqualTo(1);
        assertThat(calls).containsExactly("first", "second");
    }

    @Test
    void delaysMainTasksUntilDelayHasElapsed() {
        List<String> calls = new ArrayList<>();

        scheduler.runMainAfter(() -> calls.add("delayed"), Duration.ofNanos(10));

        now.set(9);
        assertThat(scheduler.tick()).isZero();
        assertThat(calls).isEmpty();

        now.set(10);
        assertThat(scheduler.tick()).isEqualTo(1);
        assertThat(calls).containsExactly("delayed");
    }

    @Test
    void reschedulesRepeatingMainTasksAfterEachRun() {
        List<Long> calls = new ArrayList<>();

        scheduler.runMainRepeating(() -> calls.add(now.get()), Duration.ofNanos(10), Duration.ofNanos(5));

        now.set(9);
        assertThat(scheduler.tick()).isZero();

        now.set(10);
        assertThat(scheduler.tick()).isEqualTo(1);

        now.set(14);
        assertThat(scheduler.tick()).isZero();

        now.set(15);
        assertThat(scheduler.tick()).isEqualTo(1);

        assertThat(calls).containsExactly(10L, 15L);
    }

    @Test
    void delaysMainTasksByCompletedTicks() {
        List<String> calls = new ArrayList<>();

        scheduler.runMainAfterTicks(() -> calls.add("delayed"), 2L);

        assertThat(scheduler.tick()).isZero();
        assertThat(scheduler.tick()).isZero();
        assertThat(scheduler.tick()).isEqualTo(1);
        assertThat(calls).containsExactly("delayed");
    }

    @Test
    void tickTasksAreNotBlockedByLaterWallClockTasks() {
        List<String> calls = new ArrayList<>();

        scheduler.runMainAfter(() -> calls.add("wall"), Duration.ofDays(1));
        scheduler.runMainAfterTicks(() -> calls.add("tick"), 0L);

        assertThat(scheduler.tick()).isEqualTo(1);
        assertThat(calls).containsExactly("tick");
    }

    @Test
    void reschedulesRepeatingMainTasksByTicks() {
        List<Integer> calls = new ArrayList<>();

        scheduler.runMainRepeatingTicks(() -> calls.add(calls.size()), 0L, 2L);

        assertThat(scheduler.tick()).isEqualTo(1);
        assertThat(scheduler.tick()).isZero();
        assertThat(scheduler.tick()).isEqualTo(1);
        assertThat(scheduler.tick()).isZero();
        assertThat(scheduler.tick()).isEqualTo(1);
        assertThat(calls).containsExactly(0, 1, 2);
    }

    @Test
    void cancelsMainTasksBeforeExecution() {
        List<String> calls = new ArrayList<>();
        var task = scheduler.runMain(() -> calls.add("cancelled"));

        task.cancel();
        task.cancel();

        assertThat(task.cancelled()).isTrue();
        assertThat(scheduler.tick()).isZero();
        assertThat(calls).isEmpty();
    }

    @Test
    void cancelsRepeatingMainTasks() {
        List<String> calls = new ArrayList<>();
        var task = scheduler.runMainRepeating(() -> calls.add("run"), Duration.ZERO, Duration.ofNanos(1));

        assertThat(scheduler.tick()).isEqualTo(1);
        task.cancel();
        now.incrementAndGet();

        assertThat(scheduler.tick()).isZero();
        assertThat(calls).containsExactly("run");
    }

    @Test
    void cancelAfterOneShotMainTaskCompletesHasNoEffect() {
        var task = scheduler.runMain(() -> {});

        assertThat(scheduler.tick()).isEqualTo(1);
        task.cancel();

        assertThat(task.cancelled()).isFalse();
    }

    @Test
    void rejectsNewTasksAfterClose() {
        var task = scheduler.runMain(() -> {});

        scheduler.close();

        assertThat(task.cancelled()).isTrue();
        assertThatThrownBy(() -> scheduler.runMain(() -> {}))
                .isInstanceOf(RejectedExecutionException.class);
    }

    @Test
    void validatesDurations() {
        assertThatThrownBy(() -> scheduler.runMainAfter(() -> {}, Duration.ofNanos(-1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("delay");
        assertThatThrownBy(() -> scheduler.runMainRepeating(() -> {}, Duration.ZERO, Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("period");
        assertThatThrownBy(() -> scheduler.runMainAfterTicks(() -> {}, -1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("delayTicks");
        assertThatThrownBy(() -> scheduler.runMainRepeatingTicks(() -> {}, 0L, 0L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("periodTicks");
    }

    @Test
    void runsAsyncTasks() throws InterruptedException {
        var latch = new CountDownLatch(1);

        scheduler.runAsync(latch::countDown);

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    void cancelsDelayedAsyncTasks() throws InterruptedException {
        var latch = new CountDownLatch(1);
        var task = scheduler.runAsyncAfter(latch::countDown, Duration.ofSeconds(5));

        task.cancel();

        assertThat(task.cancelled()).isTrue();
        assertThat(latch.await(50, TimeUnit.MILLISECONDS)).isFalse();
    }
}
