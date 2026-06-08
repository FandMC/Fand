package io.fand.api.event.inventory;

import java.util.Objects;
import java.util.Optional;
import net.kyori.adventure.key.Key;

/**
 * Mutable enchanting-table offer shown in one of the three enchantment rows.
 */
public final class EnchantmentOffer {

    private final int slot;
    private int cost;
    private Optional<Key> enchantment;
    private int level;

    public EnchantmentOffer(int slot, int cost, Optional<Key> enchantment, int level) {
        this.slot = slot;
        this.cost = Math.max(0, cost);
        this.enchantment = Objects.requireNonNull(enchantment, "enchantment");
        this.level = Math.max(0, level);
    }

    public int slot() {
        return slot;
    }

    public int cost() {
        return cost;
    }

    public void setCost(int cost) {
        this.cost = Math.max(0, cost);
    }

    public Optional<Key> enchantment() {
        return enchantment;
    }

    public void setEnchantment(Optional<Key> enchantment) {
        this.enchantment = Objects.requireNonNull(enchantment, "enchantment");
    }

    public int level() {
        return level;
    }

    public void setLevel(int level) {
        this.level = Math.max(0, level);
    }
}
