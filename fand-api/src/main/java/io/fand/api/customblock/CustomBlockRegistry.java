package io.fand.api.customblock;

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

    Optional<CustomBlockType> type(Key id);

    Optional<CustomBlockType> customBlock(Block block);

    Optional<CustomBlockType> blockForItem(Key itemId);

    /** Point-in-time snapshot of all registered custom-block types; immutable. */
    Collection<CustomBlockType> types();

    CustomBlockItemBinding bindItem(Key itemId, Key blockId);

    void unbindItem(Key itemId);

    boolean place(Block block, Key id);

    boolean place(Block block, Key id, DataComponentMap components);

    boolean remove(Block block);

    /** Point-in-time snapshot of custom blocks in the given chunk; immutable. */
    Collection<Block> customBlocks(World world, int chunkX, int chunkZ);

    /** Point-in-time snapshot of ticking custom blocks in the given chunk; immutable. */
    Collection<Block> tickingBlocks(World world, int chunkX, int chunkZ);
}
