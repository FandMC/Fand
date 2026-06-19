package io.fand.api.advancement;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;

public record CustomAdvancement(
        Key key,
        Optional<Key> parent,
        Optional<AdvancementDisplay> display,
        AdvancementRewards rewards,
        List<AdvancementCriterion> criteria,
        List<List<String>> requirements,
        boolean sendsTelemetryEvent
) {

    public CustomAdvancement(Key key, Component title, Component description, List<String> criteria) {
        this(
                key,
                Optional.empty(),
                Optional.of(AdvancementDisplay.task(title, description)),
                AdvancementRewards.EMPTY,
                criteria.stream().map(AdvancementCriterion::impossible).toList(),
                criteria.stream().map(List::of).toList(),
                false);
    }

    public CustomAdvancement {
        Objects.requireNonNull(key, "key");
        parent = Objects.requireNonNull(parent, "parent");
        display = Objects.requireNonNull(display, "display");
        Objects.requireNonNull(rewards, "rewards");
        criteria = List.copyOf(criteria);
        if (criteria.isEmpty()) {
            throw new IllegalArgumentException("custom advancement must have at least one criterion");
        }
        requirements = requirements == null || requirements.isEmpty()
                ? criteria.stream().map(criterion -> List.of(criterion.name())).toList()
                : requirements.stream().map(List::copyOf).toList();
        validateRequirements(criteria, requirements);
    }

    public Component title() {
        return display.map(AdvancementDisplay::title).orElseGet(() -> net.kyori.adventure.text.Component.text(key.asString()));
    }

    public Component description() {
        return display.map(AdvancementDisplay::description).orElseGet(() -> net.kyori.adventure.text.Component.empty());
    }

    public List<String> criterionNames() {
        return criteria.stream().map(AdvancementCriterion::name).toList();
    }

    public static Builder builder(Key key) {
        return new Builder(key);
    }

    public JsonObject toVanillaJson() {
        var json = new JsonObject();
        parent.ifPresent(key -> json.addProperty("parent", key.asString()));
        display.ifPresent(value -> json.add("display", value.toVanillaJson()));
        var rewardJson = rewards.toVanillaJson();
        if (rewardJson.size() > 0) {
            json.add("rewards", rewardJson);
        }
        var criteriaJson = new JsonObject();
        for (var criterion : criteria) {
            criteriaJson.add(criterion.name(), criterion.trigger());
        }
        json.add("criteria", criteriaJson);
        if (!requirements.isEmpty()) {
            var groups = new JsonArray();
            for (var group : requirements) {
                var groupJson = new JsonArray();
                group.forEach(groupJson::add);
                groups.add(groupJson);
            }
            json.add("requirements", groups);
        }
        json.addProperty("sends_telemetry_event", sendsTelemetryEvent);
        return json;
    }

    private static void validateRequirements(List<AdvancementCriterion> criteria, List<List<String>> requirements) {
        var criterionNames = criteria.stream()
                .map(AdvancementCriterion::name)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        for (var group : requirements) {
            if (group.isEmpty()) {
                throw new IllegalArgumentException("advancement requirement group must not be empty");
            }
            for (var name : group) {
                if (!criterionNames.contains(name)) {
                    throw new IllegalArgumentException("unknown advancement requirement criterion: " + name);
                }
            }
        }
    }

    public static final class Builder {
        private final Key key;
        private Optional<Key> parent = Optional.empty();
        private Optional<AdvancementDisplay> display = Optional.empty();
        private AdvancementRewards rewards = AdvancementRewards.EMPTY;
        private List<AdvancementCriterion> criteria = List.of();
        private List<List<String>> requirements = List.of();
        private boolean sendsTelemetryEvent;

        private Builder(Key key) {
            this.key = Objects.requireNonNull(key, "key");
        }

        public Builder parent(Key parent) {
            this.parent = Optional.of(parent);
            return this;
        }

        public Builder display(AdvancementDisplay display) {
            this.display = Optional.of(display);
            return this;
        }

        public Builder rewards(AdvancementRewards rewards) {
            this.rewards = Objects.requireNonNull(rewards, "rewards");
            return this;
        }

        public Builder criteria(List<AdvancementCriterion> criteria) {
            this.criteria = List.copyOf(criteria);
            return this;
        }

        public Builder requirements(List<List<String>> requirements) {
            this.requirements = requirements.stream().map(List::copyOf).toList();
            return this;
        }

        public Builder sendsTelemetryEvent(boolean sendsTelemetryEvent) {
            this.sendsTelemetryEvent = sendsTelemetryEvent;
            return this;
        }

        public CustomAdvancement build() {
            return new CustomAdvancement(key, parent, display, rewards, criteria, requirements, sendsTelemetryEvent);
        }
    }
}
