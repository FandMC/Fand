package io.fand.api.bossbar;

import io.fand.api.entity.Player;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;

/**
 * Mutable server-owned boss bar view.
 *
 * <p>The underlying {@link BossBar} remains the source of truth for title,
 * progress, color, overlay, and flags. Fand tracks only which players are
 * currently viewing this handle.
 */
public interface BossBarHandle extends AutoCloseable {

    boolean active();

    BossBar bossBar();

    Collection<? extends Player> viewers();

    void show(Player player);

    default void show(Collection<? extends Player> players) {
        Objects.requireNonNull(players, "players").forEach(this::show);
    }

    void hide(Player player);

    default void hide(Collection<? extends Player> players) {
        Objects.requireNonNull(players, "players").forEach(this::hide);
    }

    void hideAll();

    default Component title() {
        return bossBar().name();
    }

    default void setTitle(Component title) {
        bossBar().name(Objects.requireNonNull(title, "title"));
    }

    default float progress() {
        return bossBar().progress();
    }

    default void setProgress(float progress) {
        bossBar().progress(progress);
    }

    default BossBar.Color color() {
        return bossBar().color();
    }

    default void setColor(BossBar.Color color) {
        bossBar().color(Objects.requireNonNull(color, "color"));
    }

    default BossBar.Overlay overlay() {
        return bossBar().overlay();
    }

    default void setOverlay(BossBar.Overlay overlay) {
        bossBar().overlay(Objects.requireNonNull(overlay, "overlay"));
    }

    default Set<BossBar.Flag> flags() {
        return bossBar().flags();
    }

    default void setFlags(Set<BossBar.Flag> flags) {
        bossBar().flags(Set.copyOf(Objects.requireNonNull(flags, "flags")));
    }

    default void addFlag(BossBar.Flag flag) {
        bossBar().addFlag(Objects.requireNonNull(flag, "flag"));
    }

    default void removeFlag(BossBar.Flag flag) {
        bossBar().removeFlag(Objects.requireNonNull(flag, "flag"));
    }

    @Override
    void close();
}
