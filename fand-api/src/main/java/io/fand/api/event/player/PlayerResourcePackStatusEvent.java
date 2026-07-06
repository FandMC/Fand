package io.fand.api.event.player;

import io.fand.api.entity.Player;
import io.fand.api.event.Event;
import java.util.Objects;
import java.util.UUID;

/**
 * Fired on the server thread when a player responds to a server resource pack.
 */
public record PlayerResourcePackStatusEvent(Player player, UUID id, Status status) implements Event {

    public enum Status {
        ACCEPTED,
        DOWNLOADED,
        SUCCESSFULLY_LOADED,
        DECLINED,
        FAILED_DOWNLOAD,
        INVALID_URL,
        FAILED_RELOAD,
        DISCARDED
    }

    public PlayerResourcePackStatusEvent(Player player, UUID id, Status status) {
        this.player = Objects.requireNonNull(player, "player");
        this.id = Objects.requireNonNull(id, "id");
        this.status = Objects.requireNonNull(status, "status");
    }
}
