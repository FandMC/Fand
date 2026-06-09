package io.fand.server.entity;

import io.fand.api.block.BlockType;
import io.fand.api.entity.BlockDisplay;
import io.fand.server.block.FandBlockType;
import io.fand.server.world.WorldRegistry;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

public final class FandBlockDisplay extends FandDisplay implements BlockDisplay {

    public FandBlockDisplay(net.minecraft.world.entity.Display.BlockDisplay handle, WorldRegistry worldRegistry) {
        super(handle, worldRegistry);
    }

    @Override
    public net.minecraft.world.entity.Display.BlockDisplay handle() {
        return (net.minecraft.world.entity.Display.BlockDisplay) handle;
    }

    @Override
    public BlockType displayedBlock() {
        return FandBlockType.of(handle().fand$blockState().getBlock());
    }

    @Override
    public Map<String, String> displayedBlockStateProperties() {
        var state = handle().fand$blockState();
        var properties = new LinkedHashMap<String, String>();
        for (var property : state.getProperties()) {
            properties.put(property.getName(), propertyName(state, property));
        }
        return Map.copyOf(properties);
    }

    @Override
    public boolean setDisplayedBlock(BlockType type) {
        return setDisplayedBlock(type, Map.of());
    }

    @Override
    public boolean setDisplayedBlock(BlockType type, Map<String, String> properties) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(properties, "properties");
        if (!(type instanceof FandBlockType fandType)) {
            return false;
        }
        var state = applyProperties(fandType.handle().defaultBlockState(), properties);
        if (state.isEmpty()) {
            return false;
        }
        runOnServerThread(() -> handle().fand$setBlockState(state.get()));
        return true;
    }

    private static Optional<BlockState> applyProperties(BlockState state, Map<String, String> properties) {
        var current = state;
        for (var entry : properties.entrySet()) {
            var next = stateWithProperty(current, entry.getKey(), entry.getValue());
            if (next.isEmpty()) {
                return Optional.empty();
            }
            current = next.get();
        }
        return Optional.of(current);
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

    private static String propertyName(BlockState state, Property<?> property) {
        return propertyValueName(property, state.getValue(property));
    }

    private static <T extends Comparable<T>> String propertyValueName(Property<T> property, Comparable<?> value) {
        return property.getName(property.getValueClass().cast(value));
    }
}
