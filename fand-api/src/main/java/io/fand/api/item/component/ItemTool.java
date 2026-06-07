package io.fand.api.item.component;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

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

    public record Rule(ItemKeySet blocks, Optional<Float> speed, Optional<Boolean> correctForDrops) implements ItemComponentData {

        public Rule {
            blocks = Objects.requireNonNull(blocks, "blocks");
            speed = Objects.requireNonNull(speed, "speed");
            correctForDrops = Objects.requireNonNull(correctForDrops, "correctForDrops");
            speed.ifPresent(value -> {
                if (value <= 0.0F) {
                    throw new IllegalArgumentException("speed must be > 0");
                }
            });
        }

        public static Rule minesAndDrops(ItemKeySet blocks, float speed) {
            return new Rule(blocks, Optional.of(speed), Optional.of(true));
        }

        public static Rule deniesDrops(ItemKeySet blocks) {
            return new Rule(blocks, Optional.empty(), Optional.of(false));
        }

        public static Rule overrideSpeed(ItemKeySet blocks, float speed) {
            return new Rule(blocks, Optional.of(speed), Optional.empty());
        }

        public static Rule fromJson(JsonElement value) {
            var object = ItemComponentJson.object(value, "tool rule");
            return new Rule(
                    ItemKeySet.fromJson(object.get("blocks")),
                    ItemComponentJson.optionalFloat(object, "speed"),
                    object.has("correct_for_drops")
                            ? Optional.of(object.get("correct_for_drops").getAsBoolean())
                            : Optional.empty());
        }

        @Override
        public JsonObject toJson() {
            var json = new JsonObject();
            json.add("blocks", blocks.toJson());
            speed.ifPresent(value -> json.addProperty("speed", value));
            correctForDrops.ifPresent(value -> json.addProperty("correct_for_drops", value));
            return json;
        }
    }
}
