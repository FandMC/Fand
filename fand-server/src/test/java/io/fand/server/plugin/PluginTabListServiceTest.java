package io.fand.server.plugin;

import static org.assertj.core.api.Assertions.assertThat;

import io.fand.api.entity.Player;
import io.fand.api.tablist.TabListEntry;
import io.fand.api.tablist.TabListRegistration;
import io.fand.api.tablist.TabListService;
import io.fand.api.world.World;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.Test;

final class PluginTabListServiceTest {

    @Test
    void tracksAndCleansPluginEntries() {
        var delegate = new FakeTabListService();
        var tracker = new PluginResourceTracker();
        var service = new PluginTabListService(delegate, tracker);
        var viewer = fakePlayer(UUID.randomUUID());
        var entry = TabListEntry.builder(UUID.randomUUID(), "VirtualOne").latency(42).build();

        var registration = service.add(viewer, entry);

        assertThat(registration.active()).isTrue();
        assertThat(delegate.entry(viewer, entry.profile().uniqueId())).isPresent();

        tracker.close();

        assertThat(registration.active()).isFalse();
        assertThat(delegate.entry(viewer, entry.profile().uniqueId())).isEmpty();
    }

    @Test
    void updateDelegatesToTrackedRegistration() {
        var delegate = new FakeTabListService();
        var service = new PluginTabListService(delegate, new PluginResourceTracker());
        var viewer = fakePlayer(UUID.randomUUID());
        var entry = TabListEntry.builder(UUID.randomUUID(), "VirtualOne").build();

        var registration = service.add(viewer, entry);
        registration.update(entry.withLatency(120));

        assertThat(delegate.entry(viewer, entry.profile().uniqueId()).orElseThrow().entry().latency()).isEqualTo(120);
    }

    @Test
    void restoresRealPlayerVisibilityWhenPluginCloses() {
        var delegate = new FakeTabListService();
        var tracker = new PluginResourceTracker();
        var service = new PluginTabListService(delegate, tracker);
        var world = fakeWorld();
        var viewer = fakePlayer(UUID.randomUUID(), world);
        var target = fakePlayer(UUID.randomUUID(), world);
        setPlayers(world, viewer, target);

        service.setVisible(viewer, target, false);

        assertThat(delegate.visible(viewer, target)).isFalse();

        tracker.close();

        assertThat(delegate.visible(viewer, target)).isTrue();
    }

    @Test
    void explicitShowReleasesTrackedVisibilityRestore() {
        var delegate = new FakeTabListService();
        var tracker = new PluginResourceTracker();
        var service = new PluginTabListService(delegate, tracker);
        var world = fakeWorld();
        var viewer = fakePlayer(UUID.randomUUID(), world);
        var target = fakePlayer(UUID.randomUUID(), world);
        setPlayers(world, viewer, target);

        service.setVisible(viewer, target, false);
        service.setVisible(viewer, target, true);
        tracker.close();

        assertThat(delegate.visible(viewer, target)).isTrue();
        assertThat(delegate.visibilityChanges).containsExactly(
                change(viewer, target, false),
                change(viewer, target, true));
    }

    private static final class FakeTabListService implements TabListService {

        private final Map<UUID, Map<UUID, FakeRegistration>> entries = new LinkedHashMap<>();
        private final Set<String> hidden = new HashSet<>();
        private final java.util.ArrayList<String> visibilityChanges = new java.util.ArrayList<>();

        @Override
        public boolean visible(Player viewer, Player target) {
            return !hidden.contains(key(viewer, target));
        }

        @Override
        public void setVisible(Player viewer, Player target, boolean visible) {
            visibilityChanges.add(change(viewer, target, visible));
            if (visible) {
                hidden.remove(key(viewer, target));
            } else {
                hidden.add(key(viewer, target));
            }
        }

        @Override
        public Collection<? extends TabListRegistration> entries(Player viewer) {
            return entries.getOrDefault(viewer.uniqueId(), Map.of()).values();
        }

        @Override
        public Optional<FakeRegistration> entry(Player viewer, UUID entryId) {
            return Optional.ofNullable(entries.getOrDefault(viewer.uniqueId(), Map.of()).get(entryId));
        }

        @Override
        public TabListRegistration add(Player viewer, TabListEntry entry) {
            var registration = new FakeRegistration(viewer.uniqueId(), entry, () -> remove(viewer, entry.profile().uniqueId()));
            entries.computeIfAbsent(viewer.uniqueId(), ignored -> new LinkedHashMap<>())
                    .put(entry.profile().uniqueId(), registration);
            return registration;
        }

        @Override
        public boolean remove(Player viewer, UUID entryId) {
            var viewerEntries = entries.get(viewer.uniqueId());
            if (viewerEntries == null) {
                return false;
            }
            return viewerEntries.remove(entryId) != null;
        }

        @Override
        public void removeAll(Player viewer) {
            entries.remove(viewer.uniqueId());
        }
    }

    private static final class FakeRegistration implements TabListRegistration {

        private final UUID viewerId;
        private final UUID entryId;
        private final Runnable remove;
        private boolean active = true;
        private TabListEntry entry;

        private FakeRegistration(UUID viewerId, TabListEntry entry, Runnable remove) {
            this.viewerId = viewerId;
            this.entryId = entry.profile().uniqueId();
            this.entry = entry;
            this.remove = remove;
        }

        @Override
        public UUID viewerId() {
            return viewerId;
        }

        @Override
        public UUID entryId() {
            return entryId;
        }

        @Override
        public boolean active() {
            return active;
        }

        TabListEntry entry() {
            return entry;
        }

        @Override
        public void update(TabListEntry entry) {
            this.entry = entry;
        }

        @Override
        public void remove() {
            if (active) {
                active = false;
                remove.run();
            }
        }
    }

    private static Player fakePlayer(UUID uniqueId) {
        return fakePlayer(uniqueId, null);
    }

    private static Player fakePlayer(UUID uniqueId, World world) {
        return (Player) java.lang.reflect.Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[]{Player.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "uniqueId" -> uniqueId;
                    case "world" -> world;
                    case "equals" -> proxy == args[0];
                    case "hashCode" -> uniqueId.hashCode();
                    case "toString" -> "FakePlayer[" + uniqueId + "]";
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private static World fakeWorld() {
        var players = new java.util.concurrent.atomic.AtomicReference<Collection<? extends Player>>(List.of());
        return (World) java.lang.reflect.Proxy.newProxyInstance(
                World.class.getClassLoader(),
                new Class<?>[]{World.class, PlayerHolder.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "key" -> Key.key("fand", "test");
                    case "seed", "gameTime", "time" -> 0L;
                    case "players" -> players.get();
                    case "setPlayers" -> {
                        players.set((Collection<? extends Player>) args[0]);
                        yield null;
                    }
                    case "equals" -> proxy == args[0];
                    case "hashCode" -> 31;
                    case "toString" -> "FakeWorld";
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private static void setPlayers(World world, Player... players) {
        ((PlayerHolder) world).setPlayers(List.of(players));
    }

    private static String key(Player viewer, Player target) {
        return viewer.uniqueId() + "->" + target.uniqueId();
    }

    private static String change(Player viewer, Player target, boolean visible) {
        return key(viewer, target) + "=" + visible;
    }

    private interface PlayerHolder {
        void setPlayers(Collection<? extends Player> players);
    }
}
