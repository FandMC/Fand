package io.fand.server.scoreboard;

import io.fand.api.scoreboard.ScoreboardRegistration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

final class FandScoreboardRegistration implements ScoreboardRegistration {

    private final String name;
    private final BooleanSupplier live;
    private final Runnable unregister;
    private final AtomicBoolean active = new AtomicBoolean(true);

    FandScoreboardRegistration(String name, BooleanSupplier live, Runnable unregister) {
        this.name = Objects.requireNonNull(name, "name");
        this.live = Objects.requireNonNull(live, "live");
        this.unregister = Objects.requireNonNull(unregister, "unregister");
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public boolean active() {
        return active.get() && live.getAsBoolean();
    }

    @Override
    public void unregister() {
        if (active.compareAndSet(true, false)) {
            unregister.run();
        }
    }
}
