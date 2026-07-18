package io.fand.server.block;

import io.fand.api.block.Block;
import io.fand.api.block.BlockEntity;
import io.fand.api.block.BlockPhysics;
import io.fand.api.block.BlockType;
import io.fand.api.block.custom.CustomBlockType;
import io.fand.api.block.FluidState;
import io.fand.api.block.FluidType;
import io.fand.api.block.FluidTypes;
import io.fand.api.component.DataComponentContainer;
import io.fand.api.component.DataComponentMap;
import io.fand.api.item.ItemStack;
import io.fand.api.world.Vector3;
import io.fand.api.world.World;
import io.fand.server.component.BlockComponentStorage;
import io.fand.server.item.FandItemStacks;
import io.fand.server.hooks.FandHooks;
import io.fand.server.world.FandWorld;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.kyori.adventure.key.Key;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluids;

public final class FandBlock implements Block {

    private final FandWorld world;
    private final BlockPos pos;

    public FandBlock(FandWorld world, int x, int y, int z) {
        this.world = world;
        this.pos = new BlockPos(x, y, z);
    }

    public ServerLevel worldHandle() {
        return world.handle();
    }

    public BlockPos position() {
        return pos;
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
        return callOnServerThread(() -> FandHooks.customBlocks()
                .customBlock(this)
                .map(BlockType.class::cast)
                .orElseGet(() -> FandBlockType.of(level.getBlockState(pos).getBlock())));
    }

    @Override
    public FluidType fluid() {
        ServerLevel level = world.handle();
        return callOnServerThread(() -> fluidType(level.getFluidIfLoaded(pos)));
    }

    @Override
    public FluidState fluidState() {
        ServerLevel level = world.handle();
        return callOnServerThread(() -> {
            var state = level.getFluidIfLoaded(pos);
            if (state == null) {
                return FluidState.none();
            }
            return fluidState(level, pos, state);
        });
    }

    @Override
    public boolean water() {
        ServerLevel level = world.handle();
        return callOnServerThread(() -> fluidType(level.getFluidIfLoaded(pos)).water());
    }

    @Override
    public boolean lava() {
        ServerLevel level = world.handle();
        return callOnServerThread(() -> fluidType(level.getFluidIfLoaded(pos)).lava());
    }

    @Override
    public BlockPhysics physics() {
        ServerLevel level = world.handle();
        return callOnServerThread(() -> physics(level, pos, level.getBlockState(pos)));
    }

    @Override
    public boolean air() {
        ServerLevel level = world.handle();
        return callOnServerThread(() -> level.getBlockState(pos).isAir());
    }

    @Override
    public Map<String, String> stateProperties() {
        ServerLevel level = world.handle();
        return callOnServerThread(() -> {
            var state = level.getBlockState(pos);
            var properties = new LinkedHashMap<String, String>();
            for (var property : state.getProperties()) {
                properties.put(property.getName(), propertyName(state, property));
            }
            return Map.copyOf(properties);
        });
    }

    @Override
    public Optional<String> stateProperty(String name) {
        Objects.requireNonNull(name, "name");
        ServerLevel level = world.handle();
        return callOnServerThread(() -> {
            var state = level.getBlockState(pos);
            return findProperty(state, name).map(property -> propertyName(state, property));
        });
    }

    @Override
    public boolean setStateProperty(String name, String value) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(value, "value");
        ServerLevel level = world.handle();
        return callOnServerThread(() -> {
            var state = level.getBlockState(pos);
            var next = stateWithProperty(state, name, value);
            if (next.isEmpty() || next.get() == state) {
                return false;
            }
            return level.setBlockAndUpdate(pos, next.get());
        });
    }

    @Override
    public Optional<? extends BlockEntity> blockEntity() {
        ServerLevel level = world.handle();
        return callOnServerThread(() -> Optional.ofNullable(level.getBlockEntity(pos)).map(this::wrapBlockEntity));
    }

    @Override
    public boolean setType(BlockType type) {
        return setType(type, DataComponentMap.EMPTY);
    }

    @Override
    public boolean setType(BlockType type, DataComponentMap components) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(components, "components");
        if (type instanceof CustomBlockType customType) {
            return FandHooks.customBlocks().place(this, customType, components);
        }
        if (!(type instanceof FandBlockType fandType)) {
            throw new IllegalArgumentException("Block type must be obtained from BlockTypes / Server.blockType");
        }
        ServerLevel level = world.handle();
        return callOnServerThread(() -> {
            if (!FandHooks.withoutCustomBlockCarrierPreservation(
                    () -> level.setBlockAndUpdate(pos, fandType.handle().defaultBlockState()))) {
                return false;
            }
            if (components.empty()) {
                BlockComponentStorage.clear(level, pos);
            } else {
                BlockComponentStorage.put(level, pos, components);
            }
            return true;
        });
    }

    @Override
    public boolean setFluid(FluidType fluid, boolean source) {
        Objects.requireNonNull(fluid, "fluid");
        ServerLevel level = world.handle();
        return callOnServerThread(() -> {
            var vanilla = vanillaFluid(fluid, source);
            if (vanilla == null || vanilla == Fluids.EMPTY) {
                return clearFluid(level, pos);
            }
            var fluidState = vanilla.defaultFluidState();
            var blockState = level.getBlockState(pos);
            if (blockState.getBlock() instanceof LiquidBlockContainer container
                    && container.canPlaceLiquid(null, level, pos, blockState, fluidState.getType())
                    && container.placeLiquid(level, pos, blockState, fluidState)) {
                return true;
            }
            return level.setBlockAndUpdate(pos, fluidState.createLegacyBlock());
        });
    }

    @Override
    public boolean setFluid(FluidState state) {
        Objects.requireNonNull(state, "state");
        ServerLevel level = world.handle();
        return callOnServerThread(() -> {
            var vanilla = vanillaFluidState(state);
            if (vanilla == null || vanilla.isEmpty()) {
                return clearFluid(level, pos);
            }
            var blockState = level.getBlockState(pos);
            if (blockState.getBlock() instanceof LiquidBlockContainer container
                    && container.canPlaceLiquid(null, level, pos, blockState, vanilla.getType())
                    && container.placeLiquid(level, pos, blockState, vanilla)) {
                return true;
            }
            return level.setBlockAndUpdate(pos, vanilla.createLegacyBlock());
        });
    }

    @Override
    public boolean clearFluid() {
        ServerLevel level = world.handle();
        return callOnServerThread(() -> clearFluid(level, pos));
    }

    @Override
    public List<ItemStack> drops(ItemStack tool) {
        Objects.requireNonNull(tool, "tool");
        ServerLevel level = world.handle();
        return callOnServerThread(() -> {
            var state = level.getBlockState(pos);
            var blockEntity = state.hasBlockEntity() ? level.getBlockEntity(pos) : null;
            return net.minecraft.world.level.block.Block.getDrops(
                            state,
                            level,
                            pos,
                            blockEntity,
                            null,
                            FandItemStacks.toVanilla(tool))
                    .stream()
                    .map(FandItemStacks::fromVanilla)
                    .toList();
        });
    }

    @Override
    public boolean breakNaturally(boolean dropItems) {
        ServerLevel level = world.handle();
        return callOnServerThread(() -> level.destroyBlock(pos, dropItems));
    }

    @Override
    public void applyPhysics() {
        ServerLevel level = world.handle();
        runOnServerThread(() -> level.updateNeighborsAt(pos, level.getBlockState(pos).getBlock()));
    }

    @Override
    public DataComponentContainer components() {
        ServerLevel level = world.handle();
        return BlockComponentStorage.container(level, pos);
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

    void runOnServerThread(Runnable task) {
        var server = world.handle().getServer();
        if (server == null || server.isSameThread()) {
            task.run();
        } else {
            server.executeIfPossible(task);
        }
    }

    <T> T callOnServerThread(Supplier<T> task) {
        var server = world.handle().getServer();
        if (server == null || server.isSameThread()) {
            return task.get();
        }
        var future = new CompletableFuture<T>();
        server.executeIfPossible(() -> {
            try {
                future.complete(task.get());
            } catch (Throwable failure) {
                future.completeExceptionally(failure);
            }
        });
        return future.join();
    }

    private BlockEntity wrapBlockEntity(net.minecraft.world.level.block.entity.BlockEntity entity) {
        if (entity instanceof SpawnerBlockEntity spawner) {
            return new FandSpawnerBlockEntity(this, spawner);
        }
        if (entity instanceof net.minecraft.world.level.block.entity.BeaconBlockEntity beacon) {
            return new FandBeaconBlockEntity(this, beacon);
        }
        if (entity instanceof net.minecraft.world.level.block.entity.LecternBlockEntity lectern) {
            return new FandLecternBlockEntity(this, lectern);
        }
        if (entity instanceof net.minecraft.world.level.block.entity.BrewingStandBlockEntity brewingStand) {
            return new FandBrewingStandBlockEntity(this, brewingStand);
        }
        if (entity instanceof net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity furnace) {
            return new FandFurnaceBlockEntity(this, furnace);
        }
        if (entity instanceof net.minecraft.world.level.block.entity.JukeboxBlockEntity jukebox) {
            return new FandJukeboxBlockEntity(this, jukebox);
        }
        if (entity instanceof net.minecraft.world.level.block.entity.BeehiveBlockEntity beehive) {
            return new FandBeehiveBlockEntity(this, beehive);
        }
        if (entity instanceof net.minecraft.world.level.block.entity.SculkSensorBlockEntity sculkSensor) {
            return new FandSculkSensorBlockEntity(this, sculkSensor);
        }
        if (entity instanceof net.minecraft.world.level.block.entity.CommandBlockEntity commandBlock) {
            return new FandCommandBlockEntity(this, commandBlock);
        }
        if (entity instanceof net.minecraft.world.level.block.entity.DecoratedPotBlockEntity pot) {
            return new FandDecoratedPotBlockEntity(this, pot);
        }
        if (entity instanceof net.minecraft.world.level.block.entity.TheEndGatewayBlockEntity gateway) {
            return new FandEndGatewayBlockEntity(this, gateway);
        }
        if (entity instanceof net.minecraft.world.level.block.entity.BannerBlockEntity banner) {
            return new FandBannerBlockEntity(this, banner);
        }
        if (entity instanceof net.minecraft.world.level.block.entity.SkullBlockEntity skull) {
            return new FandSkullBlockEntity(this, skull);
        }
        if (entity instanceof net.minecraft.world.level.block.entity.SignBlockEntity sign) {
            return new FandSignBlockEntity(this, sign);
        }
        if (entity instanceof Container container) {
            return new FandContainerBlockEntity(this, entity, container);
        }
        return new FandBlockEntity(this, entity);
    }

    private static BlockPhysics physics(ServerLevel level, BlockPos pos, BlockState state) {
        var block = state.getBlock();
        return new BlockPhysics(
                state.getDestroySpeed(level, pos),
                block.getExplosionResistance(),
                state.getLightEmission(),
                state.getLightDampening(),
                block.getFriction(),
                block.getSpeedFactor(),
                block.getJumpFactor(),
                state.isSolid(),
                state.liquid(),
                state.canBeReplaced(),
                state.ignitedByLava(),
                state.isAir(),
                state.requiresCorrectToolForDrops(),
                state.hasBlockEntity(),
                state.canSurvive(level, pos),
                state.isRedstoneConductor(level, pos));
    }

    private static FluidState fluidState(
            ServerLevel level,
            BlockPos pos,
            net.minecraft.world.level.material.FluidState state
    ) {
        if (state.isEmpty()) {
            return FluidState.none();
        }
        var id = BuiltInRegistries.FLUID.getKey(state.getType());
        var flow = state.getFlow(level, pos);
        var falling = state.hasProperty(FlowingFluid.FALLING) && state.getValue(FlowingFluid.FALLING);
        return new FluidState(
                FluidTypes.of(Key.key(id.getNamespace(), id.getPath())),
                state.isSource(),
                state.isFull(),
                falling,
                state.getAmount(),
                state.getHeight(level, pos),
                state.getOwnHeight(),
                state.getExplosionResistance(),
                new Vector3(flow.x, flow.y, flow.z));
    }

    private static FluidType fluidType(net.minecraft.world.level.material.FluidState state) {
        if (state == null || state.isEmpty()) {
            return FluidTypes.EMPTY;
        }
        var id = BuiltInRegistries.FLUID.getKey(state.getType());
        return FluidTypes.of(Key.key(id.getNamespace(), id.getPath()));
    }

    private static net.minecraft.world.level.material.FluidState vanillaFluidState(FluidState state) {
        var fluid = vanillaFluid(state.type(), state.source());
        if (fluid == null || fluid == Fluids.EMPTY) {
            return Fluids.EMPTY.defaultFluidState();
        }
        if (fluid instanceof FlowingFluid flowing) {
            if (state.source()) {
                return flowing.getSource(state.falling());
            }
            var amount = Math.max(1, Math.min(net.minecraft.world.level.material.FluidState.AMOUNT_FULL, state.amount()));
            return flowing.getFlowing(amount, state.falling());
        }
        return fluid.defaultFluidState();
    }

    private static net.minecraft.world.level.material.Fluid vanillaFluid(FluidType fluid, boolean source) {
        if (fluid.empty()) {
            return Fluids.EMPTY;
        }
        if (fluid.water()) {
            return source ? Fluids.WATER : Fluids.FLOWING_WATER;
        }
        if (fluid.lava()) {
            return source ? Fluids.LAVA : Fluids.FLOWING_LAVA;
        }
        var id = net.minecraft.resources.Identifier.fromNamespaceAndPath(fluid.key().namespace(), fluid.key().value());
        return BuiltInRegistries.FLUID.getOptional(id).orElse(null);
    }

    private static boolean clearFluid(ServerLevel level, BlockPos pos) {
        var state = level.getBlockState(pos);
        if (state.hasProperty(BlockStateProperties.WATERLOGGED) && state.getValue(BlockStateProperties.WATERLOGGED)) {
            return level.setBlockAndUpdate(pos, state.setValue(BlockStateProperties.WATERLOGGED, false));
        }
        if (!state.getFluidState().isEmpty()) {
            return level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        }
        return false;
    }

    private static Optional<Property<?>> findProperty(BlockState state, String name) {
        for (var property : state.getProperties()) {
            if (property.getName().equals(name)) {
                return Optional.of(property);
            }
        }
        return Optional.empty();
    }

    private static String propertyName(BlockState state, Property<?> property) {
        return propertyValueName(property, state.getValue(property));
    }

    private static <T extends Comparable<T>> String propertyValueName(Property<T> property, Comparable<?> value) {
        return property.getName(property.getValueClass().cast(value));
    }

    private static Optional<BlockState> stateWithProperty(BlockState state, String name, String value) {
        for (var property : state.getProperties()) {
            if (property.getName().equals(name)) {
                return stateWithProperty(state, property, value);
            }
        }
        return Optional.empty();
    }

    private static <T extends Comparable<T>> Optional<BlockState> stateWithProperty(BlockState state, Property<T> property, String value) {
        return property.getValue(value).map(parsed -> state.setValue(property, parsed));
    }
}
