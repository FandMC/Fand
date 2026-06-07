package io.fand.api.item.component;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.List;
import java.util.Objects;

/** Typed value for {@code minecraft:fireworks}. */
public record ItemFireworks(int flightDuration, List<ItemFireworkExplosion> explosions) implements ItemComponentData {

    public static final int MAX_EXPLOSIONS = 256;

    public ItemFireworks {
        if (flightDuration < 0 || flightDuration > 255) {
            throw new IllegalArgumentException("flightDuration must be in 0..255");
        }
        explosions = List.copyOf(Objects.requireNonNull(explosions, "explosions"));
        if (explosions.size() > MAX_EXPLOSIONS) {
            throw new IllegalArgumentException("explosions size must be <= " + MAX_EXPLOSIONS);
        }
    }

    public static ItemFireworks fromJson(JsonElement value) {
        if (value == null || !value.isJsonObject()) {
            return new ItemFireworks(0, List.of());
        }
        var object = value.getAsJsonObject();
        var explosions = new java.util.ArrayList<ItemFireworkExplosion>();
        var rawExplosions = object.get("explosions");
        if (rawExplosions != null && rawExplosions.isJsonArray()) {
            for (var explosion : rawExplosions.getAsJsonArray()) {
                explosions.add(ItemFireworkExplosion.fromJson(explosion));
            }
        }
        return new ItemFireworks(
                object.has("flight_duration") ? object.get("flight_duration").getAsInt() : 0,
                explosions);
    }

    @Override
    public JsonObject toJson() {
        var json = new JsonObject();
        if (flightDuration != 0) {
            json.addProperty("flight_duration", flightDuration);
        }
        if (!explosions.isEmpty()) {
            var array = new JsonArray();
            explosions.forEach(explosion -> array.add(explosion.toJson()));
            json.add("explosions", array);
        }
        return json;
    }
}
