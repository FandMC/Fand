package io.fand.api.auth;

import io.fand.api.service.ServicePriority;
import net.kyori.adventure.key.Key;

public interface LoginAuthenticationRegistration extends AutoCloseable {

    Key key();

    LoginAuthenticator authenticator();

    String owner();

    ServicePriority priority();

    boolean active();

    void unregister();

    @Override
    default void close() {
        unregister();
    }
}
