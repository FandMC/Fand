package io.fand.server.protocol;

import io.fand.api.protocol.ProtocolCompatibilityProvider;
import io.fand.api.protocol.ProtocolCompatibilityRegistration;
import io.fand.api.protocol.ProtocolCompatibilityService;
import io.netty.channel.ChannelPipeline;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

public final class ProtocolCompatibilityManager implements ProtocolCompatibilityService, AutoCloseable {

    private final CopyOnWriteArrayList<Registration> providers = new CopyOnWriteArrayList<>();

    @Override
    public ProtocolCompatibilityRegistration register(ProtocolCompatibilityProvider provider) {
        Objects.requireNonNull(provider, "provider");
        var registration = new Registration(this, provider);
        providers.add(registration);
        return registration;
    }

    @Override
    public boolean acceptsClientProtocol(int clientProtocol, int serverProtocol) {
        if (clientProtocol == serverProtocol) {
            return true;
        }
        for (var registration : providers) {
            if (registration.active() && registration.provider.acceptsClientProtocol(clientProtocol, serverProtocol)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void inject(ChannelPipeline pipeline) {
        Objects.requireNonNull(pipeline, "pipeline");
        for (var registration : providers) {
            if (registration.active()) {
                registration.provider.inject(pipeline);
            }
        }
    }

    @Override
    public void close() {
        for (var registration : providers) {
            registration.unregister();
        }
        providers.clear();
    }

    private void release(Registration registration) {
        providers.remove(registration);
    }

    private static final class Registration implements ProtocolCompatibilityRegistration {

        private final ProtocolCompatibilityManager owner;
        private final ProtocolCompatibilityProvider provider;
        private volatile boolean active = true;

        private Registration(ProtocolCompatibilityManager owner, ProtocolCompatibilityProvider provider) {
            this.owner = owner;
            this.provider = provider;
        }

        @Override
        public boolean active() {
            return active;
        }

        @Override
        public void unregister() {
            if (active) {
                active = false;
                owner.release(this);
            }
        }
    }
}
