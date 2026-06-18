package io.fand.server.protocol;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

final class ProtocolCompatibilityServiceTest {

    @Test
    void rejectsMismatchedProtocolWithoutProvider() {
        var service = new ProtocolCompatibilityManager();

        assertThat(service.acceptsClientProtocol(800, 800)).isTrue();
        assertThat(service.acceptsClientProtocol(767, 800)).isFalse();
        assertThat(service.acceptsClientProtocol(801, 800)).isFalse();
    }

    @Test
    void registeredProviderCanAcceptProtocolRange() {
        var service = new ProtocolCompatibilityManager();
        var registration = service.register(new io.fand.api.protocol.ProtocolCompatibilityProvider() {
            @Override
            public boolean acceptsClientProtocol(int clientProtocol, int serverProtocol) {
                return clientProtocol >= 767;
            }

            @Override
            public void inject(io.netty.channel.ChannelPipeline pipeline) {
            }
        });

        try {
            assertThat(service.acceptsClientProtocol(767, 800)).isTrue();
            assertThat(service.acceptsClientProtocol(801, 800)).isTrue();
            assertThat(service.acceptsClientProtocol(766, 800)).isFalse();
        } finally {
            registration.unregister();
        }

        assertThat(service.acceptsClientProtocol(767, 800)).isFalse();
    }
}
