package io.fand.api.storage;

/** Per-plugin persistent gameplay data stores. */
public interface PluginStorage {

    ScopedStorage global();

    ScopedStorage player(java.util.UUID playerId);

    ScopedStorage world(net.kyori.adventure.key.Key world);

    ScopedStorage chunk(net.kyori.adventure.key.Key world, int chunkX, int chunkZ);

    ScopedStorage block(net.kyori.adventure.key.Key world, int x, int y, int z);

    default ScopedStorage block(io.fand.api.block.Block block) {
        java.util.Objects.requireNonNull(block, "block");
        return block(block.world().key(), block.x(), block.y(), block.z());
    }
}
