package io.fand.api.enchantment;

import com.google.gson.JsonArray;

public record Vec3i(int x, int y, int z) implements EnchantmentJsonValue {

    public static final Vec3i ZERO = new Vec3i(0, 0, 0);

    @Override
    public JsonArray toJson() {
        var json = new JsonArray();
        json.add(x);
        json.add(y);
        json.add(z);
        return json;
    }
}
