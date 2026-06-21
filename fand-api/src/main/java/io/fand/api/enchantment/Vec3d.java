package io.fand.api.enchantment;

import com.google.gson.JsonArray;

public record Vec3d(double x, double y, double z) implements EnchantmentJsonValue {

    public static final Vec3d ZERO = new Vec3d(0.0, 0.0, 0.0);
    public static final Vec3d ONE = new Vec3d(1.0, 1.0, 1.0);

    @Override
    public JsonArray toJson() {
        var json = new JsonArray();
        json.add(x);
        json.add(y);
        json.add(z);
        return json;
    }
}
