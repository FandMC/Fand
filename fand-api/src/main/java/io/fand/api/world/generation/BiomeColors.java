package io.fand.api.world.generation;

import java.util.Optional;
import org.jspecify.annotations.Nullable;

public record BiomeColors(
        @Nullable Integer fog,
        @Nullable Integer water,
        @Nullable Integer waterFog,
        @Nullable Integer sky,
        @Nullable Integer foliage,
        @Nullable Integer grass
) {

    public static BiomeColors defaults() {
        return new BiomeColors(null, null, null, null, null, null);
    }

    public Optional<Integer> fogColor() {
        return Optional.ofNullable(fog);
    }

    public Optional<Integer> waterColor() {
        return Optional.ofNullable(water);
    }

    public Optional<Integer> waterFogColor() {
        return Optional.ofNullable(waterFog);
    }

    public Optional<Integer> skyColor() {
        return Optional.ofNullable(sky);
    }

    public Optional<Integer> foliageColor() {
        return Optional.ofNullable(foliage);
    }

    public Optional<Integer> grassColor() {
        return Optional.ofNullable(grass);
    }
}
