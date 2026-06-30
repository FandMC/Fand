package io.fand.api.auth;

import io.fand.api.service.ServicePriority;
import net.kyori.adventure.key.Key;

public record LoginAuthenticatorProvider(
        Key key,
        LoginAuthenticator authenticator,
        String owner,
        ServicePriority priority
) {
}
