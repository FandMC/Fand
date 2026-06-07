package io.fand.api.item.component;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.fand.api.block.BlockEntityKey;
import io.fand.api.entity.EntityKey;
import java.util.Objects;
import net.kyori.adventure.key.Key;

/** Typed entity or block-entity data component with an explicit type key. */
public record ItemTypedEntityData(Key type, JsonObject data) implements ItemComponentData {

    public ItemTypedEntityData {
        type = Objects.requireNonNull(type, "type");
        data = ItemComponentJson.withoutId(Objects.requireNonNull(data, "data"));
    }

    public ItemTypedEntityData(EntityKey type, JsonObject data) {
        this(Objects.requireNonNull(type, "type").key(), data);
    }

    public ItemTypedEntityData(BlockEntityKey type, JsonObject data) {
        this(Objects.requireNonNull(type, "type").key(), data);
    }

    public static ItemTypedEntityData fromJson(JsonElement value) {
        var object = ItemComponentJson.object(value, "typed entity data");
        return new ItemTypedEntityData(ItemComponentJson.key(object, "id"), object);
    }

    public JsonObject data() {
        return data.deepCopy();
    }

    @Override
    public JsonObject toJson() {
        return ItemComponentJson.withId(data, type);
    }
}
