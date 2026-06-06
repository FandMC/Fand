package io.fand.server.hooks;

import io.fand.api.event.Event;
import io.fand.api.event.EventBus;
import io.fand.server.FandServer;
import io.fand.server.Main;
import io.fand.server.entity.EntityRegistry;
import io.fand.server.entity.FandPlayer;
import io.fand.server.entity.PlayerRegistry;
import io.fand.server.world.FandWorld;
import io.fand.server.world.WorldRegistry;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Static facade used by patched vanilla code to talk to the Fand runtime.
 *
 * <p>Hot paths (player move, ~20Hz per player) call several runtime accessors;
 * each direct {@code Main.runtime()} read is a volatile load. Centralising
 * those calls here also gives patch sites a single typed entry point that is
 * easier to grep for, and concentrates the {@code Main.runtime()} surface so
 * any future refactor (e.g. dependency injection) only needs to touch this
 * class rather than every patched vanilla file.
 *
 * <p>All accessors return {@code Optional} or {@code null}-tolerant results so
 * patch sites can safely run before {@link FandServer#attach attach} (which
 * wires the world/entity registries).
 */
public final class FandHooks {

    private static final Logger LOGGER = LoggerFactory.getLogger(FandHooks.class);

    private FandHooks() {
    }

    public static EventBus events() {
        return Main.runtime().events();
    }

    public static boolean hasListeners(Class<? extends Event> type) {
        return Main.runtime().events().hasListeners(type);
    }

    /**
     * Fires {@code event} and logs (but does not propagate) listener failures.
     * Returns {@code event} so callers can chain {@code .cancelled()} reads.
     */
    public static <E extends Event> E fireOrLog(E event, String description) {
        try {
            Main.runtime().events().fire(event);
        } catch (RuntimeException failure) {
            LOGGER.warn("{} listener failed", description, failure);
        }
        return event;
    }

    public static PlayerRegistry players() {
        return Main.runtime().playerRegistry();
    }

    public static @Nullable FandPlayer findPlayer(UUID id) {
        return Main.runtime().playerRegistry().find(id).orElse(null);
    }

    public static Optional<WorldRegistry> worlds() {
        return Main.runtime().worldRegistry();
    }

    public static @Nullable FandWorld wrapWorld(ServerLevel level) {
        return Main.runtime().worldRegistry().map(r -> r.wrap(level)).orElse(null);
    }

    public static Optional<EntityRegistry> entities() {
        return Main.runtime().entityRegistry();
    }
}
