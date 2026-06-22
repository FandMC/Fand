package io.fand.server.player;

import com.mojang.authlib.GameProfile;
import io.fand.api.entity.GameMode;
import io.fand.api.entity.Player;
import io.fand.api.event.EventPriority;
import io.fand.api.event.EventSubscription;
import io.fand.api.event.player.PlayerJoinEvent;
import io.fand.api.event.player.PlayerQuitEvent;
import io.fand.api.player.PlayerProfile;
import io.fand.api.player.SimulatedPlayerOptions;
import io.fand.api.player.SimulatedPlayerService;
import io.fand.api.world.Location;
import io.fand.server.entity.PlayerRegistry;
import io.fand.server.event.EventDispatcher;
import io.fand.server.util.ServerThreading;
import io.fand.server.world.FandWorld;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.NameAndId;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FandSimulatedPlayerService implements SimulatedPlayerService, AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(FandSimulatedPlayerService.class);

    private final Supplier<MinecraftServer> server;
    private final PlayerRegistry players;
    private final ConcurrentMap<UUID, SimulatedRecord> simulated = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, SimulatedPlayerOptions> pending = new ConcurrentHashMap<>();
    private final EventSubscription joinMessages;
    private final EventSubscription quitMessages;

    public FandSimulatedPlayerService(Supplier<MinecraftServer> server, PlayerRegistry players, EventDispatcher events) {
        this.server = Objects.requireNonNull(server, "server");
        this.players = Objects.requireNonNull(players, "players");
        Objects.requireNonNull(events, "events");
        this.joinMessages = events.subscribe(PlayerJoinEvent.class, EventPriority.LOWEST, this::configureJoinMessage);
        this.quitMessages = events.subscribe(PlayerQuitEvent.class, EventPriority.LOWEST, this::configureQuitMessage);
    }

    @Override
    public Collection<? extends Player> players() {
        return simulated.keySet().stream()
                .map(players::find)
                .flatMap(Optional::stream)
                .toList();
    }

    @Override
    public Optional<? extends Player> player(UUID uniqueId) {
        Objects.requireNonNull(uniqueId, "uniqueId");
        return simulated.containsKey(uniqueId) ? players.find(uniqueId) : Optional.empty();
    }

    @Override
    public CompletableFuture<? extends Player> create(PlayerProfile profile, Location location) {
        return create(profile, location, SimulatedPlayerOptions.defaults());
    }

    @Override
    public CompletableFuture<? extends Player> create(
            PlayerProfile profile,
            Location location,
            SimulatedPlayerOptions options
    ) {
        Objects.requireNonNull(profile, "profile");
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(options, "options");
        return ServerThreading.callFuture(server(), () -> createOnServerThread(profile, location, options));
    }

    @Override
    public CompletableFuture<Boolean> remove(UUID uniqueId) {
        Objects.requireNonNull(uniqueId, "uniqueId");
        return ServerThreading.callFuture(server(), () -> removeOnServerThread(uniqueId, true));
    }

    @Override
    public boolean simulated(UUID uniqueId) {
        Objects.requireNonNull(uniqueId, "uniqueId");
        return simulated.containsKey(uniqueId);
    }

    @Override
    public void close() {
        joinMessages.close();
        quitMessages.close();
        var current = server.get();
        if (current == null) {
            simulated.clear();
            pending.clear();
            return;
        }
        ServerThreading.run(current, () -> Set.copyOf(simulated.keySet()).forEach(id -> removeOnServerThread(id, true)));
        pending.clear();
    }

    public boolean removeIfSimulated(ServerPlayer player, boolean save) {
        Objects.requireNonNull(player, "player");
        var current = simulated.get(player.getUUID());
        if (current == null || current.handle != player) {
            return false;
        }
        return removeOnServerThread(player.getUUID(), save);
    }

    public void forgetIfSimulated(UUID uniqueId) {
        Objects.requireNonNull(uniqueId, "uniqueId");
        simulated.remove(uniqueId);
    }

    private Player createOnServerThread(PlayerProfile profile, Location location, SimulatedPlayerOptions options) {
        var current = server();
        var level = level(location);
        removeExistingSimulated(profile.uniqueId());
        var existing = current.getPlayerList().getPlayer(profile.uniqueId());
        if (existing != null) {
            throw new IllegalStateException("Player is already online: " + profile.name());
        }

        var gameProfile = PlayerProfiles.toGameProfile(profile);
        var nameAndId = new NameAndId(gameProfile.id(), gameProfile.name());
        var cookie = new CommonListenerCookie(gameProfile, 0, ClientInformation.createDefault(), false);
        var connection = new SimulatedPlayerConnection();
        var player = new ServerPlayer(current, level, gameProfile, cookie.clientInformation());
        loadSavedData(current, player, nameAndId);
        player.snapTo(location.x(), location.y(), location.z(), location.yaw(), location.pitch());
        options.gameMode().ifPresent(mode -> player.setGameMode(gameMode(mode)));
        pending.put(profile.uniqueId(), options);
        try {
            current.getPlayerList().placeNewPlayer(connection, player, cookie);
        } finally {
            pending.remove(profile.uniqueId());
        }
        simulated.put(profile.uniqueId(), new SimulatedRecord(player, connection, options.saveData(), options.broadcastQuitMessage()));
        return players.find(profile.uniqueId())
                .orElseThrow(() -> new IllegalStateException("Simulated player wrapper was not attached: " + profile.name()));
    }

    private void configureJoinMessage(PlayerJoinEvent event) {
        var options = pending.get(event.player().uniqueId());
        if (options != null && !options.broadcastJoinMessage()) {
            event.setMessage(null);
        }
    }

    private void configureQuitMessage(PlayerQuitEvent event) {
        var record = simulated.get(event.player().uniqueId());
        if (record != null && !record.broadcastQuitMessage) {
            event.setMessage(null);
        }
    }

    private void removeExistingSimulated(UUID uniqueId) {
        var existing = simulated.get(uniqueId);
        if (existing != null) {
            removeOnServerThread(uniqueId, existing.saveData);
        }
    }

    private boolean removeOnServerThread(UUID uniqueId, boolean saveOverride) {
        var record = simulated.remove(uniqueId);
        if (record == null) {
            return false;
        }
        var player = record.handle;
        if (saveOverride && record.saveData) {
            server().getPlayerList().save(player);
        }
        record.connection.disconnect(Component.literal("Simulated player removed"));
        record.connection.handleDisconnection();
        return true;
    }

    private void loadSavedData(MinecraftServer server, ServerPlayer player, NameAndId nameAndId) {
        try (var reporter = new ProblemReporter.ScopedCollector(player.problemPath(), LOGGER)) {
            Optional<ValueInput> input = server.getPlayerList()
                    .loadPlayerData(nameAndId)
                    .map(tag -> TagValueInput.create(reporter, server.registryAccess(), tag));
            input.ifPresent(player::load);
        }
    }

    private MinecraftServer server() {
        var current = server.get();
        if (current == null) {
            throw new IllegalStateException("Minecraft server is not attached");
        }
        return current;
    }

    private static ServerLevel level(Location location) {
        if (location.world() instanceof FandWorld world) {
            return world.handle();
        }
        throw new IllegalArgumentException("Simulated players can only be created in Fand worlds");
    }

    private static net.minecraft.world.level.GameType gameMode(GameMode mode) {
        return switch (mode) {
            case SURVIVAL -> net.minecraft.world.level.GameType.SURVIVAL;
            case CREATIVE -> net.minecraft.world.level.GameType.CREATIVE;
            case ADVENTURE -> net.minecraft.world.level.GameType.ADVENTURE;
            case SPECTATOR -> net.minecraft.world.level.GameType.SPECTATOR;
        };
    }

    private record SimulatedRecord(
            ServerPlayer handle,
            SimulatedPlayerConnection connection,
            boolean saveData,
            boolean broadcastQuitMessage
    ) {
    }
}
