package io.fand.server.advancement;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.JsonObject;
import io.fand.api.advancement.AdvancementCriterion;
import io.fand.api.advancement.AdvancementDisplay;
import io.fand.api.advancement.AdvancementEntityPredicate;
import io.fand.api.advancement.AdvancementFrame;
import io.fand.api.advancement.AdvancementItemPredicate;
import io.fand.api.advancement.AdvancementLocationPredicate;
import io.fand.api.advancement.AdvancementRange;
import io.fand.api.advancement.AdvancementRewards;
import io.fand.api.advancement.AdvancementTriggers;
import io.fand.api.advancement.CustomAdvancement;
import io.fand.api.item.ItemKey;
import io.fand.api.item.ItemType;
import io.fand.api.item.ItemStack;
import java.util.List;
import java.util.Optional;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

final class FandAdvancementRegistryTest {

    private static final Key KEY = Key.key("demo:first_step");

    @Test
    void registersCustomAdvancementWithoutAttachedServer() {
        var registry = new FandAdvancementRegistry(() -> null);
        var registration = registry.register(new CustomAdvancement(
                KEY,
                Component.text("First Step"),
                Component.text("Do the thing"),
                List.of("done")));

        assertThat(registration.key()).isEqualTo(KEY);
        assertThat(registration.active()).isTrue();
        assertThat(registry.advancement(KEY)).get().satisfies(view -> {
            assertThat(view.key()).isEqualTo(KEY);
            assertThat(view.title()).isEqualTo(Component.text("First Step"));
            assertThat(view.description()).isEqualTo(Component.text("Do the thing"));
        });

        registration.close();

        assertThat(registration.active()).isFalse();
        assertThat(registry.advancement(KEY)).isEmpty();
    }

    @Test
    void oldRegistrationCannotRemoveReplacement() {
        var registry = new FandAdvancementRegistry(() -> null);
        var first = registry.register(new CustomAdvancement(KEY, Component.text("First"), Component.text("One"), List.of("done")));
        var second = registry.register(new CustomAdvancement(KEY, Component.text("Second"), Component.text("Two"), List.of("done")));

        first.close();

        assertThat(first.active()).isFalse();
        assertThat(second.active()).isTrue();
        assertThat(registry.advancement(KEY)).get()
                .extracting(view -> view.title())
                .isEqualTo(Component.text("Second"));
    }

    @Test
    void customAdvancementCanDescribeVanillaJsonBeyondImpossibleCriteria() {
        var trigger = new JsonObject();
        trigger.addProperty("trigger", "minecraft:inventory_changed");
        var advancement = CustomAdvancement.builder(KEY)
                .parent(Key.key("minecraft:story/root"))
                .display(new AdvancementDisplay(
                        new ItemStack(testItem(ItemKey.DIAMOND), 1),
                        Component.text("Diamond"),
                        Component.text("Pick up a diamond"),
                        Key.key("minecraft:textures/gui/advancements/backgrounds/stone.png"),
                        AdvancementFrame.GOAL,
                        true,
                        false,
                        false))
                .rewards(new AdvancementRewards(
                        25,
                        List.of(Key.key("minecraft:chests/simple_dungeon")),
                        List.of(Key.key("minecraft:diamond_sword")),
                        Optional.of(Key.key("demo:reward"))))
                .criteria(List.of(AdvancementCriterion.vanilla("has_diamond", trigger)))
                .requirements(List.of(List.of("has_diamond")))
                .sendsTelemetryEvent(true)
                .build();

        var json = advancement.toVanillaJson();

        assertThat(json.get("parent").getAsString()).isEqualTo("minecraft:story/root");
        assertThat(json.getAsJsonObject("display").get("frame").getAsString()).isEqualTo("goal");
        assertThat(json.getAsJsonObject("criteria").getAsJsonObject("has_diamond").get("trigger").getAsString())
                .isEqualTo("minecraft:inventory_changed");
        assertThat(json.getAsJsonObject("rewards").get("experience").getAsInt()).isEqualTo(25);
        assertThat(json.get("sends_telemetry_event").getAsBoolean()).isTrue();
    }

    @Test
    void typedAdvancementTriggerDslBuildsInventoryChangedJson() {
        var criterion = AdvancementCriterion.trigger("has_stone_tool", AdvancementTriggers.inventoryChanged(
                AdvancementItemPredicate.builder()
                        .tag(Key.key("minecraft:stone_tool_materials"))
                        .count(AdvancementRange.atLeast(1))
                        .build()));

        var json = criterion.trigger();

        assertThat(json.get("trigger").getAsString()).isEqualTo("minecraft:inventory_changed");
        var item = json.getAsJsonObject("conditions").getAsJsonArray("items").get(0).getAsJsonObject();
        assertThat(item.get("items").getAsString()).isEqualTo("#minecraft:stone_tool_materials");
        assertThat(item.getAsJsonObject("count").get("min").getAsDouble()).isEqualTo(1.0);
    }

    @Test
    void typedAdvancementTriggerDslBuildsEntityAndLocationJson() {
        var location = AdvancementLocationPredicate.builder()
                .biome(Key.key("minecraft:badlands"))
                .canSeeSky(true)
                .build();
        var criterion = AdvancementCriterion.trigger("visit_badlands", AdvancementTriggers.location(location));

        var json = criterion.trigger();

        assertThat(json.get("trigger").getAsString()).isEqualTo("minecraft:location");
        var playerCondition = json.getAsJsonObject("conditions").getAsJsonArray("player").get(0).getAsJsonObject();
        assertThat(playerCondition.get("condition").getAsString()).isEqualTo("minecraft:entity_properties");
        assertThat(playerCondition.get("entity").getAsString()).isEqualTo("this");
        var predicate = playerCondition.getAsJsonObject("predicate");
        assertThat(predicate.getAsJsonObject("minecraft:location").get("biomes").getAsString()).isEqualTo("minecraft:badlands");
        assertThat(predicate.getAsJsonObject("minecraft:location").get("can_see_sky").getAsBoolean()).isTrue();
    }

    @Test
    void typedAdvancementTriggerDslBuildsDamageAndVictimJson() {
        var criterion = AdvancementCriterion.trigger("hurt_zombie", AdvancementTriggers.playerHurtEntity(
                io.fand.api.advancement.AdvancementDamagePredicate.builder()
                        .dealt(AdvancementRange.atLeast(4))
                        .blocked(false)
                        .build(),
                AdvancementEntityPredicate.type(Key.key("minecraft:zombie"))));

        var json = criterion.trigger();

        assertThat(json.get("trigger").getAsString()).isEqualTo("minecraft:player_hurt_entity");
        var conditions = json.getAsJsonObject("conditions");
        assertThat(conditions.getAsJsonObject("damage").getAsJsonObject("dealt").get("min").getAsDouble()).isEqualTo(4.0);
        assertThat(conditions.getAsJsonObject("damage").get("blocked").getAsBoolean()).isFalse();
        var victim = conditions.getAsJsonArray("entity").get(0).getAsJsonObject();
        assertThat(victim.getAsJsonObject("predicate").get("minecraft:entity_type").getAsString()).isEqualTo("minecraft:zombie");
    }

    private static ItemType testItem(ItemKey key) {
        return new ItemType() {
            @Override
            public Key key() {
                return key.key();
            }

            @Override
            public int maxStackSize() {
                return 64;
            }
        };
    }
}
