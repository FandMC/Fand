package io.fand.api.gamerule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.Test;

final class GameRuleDefinitionTest {

    @Test
    void validatesBooleanRulesStrictly() {
        var rule = new GameRuleDefinition(Key.key("demo:enabled"), GameRuleType.BOOLEAN, "true");

        assertThat(rule.validValue("true")).isTrue();
        assertThat(rule.validValue("false")).isTrue();
        assertThat(rule.validValue("TRUE")).isFalse();
        assertThat(rule.validValue(null)).isFalse();
    }

    @Test
    void validatesIntegerRules() {
        var rule = new GameRuleDefinition(Key.key("demo:limit"), GameRuleType.INTEGER, "10");

        assertThat(rule.validValue("-1")).isTrue();
        assertThat(rule.validValue("abc")).isFalse();
    }

    @Test
    void rejectsInvalidDefaults() {
        assertThatThrownBy(() -> new GameRuleDefinition(Key.key("demo:enabled"), GameRuleType.BOOLEAN, "yes"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new GameRuleDefinition(Key.key("demo:limit"), GameRuleType.INTEGER, "many"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
