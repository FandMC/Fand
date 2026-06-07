package io.fand.server.component;

import io.fand.api.component.DataComponentContainer;
import io.fand.api.component.DataComponentMap;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public final class BlockComponentStorage {

    private BlockComponentStorage() {
    }

    public static DataComponentContainer container(ServerLevel level, BlockPos pos) {
        var server = level.getServer();
        return new SavedDataComponentContainer(
                server,
                () -> level.getDataStorage().get(PersistentComponentData.blockType()),
                () -> level.getDataStorage().computeIfAbsent(PersistentComponentData.blockType()),
                Long.toString(pos.asLong()));
    }

    public static DataComponentMap snapshot(ServerLevel level, BlockPos pos) {
        var server = level.getServer();
        if (server == null || !server.isSameThread()) {
            return DataComponentMap.EMPTY;
        }
        var data = level.getDataStorage().get(PersistentComponentData.blockType());
        return data == null ? DataComponentMap.EMPTY : data.get(Long.toString(pos.asLong()));
    }

    public static void clearIfBlockChanged(
            ServerLevel level,
            BlockPos pos,
            net.minecraft.world.level.block.state.BlockState oldState,
            net.minecraft.world.level.block.state.BlockState newState) {
        if (oldState.getBlock() != newState.getBlock()) {
            clear(level, pos);
        }
    }

    public static void clear(ServerLevel level, BlockPos pos) {
        var server = level.getServer();
        if (server == null) {
            return;
        }
        Runnable task = () -> {
            var data = level.getDataStorage().get(PersistentComponentData.blockType());
            if (data != null) {
                data.clear(Long.toString(pos.asLong()));
            }
        };
        if (server.isSameThread()) {
            task.run();
        } else {
            server.executeIfPossible(task);
        }
    }

    public static void put(ServerLevel level, BlockPos pos, DataComponentMap components) {
        Objects.requireNonNull(components, "components");
        var server = level.getServer();
        if (server == null) {
            return;
        }
        if (components.isEmpty()) {
            clear(level, pos);
            return;
        }
        Runnable task = () -> level.getDataStorage()
                .computeIfAbsent(PersistentComponentData.blockType())
                .put(Long.toString(pos.asLong()), components);
        if (server.isSameThread()) {
            task.run();
        } else {
            server.executeIfPossible(task);
        }
    }
}
