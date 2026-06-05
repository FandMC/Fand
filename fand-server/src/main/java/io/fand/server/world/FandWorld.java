package io.fand.server.world;

import io.fand.api.block.Block;
import io.fand.api.entity.Player;
import io.fand.api.world.World;
import io.fand.server.block.FandBlock;
import io.fand.server.entity.PlayerRegistry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.key.Key;
import net.minecraft.server.level.ServerLevel;
import org.jspecify.annotations.Nullable;

public final class FandWorld implements World {

    private final ServerLevel handle;
    private final Key key;
    private final @Nullable PlayerRegistry players;

    public FandWorld(ServerLevel handle) {
        this(handle, null);
    }

    public FandWorld(ServerLevel handle, @Nullable PlayerRegistry players) {
        this.handle = handle;
        this.players = players;
        var identifier = handle.dimension().identifier();
        this.key = Key.key(identifier.getNamespace(), identifier.getPath());
    }

    public ServerLevel handle() {
        return handle;
    }

    @Override
    public Key key() {
        return key;
    }

    @Override
    public long seed() {
        return handle.getSeed();
    }

    @Override
    public Collection<? extends Player> players() {
        if (players == null) {
            return List.of();
        }
        var snapshot = new ArrayList<Player>();
        for (var candidate : players.snapshot()) {
            if (candidate.handle().level() == handle) {
                snapshot.add(candidate);
            }
        }
        return List.copyOf(snapshot);
    }

    @Override
    public Iterable<? extends Audience> audiences() {
        return players();
    }

    @Override
    public Block blockAt(int x, int y, int z) {
        return new FandBlock(this, x, y, z);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof FandWorld that && this.key.equals(that.key);
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public String toString() {
        return "FandWorld(" + key.asString() + ")";
    }
}
