package io.fand.api.map;

/**
 * Renders pixels for a custom map.
 *
 * <p>The two-argument method remains the simple global renderer. Implement
 * {@link PlayerMapRenderer} when the output should depend on the viewer.
 */
@FunctionalInterface
public interface MapRenderer {
    void render(MapView map, MapCanvas canvas);

    default void render(MapRenderContext context, MapView map, MapCanvas canvas) {
        render(map, canvas);
    }
}
