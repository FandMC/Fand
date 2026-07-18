package io.fand.api.block.custom;

import io.fand.api.block.Block;
import io.fand.api.component.DataComponentMap;
import io.fand.api.world.World;
import java.util.Collection;
import java.util.Optional;
import net.kyori.adventure.key.Key;

/** Custom-block registry and lifecycle service. */
public interface CustomBlockRegistry {

    CustomBlockRegistration register(CustomBlockType type);

    CustomBlockRegistration register(CustomBlockType type, CustomBlockListener listener);

    default CustomBlockRegistration register(
            Key id,
            io.fand.api.block.BlockType baseType,
            DataComponentMap defaultComponents,
            boolean ticking
    ) {
        return register(new CustomBlockType(id, baseType, defaultComponents, ticking));
    }

    Optional<CustomBlockType> type(Key id);

    Optional<CustomBlockType> customBlock(Block block);

    Optional<CustomBlockType> blockForItem(Key itemId);

    /** Point-in-time snapshot of all registered custom-block types; immutable. */
    Collection<CustomBlockType> types();

    /**
     * Binds a custom item to placement of a custom block and makes the bound
     * item eligible as that block's default player-break drop.
     */
    CustomBlockItemBinding bindItem(Key itemId, Key blockId);

    void unbindItem(Key itemId);

    boolean place(Block block, Key id);

    boolean place(Block block, Key id, DataComponentMap components);

    default boolean place(Block block, CustomBlockType type) {
        return place(block, type.id());
    }

    default boolean place(Block block, CustomBlockType type, DataComponentMap components) {
        return place(block, type.id(), components);
    }

    boolean remove(Block block);

    /** Point-in-time snapshot of custom blocks in the given chunk; immutable. */
    Collection<Block> customBlocks(World world, int chunkX, int chunkZ);

    /** Point-in-time snapshot of ticking custom blocks in the given chunk; immutable. */
    Collection<Block> tickingBlocks(World world, int chunkX, int chunkZ);
}
