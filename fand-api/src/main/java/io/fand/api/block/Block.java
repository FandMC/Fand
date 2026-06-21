package io.fand.api.block;

import io.fand.api.component.DataComponentContainer;
import io.fand.api.component.DataComponentMap;
import io.fand.api.event.block.BlockFace;
import io.fand.api.item.ItemStack;
import io.fand.api.world.World;
import java.util.List;
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

    /** Current fluid occupying this position, or {@code minecraft:empty}. */
    default FluidType fluid() {
        return fluidState().type();
    }

    /** Current fluid state occupying this position. */
    default FluidState fluidState() {
        return FluidState.none();
    }

    default boolean water() {
        return fluidState().water();
    }

    default boolean lava() {
        return fluidState().lava();
    }

    default boolean sourceFluid() {
        return fluidState().source();
    }

    default boolean flowingFluid() {
        return fluidState().flowing();
    }

    default boolean fullFluid() {
        return fluidState().full();
    }

    default boolean setFluid(FluidType fluid) {
        return setFluid(fluid, true);
    }

    default boolean setFluid(FluidType fluid, boolean source) {
        java.util.Objects.requireNonNull(fluid, "fluid");
        return false;
    }

    default boolean setFluid(FluidState state) {
        java.util.Objects.requireNonNull(state, "state");
        return setFluid(state.type(), state.source());
    }

    default boolean clearFluid() {
        return setFluid(FluidTypes.EMPTY);
    }

    /** Physical attributes for the live block state at this position. */
    default BlockPhysics physics() {
        return type().physics();
    }

    /** Whether the current block is vanilla air. */
    default boolean air() {
        return physics().air();
    }

    default boolean solid() {
        return physics().solid();
    }

    default boolean fluidBlock() {
        return physics().fluid();
    }

    default boolean replaceable() {
        return physics().replaceable();
    }

    default boolean flammable() {
        return physics().flammable();
    }

    default boolean requiresTool() {
        return physics().requiresTool();
    }

    default boolean hasBlockEntity() {
        return physics().hasBlockEntity();
    }

    default boolean canSurvive() {
        return physics().canSurvive();
    }

    default boolean redstoneConductor() {
        return physics().redstoneConductor();
    }

    default float hardness() {
        return physics().hardness();
    }

    default float destroySpeed() {
        return physics().destroySpeed();
    }

    default float blastResistance() {
        return physics().blastResistance();
    }

    default int lightEmission() {
        return physics().lightEmission();
    }

    default int lightLimit() {
        return physics().lightLimit();
    }

    default float friction() {
        return physics().friction();
    }

    default float speedFactor() {
        return physics().speedFactor();
    }

    default float jumpFactor() {
        return physics().jumpFactor();
    }

    default Block relative(BlockFace face) {
        return switch (java.util.Objects.requireNonNull(face, "face")) {
            case DOWN -> relative(0, -1, 0);
            case UP -> relative(0, 1, 0);
            case NORTH -> relative(0, 0, -1);
            case SOUTH -> relative(0, 0, 1);
            case WEST -> relative(-1, 0, 0);
            case EAST -> relative(1, 0, 0);
        };
    }

    default Block relative(int dx, int dy, int dz) {
        return world().blockAt(
                Math.addExact(x(), dx),
                Math.addExact(y(), dy),
                Math.addExact(z(), dz));
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

    /** Drops produced by breaking this block without a tool or breaker. */
    default List<ItemStack> drops() {
        return drops(ItemStack.EMPTY);
    }

    /** Drops produced by breaking this block with {@code tool}. */
    default List<ItemStack> drops(ItemStack tool) {
        java.util.Objects.requireNonNull(tool, "tool");
        return List.of();
    }

    /** Breaks this block and spawns vanilla drops. */
    default boolean breakNaturally() {
        return breakNaturally(true);
    }

    /** Breaks this block, optionally spawning vanilla drops. */
    default boolean breakNaturally(boolean dropItems) {
        return false;
    }

    /** Sends a neighbour physics update for this position. */
    default void applyPhysics() {
    }

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
