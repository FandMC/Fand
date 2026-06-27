package io.fand.api.world;

import static org.assertj.core.api.Assertions.assertThat;

import io.fand.api.world.generation.GenerationMode;
import io.fand.api.world.generation.WorldGeneratorSettings;
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.Test;

final class WorldCreateOptionsTest {

    @Test
    void generatedUsesCustomGenerationSettings() {
        WorldGenerator generator = chunk -> {
        };

        var options = WorldCreateOptions.generated(generator);

        assertThat(options.generator()).contains(generator);
        assertThat(options.generatorSettings().mode()).isEqualTo(GenerationMode.CUSTOM);
        assertThat(options.isVoidWorld()).isFalse();
    }

    @Test
    void generatedCanDeclareDimensionType() {
        WorldGenerator generator = chunk -> {
        };
        var dimensionType = Key.key("minecraft:the_nether");

        var options = WorldCreateOptions.generated(
                generator,
                WorldGeneratorSettings.custom().toBuilder()
                        .dimensionType(dimensionType)
                        .build());

        assertThat(options.generator()).contains(generator);
        assertThat(options.generatorSettings().dimensionType()).contains(dimensionType);
    }
}
