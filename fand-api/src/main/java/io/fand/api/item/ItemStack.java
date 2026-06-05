package io.fand.api.item;

import java.util.Objects;

/**
 * An immutable item stack: a {@link ItemType} plus a positive {@code amount}.
 *
 * <p>The empty stack is represented by {@link #EMPTY}; equality is by type +
 * amount only — components/NBT are not modelled in this phase. Use
 * {@link #withAmount(int)} to build a copy with a different amount.
 */
public record ItemStack(ItemType type, int amount) {

    /** Sentinel empty stack (type {@code null}, amount 0). */
    public static final ItemStack EMPTY = new ItemStack(null, 0);

    public ItemStack {
        if (type == null) {
            if (amount != 0) {
                throw new IllegalArgumentException("Empty stack must have amount 0");
            }
        } else {
            Objects.requireNonNull(type, "type");
            if (amount < 1) {
                throw new IllegalArgumentException("Non-empty stack amount must be >= 1, got " + amount);
            }
            if (amount > type.maxStackSize()) {
                throw new IllegalArgumentException(
                        "Amount " + amount + " exceeds max stack size " + type.maxStackSize() + " for " + type.key().asString());
            }
        }
    }

    public boolean isEmpty() {
        return type == null;
    }

    public ItemStack withAmount(int newAmount) {
        if (isEmpty()) {
            return EMPTY;
        }
        return new ItemStack(type, newAmount);
    }

    public static ItemStack empty() {
        return EMPTY;
    }
}
