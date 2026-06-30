package io.fand.api.auth;

import io.fand.api.service.ServicePriority;
import java.util.List;
import java.util.Objects;
import net.kyori.adventure.key.Key;

/**
 * Registers authentication entry points used by the server login pipeline.
 */
public interface LoginAuthenticationService {

    LoginAuthenticationRegistration register(Key key, LoginAuthenticator authenticator);

    LoginAuthenticationRegistration register(Key key, LoginAuthenticator authenticator, ServicePriority priority);

    List<LoginAuthenticatorProvider> authenticators();

    static LoginAuthenticationService empty() {
        return Empty.INSTANCE;
    }

    enum Empty implements LoginAuthenticationService {
        INSTANCE;

        @Override
        public LoginAuthenticationRegistration register(Key key, LoginAuthenticator authenticator) {
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(authenticator, "authenticator");
            throw new UnsupportedOperationException("Login authenticator registration is not supported");
        }

        @Override
        public LoginAuthenticationRegistration register(
                Key key,
                LoginAuthenticator authenticator,
                ServicePriority priority
        ) {
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(authenticator, "authenticator");
            Objects.requireNonNull(priority, "priority");
            throw new UnsupportedOperationException("Login authenticator registration is not supported");
        }

        @Override
        public List<LoginAuthenticatorProvider> authenticators() {
            return List.of();
        }
    }
}
