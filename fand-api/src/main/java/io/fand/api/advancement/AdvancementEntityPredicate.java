package io.fand.api.advancement;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.fand.api.entity.EntityType;
import io.fand.api.registry.RegistryReference;
import java.util.Objects;
import net.kyori.adventure.key.Key;

public final class AdvancementEntityPredicate {

    private final JsonObject json;

    private AdvancementEntityPredicate(JsonObject json) {
        this.json = json.deepCopy();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static AdvancementEntityPredicate type(Key entityType) {
        return builder().type(entityType).build();
    }

    public static AdvancementEntityPredicate type(EntityType entityType) {
        return builder().type(entityType).build();
    }

    public JsonObject toJson() {
        return json.deepCopy();
    }

    public JsonObject toEntityPropertiesCondition(String entityTarget) {
        var condition = new JsonObject();
        condition.addProperty("condition", "minecraft:entity_properties");
        condition.addProperty("entity", Objects.requireNonNull(entityTarget, "entityTarget"));
        condition.add("predicate", toJson());
        return condition;
    }

    public static final class Builder {
        private final JsonObject json = new JsonObject();

        private Builder() {
        }

        public Builder type(Key entityType) {
            json.addProperty("minecraft:entity_type", Objects.requireNonNull(entityType, "entityType").asString());
            return this;
        }

        public Builder type(EntityType entityType) {
            return type(Objects.requireNonNull(entityType, "entityType").key());
        }

        public Builder type(RegistryReference entityType) {
            json.addProperty("minecraft:entity_type", Objects.requireNonNull(entityType, "entityType").asString());
            return this;
        }

        public Builder distance(AdvancementDistancePredicate distance) {
            json.add("minecraft:distance", Objects.requireNonNull(distance, "distance").toJson());
            return this;
        }

        public Builder located(AdvancementLocationPredicate location) {
            json.add("minecraft:location", Objects.requireNonNull(location, "location").toJson());
            return this;
        }

        public Builder steppingOn(AdvancementLocationPredicate location) {
            json.add("minecraft:stepping_on", Objects.requireNonNull(location, "location").toJson());
            return this;
        }

        public Builder team(String team) {
            json.addProperty("minecraft:team", Objects.requireNonNull(team, "team"));
            return this;
        }

        public Builder nbt(String snbt) {
            json.addProperty("minecraft:nbt", Objects.requireNonNull(snbt, "snbt"));
            return this;
        }

        public Builder property(Key predicate, JsonElement value) {
            json.add(Objects.requireNonNull(predicate, "predicate").asString(), Objects.requireNonNull(value, "value").deepCopy());
            return this;
        }

        public AdvancementEntityPredicate build() {
            return new AdvancementEntityPredicate(json);
        }
    }
}
