package io.fand.server.world;

import com.mojang.serialization.MapCodec;
import io.fand.api.world.generation.BiomeProvider;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import net.kyori.adventure.key.Key;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

public final class FandBiomeSource extends BiomeSource {

    private final HolderLookup.RegistryLookup<Biome> biomes;
    private final BiomeProvider provider;
    private final List<Holder<Biome>> possibleBiomes;
    private final Holder<Biome> fallback;

    public FandBiomeSource(HolderLookup.RegistryLookup<Biome> biomes, BiomeProvider provider, Holder<Biome> fallback) {
        this.biomes = Objects.requireNonNull(biomes, "biomes");
        this.provider = Objects.requireNonNull(provider, "provider");
        this.fallback = Objects.requireNonNull(fallback, "fallback");
        var holders = new LinkedHashSet<Holder<Biome>>();
        for (var key : provider.possibleBiomes()) {
            holders.add(resolve(key));
        }
        if (holders.isEmpty()) {
            holders.add(fallback);
        }
        this.possibleBiomes = List.copyOf(holders);
    }

    @Override
    protected MapCodec<? extends BiomeSource> codec() {
        return MapCodec.unit(this);
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return possibleBiomes.stream();
    }

    @Override
    public Holder<Biome> getNoiseBiome(int quartX, int quartY, int quartZ, Climate.Sampler sampler) {
        return resolve(provider.biomeAt(quartX, quartY, quartZ));
    }

    private Holder<Biome> resolve(Key key) {
        var resourceKey = ResourceKey.create(
                net.minecraft.core.registries.Registries.BIOME,
                Identifier.fromNamespaceAndPath(key.namespace(), key.value()));
        return biomes.get(resourceKey).map(holder -> (Holder<Biome>) holder).orElse(fallback);
    }
}
