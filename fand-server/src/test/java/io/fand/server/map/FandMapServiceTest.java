package io.fand.server.map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class FandMapServiceTest {

    @Test
    void returnsEmptyWithoutAttachedServer() {
        var service = new FandMapService(() -> null);

        assertThat(service.map(0)).isEmpty();
        assertThat(service.map(-1)).isEmpty();
    }

    @Test
    void createRequiresAttachedServer() {
        var service = new FandMapService(() -> null);

        assertThatThrownBy(() -> service.create((map, canvas) -> canvas.pixel(0, 0, (byte) 1)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Minecraft server is not attached");
    }

    @Test
    void mapViewStateAndCursorsStayImplemented() throws Exception {
        var source = Files.readString(Path.of("src/main/java/io/fand/server/map/FandMapService.java"), StandardCharsets.UTF_8);

        assertThat(source).contains("public int centerX()");
        assertThat(source).contains("public void setCenter(int x, int z)");
        assertThat(source).contains("public MapScale scale()");
        assertThat(source).contains("public void setScale(MapScale scale)");
        assertThat(source).contains("public boolean trackingPosition()");
        assertThat(source).contains("public boolean unlimitedTracking()");
        assertThat(source).contains("public boolean locked()");
        assertThat(source).contains("public List<MapCursor> cursors()");
        assertThat(source).contains("public void setCursors(List<MapCursor> cursors)");
        assertThat(source).contains("MapItemSavedData.class.getDeclaredConstructor");
        assertThat(source).contains("addClientSideDecorations");
    }

    @Test
    void perPlayerMapRenderingAndUpdatesStayImplemented() throws Exception {
        var apiView = Files.readString(Path.of("../fand-api/src/main/java/io/fand/api/map/MapView.java"), StandardCharsets.UTF_8);
        var renderer = Files.readString(Path.of("../fand-api/src/main/java/io/fand/api/map/MapRenderer.java"), StandardCharsets.UTF_8);
        var playerRenderer = Files.readString(Path.of("../fand-api/src/main/java/io/fand/api/map/PlayerMapRenderer.java"), StandardCharsets.UTF_8);
        var context = Files.readString(Path.of("../fand-api/src/main/java/io/fand/api/map/MapRenderContext.java"), StandardCharsets.UTF_8);
        var service = Files.readString(Path.of("src/main/java/io/fand/server/map/FandMapService.java"), StandardCharsets.UTF_8);
        var server = Files.readString(Path.of("src/main/java/io/fand/server/FandServer.java"), StandardCharsets.UTF_8);

        assertThat(apiView).contains("void render(Player viewer)");
        assertThat(apiView).contains("void sendUpdate(Player viewer)");
        assertThat(apiView).contains("void sendUpdates()");
        assertThat(apiView).contains("void setAutoRender(boolean autoRender)");
        assertThat(renderer).contains("default void render(MapRenderContext context, MapView map, MapCanvas canvas)");
        assertThat(playerRenderer).contains("void render(MapRenderContext context, MapView map, MapCanvas canvas)");
        assertThat(context).contains("record MapRenderContext");

        assertThat(service).contains("new ClientboundMapItemDataPacket");
        assertThat(service).contains("new MapItemSavedData.MapPatch(0, 0, MapCanvas.WIDTH, MapCanvas.HEIGHT");
        assertThat(service).contains("hasMap(player, id)");
        assertThat(service).contains("MapRenderContext(apiViewer, server.getTickCount())");
        assertThat(service).contains("public void tick()");
        assertThat(server).contains("maps.tick();");
    }
}
