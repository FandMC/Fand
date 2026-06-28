package io.fand.server.plugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.fand.api.placeholder.PlaceholderContext;
import io.fand.api.placeholder.PlaceholderProvider;
import io.fand.server.placeholder.FandPlaceholderService;
import org.junit.jupiter.api.Test;

final class PluginPlaceholderServiceTest {

    @Test
    void scopesRegistrationToPluginNamespaceAndCleansOnClose() {
        var delegate = new FandPlaceholderService();
        var tracker = new PluginResourceTracker();
        var service = new PluginPlaceholderService(delegate, tracker, "demo");

        var registration = service.register("demo", (viewer, identifier) -> "ok");

        assertThat(registration.active()).isTrue();
        assertThat(delegate.resolve(null, "demo_value")).contains("ok");

        tracker.close();

        assertThat(registration.active()).isFalse();
        assertThat(delegate.resolve(null, "demo_value")).isEmpty();
    }

    @Test
    void forwardsContextualResolutionThroughPluginScope() {
        var delegate = new FandPlaceholderService();
        var tracker = new PluginResourceTracker();
        var service = new PluginPlaceholderService(delegate, tracker, "demo");
        service.register("demo", PlaceholderProvider.contextual((context, identifier) ->
                context.value("state", String.class).map(state -> identifier + ":" + state).orElse(null)));

        assertThat(service.resolve("demo_value", PlaceholderContext.builder().value("state", "ok").build()))
                .contains("demo_value:ok");
    }

    @Test
    void rejectsForeignNamespace() {
        var service = new PluginPlaceholderService(new FandPlaceholderService(), new PluginResourceTracker(), "demo");

        assertThatThrownBy(() -> service.register("other", (viewer, identifier) -> "nope"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("own namespace");
    }
}
