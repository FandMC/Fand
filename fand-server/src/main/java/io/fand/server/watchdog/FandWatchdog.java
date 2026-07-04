package io.fand.server.watchdog;

import com.google.common.collect.Streams;
import com.mojang.logging.LogUtils;
import io.fand.server.Main;
import io.fand.server.config.FandConfig;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.DefaultUncaughtExceptionHandlerWithName;
import net.minecraft.ReportType;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.Util;
import net.minecraft.world.level.gamerules.GameRules;
import org.slf4j.Logger;

public final class FandWatchdog implements Runnable {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final long MAX_SHUTDOWN_TIME_MILLIS = 10_000L;
    private static final int SHUTDOWN_STATUS = 1;
    private static final long CHECK_INTERVAL_MILLIS = 1_000L;
    private static final int WARNING_STACK_FRAMES = 32;
    private static final int CRASH_STACK_FRAMES = 80;
    private static final int MAX_WARNING_WORKER_DUMPS = 6;
    private static final int MAX_CRASH_WORKER_DUMPS = 12;
    private static final Comparator<ThreadInfo> THREAD_INFO_COMPARATOR = Comparator.comparing(ThreadInfo::isDaemon)
            .thenComparing(ThreadInfo::getThreadState)
            .thenComparing(ThreadInfo::getThreadName);
    private static final List<StackPattern> STACK_PATTERNS = List.of(
            new StackPattern("net.minecraft.server.level.ServerChunkCache.getChunk", "waiting for a chunk to load or generate"),
            new StackPattern("net.minecraft.world.level.Level.getChunk", "reading a chunk from the level"),
            new StackPattern("net.minecraft.world.level.chunk.storage", "reading or writing chunk storage"),
            new StackPattern("net.minecraft.world.level.levelgen", "running terrain generation"),
            new StackPattern("net.minecraft.world.entity.Entity.move", "processing entity movement or collision"),
            new StackPattern("java.util.concurrent.CompletableFuture", "waiting on asynchronous work"),
            new StackPattern("java.util.concurrent.locks.LockSupport.park", "parked while waiting for a lock or future"),
            new StackPattern("java.lang.Object.wait", "blocked in a monitor wait"));

    private final DedicatedServer server;
    private final long timeoutNanos;
    private final boolean restartOnCrash;
    private final long earlyWarningEveryNanos;
    private final long earlyWarningDelayNanos;
    private long lastEarlyWarningNanos;
    private boolean crashing;

    private FandWatchdog(final DedicatedServer server, final FandConfig.Watchdog config) {
        this.server = server;
        this.timeoutNanos = TimeUnit.SECONDS.toNanos(config.timeoutSeconds);
        this.restartOnCrash = config.restartOnCrash;
        this.earlyWarningEveryNanos = TimeUnit.MILLISECONDS.toNanos(config.earlyWarningEveryMillis);
        this.earlyWarningDelayNanos = TimeUnit.MILLISECONDS.toNanos(config.earlyWarningDelayMillis);
    }

    public static void start(final DedicatedServer server) {
        final FandConfig.Watchdog config = Main.runtime().config().watchdog;
        if (config.timeoutSeconds <= 0) {
            LOGGER.info("Fand watchdog disabled by configuration");
            return;
        }
        final Thread watchdog = new Thread(new FandWatchdog(server, config), "Fand Watchdog");
        watchdog.setUncaughtExceptionHandler(new DefaultUncaughtExceptionHandlerWithName(LOGGER));
        watchdog.setDaemon(true);
        watchdog.start();
        LOGGER.info(
                "Fand watchdog started: timeout={}s, early-warning-delay={}ms, early-warning-every={}ms",
                config.timeoutSeconds,
                config.earlyWarningDelayMillis,
                config.earlyWarningEveryMillis);
    }

    @Override
    public void run() {
        while (this.server.isRunning()) {
            final long nextTickTimeNanos = this.server.getNextTickTime();
            final long currentTimeNanos = Util.getNanos();
            final long deltaNanos = currentTimeNanos - nextTickTimeNanos;
            if (deltaNanos > this.timeoutNanos) {
                this.crash(deltaNanos);
            }
            this.warnIfStalled(deltaNanos, currentTimeNanos);
            this.sleep();
        }
    }

    private void warnIfStalled(final long deltaNanos, final long currentTimeNanos) {
        if (this.earlyWarningEveryNanos <= 0L || deltaNanos < this.earlyWarningDelayNanos) {
            return;
        }
        if (currentTimeNanos - this.lastEarlyWarningNanos < this.earlyWarningEveryNanos) {
            return;
        }
        this.lastEarlyWarningNanos = currentTimeNanos;
        this.logTargetedDiagnostics(deltaNanos, false);
    }

    private void crash(final long deltaNanos) {
        if (this.crashing) {
            return;
        }
        this.crashing = true;
        LOGGER.error(
                LogUtils.FATAL_MARKER,
                "A single server tick took {} seconds (Fand watchdog timeout: {})",
                String.format(Locale.ROOT, "%.2f", (float) deltaNanos / (float) TimeUtil.NANOSECONDS_PER_SECOND),
                String.format(Locale.ROOT, "%.2f", (float) this.timeoutNanos / (float) TimeUtil.NANOSECONDS_PER_SECOND));
        LOGGER.error(LogUtils.FATAL_MARKER, "Considering it to be crashed; Fand will shut down the server.");
        this.logTargetedDiagnostics(deltaNanos, true);
        final CrashReport report = this.createCrashReport("Fand Watchdog");
        this.server.fillSystemReport(report.getSystemReport());
        final CrashReportCategory serverStats = report.addCategory("Performance stats");
        serverStats.setDetail("Random tick rate", () -> this.server.getGameRules().getAsString(GameRules.RANDOM_TICK_SPEED));
        serverStats.setDetail(
                "Level stats",
                () -> Streams.stream(this.server.getAllLevels())
                        .map(level -> level.dimension().identifier() + ": " + level.getWatchdogStats())
                        .collect(Collectors.joining(",\n")));
        final Path file = this.server.getServerDirectory()
                .resolve("crash-reports")
                .resolve("crash-" + Util.getFilenameFormattedDateTime() + "-server.txt");
        if (report.saveToFile(file, ReportType.CRASH)) {
            LOGGER.error("This crash report has been saved to: {}", file.toAbsolutePath());
        } else {
            LOGGER.error("We were unable to save this crash report to disk.");
        }
        this.exit();
    }

    private void logTargetedDiagnostics(final long deltaNanos, final boolean finalTimeout) {
        final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        final long mainThreadId = this.server.getRunningThread().threadId();
        final ThreadInfo mainThread = threadMXBean.getThreadInfo(mainThreadId, Integer.MAX_VALUE);
        final String elapsed = formatSeconds(deltaNanos);
        LOGGER.error("------------------------------");
        if (finalTimeout) {
            LOGGER.error("Fand watchdog final timeout after {}s without a completed server tick.", elapsed);
        } else {
            LOGGER.warn(
                    "Fand watchdog early warning: server thread has not completed a tick for {}s. This is not a crash yet.",
                    elapsed);
        }
        LOGGER.error(
                "Server: Fand {} for Minecraft {}, target MSPT={}, average MSPT={}, players={}, worlds={}",
                safeVersion(),
                safeMinecraftVersion(),
                formatMilliseconds((long) (this.server.tickRateManager().millisecondsPerTick()
                        * TimeUtil.NANOSECONDS_PER_MILLISECOND)),
                formatMilliseconds(this.server.getAverageTickTimeNanos()),
                this.server.getPlayerList().getPlayerCount(),
                Streams.stream(this.server.getAllLevels()).count());
        LOGGER.error("Tick state: sprinting={}, frozen={}, thread={}", this.server.tickRateManager().isSprinting(),
                this.server.tickRateManager().isFrozen(), this.server.getRunningThread().getName());
        LOGGER.error("Likely stall source: {}", classify(mainThread));
        LOGGER.error("Thread states: {}", threadStateSummary());
        LOGGER.error("------------------------------");
        LOGGER.error("Server thread dump:");
        LOGGER.error("{}", formatThread(mainThread, finalTimeout ? CRASH_STACK_FRAMES : WARNING_STACK_FRAMES));
        this.logFandWorkerThreads(threadMXBean, mainThreadId, finalTimeout);
        if (!finalTimeout) {
            LOGGER.warn("Fand watchdog will print another targeted dump in {} ms if the same tick keeps stalling.",
                    this.earlyWarningEveryNanos / TimeUtil.NANOSECONDS_PER_MILLISECOND);
        }
        LOGGER.error("------------------------------");
    }

    private void logFandWorkerThreads(final ThreadMXBean threadMXBean, final long mainThreadId, final boolean finalTimeout) {
        final List<Thread> threads = diagnosticThreads(mainThreadId);
        if (threads.isEmpty()) {
            return;
        }
        final int limit = finalTimeout ? MAX_CRASH_WORKER_DUMPS : MAX_WARNING_WORKER_DUMPS;
        LOGGER.error("Fand worker thread dumps (showing {} of {} selected workers):", Math.min(limit, threads.size()), threads.size());
        int dumped = 0;
        for (final Thread thread : threads) {
            if (dumped >= limit) {
                break;
            }
            final ThreadInfo threadInfo = threadMXBean.getThreadInfo(thread.threadId(), Integer.MAX_VALUE);
            if (threadInfo == null) {
                continue;
            }
            LOGGER.error("{}", formatThread(threadInfo, finalTimeout ? CRASH_STACK_FRAMES : WARNING_STACK_FRAMES));
            dumped++;
        }
        if (threads.size() > limit) {
            LOGGER.error("Skipped {} additional Fand worker threads to avoid flooding the console.", threads.size() - limit);
        }
    }

    private CrashReport createCrashReport(final String message) {
        final long mainThreadId = this.server.getRunningThread().threadId();
        final ThreadInfo[] threadInfos = Util.dumpThreadInfo();
        Arrays.sort(threadInfos, THREAD_INFO_COMPARATOR);
        final StringBuilder builder = new StringBuilder();
        final Error exception = new Error("Watchdog (" + message + ")");

        for (final ThreadInfo threadInfo : threadInfos) {
            if (threadInfo.getThreadId() == mainThreadId) {
                exception.setStackTrace(threadInfo.getStackTrace());
            }
            builder.append('\n').append(threadInfo);
        }

        final CrashReport report = new CrashReport(message, exception);
        final CrashReportCategory summary = report.addCategory("Fand Watchdog Summary");
        summary.setDetail("Stall classification", () -> classify(findThreadInfo(threadInfos, mainThreadId)));
        summary.setDetail("Thread state summary", FandWatchdog::threadStateSummary);
        summary.setDetail("Console dump policy", "Console prints the server thread plus selected Fand worker threads; this file keeps the full JVM dump.");
        final CrashReportCategory threadDump = report.addCategory("Thread Dump");
        threadDump.setDetail("Threads", builder);
        return report;
    }

    private static ThreadInfo findThreadInfo(final ThreadInfo[] threads, final long threadId) {
        for (final ThreadInfo thread : threads) {
            if (thread.getThreadId() == threadId) {
                return thread;
            }
        }
        return null;
    }

    private static List<Thread> diagnosticThreads(final long mainThreadId) {
        final List<Thread> threads = new ArrayList<>();
        for (final Thread thread : Thread.getAllStackTraces().keySet()) {
            if (thread.threadId() == mainThreadId || thread == Thread.currentThread()) {
                continue;
            }
            if (isFandDiagnosticThread(thread.getName())) {
                threads.add(thread);
            }
        }
        threads.sort(Comparator
                .comparingInt((Thread thread) -> diagnosticPriority(thread.getName()))
                .thenComparing(thread -> thread.getState().ordinal())
                .thenComparing(Thread::getName));
        return threads;
    }

    private static boolean isFandDiagnosticThread(final String name) {
        return name.startsWith("Fand Chunk ")
                || name.startsWith("Fand Async Chunk Packet")
                || name.startsWith("Fand Region Worker")
                || name.startsWith("Fand Light")
                || name.startsWith("Fand Scheduler")
                || name.startsWith("Fand Performance")
                || name.contains("Worldgen")
                || name.contains("Chunk");
    }

    private static int diagnosticPriority(final String name) {
        if (name.contains("Worldgen") || name.contains("Chunk Worldgen")) {
            return 0;
        }
        if (name.contains("Async Chunk Packet")) {
            return 1;
        }
        if (name.contains("Region Worker")) {
            return 2;
        }
        if (name.contains("Light")) {
            return 3;
        }
        if (name.contains("Scheduler")) {
            return 4;
        }
        return 5;
    }

    private static String threadStateSummary() {
        final Map<Thread.State, Integer> states = new EnumMap<>(Thread.State.class);
        int fandThreads = 0;
        int liveThreads = 0;
        for (final Thread thread : Thread.getAllStackTraces().keySet()) {
            liveThreads++;
            states.merge(thread.getState(), 1, Integer::sum);
            if (thread.getName().startsWith("Fand ")) {
                fandThreads++;
            }
        }
        final StringBuilder builder = new StringBuilder("live=").append(liveThreads).append(", fand=").append(fandThreads);
        for (final Thread.State state : Thread.State.values()) {
            final Integer count = states.get(state);
            if (count != null) {
                builder.append(", ").append(state).append('=').append(count);
            }
        }
        return builder.toString();
    }

    private static String formatThread(final ThreadInfo thread, final int maxFrames) {
        if (thread == null) {
            return "<thread disappeared before it could be dumped>";
        }
        final StringBuilder builder = new StringBuilder(512);
        builder.append('"')
                .append(thread.getThreadName())
                .append("\" id=")
                .append(thread.getThreadId())
                .append(" state=")
                .append(thread.getThreadState());
        if (thread.getLockName() != null) {
            builder.append(" on ").append(thread.getLockName());
        }
        if (thread.getLockOwnerName() != null) {
            builder.append(" owned by \"")
                    .append(thread.getLockOwnerName())
                    .append("\" id=")
                    .append(thread.getLockOwnerId());
        }
        builder.append('\n');
        final StackTraceElement[] stack = thread.getStackTrace();
        final int frameLimit = Math.min(stack.length, maxFrames);
        for (int index = 0; index < frameLimit; index++) {
            builder.append("\tat ").append(stack[index]).append('\n');
        }
        if (stack.length > frameLimit) {
            builder.append("\t... ").append(stack.length - frameLimit).append(" more frames omitted\n");
        }
        return builder.toString();
    }

    private static String classify(final ThreadInfo thread) {
        if (thread == null) {
            return "server thread disappeared while dumping";
        }
        for (final StackTraceElement frame : thread.getStackTrace()) {
            final String text = frame.getClassName() + "." + frame.getMethodName();
            for (final StackPattern pattern : STACK_PATTERNS) {
                if (text.contains(pattern.needle())) {
                    return pattern.description() + " (" + frame + ")";
                }
            }
        }
        final StackTraceElement[] stack = thread.getStackTrace();
        return stack.length == 0 ? "server thread has no Java stack" : "top frame: " + stack[0];
    }

    private record StackPattern(String needle, String description) {}

    private static String safeVersion() {
        try {
            return Main.runtime().version();
        } catch (final RuntimeException ignored) {
            return "unknown";
        }
    }

    private static String safeMinecraftVersion() {
        try {
            return Main.runtime().minecraftVersion();
        } catch (final RuntimeException ignored) {
            return "unknown";
        }
    }

    private static String formatSeconds(final long nanos) {
        return String.format(Locale.ROOT, "%.2f", (float) nanos / (float) TimeUtil.NANOSECONDS_PER_SECOND);
    }

    private static String formatMilliseconds(final long nanos) {
        return String.format(Locale.ROOT, "%.2f", (float) nanos / (float) TimeUtil.NANOSECONDS_PER_MILLISECOND);
    }

    private void exit() {
        if (!this.restartOnCrash) {
            Runtime.getRuntime().halt(SHUTDOWN_STATUS);
        }
        try {
            final Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    Runtime.getRuntime().halt(SHUTDOWN_STATUS);
                }
            }, MAX_SHUTDOWN_TIME_MILLIS);
            System.exit(SHUTDOWN_STATUS);
        } catch (final Throwable ignored) {
            Runtime.getRuntime().halt(SHUTDOWN_STATUS);
        }
    }

    private void sleep() {
        try {
            Thread.sleep(CHECK_INTERVAL_MILLIS);
        } catch (final InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}
