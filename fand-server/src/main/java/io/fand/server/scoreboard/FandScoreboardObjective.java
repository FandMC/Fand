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

final class FandScoreboardObjective implements ScoreboardObjective {

    private final FandScoreboardService service;
    private final Objective handle;

    FandScoreboardObjective(FandScoreboardService service, Objective handle) {
        this.service = Objects.requireNonNull(service, "service");
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
        return ScoreboardThreading.call(service.server(), () ->
                AdventureBridge.fromVanilla(handle.getDisplayName(), service.server().registryAccess()));
    }

    @Override
    public void setDisplayName(Component displayName) {
        Objects.requireNonNull(displayName, "displayName");
        ScoreboardThreading.run(service.server(), () ->
                handle.setDisplayName(AdventureBridge.toVanilla(displayName, service.server().registryAccess())));
    }

    @Override
    public ScoreRenderType renderType() {
        return ScoreboardThreading.call(service.server(), () -> ScoreboardConversions.fromVanilla(handle.getRenderType()));
    }

    @Override
    public void setRenderType(ScoreRenderType renderType) {
        ScoreboardThreading.run(service.server(), () -> handle.setRenderType(ScoreboardConversions.toVanilla(renderType)));
    }

    @Override
    public boolean displayAutoUpdate() {
        return ScoreboardThreading.call(service.server(), handle::displayAutoUpdate);
    }

    @Override
    public void setDisplayAutoUpdate(boolean displayAutoUpdate) {
        ScoreboardThreading.run(service.server(), () -> handle.setDisplayAutoUpdate(displayAutoUpdate));
    }

    @Override
    public ScoreNumberFormat numberFormat() {
        return ScoreboardThreading.call(service.server(), () ->
                ScoreboardConversions.fromVanilla(handle.numberFormat(), service.server().registryAccess()));
    }

    @Override
    public void setNumberFormat(ScoreNumberFormat format) {
        ScoreboardThreading.run(service.server(), () ->
                handle.setNumberFormat(ScoreboardConversions.toVanilla(format, service.server().registryAccess())));
    }

    @Override
    public ScoreboardScore score(String owner) {
        var normalized = FandScoreboardService.ownerName(owner);
        return new FandScoreboardScore(service, handle, normalized);
    }

    @Override
    public Optional<? extends ScoreboardScore> existingScore(String owner) {
        var holder = FandScoreboardService.holder(owner);
        return ScoreboardThreading.call(service.server(), () ->
                service.handle().getPlayerScoreInfo(holder, handle) == null
                        ? Optional.empty()
                        : Optional.of(new FandScoreboardScore(service, handle, holder.getScoreboardName())));
    }

    @Override
    public Collection<? extends ScoreboardScore> scores() {
        return ScoreboardThreading.call(service.server(), () -> service.handle().listPlayerScores(handle).stream()
                .map(entry -> new FandScoreboardScore(service, handle, entry.owner()))
                .toList());
    }

    @Override
    public boolean resetScore(String owner) {
        var holder = FandScoreboardService.holder(owner);
        return ScoreboardThreading.call(service.server(), () -> {
            if (service.handle().getPlayerScoreInfo(holder, handle) == null) {
                return false;
            }
            service.handle().resetSinglePlayerScore(holder, handle);
            return true;
        });
    }

    @Override
    public void unregister() {
        service.removeObjective(handle.getName());
    }
}
