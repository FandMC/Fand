package io.fand.api.map;

/**
 * A map renderer that can render different pixels for each viewer.
 */
@FunctionalInterface
public interface PlayerMapRenderer extends MapRenderer {

    @Override
    default void render(MapView map, MapCanvas canvas) {
        render(MapRenderContext.global(0L), map, canvas);
    }

    @Override
    void render(MapRenderContext context, MapView map, MapCanvas canvas);
}
