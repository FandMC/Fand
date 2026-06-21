package io.fand.api.advancement;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Objects;
import net.kyori.adventure.key.Key;

public final class AdvancementDamagePredicate {

    private final JsonObject json;

    private AdvancementDamagePredicate(JsonObject json) {
        this.json = json.deepCopy();
    }

    public static Builder builder() {
        return new Builder();
    }

    public JsonObject toJson() {
        return json.deepCopy();
    }

    public static final class Builder {
        private final JsonObject json = new JsonObject();

        private Builder() {
        }

        public Builder dealt(AdvancementRange dealt) {
            json.add("dealt", Objects.requireNonNull(dealt, "dealt").toJson());
            return this;
        }

        public Builder taken(AdvancementRange taken) {
            json.add("taken", Objects.requireNonNull(taken, "taken").toJson());
            return this;
        }

        public Builder sourceEntity(AdvancementEntityPredicate sourceEntity) {
            json.add("source_entity", Objects.requireNonNull(sourceEntity, "sourceEntity").toJson());
            return this;
        }

        public Builder blocked(boolean blocked) {
            json.addProperty("blocked", blocked);
            return this;
        }

        public Builder type(JsonObject type) {
            json.add("type", Objects.requireNonNull(type, "type").deepCopy());
            return this;
        }

        public Builder typeTag(Key tag) {
            var type = new JsonObject();
            type.addProperty("tags", "#" + Objects.requireNonNull(tag, "tag").asString());
            json.add("type", type);
            return this;
        }

        public Builder raw(String field, JsonElement value) {
            json.add(Objects.requireNonNull(field, "field"), Objects.requireNonNull(value, "value").deepCopy());
            return this;
        }

        public AdvancementDamagePredicate build() {
            return new AdvancementDamagePredicate(json);
        }
    }
}
