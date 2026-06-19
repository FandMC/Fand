package io.fand.server.scoreboard;

import io.fand.api.scoreboard.ScoreboardTeam;
import io.fand.api.scoreboard.TeamCollisionRule;
import io.fand.api.scoreboard.TeamVisibility;
import io.fand.server.command.AdventureBridge;
import java.util.Objects;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.world.scores.PlayerTeam;
import org.jspecify.annotations.Nullable;

final class FandPlayerScoreboardTeam implements ScoreboardTeam {

    private final FandPlayerScoreboard scoreboard;
    private final PlayerTeam handle;

    FandPlayerScoreboardTeam(FandPlayerScoreboard scoreboard, PlayerTeam handle) {
        this.scoreboard = Objects.requireNonNull(scoreboard, "scoreboard");
        this.handle = Objects.requireNonNull(handle, "handle");
    }

    PlayerTeam handle() {
        return handle;
    }

    @Override
    public String name() {
        return handle.getName();
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
            sendChanged();
        });
    }

    @Override
    public Component prefix() {
        return AdventureBridge.fromVanilla(handle.getPlayerPrefix(), scoreboard.registries());
    }

    @Override
    public void setPrefix(Component prefix) {
        Objects.requireNonNull(prefix, "prefix");
        scoreboard.run(() -> {
            handle.setPlayerPrefix(AdventureBridge.toVanilla(prefix, scoreboard.registries()));
            sendChanged();
        });
    }

    @Override
    public Component suffix() {
        return AdventureBridge.fromVanilla(handle.getPlayerSuffix(), scoreboard.registries());
    }

    @Override
    public void setSuffix(Component suffix) {
        Objects.requireNonNull(suffix, "suffix");
        scoreboard.run(() -> {
            handle.setPlayerSuffix(AdventureBridge.toVanilla(suffix, scoreboard.registries()));
            sendChanged();
        });
    }

    @Override
    public @Nullable NamedTextColor color() {
        return ScoreboardConversions.fromVanillaTeamColor(handle.getColor());
    }

    @Override
    public void setColor(@Nullable NamedTextColor color) {
        scoreboard.run(() -> {
            handle.setColor(ScoreboardConversions.toVanillaTeamColor(color));
            sendChanged();
        });
    }

    @Override
    public boolean allowFriendlyFire() {
        return handle.isAllowFriendlyFire();
    }

    @Override
    public void setAllowFriendlyFire(boolean allow) {
        scoreboard.run(() -> {
            handle.setAllowFriendlyFire(allow);
            sendChanged();
        });
    }

    @Override
    public boolean seeFriendlyInvisibles() {
        return handle.canSeeFriendlyInvisibles();
    }

    @Override
    public void setSeeFriendlyInvisibles(boolean see) {
        scoreboard.run(() -> {
            handle.setSeeFriendlyInvisibles(see);
            sendChanged();
        });
    }

    @Override
    public TeamVisibility nameTagVisibility() {
        return ScoreboardConversions.fromVanilla(handle.getNameTagVisibility());
    }

    @Override
    public void setNameTagVisibility(TeamVisibility visibility) {
        Objects.requireNonNull(visibility, "visibility");
        scoreboard.run(() -> {
            handle.setNameTagVisibility(ScoreboardConversions.toVanilla(visibility));
            sendChanged();
        });
    }

    @Override
    public TeamVisibility deathMessageVisibility() {
        return ScoreboardConversions.fromVanilla(handle.getDeathMessageVisibility());
    }

    @Override
    public void setDeathMessageVisibility(TeamVisibility visibility) {
        Objects.requireNonNull(visibility, "visibility");
        scoreboard.run(() -> handle.setDeathMessageVisibility(ScoreboardConversions.toVanilla(visibility)));
    }

    @Override
    public TeamCollisionRule collisionRule() {
        return ScoreboardConversions.fromVanilla(handle.getCollisionRule());
    }

    @Override
    public void setCollisionRule(TeamCollisionRule rule) {
        Objects.requireNonNull(rule, "rule");
        scoreboard.run(() -> {
            handle.setCollisionRule(ScoreboardConversions.toVanilla(rule));
            sendChanged();
        });
    }

    @Override
    public Set<String> members() {
        return scoreboard.call(() -> Set.copyOf(handle.getPlayers()));
    }

    @Override
    public boolean addMember(String member) {
        var owner = FandScoreboardService.ownerName(member);
        return scoreboard.call(() -> {
            if (!scoreboard.handle().addPlayerToTeam(owner, handle)) {
                return false;
            }
            scoreboard.sendTeamPlayer(handle, owner, ClientboundSetPlayerTeamPacket.Action.ADD);
            return true;
        });
    }

    @Override
    public boolean removeMember(String member) {
        var owner = FandScoreboardService.ownerName(member);
        return scoreboard.call(() -> {
            if (scoreboard.handle().getPlayersTeam(owner) != handle) {
                return false;
            }
            scoreboard.handle().removePlayerFromTeam(owner, handle);
            scoreboard.sendTeamPlayer(handle, owner, ClientboundSetPlayerTeamPacket.Action.REMOVE);
            return true;
        });
    }

    @Override
    public boolean contains(String member) {
        var owner = FandScoreboardService.ownerName(member);
        return scoreboard.call(() -> handle.getPlayers().contains(owner));
    }

    @Override
    public void clearMembers() {
        scoreboard.run(() -> {
            for (var member : Set.copyOf(handle.getPlayers())) {
                scoreboard.handle().removePlayerFromTeam(member, handle);
                scoreboard.sendTeamPlayer(handle, member, ClientboundSetPlayerTeamPacket.Action.REMOVE);
            }
        });
    }

    @Override
    public void unregister() {
        scoreboard.removeTeam(name());
    }

    private void sendChanged() {
        scoreboard.sendTeamChanged(handle);
    }
}
