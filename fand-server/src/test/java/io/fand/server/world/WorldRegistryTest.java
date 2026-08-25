package io.fand.server.world;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.fand.server.entity.PlayerRegistry;
import io.fand.server.gamerule.FandGameRuleService;
import io.fand.server.scheduler.TaskScheduler;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

final class WorldRegistryTest {

    @BeforeAll
    static void bootstrapMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void returnsOneCanonicalWrapperPerLevel() {
        var server = mock(MinecraftServer.class);
        var players = mock(PlayerRegistry.class);
        var scheduler = mock(TaskScheduler.class);
        var gameRules = mock(FandGameRuleService.class);
        var level = mock(ServerLevel.class);
        when(level.dimension()).thenReturn(Level.OVERWORLD);
        var registry = new WorldRegistry(server, players, scheduler, gameRules);

        var first = registry.wrap(level);
        var second = registry.wrap(level);

        assertThat(second).isSameAs(first);
    }
}
