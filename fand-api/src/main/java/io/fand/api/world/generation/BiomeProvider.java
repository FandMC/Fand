package io.fand.api.world.generation;

import io.fand.api.VanillaKey;
import io.fand.api.world.BiomeKey;
import java.util.List;
import java.util.Objects;
import net.kyori.adventure.key.Key;

/**
 * Selects biomes during custom world generation.
 *
 * <p>Coordinates are quart coordinates, matching vanilla biome sampling. One
 * biome quart covers four block coordinates on each axis.
 */
@FunctionalInterface
public interface BiomeProvider {

    Key biomeAt(int quartX, int quartY, int quartZ);

    default List<Key> possibleBiomes() {
        return List.of(BiomeKey.PLAINS.key());
    }

    static BiomeProvider fixed(VanillaKey biome) {
        Objects.requireNonNull(biome, "biome");
        return fixed(biome.key());
    }

    static BiomeProvider fixed(Key biome) {
        Objects.requireNonNull(biome, "biome");
        return new BiomeProvider() {
            @Override
            public Key biomeAt(int quartX, int quartY, int quartZ) {
                return biome;
            }

            @Override
            public List<Key> possibleBiomes() {
                return List.of(biome);
            }
        };
    }
}
