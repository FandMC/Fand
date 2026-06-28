package io.fand.api.region;

import io.fand.api.world.BlockRegion;
import java.util.Map;
import java.util.Objects;
import net.kyori.adventure.key.Key;

/**
 * Immutable region definition with typed flag payloads.
 */
public record RegionDefinition(
        Key key,
        Key world,
        BlockRegion region,
        Map<Key, com.google.gson.JsonElement> flags,
        RegionProtection protection
) {

    public RegionDefinition(Key key, Key world, BlockRegion region, Map<Key, com.google.gson.JsonElement> flags) {
        this(key, world, region, flags, RegionProtection.empty());
    }

    public RegionDefinition {
        Objects.requireNonNull(key, "key");
        world = Objects.requireNonNull(world, "world");
        region = Objects.requireNonNull(region, "region");
        protection = Objects.requireNonNull(protection, "protection");
        var copied = new java.util.LinkedHashMap<Key, com.google.gson.JsonElement>();
        for (var entry : Objects.requireNonNull(flags, "flags").entrySet()) {
            copied.put(Objects.requireNonNull(entry.getKey(), "flag key"),
                    Objects.requireNonNull(entry.getValue(), "flag value").deepCopy());
        }
        flags = Map.copyOf(copied);
    }

    public static Builder builder(Key key, Key world, BlockRegion region) {
        return new Builder(key, world, region);
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public static final class Builder {
        private Key key;
        private Key world;
        private BlockRegion region;
        private final java.util.LinkedHashMap<Key, com.google.gson.JsonElement> flags = new java.util.LinkedHashMap<>();
        private RegionProtection protection = RegionProtection.empty();

        private Builder(Key key, Key world, BlockRegion region) {
            this.key = Objects.requireNonNull(key, "key");
            this.world = Objects.requireNonNull(world, "world");
            this.region = Objects.requireNonNull(region, "region");
        }

        private Builder(RegionDefinition definition) {
            this.key = definition.key;
            this.world = definition.world;
            this.region = definition.region;
            this.flags.putAll(definition.flags);
            this.protection = definition.protection;
        }

        public Builder key(Key key) {
            this.key = Objects.requireNonNull(key, "key");
            return this;
        }

        public Builder world(Key world) {
            this.world = Objects.requireNonNull(world, "world");
            return this;
        }

        public Builder region(BlockRegion region) {
            this.region = Objects.requireNonNull(region, "region");
            return this;
        }

        public <T> Builder flag(RegionFlag<T> flag, T value) {
            Objects.requireNonNull(flag, "flag");
            flags.put(flag.key(), flag.encode(value));
            return this;
        }

        public Builder protection(RegionProtection protection) {
            this.protection = Objects.requireNonNull(protection, "protection");
            return this;
        }

        public Builder priority(int priority) {
            this.protection = protection.toBuilder().priority(priority).build();
            return this;
        }

        public Builder parent(Key parent) {
            this.protection = protection.toBuilder().parent(parent).build();
            return this;
        }

        public Builder owner(String owner) {
            this.protection = protection.toBuilder().owner(owner).build();
            return this;
        }

        public Builder member(String member) {
            this.protection = protection.toBuilder().member(member).build();
            return this;
        }

        public RegionDefinition build() {
            return new RegionDefinition(key, world, region, flags, protection);
        }
    }
}
