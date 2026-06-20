package io.fand.server.bossbar;

import io.fand.api.bossbar.BossBarHandle;
import io.fand.api.bossbar.BossBarRegistration;
import io.fand.api.bossbar.BossBarService;
import io.fand.api.entity.Player;
import io.fand.api.scheduler.Scheduler;
import java.time.Duration;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.key.Key;
import net.minecraft.server.MinecraftServer;

public final class FandBossBarService implements BossBarService, AutoCloseable {

    private final Supplier<MinecraftServer> server;
    private final Scheduler scheduler;
    private final ConcurrentHashMap<Key, FandBossBarRegistration> bars = new ConcurrentHashMap<>();

    public FandBossBarService(Supplier<MinecraftServer> server, Scheduler scheduler) {
        this.server = Objects.requireNonNull(server, "server");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
    }

    @Override
    public Collection<? extends BossBarRegistration> bars() {
        return java.util.List.copyOf(bars.values());
    }

    @Override
    public Optional<? extends BossBarRegistration> bar(Key key) {
        return Optional.ofNullable(bars.get(Objects.requireNonNull(key, "key")));
    }

    @Override
    public BossBarRegistration register(Key key, BossBar bossBar) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(bossBar, "bossBar");
        var registration = new FandBossBarRegistration(key, bossBar, this::runOnServerThread, () -> remove(key));
        var previous = bars.putIfAbsent(key, registration);
        if (previous != null) {
            throw new IllegalStateException("Boss bar already exists: " + key.asString());
        }
        return registration;
    }

    @Override
    public BossBarHandle send(Collection<? extends Player> viewers, BossBar bossBar, Duration duration) {
        var snapshot = java.util.List.copyOf(Objects.requireNonNull(viewers, "viewers"));
        Objects.requireNonNull(bossBar, "bossBar");
        Objects.requireNonNull(duration, "duration");
        if (duration.isNegative() || duration.isZero()) {
            throw new IllegalArgumentException("duration must be positive: " + duration);
        }
        var handle = new FandBossBarHandle(bossBar, this::runOnServerThread);
        handle.show(snapshot);
        scheduler.runMainAfter(handle::close, duration);
        return handle;
    }

    @Override
    public boolean remove(Key key) {
        var removed = bars.remove(Objects.requireNonNull(key, "key"));
        if (removed == null) {
            return false;
        }
        removed.closeFromService();
        return true;
    }

    @Override
    public void close() {
        var snapshot = new LinkedHashMap<>(bars);
        bars.clear();
        snapshot.values().forEach(FandBossBarRegistration::closeFromService);
    }

    private void runOnServerThread(Runnable task) {
        Objects.requireNonNull(task, "task");
        var current = server.get();
        if (current == null || current.isSameThread()) {
            task.run();
            return;
        }
        current.execute(task);
    }
}
