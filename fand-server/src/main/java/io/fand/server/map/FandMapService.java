package io.fand.server.map;

import io.fand.api.map.MapCanvas;
import io.fand.api.map.MapRenderer;
import io.fand.api.map.MapService;
import io.fand.api.map.MapView;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

public final class FandMapService implements MapService {

    private final Supplier<MinecraftServer> server;

    public FandMapService(Supplier<MinecraftServer> server) {
        this.server = Objects.requireNonNull(server, "server");
    }

    @Override
    public Optional<MapView> map(int id) {
        if (id < 0) {
            return Optional.empty();
        }
        var current = server.get();
        if (current == null) {
            return Optional.empty();
        }
        return callOnServerThread(current, () -> Optional.ofNullable(mapData(current.overworld(), id))
                .map(ignored -> new FandMapView(current, id)));
    }

    @Override
    public MapView create(MapRenderer renderer) {
        Objects.requireNonNull(renderer, "renderer");
        var current = server.get();
        if (current == null) {
            throw new IllegalStateException("Minecraft server is not attached");
        }
        return callOnServerThread(current, () -> {
            var level = current.overworld();
            var id = level.getFreeMapId();
            var data = MapItemSavedData.createFresh(0.0, 0.0, (byte) 0, false, false, Level.OVERWORLD);
            level.setMapData(id, data);
            var view = new FandMapView(current, id.id());
            view.renderer(renderer);
            return view;
        });
    }

    private static MapItemSavedData mapData(ServerLevel level, int id) {
        return level.getMapData(new MapId(id));
    }

    private static <T> T callOnServerThread(MinecraftServer server, Supplier<T> task) {
        if (server.isSameThread()) {
            return task.get();
        }
        return server.submit(task::get).join();
    }

    private static final class FandMapView implements MapView {

        private final MinecraftServer server;
        private final int id;

        private FandMapView(MinecraftServer server, int id) {
            this.server = server;
            this.id = id;
        }

        @Override
        public int id() {
            return id;
        }

        @Override
        public void renderer(MapRenderer renderer) {
            Objects.requireNonNull(renderer, "renderer");
            callOnServerThread(server, () -> {
                var data = mapData(server.overworld(), id);
                if (data == null) {
                    throw new IllegalStateException("Map " + id + " is not loaded");
                }
                renderer.render(this, new FandMapCanvas(data));
                return null;
            });
        }
    }

    private static final class FandMapCanvas implements MapCanvas {

        private final MapItemSavedData data;

        private FandMapCanvas(MapItemSavedData data) {
            this.data = data;
        }

        @Override
        public void pixel(int x, int y, byte color) {
            if (x < 0 || x >= WIDTH || y < 0 || y >= HEIGHT) {
                throw new IllegalArgumentException("Map pixel out of bounds: " + x + ", " + y);
            }
            data.updateColor(x, y, color);
        }
    }
}
