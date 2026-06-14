package net.minecraft.world.level.chunk;

import static org.assertj.core.api.Assertions.assertThat;

import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

final class ChunkGeneratorStructureStateTest {

    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void randomSpreadRangeSearchMatchesFullChunkScan() {
        ChunkGeneratorStructureState state = ChunkGeneratorStructureState.createForFlat(
            null,
            987654321L,
            new EmptyBiomeSource(),
            Stream.empty()
        );
        RandomSpreadStructurePlacement placement = new RandomSpreadStructurePlacement(32, 8, RandomSpreadType.LINEAR, 10387313);
        Holder<StructureSet> structureSet = Holder.direct(new StructureSet(List.of(), placement));

        for (int sourceX = -96; sourceX <= 96; sourceX += 7) {
            for (int sourceZ = -96; sourceZ <= 96; sourceZ += 11) {
                for (int range : List.of(1, 8, 16)) {
                    assertThat(state.hasStructureChunkInRange(structureSet, sourceX, sourceZ, range))
                        .as("sourceX=%s sourceZ=%s range=%s", sourceX, sourceZ, range)
                        .isEqualTo(fullChunkScan(state, placement, sourceX, sourceZ, range));
                }
            }
        }
    }

    private static boolean fullChunkScan(
        final ChunkGeneratorStructureState state,
        final RandomSpreadStructurePlacement placement,
        final int sourceX,
        final int sourceZ,
        final int range
    ) {
        for (int testX = sourceX - range; testX <= sourceX + range; testX++) {
            for (int testZ = sourceZ - range; testZ <= sourceZ + range; testZ++) {
                if (placement.isStructureChunk(state, testX, testZ)) {
                    return true;
                }
            }
        }

        return false;
    }

    private static final class EmptyBiomeSource extends BiomeSource {
        @Override
        protected MapCodec<? extends BiomeSource> codec() {
            throw new UnsupportedOperationException();
        }

        @Override
        protected Stream<Holder<Biome>> collectPossibleBiomes() {
            return Stream.empty();
        }

        @Override
        public Holder<Biome> getNoiseBiome(final int quartX, final int quartY, final int quartZ, final Climate.Sampler sampler) {
            throw new UnsupportedOperationException();
        }
    }
}
