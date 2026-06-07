package io.fand.api.item.component;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Objects;

/** Typed value for {@code minecraft:lock}. */
public record ItemLock(JsonObject predicate) implements ItemComponentData {

    public ItemLock {
        predicate = ItemComponentJson.copyObject(Objects.requireNonNull(predicate, "predicate"));
    }

    public static ItemLock fromJson(JsonElement value) {
        return new ItemLock(ItemComponentJson.object(value, "lock predicate"));
    }

    public JsonObject predicate() {
        return predicate.deepCopy();
    }

    @Override
    public JsonObject toJson() {
        return predicate();
    }
}
