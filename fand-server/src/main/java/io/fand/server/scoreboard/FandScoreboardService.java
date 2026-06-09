package io.fand.server.scoreboard;

import io.fand.api.scoreboard.ScoreDisplaySlot;
import io.fand.api.scoreboard.ScoreRenderType;
import io.fand.api.scoreboard.ScoreboardObjective;
import io.fand.api.scoreboard.ScoreboardRegistration;
import io.fand.api.scoreboard.ScoreboardService;
import io.fand.api.scoreboard.ScoreboardTeam;
import io.fand.server.command.AdventureBridge;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import net.kyori.adventure.text.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

public final class FandScoreboardService implements ScoreboardService {

    private final Supplier<MinecraftServer> server;

    public FandScoreboardService(MinecraftServer server) {
        this(() -> Objects.requireNonNull(server, "server"));
    }

    public FandScoreboardService(Supplier<MinecraftServer> server) {
        this.server = Objects.requireNonNull(server, "server");
    }

    MinecraftServer server() {
        var current = server.get();
        if (current == null) {
            throw new IllegalStateException("Minecraft server is not attached");
        }
        return current;
    }

    net.minecraft.server.ServerScoreboard handle() {
        return server().getScoreboard();
    }

    @Override
    public Collection<? extends ScoreboardObjective> objectives() {
        return ScoreboardThreading.call(server(), () -> handle().getObjectives().stream()
                .map(objective -> new FandScoreboardObjective(this, objective))
                .toList());
    }

    @Override
    public Optional<? extends ScoreboardObjective> objective(String name) {
        var normalized = ScoreboardNames.normalizeObjective(name);
        return ScoreboardThreading.call(server(), () -> Optional.ofNullable(handle().getObjective(normalized))
                .map(objective -> new FandScoreboardObjective(this, objective)));
    }

    @Override
    public ScoreboardRegistration registerObjective(String name, Component displayName) {
        return registerObjective(name, displayName, ObjectiveCriteria.DUMMY.getName(), ScoreRenderType.INTEGER);
    }

    @Override
    public ScoreboardRegistration registerObjective(String name, Component displayName, String criteria, ScoreRenderType renderType) {
        var normalized = ScoreboardNames.normalizeObjective(name);
        Objects.requireNonNull(displayName, "displayName");
        Objects.requireNonNull(criteria, "criteria");
        Objects.requireNonNull(renderType, "renderType");
        ScoreboardThreading.run(server(), () -> {
            if (handle().getObjective(normalized) != null) {
                throw new IllegalStateException("Scoreboard objective already exists: " + normalized);
            }
            var vanillaCriteria = ObjectiveCriteria.byName(criteria)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown scoreboard criteria: " + criteria));
            handle().addObjective(
                    normalized,
                    vanillaCriteria,
                    AdventureBridge.toVanilla(displayName, server().registryAccess()),
                    ScoreboardConversions.toVanilla(renderType),
                    false,
                    null);
        });
        return new FandScoreboardRegistration(normalized, () -> removeObjective(normalized));
    }

    @Override
    public boolean removeObjective(String name) {
        var normalized = ScoreboardNames.normalizeObjective(name);
        return ScoreboardThreading.call(server(), () -> {
            var objective = handle().getObjective(normalized);
            if (objective == null) {
                return false;
            }
            handle().removeObjective(objective);
            return true;
        });
    }

    @Override
    public Optional<? extends ScoreboardObjective> displayedObjective(ScoreDisplaySlot slot) {
        var vanillaSlot = ScoreboardConversions.toVanilla(slot);
        return ScoreboardThreading.call(server(), () -> Optional.ofNullable(handle().getDisplayObjective(vanillaSlot))
                .map(objective -> new FandScoreboardObjective(this, objective)));
    }

    @Override
    public void setDisplayedObjective(ScoreDisplaySlot slot, ScoreboardObjective objective) {
        Objects.requireNonNull(objective, "objective");
        var vanillaSlot = ScoreboardConversions.toVanilla(slot);
        var name = ScoreboardNames.normalizeObjective(objective.name());
        ScoreboardThreading.run(server(), () -> {
            var vanillaObjective = handle().getObjective(name);
            if (vanillaObjective == null) {
                throw new IllegalArgumentException("Unknown scoreboard objective: " + name);
            }
            handle().setDisplayObjective(vanillaSlot, vanillaObjective);
        });
    }

    @Override
    public void clearDisplayedObjective(ScoreDisplaySlot slot) {
        var vanillaSlot = ScoreboardConversions.toVanilla(slot);
        ScoreboardThreading.run(server(), () -> handle().setDisplayObjective(vanillaSlot, null));
    }

    @Override
    public Collection<? extends ScoreboardTeam> teams() {
        return ScoreboardThreading.call(server(), () -> handle().getPlayerTeams().stream()
                .map(team -> new FandScoreboardTeam(this, team))
                .toList());
    }

    @Override
    public Optional<? extends ScoreboardTeam> team(String name) {
        var normalized = ScoreboardNames.normalizeTeam(name);
        return ScoreboardThreading.call(server(), () -> Optional.ofNullable(handle().getPlayerTeam(normalized))
                .map(team -> new FandScoreboardTeam(this, team)));
    }

    @Override
    public ScoreboardRegistration registerTeam(String name) {
        var normalized = ScoreboardNames.normalizeTeam(name);
        ScoreboardThreading.run(server(), () -> {
            if (handle().getPlayerTeam(normalized) != null) {
                throw new IllegalStateException("Scoreboard team already exists: " + normalized);
            }
            handle().addPlayerTeam(normalized);
        });
        return new FandScoreboardRegistration(normalized, () -> removeTeam(normalized));
    }

    @Override
    public boolean removeTeam(String name) {
        var normalized = ScoreboardNames.normalizeTeam(name);
        return ScoreboardThreading.call(server(), () -> {
            var team = handle().getPlayerTeam(normalized);
            if (team == null) {
                return false;
            }
            handle().removePlayerTeam(team);
            return true;
        });
    }

    @Override
    public Optional<? extends ScoreboardTeam> teamOf(String member) {
        var owner = ownerName(member);
        return ScoreboardThreading.call(server(), () -> Optional.ofNullable(handle().getPlayersTeam(owner))
                .map(team -> new FandScoreboardTeam(this, team)));
    }

    @Override
    public boolean removeMemberFromTeam(String member) {
        var owner = ownerName(member);
        return ScoreboardThreading.call(server(), () -> handle().removePlayerFromTeam(owner));
    }

    static String ownerName(String owner) {
        var normalized = Objects.requireNonNull(owner, "owner").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Score owner cannot be blank");
        }
        return normalized;
    }

    static ScoreHolder holder(String owner) {
        return ScoreHolder.forNameOnly(ownerName(owner));
    }
}
