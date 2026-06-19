package io.fand.server.scoreboard;

import io.fand.api.scoreboard.ScoreNumberFormat;
import io.fand.api.scoreboard.ScoreRenderType;
import io.fand.api.scoreboard.ScoreboardObjective;
import io.fand.api.scoreboard.ScoreboardScore;
import io.fand.server.command.AdventureBridge;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ScoreHolder;

final class FandPlayerScoreboardObjective implements ScoreboardObjective {

    private final FandPlayerScoreboard scoreboard;
    private final Objective handle;

    FandPlayerScoreboardObjective(FandPlayerScoreboard scoreboard, Objective handle) {
        this.scoreboard = Objects.requireNonNull(scoreboard, "scoreboard");
        this.handle = Objects.requireNonNull(handle, "handle");
    }

    Objective handle() {
        return handle;
    }

    @Override
    public String name() {
        return handle.getName();
    }

    @Override
    public String criteria() {
        return handle.getCriteria().getName();
    }

    @Override
    public boolean readOnly() {
        return handle.getCriteria().isReadOnly();
    }

    @Override
    public Component displayName() {
        return AdventureBridge.fromVanilla(handle.getDisplayName(), scoreboard.registries());
    }

    @Override
    public void setDisplayName(Component displayName) {
        Objects.requireNonNull(displayName, "displayName");
        scoreboard.run(() -> {
            handle.setDisplayName(AdventureBridge.toVanilla(displayName, scoreboard.registries()));
            scoreboard.sendObjectiveChanged(handle);
        });
    }

    @Override
    public ScoreRenderType renderType() {
        return ScoreboardConversions.fromVanilla(handle.getRenderType());
    }

    @Override
    public void setRenderType(ScoreRenderType renderType) {
        Objects.requireNonNull(renderType, "renderType");
        scoreboard.run(() -> {
            handle.setRenderType(ScoreboardConversions.toVanilla(renderType));
            scoreboard.sendObjectiveChanged(handle);
        });
    }

    @Override
    public boolean displayAutoUpdate() {
        return handle.displayAutoUpdate();
    }

    @Override
    public void setDisplayAutoUpdate(boolean displayAutoUpdate) {
        scoreboard.run(() -> handle.setDisplayAutoUpdate(displayAutoUpdate));
    }

    @Override
    public ScoreNumberFormat numberFormat() {
        return ScoreboardConversions.fromVanilla(handle.numberFormat(), scoreboard.registries());
    }

    @Override
    public void setNumberFormat(ScoreNumberFormat format) {
        Objects.requireNonNull(format, "format");
        scoreboard.run(() -> {
            handle.setNumberFormat(ScoreboardConversions.toVanilla(format, scoreboard.registries()));
            scoreboard.sendObjectiveChanged(handle);
        });
    }

    @Override
    public ScoreboardScore score(String owner) {
        return new FandPlayerScoreboardScore(scoreboard, handle, owner);
    }

    @Override
    public Optional<? extends ScoreboardScore> existingScore(String owner) {
        var holder = ScoreHolder.forNameOnly(FandScoreboardService.ownerName(owner));
        return scoreboard.call(() -> scoreboard.handle().getPlayerScoreInfo(holder, handle) == null
                ? Optional.empty()
                : Optional.of(new FandPlayerScoreboardScore(scoreboard, handle, holder.getScoreboardName())));
    }

    @Override
    public Collection<? extends ScoreboardScore> scores() {
        return scoreboard.call(() -> scoreboard.handle().listPlayerScores(handle).stream()
                .map(entry -> new FandPlayerScoreboardScore(scoreboard, handle, entry.owner()))
                .toList());
    }

    @Override
    public boolean resetScore(String owner) {
        var holder = ScoreHolder.forNameOnly(FandScoreboardService.ownerName(owner));
        return scoreboard.call(() -> {
            if (scoreboard.handle().getPlayerScoreInfo(holder, handle) == null) {
                return false;
            }
            scoreboard.handle().resetSinglePlayerScore(holder, handle);
            scoreboard.sendScoreRemoved(holder.getScoreboardName(), handle);
            return true;
        });
    }

    @Override
    public void unregister() {
        scoreboard.removeObjective(name());
    }
}
