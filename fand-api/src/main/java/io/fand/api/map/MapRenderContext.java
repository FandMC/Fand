package io.fand.api.map;

import io.fand.api.entity.Player;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Context for a map render pass.
 *
 * @param viewer player receiving this render, or {@code null} for a global
 *        persistent render
 * @param tick server tick when this render pass began
 */
public record MapRenderContext(@Nullable Player viewer, long tick) {

    public static MapRenderContext global(long tick) {
        return new MapRenderContext(null, tick);
    }

    public Optional<Player> viewerOptional() {
        return Optional.ofNullable(viewer);
    }

    public boolean playerSpecific() {
        return viewer != null;
    }
}
