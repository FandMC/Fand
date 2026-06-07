package io.fand.api.item.component;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.List;
import java.util.Objects;
import net.kyori.adventure.key.Key;

/** Typed value for {@code minecraft:attribute_modifiers}. */
public record ItemAttributeModifiers(List<ItemAttributeModifiers.Entry> modifiers) implements ItemComponentData {

    public static final ItemAttributeModifiers EMPTY = new ItemAttributeModifiers(List.of());

    public ItemAttributeModifiers {
        modifiers = List.copyOf(Objects.requireNonNull(modifiers, "modifiers"));
    }

    public ItemAttributeModifiers with(Entry entry) {
        var next = new java.util.ArrayList<>(modifiers);
        next.add(Objects.requireNonNull(entry, "entry"));
        return new ItemAttributeModifiers(next);
    }

    public static ItemAttributeModifiers fromJson(JsonElement value) {
        if (value == null || !value.isJsonArray()) {
            return EMPTY;
        }
        var entries = new java.util.ArrayList<Entry>();
        for (var entry : value.getAsJsonArray()) {
            entries.add(Entry.fromJson(entry));
        }
        return new ItemAttributeModifiers(entries);
    }

    @Override
    public JsonArray toJson() {
        var json = new JsonArray();
        modifiers.forEach(entry -> json.add(entry.toJson()));
        return json;
    }

    public record Entry(
            Key attribute,
            Key id,
            double amount,
            ItemAttributeModifierOperation operation,
            ItemEquipmentSlotGroup slot,
            ItemAttributeModifierDisplay display) implements ItemComponentData {

        public Entry {
            attribute = Objects.requireNonNull(attribute, "attribute");
            id = Objects.requireNonNull(id, "id");
            operation = Objects.requireNonNull(operation, "operation");
            slot = Objects.requireNonNull(slot, "slot");
            display = Objects.requireNonNull(display, "display");
        }

        public Entry(Key attribute, Key id, double amount, ItemAttributeModifierOperation operation) {
            this(attribute, id, amount, operation, ItemEquipmentSlotGroup.ANY, ItemAttributeModifierDisplay.DEFAULT);
        }

        public static Entry fromJson(JsonElement value) {
            var object = ItemComponentJson.object(value, "attribute modifier entry");
            return new Entry(
                    ItemComponentJson.key(object, "type"),
                    ItemComponentJson.key(object, "id"),
                    object.get("amount").getAsDouble(),
                    ItemAttributeModifierOperation.fromSerializedName(object.get("operation").getAsString()),
                    object.has("slot")
                            ? ItemEquipmentSlotGroup.fromSerializedName(object.get("slot").getAsString())
                            : ItemEquipmentSlotGroup.ANY,
                    object.has("display")
                            ? ItemAttributeModifierDisplay.fromJson(object.get("display"))
                            : ItemAttributeModifierDisplay.DEFAULT);
        }

        @Override
        public JsonObject toJson() {
            var json = new JsonObject();
            json.addProperty("type", attribute.asString());
            json.addProperty("id", id.asString());
            json.addProperty("amount", amount);
            json.addProperty("operation", operation.serializedName());
            if (slot != ItemEquipmentSlotGroup.ANY) {
                json.addProperty("slot", slot.serializedName());
            }
            if (!display.equals(ItemAttributeModifierDisplay.DEFAULT)) {
                json.add("display", display.toJson());
            }
            return json;
        }
    }
}
