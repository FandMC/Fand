package io.fand.server.world;

import io.fand.api.block.BlockType;
import io.fand.api.world.BlockUpdateMode;
import io.fand.api.world.GeneratedChunk;
import io.fand.server.block.FandBlockType;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.ChunkAccess;

final class FandGeneratedChunk implements GeneratedChunk {

    private final ChunkAccess handle;
    private final BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

    FandGeneratedChunk(ChunkAccess handle) {
        this.handle = Objects.requireNonNull(handle, "handle");
    }

    @Override
    public int chunkX() {
        return handle.getPos().x();
    }

    @Override
    public int chunkZ() {
        return handle.getPos().z();
    }

    @Override
    public int minY() {
        return handle.getMinY();
    }

    @Override
    public int maxY() {
        return handle.getMaxY();
    }

    @Override
    public void setBlock(int x, int y, int z, BlockType type, BlockUpdateMode updateMode) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(updateMode, "updateMode");
        if ((x >> 4) != chunkX() || (z >> 4) != chunkZ()) {
            throw new IllegalArgumentException("Generated block is outside chunk " + chunkX() + "," + chunkZ());
        }
        if (y < handle.getMinY() || y > handle.getMaxY()) {
            throw new IllegalArgumentException("Generated block y is outside world height: " + y);
        }
        handle.setBlockState(mutablePos.set(x, y, z), FandBlockType.unwrap(type).defaultBlockState(), updateFlags(updateMode));
    }

    private static int updateFlags(BlockUpdateMode mode) {
        return switch (mode) {
            case NORMAL -> Block.UPDATE_ALL;
            case CLIENTS_ONLY -> Block.UPDATE_CLIENTS;
            case SILENT -> Block.UPDATE_NONE;
        };
    }
}
