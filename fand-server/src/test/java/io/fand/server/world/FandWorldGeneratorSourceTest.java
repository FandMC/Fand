package io.fand.server.world;

import static org.assertj.core.api.Assertions.assertThat;

import io.fand.api.world.GeneratedChunk;
import io.fand.api.world.WorldGenerator;
import io.fand.api.world.generation.ChunkGenerationStage;
import io.fand.api.world.generation.GeneratorContext;
import io.fand.api.world.generation.WorldGeneratorSettings;
import java.util.concurrent.atomic.AtomicReference;
import net.kyori.adventure.key.Key;
import net.minecraft.SharedConstants;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

final class FandWorldGeneratorSourceTest {

    @BeforeAll
    static void bootstrapVanilla() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void customSpawnCallbackRunsWhenVanillaMobSpawningIsDisabled() {
        var callbackStage = new AtomicReference<ChunkGenerationStage>();
        WorldGenerator generator = new WorldGenerator() {
            @Override
            public void generate(GeneratedChunk chunk) {
            }

            @Override
            public void spawnOriginalMobs(GeneratorContext context) {
                callbackStage.set(context.stage());
            }
        };
        var source = new FandWorldGeneratorSource(
                Key.key("minecraft:overworld"),
                123L,
                VanillaRegistries.createLookup().lookupOrThrow(Registries.BIOME),
                generator,
                WorldGeneratorSettings.builder().spawnOriginalMobs(false).build());

        source.spawnOriginalMobs(null);

        assertThat(callbackStage).hasValue(ChunkGenerationStage.SPAWN);
    }
}
