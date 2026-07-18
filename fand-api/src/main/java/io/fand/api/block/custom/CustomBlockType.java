package io.fand.api.block.custom;

import io.fand.api.block.BlockPhysics;
import io.fand.api.block.BlockTagKey;
import io.fand.api.block.BlockType;
import io.fand.api.component.DataComponentMap;
import io.fand.api.tag.Tag;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.kyori.adventure.key.Key;

/**
 * Logical custom-block type backed by a vanilla block type plus Fand persistent components.
 *
 * <p>The caller may select any vanilla {@link #baseType()}; Fand does not restrict custom
 * blocks to a fixed carrier list. The base remains responsible for client rendering,
 * collision, block states, and block-entity support. Its interaction and neighbour-update
 * behavior is isolated by default so carrier side effects do not leak into the logical
 * block; {@link #inheritBaseBehavior()} opts back into those behaviors. Fand exposes
 * {@link #id()} as the logical type while persisting it in the block's component storage.
 */
public record CustomBlockType(
        Key id,
        BlockType baseType,
        Map<String, String> baseStateProperties,
        DataComponentMap defaultComponents,
        boolean ticking,
        CustomBlockMining mining,
        Set<Key> customTags,
        boolean inheritBaseBehavior
) implements BlockType {

    public CustomBlockType {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(baseType, "baseType");
        baseStateProperties = Map.copyOf(Objects.requireNonNull(baseStateProperties, "baseStateProperties"));
        defaultComponents = defaultComponents == null ? DataComponentMap.EMPTY : defaultComponents;
        mining = mining == null ? CustomBlockMining.inherit(baseType) : mining;
        customTags = Set.copyOf(Objects.requireNonNull(customTags, "customTags"));
        if (baseType instanceof CustomBlockType) {
            throw new IllegalArgumentException("baseType must be a vanilla block type");
        }
        baseStateProperties.forEach((name, value) -> {
            if (!name.matches("[a-z0-9_]+") || value.isBlank()) {
                throw new IllegalArgumentException("Invalid base block-state property: " + name + "=" + value);
            }
        });
    }

    public CustomBlockType(Key id, BlockType baseType, DataComponentMap defaultComponents, boolean ticking) {
        this(
                id,
                baseType,
                Map.of(),
                defaultComponents,
                ticking,
                CustomBlockMining.inherit(baseType),
                Set.of(),
                false);
    }

    public CustomBlockType(
            Key id,
            BlockType baseType,
            Map<String, String> baseStateProperties,
            DataComponentMap defaultComponents,
            boolean ticking,
            CustomBlockMining mining,
            Set<Key> customTags
    ) {
        this(id, baseType, baseStateProperties, defaultComponents, ticking, mining, customTags, false);
    }

    public CustomBlockType(Key id, BlockType baseType) {
        this(id, baseType, DataComponentMap.EMPTY, false);
    }

    public static Builder builder(Key id, BlockType baseType) {
        return new Builder(id, baseType);
    }

    @Override
    public Key key() {
        return id;
    }

    @Override
    public BlockPhysics physics() {
        var base = baseType.physics();
        return new BlockPhysics(
                mining.hardness(),
                mining.blastResistance(),
                base.lightEmission(),
                base.lightLimit(),
                base.friction(),
                base.speedFactor(),
                base.jumpFactor(),
                base.solid(),
                base.fluid(),
                base.replaceable(),
                base.flammable(),
                base.air(),
                mining.requiresCorrectTool(),
                base.blockEntityPresent(),
                base.canSurvive(),
                base.redstoneConductor());
    }

    @Override
    public boolean is(Tag<BlockType> tag) {
        return baseType.is(tag);
    }

    @Override
    public boolean is(BlockTagKey tag) {
        return baseType.is(tag);
    }

    @Override
    public Collection<? extends Tag<BlockType>> tags() {
        return baseType.tags();
    }

    public boolean hasCustomTag(Key tag) {
        return customTags.contains(Objects.requireNonNull(tag, "tag"));
    }

    public static final class Builder {

        private final Key id;
        private final BlockType baseType;
        private final Map<String, String> baseStateProperties = new LinkedHashMap<>();
        private final Set<Key> customTags = new LinkedHashSet<>();
        private DataComponentMap defaultComponents = DataComponentMap.EMPTY;
        private boolean ticking;
        private CustomBlockMining mining;
        private boolean inheritBaseBehavior;

        private Builder(Key id, BlockType baseType) {
            this.id = Objects.requireNonNull(id, "id");
            this.baseType = Objects.requireNonNull(baseType, "baseType");
            this.mining = CustomBlockMining.inherit(baseType);
        }

        public Builder state(String property, String value) {
            baseStateProperties.put(
                    Objects.requireNonNull(property, "property"),
                    Objects.requireNonNull(value, "value"));
            return this;
        }

        public Builder components(DataComponentMap components) {
            this.defaultComponents = Objects.requireNonNull(components, "components");
            return this;
        }

        public Builder ticking(boolean ticking) {
            this.ticking = ticking;
            return this;
        }

        /** Enables the vanilla carrier's interaction and neighbour-update behavior. */
        public Builder inheritBaseBehavior(boolean inherit) {
            this.inheritBaseBehavior = inherit;
            return this;
        }

        public Builder mining(CustomBlockMining mining) {
            this.mining = Objects.requireNonNull(mining, "mining");
            return this;
        }

        public Builder mining(float hardness, float blastResistance, BlockType toolRuleBase, boolean requiresCorrectTool) {
            return mining(new CustomBlockMining(hardness, blastResistance, toolRuleBase, requiresCorrectTool));
        }

        public Builder tag(Key tag) {
            customTags.add(Objects.requireNonNull(tag, "tag"));
            return this;
        }

        public CustomBlockType build() {
            return new CustomBlockType(
                    id,
                    baseType,
                    baseStateProperties,
                    defaultComponents,
                    ticking,
                    mining,
                    customTags,
                    inheritBaseBehavior);
        }
    }
}
