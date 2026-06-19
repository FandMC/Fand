package io.fand.server.scoreboard;

import io.fand.api.scoreboard.ScoreNumberFormat;
import io.fand.api.scoreboard.ScoreboardObjective;
import io.fand.api.scoreboard.ScoreboardScore;
import io.fand.server.command.AdventureBridge;
import java.util.Objects;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ScoreHolder;
import org.jspecify.annotations.Nullable;

final class FandPlayerScoreboardScore implements ScoreboardScore {

    private final FandPlayerScoreboard scoreboard;
    private final Objective objective;
    private final String owner;

    FandPlayerScoreboardScore(FandPlayerScoreboard scoreboard, Objective objective, String owner) {
        this.scoreboard = Objects.requireNonNull(scoreboard, "scoreboard");
        this.objective = Objects.requireNonNull(objective, "objective");
        this.owner = FandScoreboardService.ownerName(owner);
    }

    @Override
    public String owner() {
        return owner;
    }

    @Override
    public ScoreboardObjective objective() {
        return new FandPlayerScoreboardObjective(scoreboard, objective);
    }

    @Override
    public int value() {
        return scoreboard.call(() -> access().get());
    }

    @Override
    public void setValue(int value) {
        scoreboard.run(() -> {
            access().set(value);
            scoreboard.sendScore(owner, objective);
        });
    }

    @Override
    public Optional<Component> displayName() {
        return scoreboard.call(() -> {
            var display = access().display();
            return display == null
                    ? Optional.empty()
                    : Optional.of(AdventureBridge.fromVanilla(display, scoreboard.registries()));
        });
    }

    @Override
    public void setDisplayName(@Nullable Component displayName) {
        scoreboard.run(() -> {
            access().display(displayName == null
                    ? null
                    : AdventureBridge.toVanilla(displayName, scoreboard.registries()));
            scoreboard.sendScore(owner, objective);
        });
    }

    @Override
    public boolean locked() {
        return scoreboard.call(() -> access().locked());
    }

    @Override
    public void setLocked(boolean locked) {
        scoreboard.run(() -> {
            if (locked) {
                access().lock();
            } else {
                access().unlock();
            }
        });
    }

    @Override
    public ScoreNumberFormat numberFormat() {
        return scoreboard.call(() -> {
            var info = scoreboard.handle().getPlayerScoreInfo(ScoreHolder.forNameOnly(owner), objective);
            return ScoreboardConversions.fromVanilla(
                    info == null ? null : info.numberFormat(),
                    scoreboard.registries());
        });
    }

    @Override
    public void setNumberFormat(ScoreNumberFormat format) {
        Objects.requireNonNull(format, "format");
        scoreboard.run(() -> {
            access().numberFormatOverride(ScoreboardConversions.toVanilla(format, scoreboard.registries()));
            scoreboard.sendScore(owner, objective);
        });
    }

    private net.minecraft.world.scores.ScoreAccess access() {
        return scoreboard.handle().getOrCreatePlayerScore(ScoreHolder.forNameOnly(owner), objective);
    }
}
