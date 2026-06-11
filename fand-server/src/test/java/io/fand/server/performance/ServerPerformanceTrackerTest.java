package io.fand.server.performance;

import static org.assertj.core.api.Assertions.assertThat;

import io.fand.api.performance.TickWindow;
import java.util.ArrayDeque;
import java.util.Queue;
import org.junit.jupiter.api.Test;

final class ServerPerformanceTrackerTest {

    @Test
    void startsWithHealthyDefaultsBeforeFirstTick() {
        var tracker = newSynchronousTracker();

        var snapshot = tracker.snapshot();

        assertThat(snapshot.ticksPerSecond().oneMinute()).isEqualTo(20.0);
        assertThat(snapshot.currentMillisecondsPerTick()).isEqualTo(0.0);
        assertThat(snapshot.fiveSeconds().sampleCount()).isZero();
        assertThat(snapshot.window(TickWindow.FIVE_SECONDS)).isEqualTo(snapshot.fiveSeconds());
        assertThat(snapshot.tickCount()).isZero();
    }

    @Test
    void reportsMsptFromMeasuredTickDurations() {
        var tracker = newSynchronousTracker();

        tracker.recordTick(0L, 25_000_000L);
        tracker.recordTick(50_000_000L, 75_000_000L);

        var snapshot = tracker.snapshot();

        assertThat(snapshot.currentMillisecondsPerTick()).isEqualTo(50.0);
        assertThat(snapshot.millisecondsPerTick().oneMinute()).isEqualTo(50.0);
        assertThat(snapshot.fiveSeconds().millisecondsPerTick().minimum()).isEqualTo(25.0);
        assertThat(snapshot.fiveSeconds().millisecondsPerTick().maximum()).isEqualTo(75.0);
        assertThat(snapshot.fiveSeconds().millisecondsPerTick().median()).isEqualTo(50.0);
        assertThat(snapshot.tickCount()).isEqualTo(2L);
    }

    @Test
    void returnsPublishedSnapshotUntilAsyncRecomputeRuns() {
        var executor = new ManualExecutor();
        var tracker = new ServerPerformanceTracker(executor, () -> 0L);

        var first = tracker.snapshot();
        tracker.recordTick(0L, 25_000_000L);

        var second = tracker.snapshot();
        executor.runNext();
        var third = tracker.snapshot();

        assertThat(second).isSameAs(first);
        assertThat(third).isNotSameAs(first);
        assertThat(tracker.snapshot()).isSameAs(third);
    }

    @Test
    void includesMeasuredTaskExecutionInMspt() {
        var tracker = newSynchronousTracker();

        tracker.recordTick(0L, 25_000_000L, 15_000_000L);

        var snapshot = tracker.snapshot();

        assertThat(snapshot.currentMillisecondsPerTick()).isEqualTo(40.0);
        assertThat(snapshot.fiveSeconds().utilization()).isEqualTo(0.8);
    }

    @Test
    void reportsTwentyTpsWhenTicksKeepTargetPacing() {
        var tracker = newSynchronousTracker();

        tracker.recordTick(0L, 10_000_000L);
        tracker.recordTick(50_000_000L, 100_000_000L);

        var snapshot = tracker.snapshot();

        assertThat(snapshot.ticksPerSecond().oneMinute()).isEqualTo(20.0);
        assertThat(snapshot.fiveSeconds().tickIntervalMilliseconds().average()).isEqualTo(50.0);
    }

    @Test
    void treatsSmallIdleOversleepAsFullTps() {
        var tracker = newSynchronousTracker();

        tracker.recordTick(0L, 7_000_000L);
        tracker.recordTick(51_000_000L, 8_000_000L);

        var snapshot = tracker.snapshot();

        assertThat(snapshot.ticksPerSecond().oneMinute()).isEqualTo(20.0);
        assertThat(snapshot.fiveSeconds().tickIntervalMilliseconds().maximum()).isEqualTo(51.0);
    }

    @Test
    void slowsTpsWhenTickStartsFallBehindTargetPacing() {
        var tracker = newSynchronousTracker();

        tracker.recordTick(0L, 10_000_000L);
        tracker.recordTick(100_000_000L, 10_000_000L);

        var snapshot = tracker.snapshot();

        assertThat(snapshot.ticksPerSecond().oneMinute()).isCloseTo(13.333, within(0.001));
        assertThat(snapshot.fiveSeconds().tickIntervalMilliseconds().maximum()).isEqualTo(100.0);
    }

    @Test
    void coalescesQueuedAsyncRecomputes() {
        var executor = new ManualExecutor();
        var tracker = new ServerPerformanceTracker(executor, () -> 0L);

        tracker.recordTick(0L, 25_000_000L);
        tracker.recordTick(50_000_000L, 75_000_000L);
        tracker.recordTick(100_000_000L, 10_000_000L);

        assertThat(executor.pendingTasks()).isEqualTo(1);
        executor.runNext();

        var snapshot = tracker.snapshot();
        assertThat(snapshot.tickCount()).isEqualTo(3L);
        assertThat(snapshot.currentMillisecondsPerTick()).isCloseTo(36.667, within(0.001));
        assertThat(executor.pendingTasks()).isZero();
    }

    @Test
    void closeStopsFurtherSnapshotUpdates() {
        var executor = new ManualExecutor();
        var tracker = new ServerPerformanceTracker(executor, () -> 0L);

        tracker.recordTick(0L, 25_000_000L);
        assertThat(executor.pendingTasks()).isEqualTo(1);

        tracker.close();
        executor.runNext();
        tracker.recordTick(50_000_000L, 75_000_000L);

        assertThat(tracker.snapshot().tickCount()).isZero();
        assertThat(executor.pendingTasks()).isZero();
    }

    private static ServerPerformanceTracker newSynchronousTracker() {
        return new ServerPerformanceTracker(Runnable::run, () -> 0L);
    }

    private static org.assertj.core.data.Offset<Double> within(double value) {
        return org.assertj.core.data.Offset.offset(value);
    }

    private static final class ManualExecutor implements java.util.concurrent.Executor {

        private final Queue<Runnable> pendingTasks = new ArrayDeque<>();

        @Override
        public void execute(Runnable command) {
            pendingTasks.add(command);
        }

        int pendingTasks() {
            return pendingTasks.size();
        }

        void runNext() {
            var task = pendingTasks.remove();
            task.run();
        }
    }
}
