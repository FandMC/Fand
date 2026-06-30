package io.fand.api.auth;

import io.fand.api.player.PlayerProfile;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.Nullable;

/**
 * Result returned by a login authenticator.
 */
public final class LoginAuthenticationResult {

    private static final LoginAuthenticationResult PASS = new LoginAuthenticationResult(Action.PASS, null, null);

    private final Action action;
    private final @Nullable PlayerProfile profile;
    private final @Nullable Component reason;

    private LoginAuthenticationResult(Action action, @Nullable PlayerProfile profile, @Nullable Component reason) {
        this.action = Objects.requireNonNull(action, "action");
        this.profile = profile;
        this.reason = reason;
    }

    /**
     * This authenticator does not handle the request; the next registered
     * authenticator may decide.
     */
    public static LoginAuthenticationResult pass() {
        return PASS;
    }

    public static LoginAuthenticationResult allow(PlayerProfile profile) {
        return new LoginAuthenticationResult(Action.ALLOW, Objects.requireNonNull(profile, "profile"), null);
    }

    public static LoginAuthenticationResult deny(Component reason) {
        return new LoginAuthenticationResult(Action.DENY, null, Objects.requireNonNull(reason, "reason"));
    }

    public Action action() {
        return action;
    }

    public @Nullable PlayerProfile profileOrNull() {
        return profile;
    }

    public @Nullable Component reasonOrNull() {
        return reason;
    }

    public enum Action {
        PASS,
        ALLOW,
        DENY
    }
}
