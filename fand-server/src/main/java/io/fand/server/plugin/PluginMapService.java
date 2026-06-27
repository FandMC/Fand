package io.fand.server.plugin;

import io.fand.api.entity.Player;
import io.fand.api.map.MapCursor;
import io.fand.api.map.MapRenderer;
import io.fand.api.map.MapScale;
import io.fand.api.map.MapService;
import io.fand.api.map.MapView;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class PluginMapService implements MapService {

    private final MapService delegate;
    private final PluginResourceTracker tracker;

    public PluginMapService(MapService delegate, PluginResourceTracker tracker) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.tracker = Objects.requireNonNull(tracker, "tracker");
    }

    @Override
    public Optional<MapView> map(int id) {
        return delegate.map(id).map(this::wrap);
    }

    @Override
    public MapView create(MapRenderer renderer) {
        Objects.requireNonNull(renderer, "renderer");
        return wrap(delegate.create(renderer), renderer);
    }

    private MapView wrap(MapView view) {
        return new ScopedMapView(view);
    }

    private MapView wrap(MapView view, MapRenderer renderer) {
        tracker.track(new PluginResourceTracker.MapRendererBinding(delegate, view.id(), renderer));
        return new ScopedMapView(view);
    }

    private final class ScopedMapView implements MapView {

        private final MapView delegateView;

        private ScopedMapView(MapView delegateView) {
            this.delegateView = Objects.requireNonNull(delegateView, "delegateView");
        }

        @Override
        public int id() {
            return delegateView.id();
        }

        @Override
        public void renderer(MapRenderer renderer) {
            Objects.requireNonNull(renderer, "renderer");
            delegateView.renderer(renderer);
            tracker.track(new PluginResourceTracker.MapRendererBinding(delegate, id(), renderer));
        }

        @Override
        public void render() {
            delegateView.render();
        }

        @Override
        public void render(Player viewer) {
            delegateView.render(viewer);
        }

        @Override
        public boolean autoRender() {
            return delegateView.autoRender();
        }

        @Override
        public void setAutoRender(boolean autoRender) {
            delegateView.setAutoRender(autoRender);
        }

        @Override
        public void sendUpdate(Player viewer) {
            delegateView.sendUpdate(viewer);
        }

        @Override
        public void sendUpdates() {
            delegateView.sendUpdates();
        }

        @Override
        public int centerX() {
            return delegateView.centerX();
        }

        @Override
        public int centerZ() {
            return delegateView.centerZ();
        }

        @Override
        public void setCenter(int x, int z) {
            delegateView.setCenter(x, z);
        }

        @Override
        public MapScale scale() {
            return delegateView.scale();
        }

        @Override
        public void setScale(MapScale scale) {
            delegateView.setScale(scale);
        }

        @Override
        public boolean trackingPosition() {
            return delegateView.trackingPosition();
        }

        @Override
        public void setTrackingPosition(boolean tracking) {
            delegateView.setTrackingPosition(tracking);
        }

        @Override
        public boolean unlimitedTracking() {
            return delegateView.unlimitedTracking();
        }

        @Override
        public void setUnlimitedTracking(boolean unlimited) {
            delegateView.setUnlimitedTracking(unlimited);
        }

        @Override
        public boolean locked() {
            return delegateView.locked();
        }

        @Override
        public void setLocked(boolean locked) {
            delegateView.setLocked(locked);
        }

        @Override
        public List<MapCursor> cursors() {
            return delegateView.cursors();
        }

        @Override
        public void setCursors(List<MapCursor> cursors) {
            delegateView.setCursors(cursors);
        }
    }
}
