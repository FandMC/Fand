package io.fand.server.bossbar;

import io.fand.api.bossbar.BossBarRegistration;
import java.util.Objects;
import java.util.function.Consumer;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.key.Key;

final class FandBossBarRegistration extends FandBossBarHandle implements BossBarRegistration {

    private final Key key;
    private final Runnable unregister;

    FandBossBarRegistration(Key key, BossBar bossBar, Consumer<Runnable> updater, Runnable unregister) {
        super(bossBar, updater);
        this.key = Objects.requireNonNull(key, "key");
        this.unregister = Objects.requireNonNull(unregister, "unregister");
    }

    @Override
    public Key key() {
        return key;
    }

    @Override
    public void close() {
        if (active()) {
            unregister.run();
        }
    }

    void closeFromService() {
        super.close();
    }
}
