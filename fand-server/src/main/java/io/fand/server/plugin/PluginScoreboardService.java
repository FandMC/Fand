package io.fand.server.plugin;

import io.fand.api.scoreboard.ScoreDisplaySlot;
import io.fand.api.scoreboard.ScoreRenderType;
import io.fand.api.scoreboard.ScoreboardObjective;
import io.fand.api.scoreboard.ScoreboardRegistration;
import io.fand.api.scoreboard.ScoreboardService;
import io.fand.api.scoreboard.ScoreboardTeam;
import io.fand.server.scoreboard.ScoreboardNames;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import net.kyori.adventure.text.Component;

public final class PluginScoreboardService implements ScoreboardService {

    private final ScoreboardService delegate;
    private final PluginResourceTracker tracker;
    private final String namespace;

    public PluginScoreboardService(ScoreboardService delegate, PluginResourceTracker tracker, String namespace) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.tracker = Objects.requireNonNull(tracker, "tracker");
        this.namespace = Objects.requireNonNull(namespace, "namespace");
    }

    public String namespace() {
        return namespace;
    }

    @Override
    public Collection<? extends ScoreboardObjective> objectives() {
        return delegate.objectives().stream()
                .filter(this::ownedByThisPlugin)
                .toList();
    }

    @Override
    public Optional<? extends ScoreboardObjective> objective(String name) {
        return delegate.objective(scopedObjective(name)).filter(this::ownedByThisPlugin);
    }

    @Override
    public ScoreboardRegistration registerObjective(String name, Component displayName) {
        return tracker.track(delegate.registerObjective(scopedObjective(name), displayName));
    }

    @Override
    public ScoreboardRegistration registerObjective(String name, Component displayName, String criteria, ScoreRenderType renderType) {
        return tracker.track(delegate.registerObjective(scopedObjective(name), displayName, criteria, renderType));
    }

    @Override
    public boolean removeObjective(String name) {
        return delegate.removeObjective(scopedObjective(name));
    }

    @Override
    public Optional<? extends ScoreboardObjective> displayedObjective(ScoreDisplaySlot slot) {
        return delegate.displayedObjective(slot).filter(this::ownedByThisPlugin);
    }

    @Override
    public void setDisplayedObjective(ScoreDisplaySlot slot, ScoreboardObjective objective) {
        if (!ownedByThisPlugin(objective)) {
            throw new IllegalArgumentException("Objective is not owned by plugin " + namespace + ": " + objective.name());
        }
        delegate.setDisplayedObjective(slot, objective);
    }

    @Override
    public void clearDisplayedObjective(ScoreDisplaySlot slot) {
        var current = delegate.displayedObjective(slot);
        if (current.isPresent() && ownedByThisPlugin(current.get())) {
            delegate.clearDisplayedObjective(slot);
        }
    }

    @Override
    public Collection<? extends ScoreboardTeam> teams() {
        return delegate.teams().stream()
                .filter(this::ownedByThisPlugin)
                .toList();
    }

    @Override
    public Optional<? extends ScoreboardTeam> team(String name) {
        return delegate.team(scopedTeam(name)).filter(this::ownedByThisPlugin);
    }

    @Override
    public ScoreboardRegistration registerTeam(String name) {
        return tracker.track(delegate.registerTeam(scopedTeam(name)));
    }

    @Override
    public boolean removeTeam(String name) {
        return delegate.removeTeam(scopedTeam(name));
    }

    @Override
    public Optional<? extends ScoreboardTeam> teamOf(String member) {
        return delegate.teamOf(member).filter(this::ownedByThisPlugin);
    }

    @Override
    public boolean removeMemberFromTeam(String member) {
        var team = delegate.teamOf(member);
        if (team.isEmpty() || !ownedByThisPlugin(team.get())) {
            return false;
        }
        return delegate.removeMemberFromTeam(member);
    }

    private String scopedObjective(String name) {
        return ScoreboardNames.normalizePluginObjective(namespace, name);
    }

    private String scopedTeam(String name) {
        return ScoreboardNames.normalizePluginTeam(namespace, name);
    }

    private boolean ownedByThisPlugin(ScoreboardObjective objective) {
        return objective.name().startsWith(namespace + ":");
    }

    private boolean ownedByThisPlugin(ScoreboardTeam team) {
        return team.name().startsWith(namespace + ":");
    }
}
