package io.fand.api.item.component;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.List;
import java.util.Objects;

/** Typed value for {@code minecraft:bees}. */
public record ItemBees(List<ItemBees.Occupant> bees) implements ItemComponentData {

    public static final ItemBees EMPTY = new ItemBees(List.of());

    public ItemBees {
        bees = List.copyOf(Objects.requireNonNull(bees, "bees"));
    }

    public static ItemBees fromJson(JsonElement value) {
        if (value == null || !value.isJsonArray()) {
            return EMPTY;
        }
        var bees = new java.util.ArrayList<Occupant>();
        for (var bee : value.getAsJsonArray()) {
            bees.add(Occupant.fromJson(bee));
        }
        return new ItemBees(bees);
    }

    @Override
    public JsonArray toJson() {
        var json = new JsonArray();
        bees.forEach(bee -> json.add(bee.toJson()));
        return json;
    }

    public record Occupant(ItemTypedEntityData entityData, int ticksInHive, int minTicksInHive) implements ItemComponentData {

        public Occupant {
            entityData = Objects.requireNonNull(entityData, "entityData");
        }

        public static Occupant fromJson(JsonElement value) {
            var object = ItemComponentJson.object(value, "bee occupant");
            return new Occupant(
                    ItemTypedEntityData.fromJson(object.get("entity_data")),
                    object.get("ticks_in_hive").getAsInt(),
                    object.get("min_ticks_in_hive").getAsInt());
        }

        @Override
        public JsonObject toJson() {
            var json = new JsonObject();
            json.add("entity_data", entityData.toJson());
            json.addProperty("ticks_in_hive", ticksInHive);
            json.addProperty("min_ticks_in_hive", minTicksInHive);
            return json;
        }
    }
}
