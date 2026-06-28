package io.fand.server.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.fand.api.service.ServicePriority;
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.Test;

final class FandServiceRegistryTest {

    @Test
    void returnsHighestPriorityProviderForType() {
        var registry = new FandServiceRegistry();
        var low = new DemoService("low");
        var high = new DemoService("high");

        registry.register(Key.key("demo:low"), DemoApi.class, low, ServicePriority.LOW, "low-plugin");
        registry.register(Key.key("demo:high"), DemoApi.class, high, ServicePriority.HIGH, "high-plugin");

        assertThat(registry.service(DemoApi.class)).containsSame(high);
        assertThat(registry.providers(DemoApi.class))
                .extracting(provider -> provider.service().name())
                .containsExactly("high", "low");
    }

    @Test
    void samePriorityUsesNewestProviderAndFallsBackAfterUnregister() {
        var registry = new FandServiceRegistry();
        var first = registry.register(
                Key.key("demo:first"),
                DemoApi.class,
                new DemoService("first"),
                ServicePriority.NORMAL,
                "first");
        var second = registry.register(
                Key.key("demo:second"),
                DemoApi.class,
                new DemoService("second"),
                ServicePriority.NORMAL,
                "second");

        assertThat(registry.service(DemoApi.class).map(DemoApi::name)).contains("second");
        assertThat(registry.providers(DemoApi.class))
                .extracting(provider -> provider.service().name())
                .containsExactly("second", "first");

        second.unregister();

        assertThat(first.active()).isTrue();
        assertThat(registry.service(DemoApi.class).map(DemoApi::name)).contains("first");
    }

    @Test
    void replacingSameKeyAndTypeInvalidatesOldRegistration() {
        var registry = new FandServiceRegistry();
        var key = Key.key("demo:economy");
        var first = registry.register(key, DemoApi.class, new DemoService("first"), ServicePriority.NORMAL, "first");
        var second = registry.register(key, DemoApi.class, new DemoService("second"), ServicePriority.NORMAL, "second");

        assertThat(first.active()).isFalse();
        assertThat(second.active()).isTrue();
        assertThat(registry.service(key, DemoApi.class).map(DemoApi::name)).contains("second");

        second.unregister();

        assertThat(registry.service(key, DemoApi.class)).isEmpty();
    }

    @Test
    void closeInvalidatesRegistrations() {
        var registry = new FandServiceRegistry();
        var registration = registry.register(
                Key.key("demo:chat"),
                DemoApi.class,
                new DemoService("chat"),
                ServicePriority.NORMAL,
                "chat");

        registry.close();

        assertThat(registration.active()).isFalse();
        assertThat(registry.providers()).isEmpty();
    }

    private interface DemoApi {
        String name();
    }

    private record DemoService(String name) implements DemoApi {
    }
}
