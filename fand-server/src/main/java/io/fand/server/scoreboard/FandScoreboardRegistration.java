package io.fand.server.scoreboard;

import io.fand.api.scoreboard.ScoreboardRegistration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

final class FandScoreboardRegistration implements ScoreboardRegistration {

    private final String name;
    private final Runnable unregister;
    private final AtomicBoolean active = new AtomicBoolean(true);

    FandScoreboardRegistration(String name, Runnable unregister) {
        this.name = Objects.requireNonNull(name, "name");
        this.unregister = Objects.requireNonNull(unregister, "unregister");
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public boolean active() {
        return active.get();
    }

    @Override
    public void unregister() {
        if (active.compareAndSet(true, false)) {
            unregister.run();
        }
    }
}
