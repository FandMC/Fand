package io.fand.api.enchantment;

public record EnchantmentCost(int base, int perLevelAboveFirst) {

    public static EnchantmentCost constant(int base) {
        return new EnchantmentCost(base, 0);
    }

    public static EnchantmentCost dynamic(int base, int perLevelAboveFirst) {
        return new EnchantmentCost(base, perLevelAboveFirst);
    }

    public EnchantmentCost {
        if (base < 0) {
            throw new IllegalArgumentException("base must be >= 0");
        }
        if (perLevelAboveFirst < 0) {
            throw new IllegalArgumentException("perLevelAboveFirst must be >= 0");
        }
    }
}
