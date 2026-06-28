package io.fand.server.plugin;

import static org.assertj.core.api.Assertions.assertThat;

import io.fand.api.service.ServicePriority;
import io.fand.server.service.FandServiceRegistry;
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.Test;

final class PluginServiceRegistryTest {

    @Test
    void registersWithPluginOwnerAndCleansOnClose() {
        var delegate = new FandServiceRegistry();
        var tracker = new PluginResourceTracker();
        var service = new PluginServiceRegistry(delegate, tracker, "vault");
        var api = new EconomyService();

        var registration = service.register(Key.key("vault:economy"), EconomyApi.class, api, ServicePriority.HIGH);

        assertThat(registration.owner()).isEqualTo("vault");
        assertThat(delegate.service(EconomyApi.class)).containsSame(api);
        assertThat(delegate.provider(Key.key("vault:economy"), EconomyApi.class))
                .get()
                .extracting(provider -> provider.owner())
                .isEqualTo("vault");

        tracker.close();

        assertThat(registration.active()).isFalse();
        assertThat(delegate.service(EconomyApi.class)).isEmpty();
    }

    private interface EconomyApi {
    }

    private static final class EconomyService implements EconomyApi {
    }
}
