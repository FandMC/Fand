package io.fand.server.scoreboard;

import io.fand.api.scoreboard.ScoreNumberFormat;
import io.fand.api.scoreboard.ScoreboardObjective;
import io.fand.api.scoreboard.ScoreboardScore;
import io.fand.server.command.AdventureBridge;
import java.util.Objects;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ScoreAccess;
import org.jspecify.annotations.Nullable;

final class FandScoreboardScore implements ScoreboardScore {

    private final FandScoreboardService service;
    private final Objective objective;
    private final String owner;

    FandScoreboardScore(FandScoreboardService service, Objective objective, String owner) {
        this.service = Objects.requireNonNull(service, "service");
        this.objective = Objects.requireNonNull(objective, "objective");
        this.owner = FandScoreboardService.ownerName(owner);
    }

    @Override
    public String owner() {
        return owner;
    }

    @Override
    public ScoreboardObjective objective() {
        return new FandScoreboardObjective(service, objective);
    }

    @Override
    public int value() {
        return ScoreboardThreading.call(service.server(), () ->
                service.handle().getOrCreatePlayerScore(FandScoreboardService.holder(owner), objective).get());
    }

    @Override
    public void setValue(int value) {
        ScoreboardThreading.run(service.server(), () -> access().set(value));
    }

    @Override
    public Optional<Component> displayName() {
        return ScoreboardThreading.call(service.server(), () -> {
            var display = access().display();
            return display == null
                    ? Optional.empty()
                    : Optional.of(AdventureBridge.fromVanilla(display, service.server().registryAccess()));
        });
    }

    @Override
    public void setDisplayName(@Nullable Component displayName) {
        ScoreboardThreading.run(service.server(), () -> access().display(displayName == null
                ? null
                : AdventureBridge.toVanilla(displayName, service.server().registryAccess())));
    }

    @Override
    public boolean locked() {
        return ScoreboardThreading.call(service.server(), () -> access().locked());
    }

    @Override
    public void setLocked(boolean locked) {
        ScoreboardThreading.run(service.server(), () -> {
            if (locked) {
                access().lock();
            } else {
                access().unlock();
            }
        });
    }

    @Override
    public ScoreNumberFormat numberFormat() {
        return ScoreboardThreading.call(service.server(), () -> {
            var info = service.handle().getPlayerScoreInfo(FandScoreboardService.holder(owner), objective);
            return ScoreboardConversions.fromVanilla(
                    info == null ? null : info.numberFormat(),
                    service.server().registryAccess());
        });
    }

    @Override
    public void setNumberFormat(ScoreNumberFormat format) {
        ScoreboardThreading.run(service.server(), () ->
                access().numberFormatOverride(ScoreboardConversions.toVanilla(format, service.server().registryAccess())));
    }

    private ScoreAccess access() {
        return service.handle().getOrCreatePlayerScore(FandScoreboardService.holder(owner), objective);
    }
}
