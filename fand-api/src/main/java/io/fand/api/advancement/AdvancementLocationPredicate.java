package io.fand.api.advancement;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.fand.api.block.BlockType;
import io.fand.api.registry.RegistryReference;
import java.util.Objects;
import net.kyori.adventure.key.Key;

public final class AdvancementLocationPredicate {

    private final JsonObject json;

    private AdvancementLocationPredicate(JsonObject json) {
        this.json = json.deepCopy();
    }

    public static Builder builder() {
        return new Builder();
    }

    public JsonObject toJson() {
        return json.deepCopy();
    }

    public JsonObject toLocationCheckCondition() {
        var condition = new JsonObject();
        condition.addProperty("condition", "minecraft:location_check");
        condition.add("predicate", toJson());
        return condition;
    }

    public static final class Builder {
        private final JsonObject json = new JsonObject();

        private Builder() {
        }

        public Builder x(AdvancementRange x) {
            return position("x", x);
        }

        public Builder y(AdvancementRange y) {
            return position("y", y);
        }

        public Builder z(AdvancementRange z) {
            return position("z", z);
        }

        public Builder dimension(Key dimension) {
            json.addProperty("dimension", Objects.requireNonNull(dimension, "dimension").asString());
            return this;
        }

        public Builder biome(Key biome) {
            json.addProperty("biomes", Objects.requireNonNull(biome, "biome").asString());
            return this;
        }

        public Builder biomeTag(Key biomeTag) {
            json.addProperty("biomes", "#" + Objects.requireNonNull(biomeTag, "biomeTag").asString());
            return this;
        }

        public Builder structure(Key structure) {
            json.addProperty("structures", Objects.requireNonNull(structure, "structure").asString());
            return this;
        }

        public Builder structureTag(Key structureTag) {
            json.addProperty("structures", "#" + Objects.requireNonNull(structureTag, "structureTag").asString());
            return this;
        }

        public Builder block(BlockType block) {
            return block(RegistryReference.key(Objects.requireNonNull(block, "block").key()));
        }

        public Builder block(Key block) {
            return block(RegistryReference.key(Objects.requireNonNull(block, "block")));
        }

        public Builder block(RegistryReference block) {
            var predicate = new JsonObject();
            predicate.addProperty("blocks", Objects.requireNonNull(block, "block").asString());
            json.add("block", predicate);
            return this;
        }

        public Builder fluid(Key fluid) {
            var predicate = new JsonObject();
            predicate.addProperty("fluids", Objects.requireNonNull(fluid, "fluid").asString());
            json.add("fluid", predicate);
            return this;
        }

        public Builder light(AdvancementRange light) {
            var predicate = new JsonObject();
            predicate.add("light", Objects.requireNonNull(light, "light").toJson());
            json.add("light", predicate);
            return this;
        }

        public Builder smokey(boolean smokey) {
            json.addProperty("smokey", smokey);
            return this;
        }

        public Builder canSeeSky(boolean canSeeSky) {
            json.addProperty("can_see_sky", canSeeSky);
            return this;
        }

        public Builder raw(String field, JsonElement value) {
            json.add(Objects.requireNonNull(field, "field"), Objects.requireNonNull(value, "value").deepCopy());
            return this;
        }

        public AdvancementLocationPredicate build() {
            return new AdvancementLocationPredicate(json);
        }

        private Builder position(String axis, AdvancementRange range) {
            var position = json.has("position") && json.get("position").isJsonObject()
                    ? json.getAsJsonObject("position")
                    : new JsonObject();
            position.add(axis, Objects.requireNonNull(range, axis).toJson());
            json.add("position", position);
            return this;
        }
    }
}
