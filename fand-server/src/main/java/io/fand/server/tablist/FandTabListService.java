package io.fand.server.tablist;

import io.fand.api.entity.Player;
import io.fand.api.tablist.TabListEntry;
import io.fand.api.tablist.TabListRegistration;
import io.fand.api.tablist.TabListService;
import io.fand.server.util.ServerThreading;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class FandTabListService implements TabListService, RealPlayerTabListAccess, AutoCloseable {

    private final Supplier<MinecraftServer> server;
    private final Map<UUID, Map<UUID, FandTabListRegistration>> entriesByViewer = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Integer> displayedPings = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, Set<UUID>> hiddenEntriesByViewer = new ConcurrentHashMap<>();

    public FandTabListService(Supplier<MinecraftServer> server) {
        this.server = Objects.requireNonNull(server, "server");
    }

    @Override
    public Collection<? extends TabListRegistration> entries(Player viewer) {
        var viewerId = viewerId(viewer);
        var entries = entriesByViewer.get(viewerId);
        if (entries == null) {
            return java.util.List.of();
        }
        return java.util.List.copyOf(entries.values());
    }

    @Override
    public boolean visible(Player viewer, Player target) {
        return visibleInRealPlayerList(viewerId(viewer), viewerId(target));
    }

    @Override
    public void setVisible(Player viewer, Player target, boolean visible) {
        setRealEntryVisible(viewerId(viewer), viewerId(target), visible);
    }

    @Override
    public void showOnly(Player viewer, Collection<? extends Player> visibleTargets) {
        Objects.requireNonNull(visibleTargets, "visibleTargets");
        var visibleIds = new java.util.HashSet<UUID>();
        for (var target : visibleTargets) {
            visibleIds.add(viewerId(target));
        }
        var viewerId = viewerId(viewer);
        runOnServerThread(() -> {
            var current = server.get();
            if (current == null) {
                return;
            }
            for (var target : current.getPlayerList().getPlayers()) {
                var targetId = target.getUUID();
                setRealEntryVisible(viewerId, targetId, targetId.equals(viewerId) || visibleIds.contains(targetId));
            }
        });
    }

    @Override
    public Collection<UUID> showOnlyCandidateIds(Player viewer, Collection<? extends Player> visibleTargets) {
        Objects.requireNonNull(viewer, "viewer");
        Objects.requireNonNull(visibleTargets, "visibleTargets");
        var current = server.get();
        if (current == null) {
            return viewer.world().players().stream()
                    .map(Player::uniqueId)
                    .toList();
        }
        return ServerThreading.callBlocking(current, () -> current.getPlayerList().getPlayers().stream()
                .map(ServerPlayer::getUUID)
                .toList());
    }

    @Override
    public Optional<? extends TabListRegistration> entry(Player viewer, UUID entryId) {
        var entries = entriesByViewer.get(viewerId(viewer));
        return entries == null ? Optional.empty() : Optional.ofNullable(entries.get(Objects.requireNonNull(entryId, "entryId")));
    }

    @Override
    public TabListRegistration add(Player viewer, TabListEntry entry) {
        Objects.requireNonNull(entry, "entry");
        var viewerId = viewerId(viewer);
        var registration = new FandTabListRegistration(viewerId, entry, this);
        var entries = entriesByViewer.computeIfAbsent(viewerId, ignored -> new ConcurrentHashMap<>());
        var previous = entries.put(entry.profile().uniqueId(), registration);
        if (previous != null) {
            previous.closeFromService();
        }
        sendAdd(viewerId, entry);
        return registration;
    }

    @Override
    public boolean remove(Player viewer, UUID entryId) {
        var entries = entriesByViewer.get(viewerId(viewer));
        if (entries == null) {
            return false;
        }
        var removed = entries.remove(Objects.requireNonNull(entryId, "entryId"));
        if (removed == null) {
            return false;
        }
        removed.closeFromService();
        sendRemove(removed.viewerId(), removed.entryId());
        if (entries.isEmpty()) {
            entriesByViewer.remove(removed.viewerId(), entries);
        }
        return true;
    }

    @Override
    public void removeAll(Player viewer) {
        var viewerId = viewerId(viewer);
        var removed = entriesByViewer.remove(viewerId);
        if (removed == null) {
            return;
        }
        removed.values().forEach(FandTabListRegistration::closeFromService);
        if (!removed.isEmpty()) {
            sendRemove(viewerId, java.util.List.copyOf(removed.keySet()));
        }
    }

    public boolean hasPacketOverrides() {
        return !displayedPings.isEmpty() || !hiddenEntriesByViewer.isEmpty();
    }

    public void setDisplayedPing(UUID entryId, int ping) {
        Objects.requireNonNull(entryId, "entryId");
        var latency = Math.max(0, ping);
        displayedPings.put(entryId, latency);
        sendLatencyUpdate(entryId, latency);
    }

    public void resetDisplayedPing(UUID entryId) {
        Objects.requireNonNull(entryId, "entryId");
        if (displayedPings.remove(entryId) == null) {
            return;
        }
        runOnServerThread(() -> {
            var current = server.get();
            var target = viewer(entryId);
            if (current == null || target == null || target.connection == null) {
                return;
            }
            sendLatencyUpdate(current, entryId, target.connection.latency());
        });
    }

    public boolean visibleInRealPlayerList(UUID viewerId, UUID entryId) {
        Objects.requireNonNull(viewerId, "viewerId");
        Objects.requireNonNull(entryId, "entryId");
        var hiddenEntries = hiddenEntriesByViewer.get(viewerId);
        return hiddenEntries == null || !hiddenEntries.contains(entryId);
    }

    public void setRealEntryVisible(UUID viewerId, UUID entryId, boolean visible) {
        Objects.requireNonNull(viewerId, "viewerId");
        Objects.requireNonNull(entryId, "entryId");
        if (visible) {
            var hiddenEntries = hiddenEntriesByViewer.get(viewerId);
            if (hiddenEntries == null || !hiddenEntries.remove(entryId)) {
                return;
            }
            if (hiddenEntries.isEmpty()) {
                hiddenEntriesByViewer.remove(viewerId, hiddenEntries);
            }
            sendRealEntryAdd(viewerId, entryId);
            return;
        }

        var hiddenEntries = hiddenEntriesByViewer.computeIfAbsent(viewerId, ignored -> ConcurrentHashMap.newKeySet());
        if (hiddenEntries.add(entryId)) {
            sendRemove(viewerId, entryId);
        }
    }

    public Packet<?> rewriteOutboundPacket(Optional<? extends Player> viewer, Packet<?> packet) {
        Objects.requireNonNull(viewer, "viewer");
        Objects.requireNonNull(packet, "packet");
        if (!(packet instanceof ClientboundPlayerInfoUpdatePacket info) || viewer.isEmpty()) {
            return packet;
        }
        var viewerId = viewerId(viewer.orElseThrow());
        var hiddenEntries = hiddenEntriesByViewer.getOrDefault(viewerId, Set.of());
        return FandTabListPackets.rewrite(info, hiddenEntries, displayedPings);
    }

    public void clearPlayer(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        displayedPings.remove(playerId);
        entriesByViewer.remove(playerId);
        hiddenEntriesByViewer.remove(playerId);
        hiddenEntriesByViewer.forEach((viewerId, hiddenEntries) -> {
            if (hiddenEntries.remove(playerId) && hiddenEntries.isEmpty()) {
                hiddenEntriesByViewer.remove(viewerId, hiddenEntries);
            }
        });
    }

    @Override
    public void close() {
        var snapshot = new LinkedHashMap<>(entriesByViewer);
        entriesByViewer.clear();
        displayedPings.clear();
        hiddenEntriesByViewer.clear();
        for (var viewerEntry : snapshot.entrySet()) {
            viewerEntry.getValue().values().forEach(FandTabListRegistration::closeFromService);
            if (!viewerEntry.getValue().isEmpty()) {
                sendRemove(viewerEntry.getKey(), java.util.List.copyOf(viewerEntry.getValue().keySet()));
            }
        }
    }

    void sendUpdate(UUID viewerId, TabListEntry entry) {
        runOnServerThread(() -> {
            var viewer = viewer(viewerId);
            if (viewer != null && viewer.connection != null) {
                viewer.connection.send(FandTabListPackets.update(entry, viewer.level().getServer()));
            }
        });
    }

    void removeRegistration(UUID viewerId, UUID entryId, FandTabListRegistration registration) {
        var entries = entriesByViewer.get(viewerId);
        if (entries == null || !entries.remove(entryId, registration)) {
            return;
        }
        sendRemove(viewerId, entryId);
        if (entries.isEmpty()) {
            entriesByViewer.remove(viewerId, entries);
        }
    }

    private void sendAdd(UUID viewerId, TabListEntry entry) {
        runOnServerThread(() -> {
            var viewer = viewer(viewerId);
            if (viewer != null && viewer.connection != null) {
                viewer.connection.send(FandTabListPackets.add(entry, viewer.level().getServer()));
            }
        });
    }

    private void sendRemove(UUID viewerId, UUID entryId) {
        sendRemove(viewerId, java.util.List.of(entryId));
    }

    private void sendRemove(UUID viewerId, java.util.List<UUID> entryIds) {
        runOnServerThread(() -> {
            var viewer = viewer(viewerId);
            if (viewer != null && viewer.connection != null) {
                viewer.connection.send(new ClientboundPlayerInfoRemovePacket(entryIds));
            }
        });
    }

    private void sendLatencyUpdate(UUID entryId, int latency) {
        runOnServerThread(() -> {
            var current = server.get();
            if (current == null) {
                return;
            }
            sendLatencyUpdate(current, entryId, latency);
        });
    }

    private void sendLatencyUpdate(MinecraftServer current, UUID entryId, int latency) {
        var packet = FandTabListPackets.latency(entryId, latency);
        for (var viewer : current.getPlayerList().getPlayers()) {
            if (viewer.connection != null && visibleInRealPlayerList(viewer.getUUID(), entryId)) {
                viewer.connection.send(packet);
            }
        }
    }

    private void sendRealEntryAdd(UUID viewerId, UUID entryId) {
        runOnServerThread(() -> {
            var viewer = viewer(viewerId);
            var target = viewer(entryId);
            if (viewer != null && viewer.connection != null && target != null) {
                viewer.connection.send(ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(java.util.List.of(target)));
            }
        });
    }

    private ServerPlayer viewer(UUID viewerId) {
        var current = server.get();
        return current == null ? null : current.getPlayerList().getPlayer(viewerId);
    }

    private void runOnServerThread(Runnable task) {
        ServerThreading.run(server.get(), task);
    }

    private static UUID viewerId(Player viewer) {
        Objects.requireNonNull(viewer, "viewer");
        if (viewer instanceof io.fand.server.entity.FandPlayer fandPlayer) {
            return fandPlayer.uniqueId();
        }
        return viewer.uniqueId();
    }
}
