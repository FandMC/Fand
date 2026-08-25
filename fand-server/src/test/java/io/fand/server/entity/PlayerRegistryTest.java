package io.fand.server.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.fand.api.permission.PermissionService;
import io.fand.server.scoreboard.FandScoreboardService;
import io.fand.server.tablist.FandTabListService;
import io.fand.server.world.FandWorld;
import io.fand.server.world.WorldRegistry;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.level.ServerLevel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

final class PlayerRegistryTest {

    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void worldRegistryIsBoundOnceAndAlwaysResolvesCanonicalWrapper() {
        var registry = new PlayerRegistry(
                mock(PermissionService.class),
                mock(FandScoreboardService.class),
                mock(FandTabListService.class));
        var worlds = mock(WorldRegistry.class);
        var otherWorlds = mock(WorldRegistry.class);
        var level = mock(ServerLevel.class);
        var world = mock(FandWorld.class);
        when(worlds.wrap(level)).thenReturn(world);

        registry.bindWorldRegistry(worlds);

        assertThat(registry.wrapLevel(level)).isSameAs(world);
        assertThatThrownBy(() -> registry.bindWorldRegistry(otherWorlds))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("World registry is already bound");
    }
}
