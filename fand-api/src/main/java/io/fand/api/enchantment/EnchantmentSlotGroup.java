package io.fand.api.enchantment;

public enum EnchantmentSlotGroup {
    ANY("any"),
    MAINHAND("mainhand"),
    OFFHAND("offhand"),
    HAND("hand"),
    FEET("feet"),
    LEGS("legs"),
    CHEST("chest"),
    HEAD("head"),
    ARMOR("armor"),
    BODY("body"),
    SADDLE("saddle");

    private final String serializedName;

    EnchantmentSlotGroup(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
