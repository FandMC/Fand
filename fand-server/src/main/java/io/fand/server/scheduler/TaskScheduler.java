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
    private static final Comparator<MainTask> MAIN_TASK_ORDER = Comparator
            .comparingLong((MainTask task) -> task.dueNanos)
            .thenComparingLong(task -> task.sequence);

    private final Object mainLock = new Object();
    private final PriorityQueue<MainTask> mainTasks = new PriorityQueue<>(MAIN_TASK_ORDER);
    private final ScheduledExecutorService asyncExecutor;
    private final LongSupplier nanoTime;
    private final AtomicLong sequence = new AtomicLong();
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
    public Task runMainRepeating(Runnable task, Duration initialDelay, Duration period) {
        var periodNanos = durationNanos(period, "period");
        if (periodNanos <= 0L) {
            throw new IllegalArgumentException("period must be positive");
        }
        return scheduleMain(task, initialDelay, periodNanos);
    }

    @Override
    public Task runAsync(Runnable task) {
        return scheduleAsync(task, Duration.ZERO);
    }

    @Override
    public Task runAsyncAfter(Runnable task, Duration delay) {
        return scheduleAsync(task, delay);
    }

    public int tick() {
        var tickTime = nanoTime.getAsLong();
        List<MainTask> ready = new ArrayList<>();
        synchronized (mainLock) {
            compactCancelledMainTasksIfNeeded();
            while (!mainTasks.isEmpty() && mainTasks.peek().dueNanos <= tickTime) {
                var task = mainTasks.poll();
                if (task.cancelled()) {
                    task.discardCancellationCount();
                } else {
                    ready.add(task);
                }
            }
        }

        var executed = 0;
        for (var task : ready) {
            if (task.cancelled()) {
                task.discardCancellationCount();
                continue;
            }
            executed++;
            runSafely(task.runnable);
            if (task.repeating() && !task.cancelled() && !closed.get()) {
                task.dueNanos = saturatedAdd(nanoTime.getAsLong(), task.periodNanos);
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
            for (var task : mainTasks) {
                task.cancelled.set(true);
            }
            mainTasks.clear();
            cancelledMainTasks.set(0L);
        }
        asyncExecutor.shutdownNow();
    }

    private Task scheduleMain(Runnable runnable, Duration delay, long periodNanos) {
        Objects.requireNonNull(runnable, "task");
        var delayNanos = durationNanos(delay, "delay");
        ensureOpen();
        var task = new MainTask(this, runnable, saturatedAdd(nanoTime.getAsLong(), delayNanos), periodNanos, sequence.getAndIncrement());
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
            mainTasks.add(task);
        }
    }

    private void compactCancelledMainTasksIfNeeded() {
        var cancelled = cancelledMainTasks.get();
        if (cancelled < CANCELLED_COMPACTION_THRESHOLD || cancelled <= mainTasks.size() / 2L) {
            return;
        }
        var iterator = mainTasks.iterator();
        while (iterator.hasNext()) {
            var task = iterator.next();
            if (task.cancelled()) {
                iterator.remove();
                task.discardCancellationCount();
            }
        }
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
        private final long sequence;
        private final AtomicBoolean cancelled = new AtomicBoolean(false);
        private final AtomicBoolean cancellationCounted = new AtomicBoolean(false);
        private final AtomicBoolean finished = new AtomicBoolean(false);
        private long dueNanos;

        private MainTask(TaskScheduler owner, Runnable runnable, long dueNanos, long periodNanos, long sequence) {
            this.owner = owner;
            this.runnable = runnable;
            this.dueNanos = dueNanos;
            this.periodNanos = periodNanos;
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
            return periodNanos > 0L;
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
