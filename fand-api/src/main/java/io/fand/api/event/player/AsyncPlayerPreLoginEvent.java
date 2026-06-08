package io.fand.api.event.player;

import io.fand.api.event.Event;
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

    private final UUID uniqueId;
    private final String name;
    private final SocketAddress address;
    private Result result;
    private Component kickMessage;

    public AsyncPlayerPreLoginEvent(UUID uniqueId, String name, SocketAddress address, Result result, Component kickMessage) {
        this.uniqueId = Objects.requireNonNull(uniqueId, "uniqueId");
        this.name = Objects.requireNonNull(name, "name");
        this.address = Objects.requireNonNull(address, "address");
        this.result = Objects.requireNonNull(result, "result");
        this.kickMessage = Objects.requireNonNull(kickMessage, "kickMessage");
    }

    public UUID uniqueId() {
        return uniqueId;
    }

    public String name() {
        return name;
    }

    public SocketAddress address() {
        return address;
    }

    public Result result() {
        return result;
    }

    public Component kickMessage() {
        return kickMessage;
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
