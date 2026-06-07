package io.fand.server.event;

import io.fand.api.event.block.BlockChangeEvent;
import io.fand.api.event.block.BlockPhysicsEvent;
import io.fand.server.block.FandBlock;
import io.fand.server.block.FandBlockType;
import io.fand.server.hooks.FandHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BlockEvents {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlockEvents.class);

    private BlockEvents() {
    }

    public static boolean fireChange(
            ServerLevel level,
            BlockPos pos,
            BlockState oldState,
            BlockState newState,
            int updateFlags
    ) {
        if (oldState.getBlock() == newState.getBlock()) {
            return true;
        }
        var bus = FandHooks.events();
        if (!bus.hasListeners(BlockChangeEvent.class)) {
            return true;
        }
        var world = FandHooks.wrapWorld(level);
        if (world == null) {
            return true;
        }
        var event = new BlockChangeEvent(
                new FandBlock(world, pos.getX(), pos.getY(), pos.getZ()),
                FandBlockType.of(oldState.getBlock()),
                FandBlockType.of(newState.getBlock()),
                updateFlags);
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("BlockChangeEvent listener failed", failure);
            return true;
        }
        return !event.cancelled();
    }

    public static boolean firePhysics(ServerLevel level, BlockPos pos, Block sourceBlock) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(BlockPhysicsEvent.class)) {
            return true;
        }
        var world = FandHooks.wrapWorld(level);
        if (world == null) {
            return true;
        }
        var event = new BlockPhysicsEvent(
                new FandBlock(world, pos.getX(), pos.getY(), pos.getZ()),
                FandBlockType.of(sourceBlock));
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("BlockPhysicsEvent listener failed", failure);
            return true;
        }
        return !event.cancelled();
    }
}
