package io.fand.server.map;

import io.fand.api.map.MapCanvas;
import io.fand.api.map.MapCursor;
import io.fand.api.map.MapCursorType;
import io.fand.api.map.MapRenderContext;
import io.fand.api.map.MapRenderer;
import io.fand.api.map.MapScale;
import io.fand.api.map.MapService;
import io.fand.api.map.MapView;
import io.fand.server.command.AdventureBridge;
import io.fand.server.entity.FandPlayer;
import io.fand.server.util.ReflectionFields;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

public final class FandMapService implements MapService {

    private static final java.lang.reflect.Field TRACKING_POSITION = ReflectionFields.field(MapItemSavedData.class, "trackingPosition");
    private static final java.lang.reflect.Field UNLIMITED_TRACKING = ReflectionFields.field(MapItemSavedData.class, "unlimitedTracking");
    private static final Constructor<MapItemSavedData> MAP_DATA_CONSTRUCTOR = mapDataConstructor();

    private final Supplier<MinecraftServer> server;
    private final Map<Integer, RendererState> renderers = new ConcurrentHashMap<>();

    public FandMapService(Supplier<MinecraftServer> server) {
        this.server = Objects.requireNonNull(server, "server");
    }

    public void tick() {
        var current = server.get();
        if (current == null || renderers.isEmpty()) {
            return;
        }
        if (!current.isSameThread()) {
            current.execute(this::tick);
            return;
        }
        for (var entry : renderers.entrySet()) {
            if (entry.getValue().autoRender()) {
                var view = new FandMapView(current, entry.getKey());
                view.sendUpdates();
            }
        }
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

    public void clearRenderer(int id, MapRenderer renderer) {
        Objects.requireNonNull(renderer, "renderer");
        renderers.computeIfPresent(id, (ignored, state) -> state.renderer() == renderer ? null : state);
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

    private final class FandMapView implements MapView {

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
                renderers.computeIfAbsent(id, ignored -> new RendererState()).renderer(renderer);
                renderer.render(MapRenderContext.global(server.getTickCount()), this, new FandMapCanvas(data, server));
                return null;
            });
        }

        @Override
        public void render() {
            callOnServerThread(server, () -> {
                renderGlobal(data(), renderer());
                return null;
            });
        }

        @Override
        public void render(io.fand.api.entity.Player viewer) {
            Objects.requireNonNull(viewer, "viewer");
            callOnServerThread(server, () -> {
                renderFor(data(), renderer(), unwrapViewer(viewer));
                return null;
            });
        }

        @Override
        public boolean autoRender() {
            var state = renderers.get(id);
            return state != null && state.autoRender();
        }

        @Override
        public void setAutoRender(boolean autoRender) {
            renderers.computeIfAbsent(id, ignored -> new RendererState()).autoRender(autoRender);
        }

        @Override
        public void sendUpdate(io.fand.api.entity.Player viewer) {
            Objects.requireNonNull(viewer, "viewer");
            callOnServerThread(server, () -> {
                var target = unwrapViewer(viewer);
                var snapshot = renderedSnapshot(target);
                sendSnapshot(target, snapshot);
                return null;
            });
        }

        @Override
        public void sendUpdates() {
            callOnServerThread(server, () -> {
                for (var player : server.getPlayerList().getPlayers()) {
                    if (hasMap(player, id)) {
                        sendSnapshot(player, renderedSnapshot(player));
                    }
                }
                return null;
            });
        }

        @Override
        public int centerX() {
            return callOnServerThread(server, () -> data().centerX);
        }

        @Override
        public int centerZ() {
            return callOnServerThread(server, () -> data().centerZ);
        }

        @Override
        public void setCenter(int x, int z) {
            callOnServerThread(server, () -> {
                var current = data();
                replaceData(copyWith(current, x, z, current.scale, FandMapService.trackingPosition(current), FandMapService.unlimitedTracking(current), current.locked));
                return null;
            });
        }

        @Override
        public MapScale scale() {
            return callOnServerThread(server, () -> MapScale.of(data().scale));
        }

        @Override
        public void setScale(MapScale scale) {
            Objects.requireNonNull(scale, "scale");
            callOnServerThread(server, () -> {
                var current = data();
                replaceData(copyWith(current, current.centerX, current.centerZ, (byte) scale.value(), FandMapService.trackingPosition(current), FandMapService.unlimitedTracking(current), current.locked));
                return null;
            });
        }

        @Override
        public boolean trackingPosition() {
            return callOnServerThread(server, () -> FandMapService.trackingPosition(data()));
        }

        @Override
        public void setTrackingPosition(boolean tracking) {
            callOnServerThread(server, () -> {
                var current = data();
                replaceData(copyWith(current, current.centerX, current.centerZ, current.scale, tracking, FandMapService.unlimitedTracking(current), current.locked));
                return null;
            });
        }

        @Override
        public boolean unlimitedTracking() {
            return callOnServerThread(server, () -> FandMapService.unlimitedTracking(data()));
        }

        @Override
        public void setUnlimitedTracking(boolean unlimited) {
            callOnServerThread(server, () -> {
                var current = data();
                replaceData(copyWith(current, current.centerX, current.centerZ, current.scale, FandMapService.trackingPosition(current), unlimited, current.locked));
                return null;
            });
        }

        @Override
        public boolean locked() {
            return callOnServerThread(server, () -> data().locked);
        }

        @Override
        public void setLocked(boolean locked) {
            callOnServerThread(server, () -> {
                var current = data();
                replaceData(copyWith(current, current.centerX, current.centerZ, current.scale, FandMapService.trackingPosition(current), FandMapService.unlimitedTracking(current), locked));
                return null;
            });
        }

        @Override
        public List<MapCursor> cursors() {
            return callOnServerThread(server, () -> {
                var result = new java.util.ArrayList<MapCursor>();
                for (var decoration : data().getDecorations()) {
                    result.add(toApiCursor(decoration, server));
                }
                return List.copyOf(result);
            });
        }

        @Override
        public void setCursors(List<MapCursor> cursors) {
            Objects.requireNonNull(cursors, "cursors");
            var snapshot = List.copyOf(cursors);
            callOnServerThread(server, () -> {
                data().addClientSideDecorations(snapshot.stream().map(cursor -> toVanillaCursor(cursor, server)).toList());
                return null;
            });
        }

        private MapItemSavedData data() {
            var data = mapData(server.overworld(), id);
            if (data == null) {
                throw new IllegalStateException("Map " + id + " is not loaded");
            }
            return data;
        }

        private MapRenderer renderer() {
            var state = renderers.get(id);
            if (state == null || state.renderer() == null) {
                throw new IllegalStateException("Map " + id + " does not have a renderer");
            }
            return state.renderer();
        }

        private RenderedMap renderedSnapshot(ServerPlayer viewer) {
            var base = data();
            var colors = Arrays.copyOf(base.colors, base.colors.length);
            var cursors = new java.util.ArrayList<>(decorations(base));
            var rendered = new RenderedMap(base.scale, base.locked, colors, cursors);
            renderFor(rendered, renderer(), viewer);
            return rendered;
        }

        private void renderGlobal(MapItemSavedData data, MapRenderer renderer) {
            renderer.render(MapRenderContext.global(server.getTickCount()), this, new FandMapCanvas(data, server));
        }

        private void renderFor(MapItemSavedData data, MapRenderer renderer, ServerPlayer viewer) {
            var apiViewer = io.fand.server.hooks.FandHooks.players().find(viewer.getUUID()).orElse(null);
            renderer.render(new MapRenderContext(apiViewer, server.getTickCount()), this, new FandMapCanvas(data, server));
        }

        private void renderFor(RenderedMap rendered, MapRenderer renderer, ServerPlayer viewer) {
            var apiViewer = io.fand.server.hooks.FandHooks.players().find(viewer.getUUID()).orElse(null);
            renderer.render(new MapRenderContext(apiViewer, server.getTickCount()), this, new SnapshotMapCanvas(rendered, server));
        }

        private void sendSnapshot(ServerPlayer player, RenderedMap snapshot) {
            var patch = new MapItemSavedData.MapPatch(0, 0, MapCanvas.WIDTH, MapCanvas.HEIGHT, snapshot.colors());
            player.connection.send(new ClientboundMapItemDataPacket(
                    new MapId(id),
                    snapshot.scale(),
                    snapshot.locked(),
                    snapshot.decorations(),
                    patch));
        }

        private void replaceData(MapItemSavedData data) {
            server.overworld().setMapData(new MapId(id), data);
        }

        private MapItemSavedData copyWith(MapItemSavedData current, int centerX, int centerZ, byte scale, boolean tracking, boolean unlimited, boolean locked) {
            var replacement = createMapData(centerX, centerZ, scale, tracking, unlimited, locked, current.dimension);
            System.arraycopy(current.colors, 0, replacement.colors, 0, current.colors.length);
            replacement.addClientSideDecorations(java.util.stream.StreamSupport.stream(current.getDecorations().spliterator(), false).toList());
            return replacement;
        }
    }

    private static final class FandMapCanvas implements MapCanvas {

        private final MapItemSavedData data;
        private final MinecraftServer server;

        private FandMapCanvas(MapItemSavedData data, MinecraftServer server) {
            this.data = data;
            this.server = server;
        }

        @Override
        public byte pixel(int x, int y) {
            checkPixel(x, y);
            return data.colors[x + y * WIDTH];
        }

        @Override
        public void pixel(int x, int y, byte color) {
            checkPixel(x, y);
            data.updateColor(x, y, color);
        }

        @Override
        public List<MapCursor> cursors() {
            return decorations(data).stream().map(decoration -> toApiCursor(decoration, server)).toList();
        }

        @Override
        public void cursors(List<MapCursor> cursors) {
            Objects.requireNonNull(cursors, "cursors");
            data.addClientSideDecorations(cursors.stream().map(cursor -> toVanillaCursor(cursor, server)).toList());
        }
    }

    private static final class SnapshotMapCanvas implements MapCanvas {

        private final RenderedMap rendered;
        private final MinecraftServer server;

        private SnapshotMapCanvas(RenderedMap rendered, MinecraftServer server) {
            this.rendered = rendered;
            this.server = server;
        }

        @Override
        public byte pixel(int x, int y) {
            checkPixel(x, y);
            return rendered.colors()[x + y * WIDTH];
        }

        @Override
        public void pixel(int x, int y, byte color) {
            checkPixel(x, y);
            rendered.colors()[x + y * WIDTH] = color;
        }

        @Override
        public List<MapCursor> cursors() {
            return rendered.decorations().stream().map(decoration -> toApiCursor(decoration, server)).toList();
        }

        @Override
        public void cursors(List<MapCursor> cursors) {
            Objects.requireNonNull(cursors, "cursors");
            rendered.decorations().clear();
            rendered.decorations().addAll(cursors.stream().map(cursor -> toVanillaCursor(cursor, server)).toList());
        }
    }

    private static final class RendererState {
        private volatile MapRenderer renderer;
        private volatile boolean autoRender;

        private MapRenderer renderer() {
            return renderer;
        }

        private void renderer(MapRenderer renderer) {
            this.renderer = renderer;
        }

        private boolean autoRender() {
            return autoRender;
        }

        private void autoRender(boolean autoRender) {
            this.autoRender = autoRender;
        }
    }

    private record RenderedMap(byte scale, boolean locked, byte[] colors, List<MapDecoration> decorations) {
    }

    private static MapCursor toApiCursor(MapDecoration decoration, MinecraftServer server) {
        return new MapCursor(
                cursorType(decoration.type()),
                decoration.x(),
                decoration.y(),
                decoration.rot(),
                decoration.name().map(name -> AdventureBridge.fromVanilla(name, server.registryAccess())).orElse(null)
        );
    }

    private static MapDecoration toVanillaCursor(MapCursor cursor, MinecraftServer server) {
        Objects.requireNonNull(cursor, "cursor");
        return new MapDecoration(
                cursorType(cursor.type()),
                cursor.x(),
                cursor.y(),
                cursor.rotation(),
                cursor.displayName().map(name -> AdventureBridge.toVanilla(name, server.registryAccess()))
        );
    }

    private static List<MapDecoration> decorations(MapItemSavedData data) {
        return java.util.stream.StreamSupport.stream(data.getDecorations().spliterator(), false).toList();
    }

    private static void checkPixel(int x, int y) {
        if (x < 0 || x >= MapCanvas.WIDTH || y < 0 || y >= MapCanvas.HEIGHT) {
            throw new IllegalArgumentException("Map pixel out of bounds: " + x + ", " + y);
        }
    }

    private static ServerPlayer unwrapViewer(io.fand.api.entity.Player viewer) {
        if (viewer instanceof FandPlayer fandPlayer) {
            return fandPlayer.handle();
        }
        throw new IllegalArgumentException("Player is not owned by this server: " + viewer.uniqueId());
    }

    private static boolean hasMap(ServerPlayer player, int id) {
        var inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            var stack = inventory.getItem(slot);
            var mapId = stack.get(DataComponents.MAP_ID);
            if (mapId != null && mapId.id() == id) {
                return true;
            }
        }
        return false;
    }

    private static MapCursorType cursorType(Holder<MapDecorationType> type) {
        if (type.is(MapDecorationTypes.PLAYER)) return MapCursorType.PLAYER;
        if (type.is(MapDecorationTypes.FRAME)) return MapCursorType.FRAME;
        if (type.is(MapDecorationTypes.RED_MARKER)) return MapCursorType.RED_MARKER;
        if (type.is(MapDecorationTypes.BLUE_MARKER)) return MapCursorType.BLUE_MARKER;
        if (type.is(MapDecorationTypes.TARGET_X)) return MapCursorType.TARGET_X;
        if (type.is(MapDecorationTypes.TARGET_POINT)) return MapCursorType.TARGET_POINT;
        if (type.is(MapDecorationTypes.PLAYER_OFF_MAP)) return MapCursorType.PLAYER_OFF_MAP;
        if (type.is(MapDecorationTypes.PLAYER_OFF_LIMITS)) return MapCursorType.PLAYER_OFF_LIMITS;
        if (type.is(MapDecorationTypes.WOODLAND_MANSION)) return MapCursorType.WOODLAND_MANSION;
        if (type.is(MapDecorationTypes.OCEAN_MONUMENT)) return MapCursorType.OCEAN_MONUMENT;
        if (type.is(MapDecorationTypes.WHITE_BANNER)) return MapCursorType.WHITE_BANNER;
        if (type.is(MapDecorationTypes.ORANGE_BANNER)) return MapCursorType.ORANGE_BANNER;
        if (type.is(MapDecorationTypes.MAGENTA_BANNER)) return MapCursorType.MAGENTA_BANNER;
        if (type.is(MapDecorationTypes.LIGHT_BLUE_BANNER)) return MapCursorType.LIGHT_BLUE_BANNER;
        if (type.is(MapDecorationTypes.YELLOW_BANNER)) return MapCursorType.YELLOW_BANNER;
        if (type.is(MapDecorationTypes.LIME_BANNER)) return MapCursorType.LIME_BANNER;
        if (type.is(MapDecorationTypes.PINK_BANNER)) return MapCursorType.PINK_BANNER;
        if (type.is(MapDecorationTypes.GRAY_BANNER)) return MapCursorType.GRAY_BANNER;
        if (type.is(MapDecorationTypes.LIGHT_GRAY_BANNER)) return MapCursorType.LIGHT_GRAY_BANNER;
        if (type.is(MapDecorationTypes.CYAN_BANNER)) return MapCursorType.CYAN_BANNER;
        if (type.is(MapDecorationTypes.PURPLE_BANNER)) return MapCursorType.PURPLE_BANNER;
        if (type.is(MapDecorationTypes.BLUE_BANNER)) return MapCursorType.BLUE_BANNER;
        if (type.is(MapDecorationTypes.BROWN_BANNER)) return MapCursorType.BROWN_BANNER;
        if (type.is(MapDecorationTypes.GREEN_BANNER)) return MapCursorType.GREEN_BANNER;
        if (type.is(MapDecorationTypes.RED_BANNER)) return MapCursorType.RED_BANNER;
        if (type.is(MapDecorationTypes.BLACK_BANNER)) return MapCursorType.BLACK_BANNER;
        if (type.is(MapDecorationTypes.RED_X)) return MapCursorType.RED_X;
        if (type.is(MapDecorationTypes.DESERT_VILLAGE)) return MapCursorType.DESERT_VILLAGE;
        if (type.is(MapDecorationTypes.PLAINS_VILLAGE)) return MapCursorType.PLAINS_VILLAGE;
        if (type.is(MapDecorationTypes.SAVANNA_VILLAGE)) return MapCursorType.SAVANNA_VILLAGE;
        if (type.is(MapDecorationTypes.SNOWY_VILLAGE)) return MapCursorType.SNOWY_VILLAGE;
        if (type.is(MapDecorationTypes.TAIGA_VILLAGE)) return MapCursorType.TAIGA_VILLAGE;
        if (type.is(MapDecorationTypes.JUNGLE_TEMPLE)) return MapCursorType.JUNGLE_TEMPLE;
        if (type.is(MapDecorationTypes.SWAMP_HUT)) return MapCursorType.SWAMP_HUT;
        if (type.is(MapDecorationTypes.TRIAL_CHAMBERS)) return MapCursorType.TRIAL_CHAMBERS;
        return MapCursorType.PLAYER;
    }

    private static Holder<MapDecorationType> cursorType(MapCursorType type) {
        return switch (type) {
            case PLAYER -> MapDecorationTypes.PLAYER;
            case FRAME -> MapDecorationTypes.FRAME;
            case RED_MARKER -> MapDecorationTypes.RED_MARKER;
            case BLUE_MARKER -> MapDecorationTypes.BLUE_MARKER;
            case TARGET_X -> MapDecorationTypes.TARGET_X;
            case TARGET_POINT -> MapDecorationTypes.TARGET_POINT;
            case PLAYER_OFF_MAP -> MapDecorationTypes.PLAYER_OFF_MAP;
            case PLAYER_OFF_LIMITS -> MapDecorationTypes.PLAYER_OFF_LIMITS;
            case WOODLAND_MANSION -> MapDecorationTypes.WOODLAND_MANSION;
            case OCEAN_MONUMENT -> MapDecorationTypes.OCEAN_MONUMENT;
            case WHITE_BANNER -> MapDecorationTypes.WHITE_BANNER;
            case ORANGE_BANNER -> MapDecorationTypes.ORANGE_BANNER;
            case MAGENTA_BANNER -> MapDecorationTypes.MAGENTA_BANNER;
            case LIGHT_BLUE_BANNER -> MapDecorationTypes.LIGHT_BLUE_BANNER;
            case YELLOW_BANNER -> MapDecorationTypes.YELLOW_BANNER;
            case LIME_BANNER -> MapDecorationTypes.LIME_BANNER;
            case PINK_BANNER -> MapDecorationTypes.PINK_BANNER;
            case GRAY_BANNER -> MapDecorationTypes.GRAY_BANNER;
            case LIGHT_GRAY_BANNER -> MapDecorationTypes.LIGHT_GRAY_BANNER;
            case CYAN_BANNER -> MapDecorationTypes.CYAN_BANNER;
            case PURPLE_BANNER -> MapDecorationTypes.PURPLE_BANNER;
            case BLUE_BANNER -> MapDecorationTypes.BLUE_BANNER;
            case BROWN_BANNER -> MapDecorationTypes.BROWN_BANNER;
            case GREEN_BANNER -> MapDecorationTypes.GREEN_BANNER;
            case RED_BANNER -> MapDecorationTypes.RED_BANNER;
            case BLACK_BANNER -> MapDecorationTypes.BLACK_BANNER;
            case RED_X -> MapDecorationTypes.RED_X;
            case DESERT_VILLAGE -> MapDecorationTypes.DESERT_VILLAGE;
            case PLAINS_VILLAGE -> MapDecorationTypes.PLAINS_VILLAGE;
            case SAVANNA_VILLAGE -> MapDecorationTypes.SAVANNA_VILLAGE;
            case SNOWY_VILLAGE -> MapDecorationTypes.SNOWY_VILLAGE;
            case TAIGA_VILLAGE -> MapDecorationTypes.TAIGA_VILLAGE;
            case JUNGLE_TEMPLE -> MapDecorationTypes.JUNGLE_TEMPLE;
            case SWAMP_HUT -> MapDecorationTypes.SWAMP_HUT;
            case TRIAL_CHAMBERS -> MapDecorationTypes.TRIAL_CHAMBERS;
        };
    }

    private static boolean trackingPosition(MapItemSavedData data) {
        return ReflectionFields.booleanValue(TRACKING_POSITION, data);
    }

    private static boolean unlimitedTracking(MapItemSavedData data) {
        return ReflectionFields.booleanValue(UNLIMITED_TRACKING, data);
    }

    private static Constructor<MapItemSavedData> mapDataConstructor() {
        try {
            var constructor = MapItemSavedData.class.getDeclaredConstructor(
                    int.class,
                    int.class,
                    byte.class,
                    boolean.class,
                    boolean.class,
                    boolean.class,
                    ResourceKey.class);
            constructor.setAccessible(true);
            return constructor;
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Missing MapItemSavedData constructor", failure);
        }
    }

    private static MapItemSavedData createMapData(
            int centerX,
            int centerZ,
            byte scale,
            boolean tracking,
            boolean unlimited,
            boolean locked,
            ResourceKey<Level> dimension
    ) {
        try {
            return MAP_DATA_CONSTRUCTOR.newInstance(centerX, centerZ, scale, tracking, unlimited, locked, dimension);
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Cannot create map saved data", failure);
        }
    }
}
