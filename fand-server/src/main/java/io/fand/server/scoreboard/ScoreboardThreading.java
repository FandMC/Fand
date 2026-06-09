package io.fand.server.scoreboard;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.minecraft.server.MinecraftServer;

final class ScoreboardThreading {

    private ScoreboardThreading() {
    }

    static void run(MinecraftServer server, Runnable task) {
        if (server.isSameThread()) {
            task.run();
            return;
        }
        server.submit(() -> {
            task.run();
            return null;
        }).join();
    }

    static <T> T call(MinecraftServer server, Supplier<T> task) {
        if (server.isSameThread()) {
            return task.get();
        }
        return server.submit(task::get).join();
    }

    static CompletableFuture<Void> runAsync(MinecraftServer server, Runnable task) {
        if (server.isSameThread()) {
            task.run();
            return CompletableFuture.completedFuture(null);
        }
        return server.submit(() -> {
            task.run();
            return null;
        });
    }
}
