package io.fand.server.scoreboard;

import io.fand.api.scoreboard.PlayerScoreboard;
import io.fand.api.scoreboard.ScoreDisplaySlot;
import io.fand.api.scoreboard.ScoreRenderType;
import io.fand.api.scoreboard.ScoreboardRegistration;
import io.fand.api.scoreboard.ScoreboardObjective;
import io.fand.api.scoreboard.ScoreboardTeam;
import io.fand.server.command.AdventureBridge;
import io.fand.server.entity.FandPlayer;
import io.fand.server.util.ServerThreading;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import net.kyori.adventure.text.Component;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundResetScorePacket;
import net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.network.protocol.game.ClientboundSetScorePacket;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

public final class FandPlayerScoreboard implements PlayerScoreboard {

    private final FandPlayer player;
    private final FandScoreboardService scoreboards;
    private final Scoreboard handle = new Scoreboard();
    private final EnumMap<ScoreDisplaySlot, ScoreboardObjective> displayed = new EnumMap<>(ScoreDisplaySlot.class);
    private final EnumSet<ScoreDisplaySlot> cleared = EnumSet.noneOf(ScoreDisplaySlot.class);
    private final Set<String> knownObjectives = new HashSet<>();

    public FandPlayerScoreboard(FandPlayer player, FandScoreboardService scoreboards) {
        this.player = Objects.requireNonNull(player, "player");
        this.scoreboards = Objects.requireNonNull(scoreboards, "scoreboards");
    }

    Scoreboard handle() {
        return handle;
    }

    RegistryAccess registries() {
        return scoreboards.server().registryAccess();
    }

    void run(Runnable task) {
        ScoreboardThreading.run(scoreboards.server(), task);
    }

    <T> T call(Supplier<T> task) {
        return ScoreboardThreading.call(scoreboards.server(), task);
    }

    @Override
    public Map<String, ? extends ScoreboardObjective> objectives() {
        return call(() -> {
            var snapshot = new java.util.LinkedHashMap<String, ScoreboardObjective>();
            for (var objective : handle.getObjectives()) {
                snapshot.put(objective.getName(), new FandPlayerScoreboardObjective(this, objective));
            }
            return Map.copyOf(snapshot);
        });
    }

    @Override
    public Optional<? extends ScoreboardObjective> objective(String name) {
        var normalized = ScoreboardNames.normalizeObjective(name);
        return call(() -> Optional.ofNullable(handle.getObjective(normalized))
                .map(objective -> new FandPlayerScoreboardObjective(this, objective)));
    }

    @Override
    public ScoreboardRegistration registerObjective(String name, Component displayName) {
        return registerObjective(name, displayName, ScoreRenderType.INTEGER);
    }

    @Override
    public ScoreboardRegistration registerObjective(String name, Component displayName, ScoreRenderType renderType) {
        var normalized = ScoreboardNames.normalizeObjective(name);
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(renderType, "renderType");
        var objective = call(() -> {
            if (handle.getObjective(normalized) != null) {
                throw new IllegalStateException("Player scoreboard objective already exists: " + normalized);
            }
            return handle.addObjective(
                    normalized,
                    ObjectiveCriteria.DUMMY,
                    AdventureBridge.toVanilla(displayName, registries()),
                    ScoreboardConversions.toVanilla(renderType),
                    false,
                    null);
        });
        sendObjectiveAdded(objective);
        return new FandScoreboardRegistration(
                normalized,
                () -> call(() -> handle.getObjective(normalized) == objective),
                () -> removeObjective(objective));
    }

    @Override
    public boolean removeObjective(String name) {
        var normalized = ScoreboardNames.normalizeObjective(name);
        return call(() -> {
            var objective = handle.getObjective(normalized);
            if (objective == null) {
                return false;
            }
            return removeObjective(objective);
        });
    }

    @Override
    public Map<String, ? extends ScoreboardTeam> teams() {
        return call(() -> {
            var snapshot = new java.util.LinkedHashMap<String, ScoreboardTeam>();
            for (var team : handle.getPlayerTeams()) {
                snapshot.put(team.getName(), new FandPlayerScoreboardTeam(this, team));
            }
            return Map.copyOf(snapshot);
        });
    }

    @Override
    public Optional<? extends ScoreboardTeam> team(String name) {
        var normalized = ScoreboardNames.normalizeTeam(name);
        return call(() -> Optional.ofNullable(handle.getPlayerTeam(normalized))
                .map(team -> new FandPlayerScoreboardTeam(this, team)));
    }

    @Override
    public ScoreboardRegistration registerTeam(String name) {
        var normalized = ScoreboardNames.normalizeTeam(name);
        var team = call(() -> {
            if (handle.getPlayerTeam(normalized) != null) {
                throw new IllegalStateException("Player scoreboard team already exists: " + normalized);
            }
            return handle.addPlayerTeam(normalized);
        });
        sendTeamAdded(team);
        return new FandScoreboardRegistration(
                normalized,
                () -> call(() -> handle.getPlayerTeam(normalized) == team),
                () -> removeTeam(team));
    }

    @Override
    public boolean removeTeam(String name) {
        var normalized = ScoreboardNames.normalizeTeam(name);
        return call(() -> {
            var team = handle.getPlayerTeam(normalized);
            if (team == null) {
                return false;
            }
            return removeTeam(team);
        });
    }

    @Override
    public Optional<? extends ScoreboardTeam> teamOf(String member) {
        var owner = FandScoreboardService.ownerName(member);
        return call(() -> Optional.ofNullable(handle.getPlayersTeam(owner))
                .map(team -> new FandPlayerScoreboardTeam(this, team)));
    }

    @Override
    public boolean removeMemberFromTeam(String member) {
        var owner = FandScoreboardService.ownerName(member);
        return call(() -> {
            var team = handle.getPlayersTeam(owner);
            if (team == null) {
                return false;
            }
            handle.removePlayerFromTeam(owner, team);
            sendTeamPlayer(team, owner, ClientboundSetPlayerTeamPacket.Action.REMOVE);
            return true;
        });
    }

    @Override
    public Optional<? extends ScoreboardObjective> displayedObjective(ScoreDisplaySlot slot) {
        Objects.requireNonNull(slot, "slot");
        synchronized (displayed) {
            return Optional.ofNullable(displayed.get(slot));
        }
    }

    @Override
    public Map<ScoreDisplaySlot, ? extends ScoreboardObjective> displayedObjectives() {
        synchronized (displayed) {
            var snapshot = new EnumMap<ScoreDisplaySlot, ScoreboardObjective>(ScoreDisplaySlot.class);
            snapshot.putAll(displayed);
            return Map.copyOf(snapshot);
        }
    }

    @Override
    public void setDisplayedObjective(ScoreDisplaySlot slot, ScoreboardObjective objective) {
        Objects.requireNonNull(slot, "slot");
        var display = requireDisplayObjective(objective);
        synchronized (displayed) {
            displayed.put(slot, display.objective());
            cleared.remove(slot);
        }
        display.ensureKnown().run();
        send(slot, display.handle());
    }

    @Override
    public void clearDisplayedObjective(ScoreDisplaySlot slot) {
        Objects.requireNonNull(slot, "slot");
        synchronized (displayed) {
            displayed.remove(slot);
            cleared.add(slot);
        }
        send(slot, null);
    }

    @Override
    public void clearDisplayedObjectives() {
        for (var slot : ScoreDisplaySlot.values()) {
            clearDisplayedObjective(slot);
        }
    }

    @Override
    public void resetDisplayedObjective(ScoreDisplaySlot slot) {
        Objects.requireNonNull(slot, "slot");
        synchronized (displayed) {
            displayed.remove(slot);
            cleared.remove(slot);
        }
        send(slot, globalObjective(slot));
    }

    @Override
    public void resetDisplayedObjectives() {
        for (var slot : ScoreDisplaySlot.values()) {
            resetDisplayedObjective(slot);
        }
    }

    public void resendDisplayedObjectives() {
        resendPlayerTeams();
        resendPlayerObjectives();
        EnumMap<ScoreDisplaySlot, ScoreboardObjective> snapshot;
        synchronized (displayed) {
            snapshot = new EnumMap<>(displayed);
        }
        for (var entry : snapshot.entrySet()) {
            var display = requireDisplayObjective(entry.getValue());
            display.ensureKnown().run();
            send(entry.getKey(), display.handle());
        }
        ScoreDisplaySlot[] clearedSnapshot;
        synchronized (displayed) {
            clearedSnapshot = cleared.toArray(ScoreDisplaySlot[]::new);
        }
        for (var slot : clearedSnapshot) {
            send(slot, null);
        }
    }

    void resendDisplayedObjective(ScoreDisplaySlot slot) {
        ScoreboardObjective objective;
        boolean clear;
        synchronized (displayed) {
            objective = displayed.get(slot);
            clear = cleared.contains(slot);
            if (objective == null && !clear) {
                return;
            }
        }
        if (clear) {
            send(slot, null);
            return;
        }
        var display = requireDisplayObjective(objective);
        display.ensureKnown().run();
        send(slot, display.handle());
    }

    public void clearTransientState() {
        clearPlayerTeams();
        clearPlayerObjectives();
        synchronized (displayed) {
            displayed.clear();
            cleared.clear();
        }
    }

    public void unregister() {
        scoreboards.unregisterPlayerScoreboard(this);
    }

    void sendObjectiveChanged(Objective objective) {
        sendPacket(new ClientboundSetObjectivePacket(objective, ClientboundSetObjectivePacket.METHOD_CHANGE));
    }

    void sendScore(String owner, Objective objective) {
        handle.listPlayerScores(objective).stream()
                .filter(score -> score.owner().equals(owner))
                .findFirst()
                .ifPresent(score -> sendPacket(new ClientboundSetScorePacket(
                        owner,
                        objective.getName(),
                        score.value(),
                        Optional.ofNullable(score.display()),
                        Optional.ofNullable(score.numberFormatOverride()))));
    }

    void sendScoreRemoved(String owner, Objective objective) {
        sendPacket(new ClientboundResetScorePacket(owner, objective.getName()));
    }

    void sendTeamChanged(PlayerTeam team) {
        sendPacket(ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(team, false));
    }

    void sendTeamPlayer(PlayerTeam team, String member, ClientboundSetPlayerTeamPacket.Action action) {
        sendPacket(ClientboundSetPlayerTeamPacket.createPlayerPacket(team, member, action));
    }

    private DisplayObjective requireDisplayObjective(ScoreboardObjective objective) {
        Objects.requireNonNull(objective, "objective");
        var name = ScoreboardNames.normalizeObjective(objective.name());
        return call(() -> {
            var playerObjective = handle.getObjective(name);
            if (playerObjective != null) {
                return new DisplayObjective(
                        new FandPlayerScoreboardObjective(this, playerObjective),
                        playerObjective,
                        () -> sendObjectiveAdded(playerObjective));
            }
            var globalObjective = scoreboards.handle().getObjective(name);
            if (globalObjective == null) {
                throw new IllegalArgumentException("Unknown scoreboard objective: " + name);
            }
            return new DisplayObjective(
                    new FandScoreboardObjective(scoreboards, globalObjective),
                    globalObjective,
                    () -> {});
        });
    }

    private Objective globalObjective(ScoreDisplaySlot slot) {
        var vanillaSlot = ScoreboardConversions.toVanilla(slot);
        return ScoreboardThreading.call(scoreboards.server(), () -> scoreboards.handle().getDisplayObjective(vanillaSlot));
    }

    private void send(ScoreDisplaySlot slot, Objective objective) {
        var vanillaSlot = ScoreboardConversions.toVanilla(slot);
        sendPacket(new ClientboundSetDisplayObjectivePacket(vanillaSlot, objective));
    }

    private boolean removeObjective(Objective objective) {
        if (handle.getObjective(objective.getName()) != objective) {
            return false;
        }
        ScoreDisplaySlot[] affected;
        synchronized (displayed) {
            affected = displayed.entrySet().stream()
                    .filter(entry -> entry.getValue().name().equals(objective.getName()))
                    .map(Map.Entry::getKey)
                    .toArray(ScoreDisplaySlot[]::new);
            displayed.entrySet().removeIf(entry -> entry.getValue().name().equals(objective.getName()));
        }
        handle.removeObjective(objective);
        synchronized (knownObjectives) {
            knownObjectives.remove(objective.getName());
        }
        sendPacket(new ClientboundSetObjectivePacket(objective, ClientboundSetObjectivePacket.METHOD_REMOVE));
        for (var slot : affected) {
            send(slot, globalObjective(slot));
        }
        return true;
    }

    private void sendObjectiveAdded(Objective objective) {
        synchronized (knownObjectives) {
            if (!knownObjectives.add(objective.getName())) {
                return;
            }
        }
        if (!sendPacket(new ClientboundSetObjectivePacket(objective, ClientboundSetObjectivePacket.METHOD_ADD))) {
            synchronized (knownObjectives) {
                knownObjectives.remove(objective.getName());
            }
            return;
        }
        for (PlayerScoreEntry score : handle.listPlayerScores(objective)) {
            sendPacket(new ClientboundSetScorePacket(
                    score.owner(),
                    objective.getName(),
                    score.value(),
                    Optional.ofNullable(score.display()),
                    Optional.ofNullable(score.numberFormatOverride())));
        }
    }

    private void resendPlayerObjectives() {
        Collection<Objective> objectives = call(() -> java.util.List.copyOf(handle.getObjectives()));
        for (var objective : objectives) {
            sendObjectiveAdded(objective);
        }
    }

    private void clearPlayerObjectives() {
        Collection<Objective> objectives = call(() -> java.util.List.copyOf(handle.getObjectives()));
        synchronized (knownObjectives) {
            knownObjectives.clear();
        }
        for (var objective : objectives) {
            sendPacket(new ClientboundSetObjectivePacket(objective, ClientboundSetObjectivePacket.METHOD_REMOVE));
        }
        call(() -> {
            for (var objective : objectives) {
                if (handle.getObjective(objective.getName()) == objective) {
                    handle.removeObjective(objective);
                }
            }
            return null;
        });
    }

    private boolean removeTeam(PlayerTeam team) {
        if (handle.getPlayerTeam(team.getName()) != team) {
            return false;
        }
        handle.removePlayerTeam(team);
        sendPacket(ClientboundSetPlayerTeamPacket.createRemovePacket(team));
        return true;
    }

    private void sendTeamAdded(PlayerTeam team) {
        sendPacket(ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(team, true));
    }

    private void resendPlayerTeams() {
        Collection<PlayerTeam> teams = call(() -> java.util.List.copyOf(handle.getPlayerTeams()));
        for (var team : teams) {
            sendTeamAdded(team);
        }
    }

    private void clearPlayerTeams() {
        Collection<PlayerTeam> teams = call(() -> java.util.List.copyOf(handle.getPlayerTeams()));
        for (var team : teams) {
            sendPacket(ClientboundSetPlayerTeamPacket.createRemovePacket(team));
        }
        call(() -> {
            for (var team : teams) {
                if (handle.getPlayerTeam(team.getName()) == team) {
                    handle.removePlayerTeam(team);
                }
            }
            return null;
        });
    }

    private boolean sendPacket(Packet<?> packet) {
        var server = player.handle().level().getServer();
        var sent = new AtomicBoolean();
        if (!ServerThreading.run(server, () -> {
            var handle = player.handle();
            if (player.online() && handle.connection != null) {
                handle.connection.send(packet);
                sent.set(true);
            }
        })) {
            return false;
        }
        return sent.get();
    }

    private record DisplayObjective(ScoreboardObjective objective, Objective handle, Runnable ensureKnown) {
    }
}
