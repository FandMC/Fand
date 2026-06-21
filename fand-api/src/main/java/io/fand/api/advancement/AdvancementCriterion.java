package io.fand.api.advancement;

import com.google.gson.JsonObject;
import java.util.Objects;

public record AdvancementCriterion(String name, JsonObject trigger) {

    public AdvancementCriterion {
        Objects.requireNonNull(name, "name");
        if (name.isBlank()) {
            throw new IllegalArgumentException("criterion name must not be blank");
        }
        Objects.requireNonNull(trigger, "trigger");
        trigger = trigger.deepCopy();
    }

    public static AdvancementCriterion impossible(String name) {
        var trigger = new JsonObject();
        trigger.addProperty("trigger", "minecraft:impossible");
        return new AdvancementCriterion(name, trigger);
    }

    public static AdvancementCriterion vanilla(String name, JsonObject trigger) {
        return new AdvancementCriterion(name, trigger);
    }

    public static AdvancementCriterion trigger(String name, AdvancementTrigger trigger) {
        return new AdvancementCriterion(name, Objects.requireNonNull(trigger, "trigger").toVanillaJson());
    }
}
