package io.fand.api.enchantment;

import io.fand.api.registry.RegistryReference;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record EnchantmentDefinition(
        List<RegistryReference> supportedItems,
        Optional<List<RegistryReference>> primaryItems,
        int weight,
        int maxLevel,
        EnchantmentCost minCost,
        EnchantmentCost maxCost,
        int anvilCost,
        List<EnchantmentSlotGroup> slots
) {

    public EnchantmentDefinition {
        supportedItems = List.copyOf(supportedItems);
        primaryItems = Objects.requireNonNull(primaryItems, "primaryItems")
                .map(List::copyOf);
        Objects.requireNonNull(minCost, "minCost");
        Objects.requireNonNull(maxCost, "maxCost");
        slots = List.copyOf(slots);
        if (supportedItems.isEmpty()) {
            throw new IllegalArgumentException("supportedItems must not be empty");
        }
        if (weight < 1 || weight > 1024) {
            throw new IllegalArgumentException("weight must be in 1..1024");
        }
        if (maxLevel < 1 || maxLevel > 255) {
            throw new IllegalArgumentException("maxLevel must be in 1..255");
        }
        if (anvilCost < 0) {
            throw new IllegalArgumentException("anvilCost must be >= 0");
        }
        if (slots.isEmpty()) {
            throw new IllegalArgumentException("slots must not be empty");
        }
    }

    public static EnchantmentDefinition allItems(int maxLevel) {
        return new EnchantmentDefinition(
                List.of(RegistryReference.all()),
                Optional.empty(),
                1,
                maxLevel,
                EnchantmentCost.constant(1),
                EnchantmentCost.constant(1),
                0,
                List.of(EnchantmentSlotGroup.ANY));
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private List<RegistryReference> supportedItems = List.of();
        private Optional<List<RegistryReference>> primaryItems = Optional.empty();
        private int weight = 1;
        private int maxLevel = 1;
        private EnchantmentCost minCost = EnchantmentCost.constant(1);
        private EnchantmentCost maxCost = EnchantmentCost.constant(1);
        private int anvilCost;
        private List<EnchantmentSlotGroup> slots = List.of(EnchantmentSlotGroup.ANY);

        private Builder() {
        }

        public Builder supportedItems(List<RegistryReference> supportedItems) {
            this.supportedItems = List.copyOf(supportedItems);
            return this;
        }

        public Builder primaryItems(List<RegistryReference> primaryItems) {
            this.primaryItems = Optional.of(List.copyOf(primaryItems));
            return this;
        }

        public Builder weight(int weight) {
            this.weight = weight;
            return this;
        }

        public Builder maxLevel(int maxLevel) {
            this.maxLevel = maxLevel;
            return this;
        }

        public Builder minCost(EnchantmentCost minCost) {
            this.minCost = Objects.requireNonNull(minCost, "minCost");
            return this;
        }

        public Builder maxCost(EnchantmentCost maxCost) {
            this.maxCost = Objects.requireNonNull(maxCost, "maxCost");
            return this;
        }

        public Builder anvilCost(int anvilCost) {
            this.anvilCost = anvilCost;
            return this;
        }

        public Builder slots(List<EnchantmentSlotGroup> slots) {
            this.slots = List.copyOf(slots);
            return this;
        }

        public EnchantmentDefinition build() {
            return new EnchantmentDefinition(
                    supportedItems,
                    primaryItems,
                    weight,
                    maxLevel,
                    minCost,
                    maxCost,
                    anvilCost,
                    slots);
        }
    }
}
