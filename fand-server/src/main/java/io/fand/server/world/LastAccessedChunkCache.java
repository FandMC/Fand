package io.fand.server.world;

import java.lang.ref.WeakReference;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jspecify.annotations.Nullable;

public final class LastAccessedChunkCache {

    private static final LastAccessedChunkCache INSTANCE = new LastAccessedChunkCache();

    public static LastAccessedChunkCache getInstance() {
        return INSTANCE;
    }

    private final ThreadLocal<Entry> entries = ThreadLocal.withInitial(Entry::new);

    private LastAccessedChunkCache() {}

    @Nullable
    LevelChunk getIfLoaded(Level level, int chunkX, int chunkZ) {
        var entry = entries.get();
        var cached = entry.get(level, chunkX, chunkZ);
        if (cached != null) {
            return cached;
        }
        var chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
        if (chunk != null) {
            entry.update(level, chunkX, chunkZ, chunk);
        }
        return chunk;
    }

    LevelChunk getOrLoad(Level level, int chunkX, int chunkZ) {
        var entry = entries.get();
        var cached = entry.get(level, chunkX, chunkZ);
        if (cached != null) {
            return cached;
        }
        var chunk = level.getChunk(chunkX, chunkZ);
        entry.update(level, chunkX, chunkZ, chunk);
        return chunk;
    }

    boolean isLoaded(Level level, int chunkX, int chunkZ) {
        return getIfLoaded(level, chunkX, chunkZ) != null;
    }

    void remember(Level level, int chunkX, int chunkZ, @Nullable ChunkAccess chunk) {
        if (chunk instanceof LevelChunk levelChunk) {
            update(level, chunkX, chunkZ, levelChunk);
        }
    }

    void update(Level level, int chunkX, int chunkZ, LevelChunk chunk) {
        entries.get().update(level, chunkX, chunkZ, chunk);
    }

    void invalidate(Level level, int chunkX, int chunkZ) {
        entries.get().clearIf(level, chunkX, chunkZ);
    }

    void invalidate(LevelChunk chunk) {
        invalidateChunk(chunk);
    }

    void invalidateChunk(Object chunk) {
        entries.get().clearIfChunk(chunk);
    }

    private static final class Entry {

        private @Nullable WeakReference<Level> level;
        private @Nullable WeakReference<LevelChunk> chunk;
        private int chunkX;
        private int chunkZ;

        private @Nullable LevelChunk get(Level level, int chunkX, int chunkZ) {
            if (this.chunkX != chunkX || this.chunkZ != chunkZ) {
                return null;
            }
            var levelRef = this.level;
            var chunkRef = this.chunk;
            if (levelRef == null || chunkRef == null) {
                return null;
            }
            var cachedLevel = levelRef.get();
            var cachedChunk = chunkRef.get();
            if (cachedLevel == null || cachedChunk == null) {
                clear();
                return null;
            }
            return cachedLevel == level ? cachedChunk : null;
        }

        private void update(Level level, int chunkX, int chunkZ, LevelChunk chunk) {
            this.level = new WeakReference<>(level);
            this.chunk = new WeakReference<>(chunk);
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
        }

        private void clearIf(Level level, int chunkX, int chunkZ) {
            if (this.chunkX == chunkX && this.chunkZ == chunkZ && matchesLevel(level)) {
                clear();
            }
        }

        private void clearIfChunk(Object chunk) {
            var chunkRef = this.chunk;
            if (chunkRef != null && chunkRef.get() == chunk) {
                clear();
            }
        }

        private boolean matchesLevel(Level level) {
            var levelRef = this.level;
            return levelRef != null && levelRef.get() == level;
        }

        private void clear() {
            level = null;
            chunk = null;
            chunkX = 0;
            chunkZ = 0;
        }
    }
}
