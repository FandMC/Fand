package io.fand.api.map;

import io.fand.api.entity.Player;
import java.util.List;

public interface MapView {
    int id();

    /**
     * Installs a renderer for this map. Plugin-scoped services remove renderers
     * registered by the plugin when it unloads.
     */
    void renderer(MapRenderer renderer);

    default void render() {
        throw new UnsupportedOperationException("Map rendering is not supported");
    }

    default void render(Player viewer) {
        java.util.Objects.requireNonNull(viewer, "viewer");
        throw new UnsupportedOperationException("Per-player map rendering is not supported");
    }

    default boolean autoRender() {
        return false;
    }

    default void setAutoRender(boolean autoRender) {
        throw new UnsupportedOperationException("Map auto-rendering is not supported");
    }

    default void sendUpdate(Player viewer) {
        java.util.Objects.requireNonNull(viewer, "viewer");
        throw new UnsupportedOperationException("Map update packets are not supported");
    }

    default void sendUpdates() {
        throw new UnsupportedOperationException("Map update packets are not supported");
    }

    default int centerX() {
        throw new UnsupportedOperationException("Map center is not supported");
    }

    default int centerZ() {
        throw new UnsupportedOperationException("Map center is not supported");
    }

    /**
     * Sets the map center in world coordinates. This mutates the underlying
     * saved map data and is not automatically reverted when a plugin unloads.
     */
    default void setCenter(int x, int z) {
        throw new UnsupportedOperationException("Map center is not supported");
    }

    default MapScale scale() {
        throw new UnsupportedOperationException("Map scale is not supported");
    }

    /**
     * Sets the map scale. This mutates the underlying saved map data and is not
     * automatically reverted when a plugin unloads.
     */
    default void setScale(MapScale scale) {
        throw new UnsupportedOperationException("Map scale is not supported");
    }

    default boolean trackingPosition() {
        return false;
    }

    /**
     * Sets whether the map tracks player positions. This mutates the underlying
     * saved map data and is not automatically reverted when a plugin unloads.
     */
    default void setTrackingPosition(boolean tracking) {
        throw new UnsupportedOperationException("Map tracking is not supported");
    }

    default boolean unlimitedTracking() {
        return false;
    }

    /**
     * Sets whether the map tracks positions outside its normal range. This
     * mutates the underlying saved map data and is not automatically reverted
     * when a plugin unloads.
     */
    default void setUnlimitedTracking(boolean unlimited) {
        throw new UnsupportedOperationException("Map unlimited tracking is not supported");
    }

    default boolean locked() {
        return false;
    }

    /**
     * Sets whether the map is locked. This mutates the underlying saved map data
     * and is not automatically reverted when a plugin unloads.
     */
    default void setLocked(boolean locked) {
        throw new UnsupportedOperationException("Map locking is not supported");
    }

    default List<MapCursor> cursors() {
        return List.of();
    }

    /**
     * Replaces client-side cursors stored on this map view. This mutates the
     * underlying map state and is not automatically reverted when a plugin unloads.
     */
    default void setCursors(List<MapCursor> cursors) {
        throw new UnsupportedOperationException("Map cursors are not supported");
    }
}
