package io.fand.server.block;

import io.fand.api.block.Block;
import io.fand.api.block.BlockType;
import io.fand.api.world.World;
import io.fand.server.world.FandWorld;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public final class FandBlock implements Block {

    private final FandWorld world;
    private final BlockPos pos;

    public FandBlock(FandWorld world, int x, int y, int z) {
        this.world = world;
        this.pos = new BlockPos(x, y, z);
    }

    @Override
    public World world() {
        return world;
    }

    @Override
    public int x() {
        return pos.getX();
    }

    @Override
    public int y() {
        return pos.getY();
    }

    @Override
    public int z() {
        return pos.getZ();
    }

    @Override
    public BlockType type() {
        ServerLevel level = world.handle();
        requireMainThread(level, "Block.type() must be read on the server thread");
        var state = level.getBlockState(pos);
        return FandBlockType.of(state.getBlock());
    }

    @Override
    public boolean setType(BlockType type) {
        Objects.requireNonNull(type, "type");
        if (!(type instanceof FandBlockType fandType)) {
            throw new IllegalArgumentException("Block type must be obtained from BlockTypes / Server.blockType");
        }
        ServerLevel level = world.handle();
        var server = level.getServer();
        Runnable run = () -> level.setBlockAndUpdate(pos, fandType.handle().defaultBlockState());
        if (server == null || server.isSameThread()) {
            run.run();
        } else {
            server.executeIfPossible(run);
        }
        return true;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof FandBlock that
                && this.world.key().equals(that.world.key())
                && this.pos.equals(that.pos);
    }

    @Override
    public int hashCode() {
        return 31 * world.key().hashCode() + pos.hashCode();
    }

    @Override
    public String toString() {
        return "FandBlock(" + world.key().asString() + " " + pos.toShortString() + ")";
    }

    private static void requireMainThread(ServerLevel level, String message) {
        var server = level.getServer();
        if (server != null && !server.isSameThread()) {
            throw new IllegalStateException(message);
        }
    }
}
