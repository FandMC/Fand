package io.fand.server.gamerule;

import static org.assertj.core.api.Assertions.assertThat;

import io.fand.api.gamerule.GameRuleDefinition;
import io.fand.api.gamerule.GameRuleType;
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.Test;

final class FandGameRuleServiceTest {

    @Test
    void storesCustomRuleValuesPerWorld() {
        var service = new FandGameRuleService();
        var rule = Key.key("demo:limit");
        var overworld = Key.key("minecraft:overworld");
        var nether = Key.key("minecraft:the_nether");

        service.register(new GameRuleDefinition(rule, GameRuleType.INTEGER, "5"));

        assertThat(service.value(overworld, rule)).contains("5");
        assertThat(service.setValue(overworld, rule, "9")).isTrue();
        assertThat(service.value(overworld, rule)).contains("9");
        assertThat(service.value(nether, rule)).contains("5");
    }

    @Test
    void unregisterRemovesDefinitionAndValues() {
        var service = new FandGameRuleService();
        var rule = Key.key("demo:enabled");
        var world = Key.key("minecraft:overworld");
        var registration = service.register(new GameRuleDefinition(rule, GameRuleType.BOOLEAN, "true"));

        service.setValue(world, rule, "false");
        registration.unregister();

        assertThat(registration.active()).isFalse();
        assertThat(service.definition(rule)).isEmpty();
        assertThat(service.value(world, rule)).isEmpty();
    }

    @Test
    void rejectsInvalidValuesWithoutMutatingCurrentValue() {
        var service = new FandGameRuleService();
        var rule = Key.key("demo:enabled");
        var world = Key.key("minecraft:overworld");

        service.register(new GameRuleDefinition(rule, GameRuleType.BOOLEAN, "true"));

        assertThat(service.setValue(world, rule, "yes")).isFalse();
        assertThat(service.value(world, rule)).contains("true");
    }
}
