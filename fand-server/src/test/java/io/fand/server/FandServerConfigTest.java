package io.fand.server;

import static org.assertj.core.api.Assertions.assertThat;

import io.fand.api.network.ProxyForwardingMode;
import io.fand.server.config.FandConfig;
import org.junit.jupiter.api.Test;

final class FandServerConfigTest {

    @Test
    void reportsConfiguredBrand() {
        var config = new FandConfig();
        config.identity.brand = "Configured Brand";

        var server = new FandServer(config, getClass().getClassLoader());

        assertThat(server.brand()).isEqualTo("Configured Brand");
    }

    @Test
    void reportsConfiguredProxyForwardingMode() {
        var config = new FandConfig();
        config.network.forwarding.mode = "velocity-modern";

        var server = new FandServer(config, getClass().getClassLoader());

        assertThat(server.proxyForwardingMode()).isEqualTo(ProxyForwardingMode.VELOCITY_MODERN);
    }
}
