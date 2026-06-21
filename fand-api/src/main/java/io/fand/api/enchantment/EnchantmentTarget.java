package io.fand.api.enchantment;

public enum EnchantmentTarget {
    ATTACKER("attacker"),
    DAMAGING_ENTITY("damaging_entity"),
    VICTIM("victim");

    private final String serializedName;

    EnchantmentTarget(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
