package io.fand.api.protocol;

import io.netty.channel.ChannelPipeline;

public interface ProtocolCompatibilityService {

    ProtocolCompatibilityRegistration register(ProtocolCompatibilityProvider provider);

    boolean acceptsClientProtocol(int clientProtocol, int serverProtocol);

    void inject(ChannelPipeline pipeline);

    static ProtocolCompatibilityService unsupported() {
        return UnsupportedProtocolCompatibilityService.INSTANCE;
    }

    enum UnsupportedProtocolCompatibilityService implements ProtocolCompatibilityService {
        INSTANCE;

        @Override
        public ProtocolCompatibilityRegistration register(ProtocolCompatibilityProvider provider) {
            throw new UnsupportedOperationException("Protocol compatibility is not available");
        }

        @Override
        public boolean acceptsClientProtocol(int clientProtocol, int serverProtocol) {
            return clientProtocol == serverProtocol;
        }

        @Override
        public void inject(ChannelPipeline pipeline) {
        }
    }
}
