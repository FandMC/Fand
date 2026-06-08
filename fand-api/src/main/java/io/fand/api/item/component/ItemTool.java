package io.fand.api.item.component;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** Typed value for {@code minecraft:tool}. */
public record ItemTool(
        List<ItemTool.Rule> rules,
        float defaultMiningSpeed,
        int damagePerBlock,
        boolean canDestroyBlocksInCreative) implements ItemComponentData {

    public static final ItemTool DEFAULT = new ItemTool(List.of(), 1.0F, 1, true);

    public ItemTool {
        rules = List.copyOf(Objects.requireNonNull(rules, "rules"));
        if (damagePerBlock < 0) {
            throw new IllegalArgumentException("damagePerBlock must be >= 0");
        }
    }

    public static ItemTool fromJson(JsonElement value) {
        var object = ItemComponentJson.objectOrEmpty(value);
        var rules = new java.util.ArrayList<Rule>();
        var rawRules = object.get("rules");
        if (rawRules != null && rawRules.isJsonArray()) {
            for (var rule : rawRules.getAsJsonArray()) {
                rules.add(Rule.fromJson(rule));
            }
        }
        return new ItemTool(
                rules,
                ItemComponentJson.floatOr(object, "default_mining_speed", 1.0F),
                ItemComponentJson.intOr(object, "damage_per_block", 1),
                ItemComponentJson.booleanOr(object, "can_destroy_blocks_in_creative", true));
    }

    @Override
    public JsonObject toJson() {
        var json = new JsonObject();
        var ruleArray = new JsonArray();
        rules.forEach(rule -> ruleArray.add(rule.toJson()));
        json.add("rules", ruleArray);
        if (defaultMiningSpeed != 1.0F) {
            json.addProperty("default_mining_speed", defaultMiningSpeed);
        }
        if (damagePerBlock != 1) {
            json.addProperty("damage_per_block", damagePerBlock);
        }
        if (!canDestroyBlocksInCreative) {
            json.addProperty("can_destroy_blocks_in_creative", false);
        }
        return json;
    }

    public static final class Rule implements ItemComponentData {

        private final ItemKeySet blocks;
        private final @Nullable Float speed;
        private final @Nullable Boolean correctForDrops;

        public Rule(ItemKeySet blocks, @Nullable Float speed, @Nullable Boolean correctForDrops) {
            this.blocks = Objects.requireNonNull(blocks, "blocks");
            this.speed = speed;
            this.correctForDrops = correctForDrops;
            if (speed != null && speed <= 0.0F) {
                throw new IllegalArgumentException("speed must be > 0");
            }
        }

        public static Rule minesAndDrops(ItemKeySet blocks, float speed) {
            return new Rule(blocks, speed, true);
        }

        public static Rule deniesDrops(ItemKeySet blocks) {
            return new Rule(blocks, null, false);
        }

        public static Rule overrideSpeed(ItemKeySet blocks, float speed) {
            return new Rule(blocks, speed, null);
        }

        public static Rule fromJson(JsonElement value) {
            var object = ItemComponentJson.object(value, "tool rule");
            return new Rule(
                    ItemKeySet.fromJson(object.get("blocks")),
                    ItemComponentJson.optionalFloat(object, "speed").orElse(null),
                    object.has("correct_for_drops") ? object.get("correct_for_drops").getAsBoolean() : null);
        }

        public ItemKeySet blocks() {
            return blocks;
        }

        public Optional<Float> speed() {
            return Optional.ofNullable(speed);
        }

        public Optional<Boolean> correctForDrops() {
            return Optional.ofNullable(correctForDrops);
        }

        @Override
        public JsonObject toJson() {
            var json = new JsonObject();
            json.add("blocks", blocks.toJson());
            if (speed != null) {
                json.addProperty("speed", speed);
            }
            if (correctForDrops != null) {
                json.addProperty("correct_for_drops", correctForDrops);
            }
            return json;
        }

        @Override
        public boolean equals(@Nullable Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof Rule that)) {
                return false;
            }
            return blocks.equals(that.blocks)
                    && Objects.equals(speed, that.speed)
                    && Objects.equals(correctForDrops, that.correctForDrops);
        }

        @Override
        public int hashCode() {
            return Objects.hash(blocks, speed, correctForDrops);
        }

        @Override
        public String toString() {
            return "Rule[blocks=" + blocks + ", speed=" + speed() + ", correctForDrops=" + correctForDrops() + "]";
        }
    }
}
