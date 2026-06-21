package io.fand.server.block;

import io.fand.api.block.BlockPhysics;
import io.fand.api.block.BlockType;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.key.Key;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;

public final class FandBlockType implements BlockType {

    private static final ConcurrentHashMap<Block, FandBlockType> CACHE = new ConcurrentHashMap<>();

    private final Block handle;
    private final Key key;

    private FandBlockType(Block handle) {
        this.handle = handle;
        var id = handle.builtInRegistryHolder().key().identifier();
        this.key = Key.key(id.getNamespace(), id.getPath());
    }

    public static FandBlockType of(Block handle) {
        var existing = CACHE.get(handle);
        return existing != null ? existing : CACHE.computeIfAbsent(handle, FandBlockType::new);
    }

    public Block handle() {
        return handle;
    }

    @Override
    public BlockPhysics physics() {
        var state = handle.defaultBlockState();
        return new BlockPhysics(
                state.getDestroySpeed(EmptyBlockGetter.INSTANCE, BlockPos.ZERO),
                handle.getExplosionResistance(),
                state.getLightEmission(),
                state.getLightDampening(),
                handle.getFriction(),
                handle.getSpeedFactor(),
                handle.getJumpFactor(),
                state.isSolid(),
                state.liquid(),
                state.canBeReplaced(),
                state.ignitedByLava(),
                state.isAir(),
                state.requiresCorrectToolForDrops(),
                state.hasBlockEntity(),
                true,
                state.isRedstoneConductor(EmptyBlockGetter.INSTANCE, BlockPos.ZERO));
    }

    public static Block unwrap(BlockType type) {
        if (type instanceof FandBlockType fand) {
            return fand.handle;
        }
        throw new IllegalArgumentException("Block type is not owned by this server: " + type.key().asString());
    }

    @Override
    public Key key() {
        return key;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof FandBlockType that && this.handle == that.handle;
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public String toString() {
        return "FandBlockType(" + key.asString() + ")";
    }
}
