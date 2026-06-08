package io.fand.server;

import static org.assertj.core.api.Assertions.assertThat;

import io.fand.api.entity.EntityKey;
import io.fand.server.config.FandConfig;
import net.kyori.adventure.key.Key;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

final class FandServerRegistryLookupTest {

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void resolvesEntityTypesByKey() {
        try (var server = new FandServer(new FandConfig(), getClass().getClassLoader())) {
            var zombie = server.entityType(Key.key("minecraft:zombie"));
            var zombieByGeneratedKey = server.entityType(EntityKey.ZOMBIE);
            var missing = server.entityType(Key.key("minecraft:not_real"));

            assertThat(zombie).isPresent();
            assertThat(zombie.get().key()).isEqualTo(Key.key("minecraft:zombie"));
            assertThat(zombie.get().spawnable()).isTrue();
            assertThat(zombieByGeneratedKey).isPresent();
            assertThat(zombieByGeneratedKey.get()).isSameAs(zombie.get());
            assertThat(missing).isEmpty();
        }
    }
}
