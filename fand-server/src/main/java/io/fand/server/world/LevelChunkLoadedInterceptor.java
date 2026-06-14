package io.fand.server.world;

import java.util.concurrent.Callable;
import net.bytebuddy.implementation.bind.annotation.Argument;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;

public final class LevelChunkLoadedInterceptor {

    private LevelChunkLoadedInterceptor() {}

    public static void intercept(
            @This Object chunk,
            @Argument(0) boolean loaded,
            @SuperCall Callable<Void> original
    ) throws Exception {
        original.call();
        if (!loaded) {
            LastAccessedChunkCache.getInstance().invalidateChunk(chunk);
        }
    }
}
