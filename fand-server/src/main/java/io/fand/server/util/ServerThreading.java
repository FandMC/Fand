package io.fand.server.util;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Supplier;
import net.minecraft.server.MinecraftServer;
import org.jspecify.annotations.Nullable;

public final class ServerThreading {

    private ServerThreading() {
    }

    public static IllegalStateException serverStopping(RejectedExecutionException cause) {
        return new IllegalStateException("Minecraft server is stopping", cause);
    }

    public static IllegalStateException serverStopping() {
        return new IllegalStateException("Minecraft server is stopping");
    }

    public static boolean run(@Nullable MinecraftServer server, Runnable task) {
        Objects.requireNonNull(task, "task");
        if (server == null || server.isSameThread()) {
            task.run();
            return true;
        }
        try {
            server.executeIfPossible(task);
            return true;
        } catch (RejectedExecutionException ignored) {
            return false;
        }
    }

    public static CompletableFuture<Void> runFuture(@Nullable MinecraftServer server, Runnable task) {
        Objects.requireNonNull(task, "task");
        return callFuture(server, () -> {
            task.run();
            return null;
        });
    }

    public static <T> CompletableFuture<T> callFuture(@Nullable MinecraftServer server, Supplier<T> task) {
        Objects.requireNonNull(task, "task");
        if (server == null || server.isSameThread()) {
            try {
                return CompletableFuture.completedFuture(task.get());
            } catch (Throwable failure) {
                return CompletableFuture.failedFuture(failure);
            }
        }
        var future = new CompletableFuture<T>();
        try {
            server.executeIfPossible(() -> {
                try {
                    future.complete(task.get());
                } catch (Throwable failure) {
                    future.completeExceptionally(failure);
                }
            });
        } catch (RejectedExecutionException failure) {
            future.completeExceptionally(serverStopping(failure));
        }
        return future;
    }

    public static <T> T callBlocking(@Nullable MinecraftServer server, Supplier<T> task) {
        Objects.requireNonNull(task, "task");
        if (server == null || server.isSameThread()) {
            return task.get();
        }
        var future = new CompletableFuture<T>();
        try {
            server.executeIfPossible(() -> {
                try {
                    future.complete(task.get());
                } catch (Throwable failure) {
                    future.completeExceptionally(failure);
                }
            });
        } catch (RejectedExecutionException failure) {
            throw serverStopping(failure);
        }
        try {
            return future.join();
        } catch (CompletionException failure) {
            throw failure;
        }
    }
}
