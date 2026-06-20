package io.fand.server.plugin;

import static org.assertj.core.api.Assertions.assertThat;

import io.fand.api.bossbar.BossBarHandle;
import io.fand.api.bossbar.BossBarRegistration;
import io.fand.api.bossbar.BossBarService;
import io.fand.api.entity.Player;
import java.time.Duration;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

final class PluginBossBarServiceTest {

    @Test
    void scopesAndCleansPluginRegistrations() {
        var delegate = new FakeBossBarService();
        var tracker = new PluginResourceTracker();
        var service = new PluginBossBarService(delegate, tracker, "plug");

        var registration = service.register(
                Key.key("minecraft:status"),
                BossBar.bossBar(Component.text("Status"), 1.0f, BossBar.Color.BLUE, BossBar.Overlay.PROGRESS));

        assertThat(registration.key()).isEqualTo(Key.key("plug:status"));
        assertThat(delegate.bar(Key.key("plug:status"))).isPresent();

        tracker.close();

        assertThat(registration.active()).isFalse();
        assertThat(delegate.bar(Key.key("plug:status"))).isEmpty();
    }

    @Test
    void filtersLookupToPluginNamespace() {
        var delegate = new FakeBossBarService();
        delegate.register(Key.key("plug:status"), BossBar.bossBar(Component.text("Status"), 1.0f, BossBar.Color.BLUE, BossBar.Overlay.PROGRESS));
        delegate.register(Key.key("other:status"), BossBar.bossBar(Component.text("Other"), 1.0f, BossBar.Color.RED, BossBar.Overlay.PROGRESS));

        var service = new PluginBossBarService(delegate, new PluginResourceTracker(), "plug");

        assertThat(service.bars()).extracting(BossBarRegistration::key).containsExactly(Key.key("plug:status"));
        assertThat(service.bar(Key.key("minecraft:status"))).isPresent();
        assertThat(service.bar(Key.key("other:missing"))).isEmpty();
    }

    @Test
    void tracksTemporaryBossBars() {
        var delegate = new FakeBossBarService();
        var tracker = new PluginResourceTracker();
        var service = new PluginBossBarService(delegate, tracker, "plug");

        var handle = service.send(
                java.util.List.of(),
                BossBar.bossBar(Component.text("Temporary"), 0.5f, BossBar.Color.GREEN, BossBar.Overlay.PROGRESS),
                Duration.ofSeconds(1));

        assertThat(handle.active()).isTrue();

        tracker.close();

        assertThat(handle.active()).isFalse();
    }

    private static final class FakeBossBarService implements BossBarService {

        private final Map<Key, FakeRegistration> registrations = new LinkedHashMap<>();

        @Override
        public Collection<? extends BossBarRegistration> bars() {
            return registrations.values();
        }

        @Override
        public Optional<? extends BossBarRegistration> bar(Key key) {
            return Optional.ofNullable(registrations.get(key));
        }

        @Override
        public BossBarRegistration register(Key key, BossBar bossBar) {
            var registration = new FakeRegistration(key, bossBar, () -> registrations.remove(key));
            registrations.put(key, registration);
            return registration;
        }

        @Override
        public BossBarHandle send(Collection<? extends Player> viewers, BossBar bossBar, Duration duration) {
            return new FakeHandle(bossBar);
        }

        @Override
        public boolean remove(Key key) {
            var removed = registrations.remove(key);
            if (removed == null) {
                return false;
            }
            removed.close();
            return true;
        }
    }

    private static class FakeHandle implements BossBarHandle {

        private final BossBar bossBar;
        private boolean active = true;

        private FakeHandle(BossBar bossBar) {
            this.bossBar = bossBar;
        }

        @Override
        public boolean active() {
            return active;
        }

        @Override
        public BossBar bossBar() {
            return bossBar;
        }

        @Override
        public Collection<? extends Player> viewers() {
            return java.util.List.of();
        }

        @Override
        public void show(Player player) {
        }

        @Override
        public void hide(Player player) {
        }

        @Override
        public void hideAll() {
        }

        @Override
        public void close() {
            active = false;
        }
    }

    private static final class FakeRegistration extends FakeHandle implements BossBarRegistration {

        private final Key key;
        private final Runnable unregister;

        private FakeRegistration(Key key, BossBar bossBar, Runnable unregister) {
            super(bossBar);
            this.key = key;
            this.unregister = unregister;
        }

        @Override
        public Key key() {
            return key;
        }

        @Override
        public void close() {
            if (active()) {
                super.close();
                unregister.run();
            }
        }
    }
}
