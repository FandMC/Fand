package io.fand.api.advancement;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.fand.api.item.ItemType;
import io.fand.api.registry.RegistryReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.kyori.adventure.key.Key;

public final class AdvancementItemPredicate {

    private final List<RegistryReference> items;
    private final AdvancementRange count;
    private final JsonObject components;
    private final JsonObject predicates;

    private AdvancementItemPredicate(
            List<RegistryReference> items,
            AdvancementRange count,
            JsonObject components,
            JsonObject predicates
    ) {
        this.items = List.copyOf(items);
        this.count = Objects.requireNonNull(count, "count");
        this.components = components.deepCopy();
        this.predicates = predicates.deepCopy();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static AdvancementItemPredicate item(Key item) {
        return builder().item(item).build();
    }

    public static AdvancementItemPredicate item(ItemType item) {
        return builder().item(item).build();
    }

    public static AdvancementItemPredicate tag(Key tag) {
        return builder().tag(tag).build();
    }

    public JsonObject toJson() {
        var json = new JsonObject();
        if (!items.isEmpty()) {
            if (items.size() == 1) {
                json.addProperty("items", items.getFirst().asString());
            } else {
                var array = new com.google.gson.JsonArray();
                items.forEach(reference -> array.add(reference.asString()));
                json.add("items", array);
            }
        }
        if (!count.any()) {
            json.add("count", count.toJson());
        }
        if (components.size() > 0) {
            json.add("components", components.deepCopy());
        }
        if (predicates.size() > 0) {
            json.add("predicates", predicates.deepCopy());
        }
        return json;
    }

    public static final class Builder {
        private final List<RegistryReference> items = new ArrayList<>();
        private AdvancementRange count = AdvancementRange.ANY;
        private JsonObject components = new JsonObject();
        private JsonObject predicates = new JsonObject();

        private Builder() {
        }

        public Builder item(Key item) {
            items.add(RegistryReference.key(Objects.requireNonNull(item, "item")));
            return this;
        }

        public Builder item(ItemType item) {
            return item(Objects.requireNonNull(item, "item").key());
        }

        public Builder tag(Key tag) {
            items.add(RegistryReference.tag(Objects.requireNonNull(tag, "tag")));
            return this;
        }

        public Builder reference(RegistryReference reference) {
            items.add(Objects.requireNonNull(reference, "reference"));
            return this;
        }

        public Builder count(AdvancementRange count) {
            this.count = Objects.requireNonNull(count, "count");
            return this;
        }

        public Builder component(Key component, JsonElement value) {
            components.add(Objects.requireNonNull(component, "component").asString(), Objects.requireNonNull(value, "value").deepCopy());
            return this;
        }

        public Builder predicate(Key predicate, JsonElement value) {
            predicates.add(Objects.requireNonNull(predicate, "predicate").asString(), Objects.requireNonNull(value, "value").deepCopy());
            return this;
        }

        public Builder components(JsonObject components) {
            this.components = Objects.requireNonNull(components, "components").deepCopy();
            return this;
        }

        public Builder predicates(JsonObject predicates) {
            this.predicates = Objects.requireNonNull(predicates, "predicates").deepCopy();
            return this;
        }

        public AdvancementItemPredicate build() {
            return new AdvancementItemPredicate(items, count, components, predicates);
        }
    }
}
