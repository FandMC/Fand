package io.fand.server.entity;

import static org.assertj.core.api.Assertions.assertThat;

import net.kyori.adventure.key.Key;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

final class FandEntityTypeTest {

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void exposesRegistryKeyAndBasicCategories() {
        var zombie = FandEntityType.of(net.minecraft.world.entity.EntityType.ZOMBIE);
        var player = FandEntityType.of(net.minecraft.world.entity.EntityType.PLAYER);

        assertThat(zombie.key()).isEqualTo(Key.key("minecraft:zombie"));
        assertThat(zombie.spawnable()).isTrue();
        assertThat(zombie.player()).isFalse();

        assertThat(player.key()).isEqualTo(Key.key("minecraft:player"));
        assertThat(player.spawnable()).isFalse();
        assertThat(player.player()).isTrue();
    }
}
