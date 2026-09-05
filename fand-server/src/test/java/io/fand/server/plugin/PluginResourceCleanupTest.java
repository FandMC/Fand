package io.fand.server.plugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.fand.server.datapack.FandDataPackService;
import io.fand.server.loot.FandLootTableService;
import io.fand.server.map.FandMapService;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.kyori.adventure.key.Key;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Behavior coverage for cleanup previously checked through local-variable source strings. */
class PluginResourceCleanupTest {
    @TempDir
    Path packsDirectory;

    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void closingTheTrackerRemovesItsLootTablesAndKeepsOtherOwners() {
        var delegate = new FandLootTableService(() -> null);
        var tracker = new PluginResourceTracker();
        var plugin = new PluginLootTableService(delegate, tracker, "demo");
        var owned = plugin.replace(Key.key("demo:loot"), ignored -> List.of());
        var other = delegate.replace(Key.key("other:loot"), ignored -> List.of());

        tracker.close();
        tracker.close();

        assertThat(owned.active()).isFalse();
        assertThat(delegate.table(owned.key())).isEmpty();
        assertThat(other.active()).isTrue();
        assertThat(delegate.table(other.key())).isPresent();
    }

    @Test
    void closingTheTrackerDeletesItsManagedPacksAndKeepsOtherOwners() {
        var delegate = new FandDataPackService(packsDirectory, () -> null);
        var tracker = new PluginResourceTracker();
        var plugin = new PluginDataPackService(delegate, tracker, "demo");
        var owned = plugin.create("recipes", "Demo recipes");
        var other = delegate.create("other-recipes", "Other recipes");
        assertThat(delegate.pack(owned.id())).isPresent();

        tracker.close();
        tracker.close();

        assertThat(owned.active()).isFalse();
        assertThat(delegate.pack(owned.id())).isEmpty();
        assertThat(delegate.pack(other.id())).isPresent();
    }

    @Test
    void closingTheTrackerRemovesItsRendererAndPreservesSavedMapData() {
        var delegate = maps();
        var tracker = new PluginResourceTracker();
        var plugin = new PluginMapService(delegate, tracker);
        var view = plugin.map(1).orElseThrow();
        var rendered = new AtomicInteger();
        view.renderer((map, canvas) -> rendered.incrementAndGet());
        view.render();
        assertThat(rendered).hasValue(2);

        tracker.close();
        tracker.close();

        assertThat(delegate.map(1)).isPresent();
        assertThatThrownBy(view::render).hasMessageContaining("does not have a renderer");
        assertThat(rendered).hasValue(2);
    }

    @Test
    void anOldPluginCannotRemoveTheRendererInstalledByANewerPlugin() {
        var delegate = maps();
        var oldTracker = new PluginResourceTracker();
        var newTracker = new PluginResourceTracker();
        var oldView = new PluginMapService(delegate, oldTracker).map(1).orElseThrow();
        var newView = new PluginMapService(delegate, newTracker).map(1).orElseThrow();
        var oldRenders = new AtomicInteger();
        var newRenders = new AtomicInteger();
        oldView.renderer((map, canvas) -> oldRenders.incrementAndGet());
        newView.renderer((map, canvas) -> newRenders.incrementAndGet());

        oldTracker.close();
        newView.render();

        assertThat(oldRenders).hasValue(1);
        assertThat(newRenders).hasValue(2);
        newTracker.close();
        assertThatThrownBy(newView::render).hasMessageContaining("does not have a renderer");
    }

    private static FandMapService maps() {
        var server = mock(MinecraftServer.class);
        var world = mock(ServerLevel.class);
        when(server.isSameThread()).thenReturn(true);
        when(server.overworld()).thenReturn(world);
        var data = MapItemSavedData.createFresh(0, 0, (byte) 0, false, false, Level.OVERWORLD);
        when(world.getMapData(new MapId(1))).thenReturn(data);
        return new FandMapService(() -> server);
    }
}
