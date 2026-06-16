package io.fand.server.hooks;

import static org.assertj.core.api.Assertions.assertThat;

import io.fand.server.config.FandConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

final class FandHooksProtocolCompatibilityTest {

    @AfterEach
    void resetNetworkConfig() {
        FandHooks.applyNetworkConfig(new FandConfig().network);
    }

    @Test
    void acceptsOnlyExactProtocolByDefault() {
        FandHooks.applyNetworkConfig(new FandConfig().network);

        assertThat(FandHooks.acceptsClientProtocol(800, 800)).isTrue();
        assertThat(FandHooks.acceptsClientProtocol(767, 800)).isFalse();
        assertThat(FandHooks.acceptsClientProtocol(801, 800)).isFalse();
    }

    @Test
    void acceptsMinecraft21AndNewerWhenCompatibilityIsEnabled() {
        var config = new FandConfig();
        config.network.protocolCompatibility.allowMinecraft21AndNewer = true;
        FandHooks.applyNetworkConfig(config.network);

        assertThat(FandHooks.acceptsClientProtocol(766, 800)).isFalse();
        assertThat(FandHooks.acceptsClientProtocol(767, 800)).isTrue();
        assertThat(FandHooks.acceptsClientProtocol(801, 800)).isTrue();
    }
}
