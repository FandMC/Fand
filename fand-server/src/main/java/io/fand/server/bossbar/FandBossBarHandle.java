package io.fand.server.bossbar;

import io.fand.api.bossbar.BossBarHandle;
import io.fand.api.entity.Player;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;

public class FandBossBarHandle implements BossBarHandle {

    private final BossBar bossBar;
    private final Consumer<Runnable> updater;
    private final LinkedHashMap<UUID, Player> viewers = new LinkedHashMap<>();
    private final AtomicBoolean active = new AtomicBoolean(true);

    public FandBossBarHandle(BossBar bossBar) {
        this(bossBar, Runnable::run);
    }

    public FandBossBarHandle(BossBar bossBar, Consumer<Runnable> updater) {
        this.bossBar = Objects.requireNonNull(bossBar, "bossBar");
        this.updater = Objects.requireNonNull(updater, "updater");
    }

    @Override
    public boolean active() {
        return active.get();
    }

    @Override
    public BossBar bossBar() {
        return bossBar;
    }

    @Override
    public Collection<? extends Player> viewers() {
        synchronized (viewers) {
            return List.copyOf(viewers.values());
        }
    }

    @Override
    public void show(Player player) {
        Objects.requireNonNull(player, "player");
        ensureActive();
        synchronized (viewers) {
            if (viewers.putIfAbsent(player.uniqueId(), player) != null) {
                return;
            }
        }
        player.showBossBar(bossBar);
    }

    @Override
    public void hide(Player player) {
        Objects.requireNonNull(player, "player");
        var removed = false;
        synchronized (viewers) {
            removed = viewers.remove(player.uniqueId()) != null;
        }
        if (removed) {
            player.hideBossBar(bossBar);
        }
    }

    @Override
    public void hideAll() {
        Collection<? extends Player> snapshot;
        synchronized (viewers) {
            snapshot = List.copyOf(viewers.values());
            viewers.clear();
        }
        for (var viewer : snapshot) {
            viewer.hideBossBar(bossBar);
        }
    }

    @Override
    public void setTitle(Component title) {
        Objects.requireNonNull(title, "title");
        update(() -> bossBar.name(title));
    }

    @Override
    public void setProgress(float progress) {
        update(() -> bossBar.progress(progress));
    }

    @Override
    public void setColor(BossBar.Color color) {
        Objects.requireNonNull(color, "color");
        update(() -> bossBar.color(color));
    }

    @Override
    public void setOverlay(BossBar.Overlay overlay) {
        Objects.requireNonNull(overlay, "overlay");
        update(() -> bossBar.overlay(overlay));
    }

    @Override
    public void setFlags(Set<BossBar.Flag> flags) {
        var copy = Set.copyOf(Objects.requireNonNull(flags, "flags"));
        update(() -> bossBar.flags(copy));
    }

    @Override
    public void addFlag(BossBar.Flag flag) {
        Objects.requireNonNull(flag, "flag");
        update(() -> bossBar.addFlag(flag));
    }

    @Override
    public void removeFlag(BossBar.Flag flag) {
        Objects.requireNonNull(flag, "flag");
        update(() -> bossBar.removeFlag(flag));
    }

    @Override
    public void close() {
        if (active.compareAndSet(true, false)) {
            hideAll();
        }
    }

    protected void ensureActive() {
        if (!active()) {
            throw new IllegalStateException("Boss bar is closed");
        }
    }

    private void update(Runnable task) {
        ensureActive();
        updater.accept(task);
    }
}
