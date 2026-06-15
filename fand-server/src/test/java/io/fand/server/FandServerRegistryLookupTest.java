package io.fand.server;

import static org.assertj.core.api.Assertions.assertThat;

import io.fand.api.entity.EntityKey;
import io.fand.server.config.FandConfig;
import io.fand.server.advancement.FandAdvancementRegistry;
import io.fand.server.enchantment.FandEnchantmentRegistry;
import io.fand.server.loot.FandLootTableService;
import io.fand.server.map.FandMapService;
import io.fand.server.structure.FandStructureService;
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

    @Test
    void exposesVanillaDataRegistriesWithoutAttachedServer() {
        try (var server = new FandServer(new FandConfig(), getClass().getClassLoader())) {
            assertThat(server.lootTables()).isInstanceOf(FandLootTableService.class);
            assertThat(server.lootTables().table(Key.key("minecraft:chests/simple_dungeon"))).isEmpty();
            assertThat(server.advancements()).isInstanceOf(FandAdvancementRegistry.class);
            assertThat(server.advancements().advancement(Key.key("minecraft:story/root"))).isEmpty();
            assertThat(server.enchantments()).isInstanceOf(FandEnchantmentRegistry.class);
            assertThat(server.enchantments().enchantment(Key.key("minecraft:sharpness"))).isEmpty();
            assertThat(server.structures()).isInstanceOf(FandStructureService.class);
            assertThat(server.structures().template(Key.key("minecraft:village/plains/houses/plains_small_house_1"))).isEmpty();
            assertThat(server.maps()).isInstanceOf(FandMapService.class);
            assertThat(server.maps().map(0)).isEmpty();
        }
    }
}
