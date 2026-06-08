package io.fand.server.scheduler;

import io.fand.api.scheduler.Scheduler;
import io.fand.api.scheduler.Task;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TaskScheduler implements Scheduler, AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskScheduler.class);
    private static final int CANCELLED_COMPACTION_THRESHOLD = 1024;
    private static final long WALL_CLOCK_TASK = -1L;
    private static final Comparator<MainTask> TIMED_MAIN_TASK_ORDER = Comparator
            .comparingLong((MainTask task) -> task.dueNanos)
            .thenComparingLong(task -> task.sequence);
    private static final Comparator<MainTask> TICK_MAIN_TASK_ORDER = Comparator
            .comparingLong((MainTask task) -> task.dueTick)
            .thenComparingLong(task -> task.sequence);
    private static final Comparator<MainTask> READY_TASK_ORDER = Comparator.comparingLong(task -> task.sequence);

    private final Object mainLock = new Object();
    private final PriorityQueue<MainTask> timedMainTasks = new PriorityQueue<>(TIMED_MAIN_TASK_ORDER);
    private final PriorityQueue<MainTask> tickMainTasks = new PriorityQueue<>(TICK_MAIN_TASK_ORDER);
    private final ScheduledExecutorService asyncExecutor;
    private final LongSupplier nanoTime;
    private final AtomicLong sequence = new AtomicLong();
    private final AtomicLong currentTick = new AtomicLong();
    private final AtomicLong cancelledMainTasks = new AtomicLong();
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public TaskScheduler() {
        this(0);
    }

    public TaskScheduler(int configuredAsyncThreads) {
        this(System::nanoTime, createAsyncExecutor(configuredAsyncThreads));
    }

    TaskScheduler(LongSupplier nanoTime, ScheduledExecutorService asyncExecutor) {
        this.nanoTime = Objects.requireNonNull(nanoTime, "nanoTime");
        this.asyncExecutor = Objects.requireNonNull(asyncExecutor, "asyncExecutor");
    }

    @Override
    public Task runMain(Runnable task) {
        return scheduleMain(task, Duration.ZERO, 0L);
    }

    @Override
    public Task runMainAfter(Runnable task, Duration delay) {
        return scheduleMain(task, delay, 0L);
    }

    @Override
    public Task runMainAfterTicks(Runnable task, long delayTicks) {
        return scheduleMainTicks(task, delayTicks, 0L);
    }

    @Override
    public Task runMainRepeating(Runnable task, Duration initialDelay, Duration period) {
        var periodNanos = durationNanos(period, "period");
        if (periodNanos <= 0L) {
            throw new IllegalArgumentException("period must be positive");
        }
        return scheduleMain(task, initialDelay, periodNanos);
    }

    @Override
    public Task runMainRepeatingTicks(Runnable task, long initialDelayTicks, long periodTicks) {
        if (periodTicks <= 0L) {
            throw new IllegalArgumentException("periodTicks must be positive");
        }
        return scheduleMainTicks(task, initialDelayTicks, periodTicks);
    }

    @Override
    public Task runAsync(Runnable task) {
        return scheduleAsync(task, Duration.ZERO);
    }

    @Override
    public Task runAsyncAfter(Runnable task, Duration delay) {
        return scheduleAsync(task, delay);
    }

    public void reconfigureAsyncThreads(int configuredAsyncThreads) {
        ensureOpen();
        if (asyncExecutor instanceof ScheduledThreadPoolExecutor executor) {
            executor.setCorePoolSize(asyncThreadCount(configuredAsyncThreads));
        }
    }

    public int tick() {
        var tick = currentTick.incrementAndGet();
        var tickTime = nanoTime.getAsLong();
        List<MainTask> ready = new ArrayList<>();
        synchronized (mainLock) {
            compactCancelledMainTasksIfNeeded();
            collectReadyTimedTasks(tickTime, ready);
            collectReadyTickTasks(tick, ready);
        }
        ready.sort(READY_TASK_ORDER);

        var executed = 0;
        for (var task : ready) {
            if (task.cancelled()) {
                task.discardCancellationCount();
                continue;
            }
            executed++;
            runSafely(task.runnable);
            if (task.repeating() && !task.cancelled() && !closed.get()) {
                if (task.tickBased()) {
                    task.dueTick = saturatedAdd(currentTick.get(), task.periodTicks);
                } else {
                    task.dueNanos = saturatedAdd(nanoTime.getAsLong(), task.periodNanos);
                }
                enqueue(task);
            } else {
                task.finish();
            }
        }
        return executed;
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        synchronized (mainLock) {
            for (var task : timedMainTasks) {
                task.cancelled.set(true);
            }
            for (var task : tickMainTasks) {
                task.cancelled.set(true);
            }
            timedMainTasks.clear();
            tickMainTasks.clear();
            cancelledMainTasks.set(0L);
        }
        asyncExecutor.shutdownNow();
    }

    private Task scheduleMain(Runnable runnable, Duration delay, long periodNanos) {
        Objects.requireNonNull(runnable, "task");
        var delayNanos = durationNanos(delay, "delay");
        ensureOpen();
        var task = new MainTask(
                this,
                runnable,
                WALL_CLOCK_TASK,
                saturatedAdd(nanoTime.getAsLong(), delayNanos),
                periodNanos,
                0L,
                sequence.getAndIncrement()
        );
        enqueue(task);
        return task;
    }

    private Task scheduleMainTicks(Runnable runnable, long delayTicks, long periodTicks) {
        Objects.requireNonNull(runnable, "task");
        if (delayTicks < 0L) {
            throw new IllegalArgumentException("delayTicks must not be negative");
        }
        if (delayTicks == Long.MAX_VALUE) {
            throw new IllegalArgumentException("delayTicks is too large");
        }
        ensureOpen();
        var task = new MainTask(
                this,
                runnable,
                saturatedAdd(currentTick.get(), delayTicks + 1L),
                0L,
                0L,
                periodTicks,
                sequence.getAndIncrement()
        );
        enqueue(task);
        return task;
    }

    private Task scheduleAsync(Runnable runnable, Duration delay) {
        Objects.requireNonNull(runnable, "task");
        var delayNanos = durationNanos(delay, "delay");
        ensureOpen();
        var task = new AsyncTask();
        var future = asyncExecutor.schedule(() -> {
            if (!task.cancelled()) {
                runSafely(runnable);
            }
        }, delayNanos, TimeUnit.NANOSECONDS);
        task.bind(future);
        return task;
    }

    private void enqueue(MainTask task) {
        synchronized (mainLock) {
            if (closed.get()) {
                task.cancelled.set(true);
                return;
            }
            if (task.tickBased()) {
                tickMainTasks.add(task);
            } else {
                timedMainTasks.add(task);
            }
        }
    }

    private void collectReadyTimedTasks(long tickTime, List<MainTask> ready) {
        while (!timedMainTasks.isEmpty() && timedMainTasks.peek().dueNanos <= tickTime) {
            collectReadyTask(timedMainTasks.poll(), ready);
        }
    }

    private void collectReadyTickTasks(long tick, List<MainTask> ready) {
        while (!tickMainTasks.isEmpty() && tickMainTasks.peek().dueTick <= tick) {
            collectReadyTask(tickMainTasks.poll(), ready);
        }
    }

    private static void collectReadyTask(MainTask task, List<MainTask> ready) {
        if (task.cancelled()) {
            task.discardCancellationCount();
        } else {
            ready.add(task);
        }
    }

    private void compactCancelledMainTasksIfNeeded() {
        var cancelled = cancelledMainTasks.get();
        var totalQueued = timedMainTasks.size() + tickMainTasks.size();
        if (cancelled < CANCELLED_COMPACTION_THRESHOLD || cancelled <= totalQueued / 2L) {
            return;
        }
        removeCancelled(timedMainTasks);
        removeCancelled(tickMainTasks);
    }

    private static void removeCancelled(PriorityQueue<MainTask> tasks) {
        tasks.removeIf(task -> {
            if (!task.cancelled()) {
                return false;
            }
            task.discardCancellationCount();
            return true;
        });
    }

    private void decrementCancelledMainTasks() {
        cancelledMainTasks.updateAndGet(current -> current > 0L ? current - 1L : 0L);
    }

    private void ensureOpen() {
        if (closed.get()) {
            throw new RejectedExecutionException("scheduler is closed");
        }
    }

    private static void runSafely(Runnable runnable) {
        try {
            runnable.run();
        } catch (RuntimeException failure) {
            LOGGER.warn("Scheduled task failed", failure);
        }
    }

    private static long durationNanos(Duration duration, String name) {
        Objects.requireNonNull(duration, name);
        if (duration.isNegative()) {
            throw new IllegalArgumentException(name + " must not be negative");
        }
        try {
            return duration.toNanos();
        } catch (ArithmeticException failure) {
            throw new IllegalArgumentException(name + " is too large", failure);
        }
    }

    private static long saturatedAdd(long left, long right) {
        if (right > 0L && left > Long.MAX_VALUE - right) {
            return Long.MAX_VALUE;
        }
        return left + right;
    }

    private static int asyncThreadCount(int configuredAsyncThreads) {
        if (configuredAsyncThreads < 0) {
            throw new IllegalArgumentException("configuredAsyncThreads must not be negative");
        }
        return configuredAsyncThreads == 0 ? Math.max(2, Runtime.getRuntime().availableProcessors() / 2) : configuredAsyncThreads;
    }

    private static ScheduledThreadPoolExecutor createAsyncExecutor(int configuredAsyncThreads) {
        var executor = new ScheduledThreadPoolExecutor(asyncThreadCount(configuredAsyncThreads), asyncThreadFactory());
        executor.setRemoveOnCancelPolicy(true);
        executor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        executor.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
        return executor;
    }

    private static ThreadFactory asyncThreadFactory() {
        var threadId = new AtomicLong();
        return task -> {
            var thread = new Thread(task, "Fand Scheduler Async-" + threadId.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }

    private static final class MainTask implements Task {

        private final TaskScheduler owner;
        private final Runnable runnable;
        private final long periodNanos;
        private final long periodTicks;
        private final long sequence;
        private final AtomicBoolean cancelled = new AtomicBoolean(false);
        private final AtomicBoolean cancellationCounted = new AtomicBoolean(false);
        private final AtomicBoolean finished = new AtomicBoolean(false);
        private long dueTick;
        private long dueNanos;

        private MainTask(
                TaskScheduler owner,
                Runnable runnable,
                long dueTick,
                long dueNanos,
                long periodNanos,
                long periodTicks,
                long sequence
        ) {
            this.owner = owner;
            this.runnable = runnable;
            this.dueTick = dueTick;
            this.dueNanos = dueNanos;
            this.periodNanos = periodNanos;
            this.periodTicks = periodTicks;
            this.sequence = sequence;
        }

        @Override
        public boolean cancelled() {
            return cancelled.get();
        }

        @Override
        public void cancel() {
            if (finished.get()) {
                return;
            }
            if (cancelled.compareAndSet(false, true)) {
                countCancellation();
                if (finished.get()) {
                    discardCancellationCount();
                }
            }
        }

        private void finish() {
            if (finished.compareAndSet(false, true) && cancelled.get()) {
                discardCancellationCount();
            }
        }

        private void countCancellation() {
            if (cancellationCounted.compareAndSet(false, true)) {
                owner.cancelledMainTasks.incrementAndGet();
            }
        }

        private void discardCancellationCount() {
            if (cancellationCounted.compareAndSet(true, false)) {
                owner.decrementCancelledMainTasks();
            }
        }

        private boolean repeating() {
            return periodNanos > 0L || periodTicks > 0L;
        }

        private boolean tickBased() {
            return dueTick != WALL_CLOCK_TASK;
        }

    }

    private static final class AsyncTask implements Task {

        private final AtomicBoolean cancelled = new AtomicBoolean(false);
        private volatile ScheduledFuture<?> future;

        @Override
        public boolean cancelled() {
            var local = future;
            return cancelled.get() || local != null && local.isCancelled();
        }

        @Override
        public void cancel() {
            cancelled.set(true);
            var local = future;
            if (local != null) {
                local.cancel(false);
            }
        }

        private void bind(ScheduledFuture<?> future) {
            this.future = future;
            if (cancelled.get()) {
                future.cancel(false);
            }
        }
    }
}
