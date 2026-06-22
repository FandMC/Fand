package io.fand.api.world;

import static org.assertj.core.api.Assertions.assertThat;

import io.fand.api.world.generation.GenerationMode;
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.Test;

final class WorldCreateOptionsTest {

    @Test
    void customWorldUsesCustomGenerationSettings() {
        WorldGenerator generator = chunk -> {
        };

        var options = WorldCreateOptions.customWorld(generator);

        assertThat(options.generator()).contains(generator);
        assertThat(options.generatorSettings().mode()).isEqualTo(GenerationMode.CUSTOM);
        assertThat(options.isVoidWorld()).isFalse();
    }

    @Test
    void customWorldCanDeclareDimensionType() {
        WorldGenerator generator = chunk -> {
        };
        var dimensionType = Key.key("minecraft:the_nether");

        var options = WorldCreateOptions.customWorld(generator, dimensionType);

        assertThat(options.generator()).contains(generator);
        assertThat(options.generatorSettings().dimensionType()).contains(dimensionType);
    }
}
