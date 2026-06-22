package io.fand.server.plugin;

import static org.assertj.core.api.Assertions.assertThat;

import io.fand.api.gamerule.GameRuleDefinition;
import io.fand.api.gamerule.GameRuleType;
import io.fand.server.gamerule.FandGameRuleService;
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.Test;

final class PluginGameRuleServiceTest {

    @Test
    void scopesDefinitionsToPluginNamespace() {
        var delegate = new FandGameRuleService();
        var service = new PluginGameRuleService(delegate, new PluginResourceTracker(), "demo");

        var registration = service.register(new GameRuleDefinition(
                Key.key("external:enabled"),
                GameRuleType.BOOLEAN,
                "true"));

        assertThat(registration.key()).isEqualTo(Key.key("demo:enabled"));
        assertThat(delegate.definition(Key.key("external:enabled"))).isEmpty();
        assertThat(delegate.definition(Key.key("demo:enabled"))).isPresent();
        assertThat(service.definition(Key.key("other:enabled")).map(GameRuleDefinition::key))
                .contains(Key.key("demo:enabled"));
        assertThat(service.definitions()).extracting(GameRuleDefinition::key)
                .containsExactly(Key.key("demo:enabled"));
    }

    @Test
    void trackerUnregistersPluginGameRules() {
        var delegate = new FandGameRuleService();
        var tracker = new PluginResourceTracker();
        var service = new PluginGameRuleService(delegate, tracker, "demo");
        var registration = service.register(new GameRuleDefinition(
                Key.key("demo:enabled"),
                GameRuleType.BOOLEAN,
                "true"));

        tracker.close();

        assertThat(registration.active()).isFalse();
        assertThat(delegate.definition(Key.key("demo:enabled"))).isEmpty();
    }
}
