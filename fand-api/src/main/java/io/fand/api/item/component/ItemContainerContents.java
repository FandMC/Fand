package io.fand.api.item.component;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.List;
import java.util.Objects;

/** Typed value for {@code minecraft:container}. */
public record ItemContainerContents(List<ItemContainerContents.Slot> slots) implements ItemComponentData {

    public static final int MAX_SIZE = 256;
    public static final ItemContainerContents EMPTY = new ItemContainerContents(List.of());

    public ItemContainerContents {
        slots = List.copyOf(Objects.requireNonNull(slots, "slots"));
        if (slots.size() > MAX_SIZE) {
            throw new IllegalArgumentException("slots size must be <= " + MAX_SIZE);
        }
    }

    public static ItemContainerContents fromJson(JsonElement value) {
        if (value == null || !value.isJsonArray()) {
            return EMPTY;
        }
        var slots = new java.util.ArrayList<Slot>();
        for (var element : value.getAsJsonArray()) {
            if (element.isJsonObject()) {
                var object = element.getAsJsonObject();
                slots.add(new Slot(object.get("slot").getAsInt(), ItemTemplate.fromJson(object.get("item"))));
            }
        }
        return new ItemContainerContents(slots);
    }

    public ItemContainerContents withSlot(int slot, ItemTemplate item) {
        var next = new java.util.ArrayList<Slot>();
        for (var existing : slots) {
            if (existing.index() != slot) {
                next.add(existing);
            }
        }
        next.add(new Slot(slot, item));
        return new ItemContainerContents(next);
    }

    @Override
    public JsonArray toJson() {
        var array = new JsonArray();
        slots.forEach(slot -> array.add(slot.toJson()));
        return array;
    }

    public record Slot(int index, ItemTemplate item) implements ItemComponentData {

        public Slot {
            if (index < 0 || index > 255) {
                throw new IllegalArgumentException("slot index must be in 0..255");
            }
            item = Objects.requireNonNull(item, "item");
        }

        @Override
        public JsonObject toJson() {
            var json = new JsonObject();
            json.addProperty("slot", index);
            json.add("item", item.toJson());
            return json;
        }
    }
}
