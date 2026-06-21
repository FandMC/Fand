package io.fand.api.enchantment;

public enum EnchantmentAttributeOperation {
    ADD_VALUE("add_value"),
    ADD_MULTIPLIED_BASE("add_multiplied_base"),
    ADD_MULTIPLIED_TOTAL("add_multiplied_total");

    private final String serializedName;

    EnchantmentAttributeOperation(String serializedName) {
        this.serializedName = serializedName;
    }

    public String serializedName() {
        return serializedName;
    }
}
