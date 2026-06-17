package io.fand.api.protocol;

import io.netty.channel.ChannelPipeline;

/**
 * Provider installed by an external plugin to accept and translate client
 * protocol versions that differ from the running server version.
 */
public interface ProtocolCompatibilityProvider {

    boolean acceptsClientProtocol(int clientProtocol, int serverProtocol);

    void inject(ChannelPipeline pipeline);
}
