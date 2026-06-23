package io.fand.server.scoreboard;

import io.fand.api.scoreboard.ScoreboardTeam;
import io.fand.api.scoreboard.TeamCollisionRule;
import io.fand.api.scoreboard.TeamVisibility;
import io.fand.server.command.AdventureBridge;
import java.util.Objects;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.world.scores.PlayerTeam;
import org.jspecify.annotations.Nullable;

final class FandScoreboardTeam implements ScoreboardTeam {

    private final FandScoreboardService service;
    private final PlayerTeam handle;

    FandScoreboardTeam(FandScoreboardService service, PlayerTeam handle) {
        this.service = Objects.requireNonNull(service, "service");
        this.handle = Objects.requireNonNull(handle, "handle");
    }

    @Override
    public String name() {
        return handle.getName();
    }

    @Override
    public Component displayName() {
        return ScoreboardThreading.call(service.server(), () ->
                AdventureBridge.fromVanilla(handle.getDisplayName(), service.server().registryAccess()));
    }

    @Override
    public void setDisplayName(Component displayName) {
        Objects.requireNonNull(displayName, "displayName");
        ScoreboardThreading.run(service.server(), () -> {
            var value = AdventureBridge.toVanilla(displayName, service.server().registryAccess());
            if (!handle.getDisplayName().equals(value)) {
                handle.setDisplayName(value);
            }
        });
    }

    @Override
    public Component prefix() {
        return ScoreboardThreading.call(service.server(), () ->
                AdventureBridge.fromVanilla(handle.getPlayerPrefix(), service.server().registryAccess()));
    }

    @Override
    public void setPrefix(Component prefix) {
        Objects.requireNonNull(prefix, "prefix");
        ScoreboardThreading.run(service.server(), () -> {
            var value = AdventureBridge.toVanilla(prefix, service.server().registryAccess());
            if (!handle.getPlayerPrefix().equals(value)) {
                handle.setPlayerPrefix(value);
            }
        });
    }

    @Override
    public Component suffix() {
        return ScoreboardThreading.call(service.server(), () ->
                AdventureBridge.fromVanilla(handle.getPlayerSuffix(), service.server().registryAccess()));
    }

    @Override
    public void setSuffix(Component suffix) {
        Objects.requireNonNull(suffix, "suffix");
        ScoreboardThreading.run(service.server(), () -> {
            var value = AdventureBridge.toVanilla(suffix, service.server().registryAccess());
            if (!handle.getPlayerSuffix().equals(value)) {
                handle.setPlayerSuffix(value);
            }
        });
    }

    @Override
    public @Nullable NamedTextColor color() {
        return ScoreboardThreading.call(service.server(), () -> ScoreboardConversions.fromVanillaTeamColor(handle.getColor()));
    }

    @Override
    public void setColor(@Nullable NamedTextColor color) {
        ScoreboardThreading.run(service.server(), () -> {
            var value = ScoreboardConversions.toVanillaTeamColor(color);
            if (handle.getColor() != value) {
                handle.setColor(value);
            }
        });
    }

    @Override
    public boolean allowFriendlyFire() {
        return ScoreboardThreading.call(service.server(), handle::isAllowFriendlyFire);
    }

    @Override
    public void setAllowFriendlyFire(boolean allow) {
        ScoreboardThreading.run(service.server(), () -> {
            if (handle.isAllowFriendlyFire() != allow) {
                handle.setAllowFriendlyFire(allow);
            }
        });
    }

    @Override
    public boolean seeFriendlyInvisibles() {
        return ScoreboardThreading.call(service.server(), handle::canSeeFriendlyInvisibles);
    }

    @Override
    public void setSeeFriendlyInvisibles(boolean see) {
        ScoreboardThreading.run(service.server(), () -> {
            if (handle.canSeeFriendlyInvisibles() != see) {
                handle.setSeeFriendlyInvisibles(see);
            }
        });
    }

    @Override
    public TeamVisibility nameTagVisibility() {
        return ScoreboardThreading.call(service.server(), () -> ScoreboardConversions.fromVanilla(handle.getNameTagVisibility()));
    }

    @Override
    public void setNameTagVisibility(TeamVisibility visibility) {
        ScoreboardThreading.run(service.server(), () -> {
            var value = ScoreboardConversions.toVanilla(visibility);
            if (handle.getNameTagVisibility() != value) {
                handle.setNameTagVisibility(value);
            }
        });
    }

    @Override
    public TeamVisibility deathMessageVisibility() {
        return ScoreboardThreading.call(service.server(), () -> ScoreboardConversions.fromVanilla(handle.getDeathMessageVisibility()));
    }

    @Override
    public void setDeathMessageVisibility(TeamVisibility visibility) {
        ScoreboardThreading.run(service.server(), () -> {
            var value = ScoreboardConversions.toVanilla(visibility);
            if (handle.getDeathMessageVisibility() != value) {
                handle.setDeathMessageVisibility(value);
            }
        });
    }

    @Override
    public TeamCollisionRule collisionRule() {
        return ScoreboardThreading.call(service.server(), () -> ScoreboardConversions.fromVanilla(handle.getCollisionRule()));
    }

    @Override
    public void setCollisionRule(TeamCollisionRule rule) {
        ScoreboardThreading.run(service.server(), () -> {
            var value = ScoreboardConversions.toVanilla(rule);
            if (handle.getCollisionRule() != value) {
                handle.setCollisionRule(value);
            }
        });
    }

    @Override
    public Set<String> members() {
        return ScoreboardThreading.call(service.server(), () -> Set.copyOf(handle.getPlayers()));
    }

    @Override
    public boolean addMember(String member) {
        var owner = FandScoreboardService.ownerName(member);
        return ScoreboardThreading.call(service.server(), () -> service.handle().addPlayerToTeam(owner, handle));
    }

    @Override
    public boolean removeMember(String member) {
        var owner = FandScoreboardService.ownerName(member);
        return ScoreboardThreading.call(service.server(), () -> {
            if (service.handle().getPlayersTeam(owner) != handle) {
                return false;
            }
            service.handle().removePlayerFromTeam(owner, handle);
            return true;
        });
    }

    @Override
    public boolean contains(String member) {
        var owner = FandScoreboardService.ownerName(member);
        return ScoreboardThreading.call(service.server(), () -> handle.getPlayers().contains(owner));
    }

    @Override
    public void clearMembers() {
        ScoreboardThreading.run(service.server(), () -> {
            for (var member : Set.copyOf(handle.getPlayers())) {
                service.handle().removePlayerFromTeam(member, handle);
            }
        });
    }

    @Override
    public void unregister() {
        service.removeTeam(handle);
    }
}
