package io.fand.server.network;

import io.fand.server.config.FandConfig;

public record ProxyForwardingSettings(ProxyForwardingMode mode, String secret) {

    public static ProxyForwardingSettings fromConfig(FandConfig config) {
        return new ProxyForwardingSettings(
                ProxyForwardingMode.fromConfig(config.network.forwarding.mode),
                config.network.forwarding.secret);
    }
}
