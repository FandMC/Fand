package io.fand.api.item.component;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

/** Typed block position encoded like vanilla {@code BlockPos.CODEC}. */
public record ItemBlockPosition(int x, int y, int z) implements ItemComponentData {

    public static ItemBlockPosition fromJson(JsonElement value) {
        if (value == null || !value.isJsonArray() || value.getAsJsonArray().size() != 3) {
            throw new IllegalArgumentException("block position must be an array of 3 integers");
        }
        var array = value.getAsJsonArray();
        return new ItemBlockPosition(
                array.get(0).getAsInt(),
                array.get(1).getAsInt(),
                array.get(2).getAsInt());
    }

    @Override
    public JsonArray toJson() {
        var json = new JsonArray();
        json.add(x);
        json.add(y);
        json.add(z);
        return json;
    }
}
