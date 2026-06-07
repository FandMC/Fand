package io.fand.api.item.component;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Objects;
import java.util.Optional;

/** Typed block predicate entry used by adventure-mode item components. */
public record ItemBlockPredicate(
        Optional<ItemKeySet> blocks,
        Optional<JsonObject> state,
        Optional<JsonObject> nbt,
        Optional<JsonObject> components) implements ItemComponentData {

    public ItemBlockPredicate {
        blocks = Objects.requireNonNull(blocks, "blocks");
        state = copyOptional(state, "state");
        nbt = copyOptional(nbt, "nbt");
        components = copyOptional(components, "components");
    }

    public static ItemBlockPredicate blocks(ItemKeySet blocks) {
        return new ItemBlockPredicate(Optional.of(blocks), Optional.empty(), Optional.empty(), Optional.empty());
    }

    public static ItemBlockPredicate fromJson(JsonElement value) {
        var object = ItemComponentJson.object(value, "block predicate");
        return new ItemBlockPredicate(
                object.has("blocks") ? Optional.of(ItemKeySet.fromJson(object.get("blocks"))) : Optional.empty(),
                ItemComponentJson.optionalObject(object, "state"),
                ItemComponentJson.optionalObject(object, "nbt"),
                ItemComponentJson.optionalObject(object, "components"));
    }

    @Override
    public JsonObject toJson() {
        var json = new JsonObject();
        blocks.ifPresent(value -> json.add("blocks", value.toJson()));
        state.ifPresent(value -> json.add("state", value.deepCopy()));
        nbt.ifPresent(value -> json.add("nbt", value.deepCopy()));
        components.ifPresent(value -> json.add("components", value.deepCopy()));
        return json;
    }

    private static Optional<JsonObject> copyOptional(Optional<JsonObject> value, String name) {
        Objects.requireNonNull(value, name);
        return value.map(JsonObject::deepCopy);
    }
}
