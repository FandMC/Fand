package io.fand.api.auth;

import java.net.InetAddress;
import java.net.SocketAddress;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Immutable data supplied to a login authenticator.
 */
public final class LoginAuthenticationRequest {

    private final String name;
    private final String serverId;
    private final SocketAddress remoteAddress;
    private final @Nullable InetAddress authenticationAddress;
    private final @Nullable UUID requestedProfileId;

    public LoginAuthenticationRequest(
            String name,
            String serverId,
            SocketAddress remoteAddress,
            @Nullable InetAddress authenticationAddress
    ) {
        this(name, serverId, remoteAddress, authenticationAddress, null);
    }

    public LoginAuthenticationRequest(
            String name,
            String serverId,
            SocketAddress remoteAddress,
            @Nullable InetAddress authenticationAddress,
            @Nullable UUID requestedProfileId
    ) {
        this.name = Objects.requireNonNull(name, "name");
        this.serverId = Objects.requireNonNull(serverId, "serverId");
        this.remoteAddress = Objects.requireNonNull(remoteAddress, "remoteAddress");
        this.authenticationAddress = authenticationAddress;
        this.requestedProfileId = requestedProfileId;
    }

    public String name() {
        return name;
    }

    /**
     * Server id digest produced by the encrypted vanilla login handshake.
     */
    public String serverId() {
        return serverId;
    }

    public SocketAddress remoteAddress() {
        return remoteAddress;
    }

    /**
     * Optional IP address passed to Yggdrasil-compatible session verification
     * when server-side proxy prevention is enabled.
     */
    public Optional<InetAddress> authenticationAddress() {
        return Optional.ofNullable(authenticationAddress);
    }

    public @Nullable InetAddress authenticationAddressOrNull() {
        return authenticationAddress;
    }

    /**
     * UUID supplied by the client in the login hello packet. Authenticators
     * that allow a login without Mojang session verification should preserve
     * this UUID when present, otherwise the vanilla client may reject the login
     * finished packet as a profile mismatch.
     */
    public Optional<UUID> requestedProfileId() {
        return Optional.ofNullable(requestedProfileId);
    }

    public @Nullable UUID requestedProfileIdOrNull() {
        return requestedProfileId;
    }
}
