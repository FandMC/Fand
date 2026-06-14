package io.fand.server.world;

import java.util.concurrent.Callable;
import net.bytebuddy.implementation.bind.annotation.Argument;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;

public final class LevelChunkInterceptor {

    private LevelChunkInterceptor() {}

    @RuntimeType
    public static ChunkAccess intercept(
            @This Level level,
            @Argument(0) int chunkX,
            @Argument(1) int chunkZ,
            @Argument(2) ChunkStatus status,
            @Argument(3) boolean load,
            @SuperCall Callable<ChunkAccess> original
    ) throws Exception {
        var cache = LastAccessedChunkCache.getInstance();
        var server = level.getServer();
        if (server != null && server.isSameThread() && status == ChunkStatus.FULL) {
            var cached = cache.getIfLoaded(level, chunkX, chunkZ);
            if (cached != null) {
                return cached;
            }
        }

        var chunk = original.call();
        if (chunk instanceof LevelChunk levelChunk && status == ChunkStatus.FULL) {
            cache.update(level, chunkX, chunkZ, levelChunk);
        }
        return chunk;
    }
}
