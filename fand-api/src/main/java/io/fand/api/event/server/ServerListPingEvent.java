package io.fand.api.event.server;

import io.fand.api.event.Event;
import java.util.Objects;
import net.kyori.adventure.text.Component;

/**
 * Fired on the server thread while building the status response shown in the
 * multiplayer server list.
 */
public final class ServerListPingEvent implements Event {

    private Component motd;
    private int onlinePlayers;
    private int maxPlayers;
    private boolean hidePlayers;

    public ServerListPingEvent(Component motd, int onlinePlayers, int maxPlayers, boolean hidePlayers) {
        this.motd = Objects.requireNonNull(motd, "motd");
        this.onlinePlayers = Math.max(0, onlinePlayers);
        this.maxPlayers = Math.max(0, maxPlayers);
        this.hidePlayers = hidePlayers;
    }

    public Component motd() {
        return motd;
    }

    public void setMotd(Component motd) {
        this.motd = Objects.requireNonNull(motd, "motd");
    }

    public int onlinePlayers() {
        return onlinePlayers;
    }

    public void setOnlinePlayers(int onlinePlayers) {
        this.onlinePlayers = Math.max(0, onlinePlayers);
    }

    public int maxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = Math.max(0, maxPlayers);
    }

    public boolean hidePlayers() {
        return hidePlayers;
    }

    public void setHidePlayers(boolean hidePlayers) {
        this.hidePlayers = hidePlayers;
    }
}
