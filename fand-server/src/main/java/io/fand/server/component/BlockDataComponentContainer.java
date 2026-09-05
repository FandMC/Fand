package io.fand.server.component;

import com.google.gson.JsonElement;
import io.fand.api.component.DataComponentContainer;
import io.fand.api.component.DataComponentMap;
import io.fand.server.util.ServerThreading;
import java.util.Objects;
import java.util.function.Supplier;
import net.kyori.adventure.key.Key;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.RegionTickScope;
import net.minecraft.server.level.ServerLevel;

/** Block components follow their location; saved-data publication follows the world phase. */
final class BlockDataComponentContainer implements DataComponentContainer {
    private final ServerLevel level;
    private final BlockPos pos;

    BlockDataComponentContainer(ServerLevel level, BlockPos pos) {
        this.level = level;
        this.pos = pos.immutable();
    }

    @Override
    public DataComponentMap snapshot() {
        return this.call(() -> BlockComponentStorage.snapshot(this.level, this.pos));
    }

    @Override
    public void set(Key key, JsonElement value) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");
        this.call(() -> {
            BlockComponentStorage.put(this.level, this.pos, BlockComponentStorage.snapshot(this.level, this.pos).with(key, value));
            return null;
        });
    }

    @Override
    public void remove(Key key) {
        Objects.requireNonNull(key, "key");
        this.call(() -> {
            BlockComponentStorage.put(this.level, this.pos, BlockComponentStorage.snapshot(this.level, this.pos).without(key));
            return null;
        });
    }

    @Override
    public void clear() {
        this.call(() -> {
            BlockComponentStorage.clear(this.level, this.pos);
            return null;
        });
    }

    private <T> T call(Supplier<T> task) {
        RegionTickScope scope = RegionTickScope.current();
        if (scope != null) {
            scope.requirePosition(this.level, this.pos);
            return task.get();
        }
        return ServerThreading.callBlocking(this.level.getServer(), task);
    }
}
