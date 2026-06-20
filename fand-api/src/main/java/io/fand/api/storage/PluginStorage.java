package io.fand.api.storage;

/**
 * Per-plugin persistent gameplay data stores.
 *
 * <p><b>Threading:</b> scope accessors ({@code global()}, {@code player(...)},
 * etc.) may be called from any thread; each returned scope is independently
 * thread-safe (see {@link ScopedStorage}). {@link #flush()} persists all dirty
 * scopes and may block on file I/O — call it from the server thread or an
 * explicit async task, never from a hot event handler.
 */
public interface PluginStorage {

    ScopedStorage global();

    ScopedStorage player(java.util.UUID playerId);

    ScopedStorage entity(java.util.UUID entityId);

    default ScopedStorage entity(io.fand.api.entity.Entity entity) {
        java.util.Objects.requireNonNull(entity, "entity");
        return entity(entity.uniqueId());
    }

    ScopedStorage world(net.kyori.adventure.key.Key world);

    ScopedStorage chunk(net.kyori.adventure.key.Key world, int chunkX, int chunkZ);

    ScopedStorage block(net.kyori.adventure.key.Key world, int x, int y, int z);

    default ScopedStorage block(io.fand.api.block.Block block) {
        java.util.Objects.requireNonNull(block, "block");
        return block(block.world().key(), block.x(), block.y(), block.z());
    }

    /** Flushes every dirty scope owned by this plugin to disk. */
    default void flush() {
    }
}
