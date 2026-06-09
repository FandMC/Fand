package io.fand.api.event.server;

import io.fand.api.event.Event;
import io.fand.api.player.PlayerProfile;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.Nullable;

/**
 * Fired on the server thread while building the status response shown in the
 * multiplayer server list.
 */
public final class ServerListPingEvent implements Event {

    private Component motd;
    private int onlinePlayers;
    private int maxPlayers;
    private boolean hidePlayers;
    private @Nullable ServerListIcon icon;
    private ServerListVersion version;
    private List<PlayerProfile> samplePlayers;

    public ServerListPingEvent(Component motd, int onlinePlayers, int maxPlayers, boolean hidePlayers) {
        this(motd, onlinePlayers, maxPlayers, hidePlayers, null, new ServerListVersion("unknown", 0), List.of());
    }

    public ServerListPingEvent(
            Component motd,
            int onlinePlayers,
            int maxPlayers,
            boolean hidePlayers,
            @Nullable ServerListIcon icon,
            ServerListVersion version,
            List<PlayerProfile> samplePlayers
    ) {
        this.motd = Objects.requireNonNull(motd, "motd");
        this.onlinePlayers = Math.max(0, onlinePlayers);
        this.maxPlayers = Math.max(0, maxPlayers);
        this.hidePlayers = hidePlayers;
        this.icon = icon;
        this.version = Objects.requireNonNull(version, "version");
        this.samplePlayers = List.copyOf(Objects.requireNonNull(samplePlayers, "samplePlayers"));
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

    public Optional<ServerListIcon> icon() {
        return Optional.ofNullable(icon);
    }

    public void setIcon(@Nullable ServerListIcon icon) {
        this.icon = icon;
    }

    public void clearIcon() {
        this.icon = null;
    }

    public ServerListVersion version() {
        return version;
    }

    public void setVersion(ServerListVersion version) {
        this.version = Objects.requireNonNull(version, "version");
    }

    public List<PlayerProfile> samplePlayers() {
        return samplePlayers;
    }

    public void setSamplePlayers(List<PlayerProfile> samplePlayers) {
        this.samplePlayers = List.copyOf(Objects.requireNonNull(samplePlayers, "samplePlayers"));
    }
}
