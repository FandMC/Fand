package io.fand.api.bossbar;

import io.fand.api.entity.Player;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;

/**
 * Keyed boss bar service with per-player display control.
 */
public interface BossBarService {

    Collection<? extends BossBarRegistration> bars();

    Optional<? extends BossBarRegistration> bar(Key key);

    BossBarRegistration register(Key key, BossBar bossBar);

    default BossBarRegistration register(
            Key key,
            Component title,
            float progress,
            BossBar.Color color,
            BossBar.Overlay overlay
    ) {
        return register(key, BossBar.bossBar(title, progress, color, overlay));
    }

    BossBarHandle send(Collection<? extends Player> viewers, BossBar bossBar, Duration duration);

    default BossBarHandle send(Player viewer, BossBar bossBar, Duration duration) {
        return send(List.of(Objects.requireNonNull(viewer, "viewer")), bossBar, duration);
    }

    default BossBarHandle send(
            Collection<? extends Player> viewers,
            Component title,
            float progress,
            BossBar.Color color,
            BossBar.Overlay overlay,
            Duration duration
    ) {
        return send(viewers, BossBar.bossBar(title, progress, color, overlay), duration);
    }

    default BossBarHandle send(
            Player viewer,
            Component title,
            float progress,
            BossBar.Color color,
            BossBar.Overlay overlay,
            Duration duration
    ) {
        return send(List.of(Objects.requireNonNull(viewer, "viewer")), title, progress, color, overlay, duration);
    }

    boolean remove(Key key);

    static BossBarService empty() {
        return new BossBarService() {
            @Override
            public Collection<? extends BossBarRegistration> bars() {
                return List.of();
            }

            @Override
            public Optional<? extends BossBarRegistration> bar(Key key) {
                Objects.requireNonNull(key, "key");
                return Optional.empty();
            }

            @Override
            public BossBarRegistration register(Key key, BossBar bossBar) {
                throw new UnsupportedOperationException("Boss bars are not supported");
            }

            @Override
            public BossBarHandle send(Collection<? extends Player> viewers, BossBar bossBar, Duration duration) {
                throw new UnsupportedOperationException("Boss bars are not supported");
            }

            @Override
            public boolean remove(Key key) {
                Objects.requireNonNull(key, "key");
                return false;
            }
        };
    }
}
