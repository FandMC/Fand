package io.fand.api.structure;

import java.util.List;
import java.util.Objects;
import net.kyori.adventure.key.Key;

/**
 * Runtime structure-set definition used by the vanilla structure scheduler.
 */
public record CustomStructureSet(
        Key key,
        List<StructureSetEntry> structures,
        StructureGenerationPlacement placement
) {

    public CustomStructureSet {
        Objects.requireNonNull(key, "key");
        structures = List.copyOf(Objects.requireNonNull(structures, "structures"));
        if (structures.isEmpty()) {
            throw new IllegalArgumentException("structures must not be empty");
        }
        placement = Objects.requireNonNull(placement, "placement");
    }

    public CustomStructureSet(Key key, Key structure, StructureGenerationPlacement placement) {
        this(key, List.of(new StructureSetEntry(structure, 1)), placement);
    }

    public static Builder builder(Key key, StructureGenerationPlacement placement) {
        return new Builder(key, placement);
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public static final class Builder {
        private Key key;
        private final java.util.ArrayList<StructureSetEntry> structures = new java.util.ArrayList<>();
        private StructureGenerationPlacement placement;

        private Builder(Key key, StructureGenerationPlacement placement) {
            this.key = Objects.requireNonNull(key, "key");
            this.placement = Objects.requireNonNull(placement, "placement");
        }

        private Builder(CustomStructureSet set) {
            this.key = set.key;
            this.structures.addAll(set.structures);
            this.placement = set.placement;
        }

        public Builder key(Key key) {
            this.key = Objects.requireNonNull(key, "key");
            return this;
        }

        public Builder addStructure(Key structure) {
            return addStructure(structure, 1);
        }

        public Builder addStructure(Key structure, int weight) {
            this.structures.add(new StructureSetEntry(structure, weight));
            return this;
        }

        public Builder structures(List<StructureSetEntry> structures) {
            this.structures.clear();
            this.structures.addAll(List.copyOf(Objects.requireNonNull(structures, "structures")));
            return this;
        }

        public Builder placement(StructureGenerationPlacement placement) {
            this.placement = Objects.requireNonNull(placement, "placement");
            return this;
        }

        public CustomStructureSet build() {
            return new CustomStructureSet(key, structures, placement);
        }
    }
}
