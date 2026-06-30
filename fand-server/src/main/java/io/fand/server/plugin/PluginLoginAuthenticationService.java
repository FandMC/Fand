package io.fand.server.plugin;

import io.fand.api.auth.LoginAuthenticationRegistration;
import io.fand.api.auth.LoginAuthenticationService;
import io.fand.api.auth.LoginAuthenticator;
import io.fand.api.auth.LoginAuthenticatorProvider;
import io.fand.api.service.ServicePriority;
import io.fand.server.auth.FandLoginAuthenticationService;
import java.util.List;
import java.util.Objects;
import net.kyori.adventure.key.Key;

final class PluginLoginAuthenticationService implements LoginAuthenticationService {

    private final LoginAuthenticationService delegate;
    private final PluginResourceTracker tracker;
    private final String owner;

    PluginLoginAuthenticationService(
            LoginAuthenticationService delegate,
            PluginResourceTracker tracker,
            String owner
    ) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.tracker = Objects.requireNonNull(tracker, "tracker");
        this.owner = Objects.requireNonNull(owner, "owner");
    }

    @Override
    public LoginAuthenticationRegistration register(Key key, LoginAuthenticator authenticator) {
        return register(key, authenticator, ServicePriority.NORMAL);
    }

    @Override
    public LoginAuthenticationRegistration register(
            Key key,
            LoginAuthenticator authenticator,
            ServicePriority priority
    ) {
        if (delegate instanceof FandLoginAuthenticationService registry) {
            return tracker.track(registry.register(key, authenticator, priority, owner));
        }
        return tracker.track(delegate.register(key, authenticator, priority));
    }

    @Override
    public List<LoginAuthenticatorProvider> authenticators() {
        return delegate.authenticators();
    }
}
