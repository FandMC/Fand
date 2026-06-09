package io.fand.api.block;

import io.fand.api.component.DataComponentContainer;
import io.fand.api.component.DataComponentMap;
import io.fand.api.world.World;
import java.util.Map;
import java.util.Optional;

/**
 * A positional block handle within a {@link World}. Instances are thin handles —
 * {@link #type()} and {@link #setType(BlockType)} read or mutate the live world.
 *
 * <p>Reads and writes are resolved on the server thread by the implementation.
 * Equality is by world key plus integer block coordinates.
 */
public interface Block {

    World world();

    int x();

    int y();

    int z();

    /** Current block type at this position. */
    BlockType type();

    /** Whether the current block is vanilla air. */
    default boolean air() {
        return type().key().asString().equals("minecraft:air");
    }

    /** Current vanilla block-state properties as {@code propertyName -> valueName}. */
    default Map<String, String> stateProperties() {
        return Map.of();
    }

    /** Reads one vanilla block-state property by name. */
    default Optional<String> stateProperty(String name) {
        return Optional.ofNullable(stateProperties().get(name));
    }

    /**
     * Sets one vanilla block-state property by its string value.
     *
     * @return false when the property or value does not exist for this block state
     */
    default boolean setStateProperty(String name, String value) {
        return false;
    }

    /** Live block entity at this position, when the current block has one. */
    default Optional<? extends BlockEntity> blockEntity() {
        return Optional.empty();
    }

    /**
     * Replaces the block at this position with {@code type}'s default state and
     * triggers neighbour updates as if a player had placed the block. Returns
     * {@code true} if the world accepted the change.
     */
    boolean setType(BlockType type);

    /**
     * Replaces this block and stores persistent Fand components for the new
     * block state as one operation.
     *
     * <p>Off-thread calls marshal to the server thread like {@link #setType(BlockType)}.
     */
    boolean setType(BlockType type, DataComponentMap components);

    /**
     * Persistent Fand components attached to this block position.
     *
     * <p>The returned container is live and backed by world save data. Component
     * reads and writes must happen on the server thread. Data is cleared when
     * the block is replaced through Fand APIs or player-driven break/place
     * events.
     */
    DataComponentContainer components();
}
