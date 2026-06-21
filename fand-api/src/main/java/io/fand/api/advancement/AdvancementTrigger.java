package io.fand.api.advancement;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Objects;
import net.kyori.adventure.key.Key;

public final class AdvancementTrigger {

    private final Key type;
    private final JsonObject conditions;

    AdvancementTrigger(Key type, JsonObject conditions) {
        this.type = Objects.requireNonNull(type, "type");
        this.conditions = conditions.deepCopy();
    }

    public Key type() {
        return type;
    }

    public JsonObject conditions() {
        return conditions.deepCopy();
    }

    public JsonObject toVanillaJson() {
        var json = new JsonObject();
        if (conditions.size() > 0) {
            json.add("conditions", conditions());
        }
        json.addProperty("trigger", type.asString());
        return json;
    }

    public static Builder builder(Key type) {
        return new Builder(type);
    }

    public static final class Builder {
        private final Key type;
        private final JsonObject conditions = new JsonObject();

        private Builder(Key type) {
            this.type = Objects.requireNonNull(type, "type");
        }

        public Builder condition(String name, JsonElement value) {
            conditions.add(Objects.requireNonNull(name, "name"), Objects.requireNonNull(value, "value").deepCopy());
            return this;
        }

        public Builder conditions(JsonObject conditions) {
            this.conditions.entrySet().clear();
            Objects.requireNonNull(conditions, "conditions")
                    .entrySet()
                    .forEach(entry -> this.conditions.add(entry.getKey(), entry.getValue().deepCopy()));
            return this;
        }

        public Builder player(AdvancementEntityPredicate player) {
            var array = new com.google.gson.JsonArray();
            array.add(Objects.requireNonNull(player, "player").toEntityPropertiesCondition("this"));
            return condition("player", array);
        }

        public Builder entity(AdvancementEntityPredicate entity) {
            var array = new com.google.gson.JsonArray();
            array.add(Objects.requireNonNull(entity, "entity").toEntityPropertiesCondition("this"));
            return condition("entity", array);
        }

        public Builder item(AdvancementItemPredicate item) {
            return condition("item", Objects.requireNonNull(item, "item").toJson());
        }

        public Builder location(AdvancementLocationPredicate location) {
            var array = new com.google.gson.JsonArray();
            array.add(Objects.requireNonNull(location, "location").toLocationCheckCondition());
            return condition("location", array);
        }

        public Builder damage(AdvancementDamagePredicate damage) {
            return condition("damage", Objects.requireNonNull(damage, "damage").toJson());
        }

        public AdvancementTrigger build() {
            return new AdvancementTrigger(type, conditions);
        }
    }
}
