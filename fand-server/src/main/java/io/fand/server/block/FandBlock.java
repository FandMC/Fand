package io.fand.server.block;

import io.fand.api.block.Block;
import io.fand.api.block.BlockEntity;
import io.fand.api.block.BlockType;
import io.fand.api.component.DataComponentContainer;
import io.fand.api.component.DataComponentMap;
import io.fand.api.world.World;
import io.fand.server.component.BlockComponentStorage;
import io.fand.server.world.FandWorld;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

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
        return callOnServerThread(() -> FandBlockType.of(level.getBlockState(pos).getBlock()));
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
        if (!(type instanceof FandBlockType fandType)) {
            throw new IllegalArgumentException("Block type must be obtained from BlockTypes / Server.blockType");
        }
        ServerLevel level = world.handle();
        return callOnServerThread(() -> {
            if (!level.setBlockAndUpdate(pos, fandType.handle().defaultBlockState())) {
                return false;
            }
            if (components.isEmpty()) {
                BlockComponentStorage.clear(level, pos);
            } else {
                BlockComponentStorage.put(level, pos, components);
            }
            return true;
        });
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
