package io.fand.api.event.player;

import io.fand.api.event.Event;
import io.fand.api.player.PlayerProfile;
import java.net.SocketAddress;
import java.util.Objects;
import java.util.UUID;
import net.kyori.adventure.text.Component;

/**
 * Fired from the login/authentication path before a player object exists.
 *
 * <p>This event may be fired off the server thread. Listeners must not touch
 * live world state directly.
 */
public final class AsyncPlayerPreLoginEvent implements Event {

    private final SocketAddress address;
    private PlayerProfile profile;
    private Result result;
    private Component kickMessage;

    public AsyncPlayerPreLoginEvent(UUID uniqueId, String name, SocketAddress address, Result result, Component kickMessage) {
        this(new PlayerProfile(uniqueId, name), address, result, kickMessage);
    }

    public AsyncPlayerPreLoginEvent(
            PlayerProfile profile,
            SocketAddress address,
            Result result,
            Component kickMessage
    ) {
        this.profile = Objects.requireNonNull(profile, "profile");
        this.address = Objects.requireNonNull(address, "address");
        this.result = Objects.requireNonNull(result, "result");
        this.kickMessage = Objects.requireNonNull(kickMessage, "kickMessage");
    }

    public UUID uniqueId() {
        return profile.uniqueId();
    }

    public String name() {
        return profile.name();
    }

    public PlayerProfile profile() {
        return profile;
    }

    public void setProfile(PlayerProfile profile) {
        this.profile = Objects.requireNonNull(profile, "profile");
    }

    public SocketAddress address() {
        return address;
    }

    public Result result() {
        return result;
    }

    public void setResult(Result result) {
        this.result = Objects.requireNonNull(result, "result");
    }

    public Component kickMessage() {
        return kickMessage;
    }

    public void setKickMessage(Component kickMessage) {
        this.kickMessage = Objects.requireNonNull(kickMessage, "kickMessage");
    }

    public void allow() {
        this.result = Result.ALLOWED;
    }

    public void disallow(Result result, Component kickMessage) {
        if (result == Result.ALLOWED) {
            throw new IllegalArgumentException("disallow result must not be ALLOWED");
        }
        this.result = Objects.requireNonNull(result, "result");
        this.kickMessage = Objects.requireNonNull(kickMessage, "kickMessage");
    }

    public enum Result {
        ALLOWED,
        KICK_FULL,
        KICK_BANNED,
        KICK_WHITELIST,
        KICK_OTHER
    }
}
