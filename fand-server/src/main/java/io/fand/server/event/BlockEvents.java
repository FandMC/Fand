package io.fand.server.event;

import io.fand.api.event.block.BlockBurnEvent;
import io.fand.api.event.block.BlockChangeEvent;
import io.fand.api.event.block.BlockDispenseEvent;
import io.fand.api.event.block.BlockExplodeEvent;
import io.fand.api.event.block.BlockFadeEvent;
import io.fand.api.event.block.BlockFertilizeEvent;
import io.fand.api.event.block.BlockFormEvent;
import io.fand.api.event.block.BlockFace;
import io.fand.api.event.block.BlockFromToEvent;
import io.fand.api.event.block.BlockGrowEvent;
import io.fand.api.event.block.BlockIgniteEvent;
import io.fand.api.event.block.BlockPhysicsEvent;
import io.fand.api.event.block.BlockPistonExtendEvent;
import io.fand.api.event.block.BlockPistonPushEvent;
import io.fand.api.event.block.BlockPistonRetractEvent;
import io.fand.api.event.block.BlockRedstoneEvent;
import io.fand.api.event.block.BlockSpreadEvent;
import io.fand.api.event.block.CauldronLevelChangeEvent;
import io.fand.api.event.block.FluidFlowEvent;
import io.fand.api.event.block.LeavesDecayEvent;
import io.fand.api.event.block.PortalCreateEvent;
import io.fand.api.event.block.SignChangeEvent;
import io.fand.api.event.block.SpongeAbsorbEvent;
import io.fand.server.entity.FandPlayer;
import io.fand.server.block.FandBlock;
import io.fand.server.block.FandBlockType;
import io.fand.server.hooks.FandHooks;
import io.fand.server.item.FandItemStacks;
import io.fand.server.world.FandWorld;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.kyori.adventure.key.Key;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BlockEvents {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlockEvents.class);
    private static final ThreadLocal<IgniteContext> IGNITE_CONTEXT = new ThreadLocal<>();

    private BlockEvents() {
    }

    public static boolean withIgniteContext(BlockIgniteEvent.Cause cause, @Nullable BlockPos sourcePos, BooleanSupplier task) {
        IgniteContext previous = IGNITE_CONTEXT.get();
        IGNITE_CONTEXT.set(new IgniteContext(cause, sourcePos));
        try {
            return task.getAsBoolean();
        } finally {
            if (previous == null) {
                IGNITE_CONTEXT.remove();
            } else {
                IGNITE_CONTEXT.set(previous);
            }
        }
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
        IgniteContext igniteContext = IGNITE_CONTEXT.get();
        BlockIgniteEvent.Cause igniteCause = igniteContext == null ? BlockIgniteEvent.Cause.UNKNOWN : igniteContext.cause();
        BlockPos igniteSource = igniteContext == null ? null : igniteContext.sourcePos();
        if (!fireIgnite(level, pos, newState, igniteCause, igniteSource)) {
            return false;
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

    public static boolean fireBurn(ServerLevel level, BlockPos sourcePos, BlockPos pos, BlockState state) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(BlockBurnEvent.class)) {
            return true;
        }
        var world = FandHooks.wrapWorld(level);
        if (world == null) {
            return true;
        }
        var event = new BlockBurnEvent(
                block(world, pos),
                FandBlockType.of(state.getBlock()),
                block(world, sourcePos));
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("BlockBurnEvent listener failed", failure);
            return true;
        }
        return !event.cancelled();
    }

    public static net.minecraft.world.item.@Nullable ItemStack fireDispense(
            ServerLevel level,
            BlockPos pos,
            Direction direction,
            net.minecraft.world.item.ItemStack itemStack
    ) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(BlockDispenseEvent.class)) {
            return itemStack;
        }
        var world = FandHooks.wrapWorld(level);
        if (world == null) {
            return itemStack;
        }
        var event = new BlockDispenseEvent(
                block(world, pos),
                face(direction),
                FandItemStacks.fromVanilla(itemStack));
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("BlockDispenseEvent listener failed", failure);
            return itemStack;
        }
        if (event.cancelled() || event.item().isEmpty()) {
            return null;
        }
        try {
            return FandItemStacks.toVanilla(event.item());
        } catch (RuntimeException failure) {
            LOGGER.warn("BlockDispenseEvent supplied an invalid item stack", failure);
            return itemStack;
        }
    }

    public static boolean fireFade(ServerLevel level, BlockPos pos, BlockState oldState, BlockState newState, BlockFadeEvent.Cause cause) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(BlockFadeEvent.class)) {
            return true;
        }
        var world = FandHooks.wrapWorld(level);
        if (world == null) {
            return true;
        }
        var event = new BlockFadeEvent(
                block(world, pos),
                FandBlockType.of(oldState.getBlock()),
                FandBlockType.of(newState.getBlock()),
                cause);
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("BlockFadeEvent listener failed", failure);
            return true;
        }
        return !event.cancelled();
    }

    public static boolean fireGrow(ServerLevel level, BlockPos pos, BlockState oldState, BlockState newState, BlockGrowEvent.Cause cause) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(BlockGrowEvent.class)) {
            return true;
        }
        var world = FandHooks.wrapWorld(level);
        if (world == null) {
            return true;
        }
        var event = new BlockGrowEvent(
                block(world, pos),
                FandBlockType.of(oldState.getBlock()),
                FandBlockType.of(newState.getBlock()),
                cause);
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("BlockGrowEvent listener failed", failure);
            return true;
        }
        return !event.cancelled();
    }

    public static boolean fireForm(ServerLevel level, BlockPos pos, BlockState oldState, BlockState newState, BlockFormEvent.Cause cause) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(BlockFormEvent.class)) {
            return true;
        }
        var world = FandHooks.wrapWorld(level);
        if (world == null) {
            return true;
        }
        var event = new BlockFormEvent(
                block(world, pos),
                FandBlockType.of(oldState.getBlock()),
                FandBlockType.of(newState.getBlock()),
                cause);
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("BlockFormEvent listener failed", failure);
            return true;
        }
        return !event.cancelled();
    }

    public static boolean fireIgnite(
            ServerLevel level,
            BlockPos pos,
            BlockState newState,
            BlockIgniteEvent.Cause cause,
            @Nullable BlockPos sourcePos
    ) {
        if (!isFire(newState)) {
            return true;
        }
        var bus = FandHooks.events();
        if (!bus.hasListeners(BlockIgniteEvent.class)) {
            return true;
        }
        var world = FandHooks.wrapWorld(level);
        if (world == null) {
            return true;
        }
        var event = new BlockIgniteEvent(
                block(world, pos),
                FandBlockType.of(newState.getBlock()),
                cause,
                Optional.ofNullable(sourcePos).map(source -> block(world, source)));
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("BlockIgniteEvent listener failed", failure);
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

    public static @Nullable List<BlockPos> fireBlockExplode(ServerLevel level, BlockPos centerPos, List<BlockPos> affectedPositions) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(BlockExplodeEvent.class)) {
            return affectedPositions;
        }
        var world = FandHooks.wrapWorld(level);
        if (world == null) {
            return affectedPositions;
        }
        var affected = affectedPositions.stream()
                .map(pos -> block(world, pos))
                .map(io.fand.api.block.Block.class::cast)
                .toList();
        var event = new BlockExplodeEvent(block(world, centerPos), affected);
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("BlockExplodeEvent listener failed", failure);
            return affectedPositions;
        }
        if (event.cancelled()) {
            return null;
        }
        return positionsInWorld(world, event.affectedBlocks());
    }

    public static int fireRedstone(ServerLevel level, BlockPos pos, int oldCurrent, int newCurrent) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(BlockRedstoneEvent.class)) {
            return newCurrent;
        }
        var world = FandHooks.wrapWorld(level);
        if (world == null) {
            return newCurrent;
        }
        var event = new BlockRedstoneEvent(block(world, pos), oldCurrent, newCurrent);
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("BlockRedstoneEvent listener failed", failure);
            return newCurrent;
        }
        return event.newCurrent();
    }

    public static @Nullable List<String> fireSignChange(
            net.minecraft.server.level.ServerPlayer player,
            BlockPos pos,
            boolean frontText,
            List<String> lines
    ) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(SignChangeEvent.class)) {
            return lines;
        }
        FandPlayer fandPlayer = FandHooks.findPlayer(player.getUUID());
        var world = FandHooks.wrapWorld(player.level());
        if (fandPlayer == null || world == null) {
            return lines;
        }
        var event = new SignChangeEvent(fandPlayer, block(world, pos), frontText, lines);
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("SignChangeEvent listener failed", failure);
            return lines;
        }
        return event.cancelled() ? null : new ArrayList<>(event.lines());
    }

    public static boolean firePistonMove(
            ServerLevel level,
            BlockPos pistonPos,
            Direction direction,
            List<BlockPos> affectedPositions,
            boolean extending
    ) {
        var bus = FandHooks.events();
        Class<? extends io.fand.api.event.Event> eventType = extending
                ? BlockPistonPushEvent.class
                : BlockPistonRetractEvent.class;
        if (!bus.hasListeners(eventType)) {
            return true;
        }
        var world = FandHooks.wrapWorld(level);
        if (world == null) {
            return true;
        }
        var block = block(world, pistonPos);
        var affected = affectedPositions.stream()
                .map(pos -> block(world, pos))
                .map(io.fand.api.block.Block.class::cast)
                .toList();
        try {
            if (extending) {
                var event = new BlockPistonPushEvent(block, face(direction), affected);
                bus.fire(event);
                return !event.cancelled();
            }
            var event = new BlockPistonRetractEvent(block, face(direction), affected);
            bus.fire(event);
            return !event.cancelled();
        } catch (RuntimeException failure) {
            LOGGER.warn("{} listener failed", extending ? "BlockPistonExtendEvent" : "BlockPistonRetractEvent", failure);
            return true;
        }
    }

    public static boolean fireFromTo(
            ServerLevel level,
            BlockPos sourcePos,
            BlockPos pos,
            BlockState sourceState,
            BlockState newState,
            Direction direction,
            BlockFromToEvent.Cause cause
    ) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(BlockFromToEvent.class)) {
            return true;
        }
        var world = FandHooks.wrapWorld(level);
        if (world == null) {
            return true;
        }
        var event = new BlockFromToEvent(
                block(world, sourcePos),
                block(world, pos),
                FandBlockType.of(sourceState.getBlock()),
                FandBlockType.of(newState.getBlock()),
                face(direction),
                cause);
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("BlockFromToEvent listener failed", failure);
            return true;
        }
        return !event.cancelled();
    }

    public static boolean fireFluidFlow(
            ServerLevel level,
            BlockPos sourcePos,
            BlockPos pos,
            Direction direction,
            FluidState targetFluid
    ) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(FluidFlowEvent.class)) {
            return true;
        }
        var world = FandHooks.wrapWorld(level);
        if (world == null) {
            return true;
        }
        var identifier = BuiltInRegistries.FLUID.getKey(targetFluid.getType());
        var event = new FluidFlowEvent(
                block(world, sourcePos),
                block(world, pos),
                identifier == null ? Key.key("minecraft:empty") : Key.key(identifier.getNamespace(), identifier.getPath()),
                face(direction));
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("FluidFlowEvent listener failed", failure);
            return true;
        }
        return !event.cancelled();
    }

    public static boolean fireFertilize(
            ServerLevel level,
            @Nullable ServerPlayer player,
            BlockPos pos,
            net.minecraft.world.item.ItemStack itemStack,
            BlockFertilizeEvent.Cause cause
    ) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(BlockFertilizeEvent.class)) {
            return true;
        }
        var world = FandHooks.wrapWorld(level);
        if (world == null) {
            return true;
        }
        Optional<io.fand.api.entity.Player> fandPlayer = Optional.ofNullable(player)
                .map(serverPlayer -> FandHooks.findPlayer(serverPlayer.getUUID()));
        var event = new BlockFertilizeEvent(
                fandPlayer,
                block(world, pos),
                FandItemStacks.fromVanilla(itemStack),
                cause);
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("BlockFertilizeEvent listener failed", failure);
            return true;
        }
        return !event.cancelled();
    }

    public static boolean firePortalCreate(
            ServerLevel level,
            List<BlockPos> positions,
            PortalCreateEvent.Type type,
            PortalCreateEvent.Cause cause
    ) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(PortalCreateEvent.class)) {
            return true;
        }
        var world = FandHooks.wrapWorld(level);
        if (world == null) {
            return true;
        }
        var blocks = positions.stream()
                .map(pos -> block(world, pos))
                .map(io.fand.api.block.Block.class::cast)
                .toList();
        var event = new PortalCreateEvent(world, blocks, type, cause);
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("PortalCreateEvent listener failed", failure);
            return true;
        }
        return !event.cancelled();
    }

    public static boolean fireSpongeAbsorb(ServerLevel level, BlockPos spongePos, List<BlockPos> absorbedPositions) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(SpongeAbsorbEvent.class)) {
            return true;
        }
        var world = FandHooks.wrapWorld(level);
        if (world == null) {
            return true;
        }
        var event = new SpongeAbsorbEvent(
                block(world, spongePos),
                absorbedPositions.stream()
                        .map(pos -> block(world, pos))
                        .map(io.fand.api.block.Block.class::cast)
                        .toList());
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("SpongeAbsorbEvent listener failed", failure);
            return true;
        }
        return !event.cancelled();
    }

    public static boolean fireCauldronLevelChange(
            net.minecraft.world.level.Level level,
            BlockPos pos,
            BlockState oldState,
            BlockState newState,
            net.minecraft.world.entity.@Nullable Entity entity,
            CauldronLevelChangeEvent.Cause cause
    ) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(CauldronLevelChangeEvent.class) || !(level instanceof ServerLevel serverLevel)) {
            return true;
        }
        var world = FandHooks.wrapWorld(serverLevel);
        if (world == null) {
            return true;
        }
        var event = new CauldronLevelChangeEvent(
                block(world, pos),
                FandBlockType.of(oldState.getBlock()),
                FandBlockType.of(newState.getBlock()),
                cauldronLevel(oldState),
                cauldronLevel(newState),
                Optional.ofNullable(entity).map(FandHooks::wrapEntity),
                cause);
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("CauldronLevelChangeEvent listener failed", failure);
            return true;
        }
        return !event.cancelled();
    }

    public static boolean fireSpread(
            ServerLevel level,
            BlockPos sourcePos,
            BlockPos pos,
            BlockState sourceState,
            BlockState newState,
            BlockSpreadEvent.Cause cause
    ) {
        var bus = FandHooks.events();
        boolean hasSpread = bus.hasListeners(BlockSpreadEvent.class);
        if (!hasSpread) {
            return true;
        }
        var world = FandHooks.wrapWorld(level);
        if (world == null) {
            return true;
        }
        if (hasSpread) {
            var event = new BlockSpreadEvent(
                    block(world, sourcePos),
                    block(world, pos),
                    FandBlockType.of(sourceState.getBlock()),
                    FandBlockType.of(newState.getBlock()),
                    cause);
            try {
                bus.fire(event);
            } catch (RuntimeException failure) {
                LOGGER.warn("BlockSpreadEvent listener failed", failure);
            }
            if (event.cancelled()) {
                return false;
            }
        }
        return true;
    }

    public static boolean fireLeavesDecay(ServerLevel level, BlockPos pos, BlockState state) {
        var bus = FandHooks.events();
        if (!bus.hasListeners(LeavesDecayEvent.class)) {
            return true;
        }
        var world = FandHooks.wrapWorld(level);
        if (world == null) {
            return true;
        }
        var event = new LeavesDecayEvent(block(world, pos), FandBlockType.of(state.getBlock()));
        try {
            bus.fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("LeavesDecayEvent listener failed", failure);
            return true;
        }
        return !event.cancelled();
    }

    private static FandBlock block(FandWorld world, BlockPos pos) {
        return new FandBlock(world, pos.getX(), pos.getY(), pos.getZ());
    }

    private static List<BlockPos> positionsInWorld(FandWorld world, List<io.fand.api.block.Block> blocks) {
        var positions = new LinkedHashSet<BlockPos>();
        for (var block : blocks) {
            if (!block.world().equals(world)) {
                continue;
            }
            positions.add(new BlockPos(block.x(), block.y(), block.z()));
        }
        return new ArrayList<>(positions);
    }

    private static BlockFace face(Direction direction) {
        return switch (direction) {
            case DOWN -> BlockFace.DOWN;
            case UP -> BlockFace.UP;
            case NORTH -> BlockFace.NORTH;
            case SOUTH -> BlockFace.SOUTH;
            case WEST -> BlockFace.WEST;
            case EAST -> BlockFace.EAST;
        };
    }

    private static boolean isFire(BlockState state) {
        return state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE);
    }

    private static int cauldronLevel(BlockState state) {
        if (state.hasProperty(LayeredCauldronBlock.LEVEL)) {
            return state.getValue(LayeredCauldronBlock.LEVEL);
        }
        if (state.is(Blocks.WATER_CAULDRON) || state.is(Blocks.LAVA_CAULDRON) || state.is(Blocks.POWDER_SNOW_CAULDRON)) {
            return 3;
        }
        return 0;
    }

    private record IgniteContext(BlockIgniteEvent.Cause cause, @Nullable BlockPos sourcePos) {
    }
}
