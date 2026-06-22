package io.fand.api.structure;

import io.fand.api.VanillaKey;
import io.fand.api.registry.RegistryReference;
import io.fand.api.world.BiomeKey;
import io.fand.api.world.generation.DecorationStep;
import java.util.List;
import java.util.Objects;
import net.kyori.adventure.key.Key;

/**
 * Runtime structure definition used by world generation.
 *
 * <p>This is the registration layer above {@link StructureTemplate}: the
 * template defines what blocks are placed, while this definition makes the
 * template visible to the structure generation pipeline.
 */
public record CustomStructure(
        Key key,
        Key template,
        List<RegistryReference> biomes,
        DecorationStep step,
        StructureTerrainAdjustment terrainAdjustment,
        StructureHeightPlacement heightPlacement,
        boolean includeEntities
) {

    public CustomStructure {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(template, "template");
        biomes = List.copyOf(Objects.requireNonNull(biomes, "biomes"));
        if (biomes.isEmpty()) {
            throw new IllegalArgumentException("biomes must not be empty");
        }
        step = Objects.requireNonNull(step, "step");
        terrainAdjustment = Objects.requireNonNull(terrainAdjustment, "terrainAdjustment");
        heightPlacement = Objects.requireNonNull(heightPlacement, "heightPlacement");
    }

    public CustomStructure(Key key, Key template) {
        this(
                key,
                template,
                List.of(RegistryReference.key(BiomeKey.PLAINS.key())),
                DecorationStep.SURFACE_STRUCTURES,
                StructureTerrainAdjustment.NONE,
                StructureHeightPlacement.worldSurface(),
                true);
    }

    public static Builder builder(Key key, Key template) {
        return new Builder(key, template);
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public static final class Builder {
        private Key key;
        private Key template;
        private List<RegistryReference> biomes = List.of(RegistryReference.key(BiomeKey.PLAINS.key()));
        private DecorationStep step = DecorationStep.SURFACE_STRUCTURES;
        private StructureTerrainAdjustment terrainAdjustment = StructureTerrainAdjustment.NONE;
        private StructureHeightPlacement heightPlacement = StructureHeightPlacement.worldSurface();
        private boolean includeEntities = true;

        private Builder(Key key, Key template) {
            this.key = Objects.requireNonNull(key, "key");
            this.template = Objects.requireNonNull(template, "template");
        }

        private Builder(CustomStructure structure) {
            this.key = structure.key;
            this.template = structure.template;
            this.biomes = structure.biomes;
            this.step = structure.step;
            this.terrainAdjustment = structure.terrainAdjustment;
            this.heightPlacement = structure.heightPlacement;
            this.includeEntities = structure.includeEntities;
        }

        public Builder key(Key key) {
            this.key = Objects.requireNonNull(key, "key");
            return this;
        }

        public Builder template(Key template) {
            this.template = Objects.requireNonNull(template, "template");
            return this;
        }

        public Builder biomes(List<RegistryReference> biomes) {
            this.biomes = List.copyOf(Objects.requireNonNull(biomes, "biomes"));
            return this;
        }

        public Builder biomes(VanillaKey... biomes) {
            Objects.requireNonNull(biomes, "biomes");
            var references = new java.util.ArrayList<RegistryReference>(biomes.length);
            for (var biome : biomes) {
                references.add(RegistryReference.key(Objects.requireNonNull(biome, "biomes cannot contain null").key()));
            }
            return biomes(references);
        }

        public Builder biomes(Key... biomes) {
            Objects.requireNonNull(biomes, "biomes");
            var references = new java.util.ArrayList<RegistryReference>(biomes.length);
            for (var biome : biomes) {
                references.add(RegistryReference.key(Objects.requireNonNull(biome, "biomes cannot contain null")));
            }
            return biomes(references);
        }

        public Builder biomeTag(Key biomeTag) {
            return biomes(List.of(RegistryReference.tag(biomeTag)));
        }

        public Builder anyBiome() {
            return biomes(List.of(RegistryReference.all()));
        }

        public Builder step(DecorationStep step) {
            this.step = Objects.requireNonNull(step, "step");
            return this;
        }

        public Builder terrainAdjustment(StructureTerrainAdjustment terrainAdjustment) {
            this.terrainAdjustment = Objects.requireNonNull(terrainAdjustment, "terrainAdjustment");
            return this;
        }

        public Builder heightPlacement(StructureHeightPlacement heightPlacement) {
            this.heightPlacement = Objects.requireNonNull(heightPlacement, "heightPlacement");
            return this;
        }

        public Builder includeEntities(boolean includeEntities) {
            this.includeEntities = includeEntities;
            return this;
        }

        public CustomStructure build() {
            return new CustomStructure(key, template, biomes, step, terrainAdjustment, heightPlacement, includeEntities);
        }
    }
}
