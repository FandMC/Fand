package io.fand.api.map;

import io.fand.api.entity.Player;
import java.util.List;

public interface MapView {
    int id();

    void renderer(MapRenderer renderer);

    default void renderer(PlayerMapRenderer renderer) {
        renderer((MapRenderer) renderer);
    }

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

    default void setCenter(int x, int z) {
        throw new UnsupportedOperationException("Map center is not supported");
    }

    default MapScale scale() {
        throw new UnsupportedOperationException("Map scale is not supported");
    }

    default void setScale(MapScale scale) {
        throw new UnsupportedOperationException("Map scale is not supported");
    }

    default boolean trackingPosition() {
        return false;
    }

    default void setTrackingPosition(boolean tracking) {
        throw new UnsupportedOperationException("Map tracking is not supported");
    }

    default boolean unlimitedTracking() {
        return false;
    }

    default void setUnlimitedTracking(boolean unlimited) {
        throw new UnsupportedOperationException("Map unlimited tracking is not supported");
    }

    default boolean locked() {
        return false;
    }

    default void setLocked(boolean locked) {
        throw new UnsupportedOperationException("Map locking is not supported");
    }

    default List<MapCursor> cursors() {
        return List.of();
    }

    default void setCursors(List<MapCursor> cursors) {
        throw new UnsupportedOperationException("Map cursors are not supported");
    }
}
