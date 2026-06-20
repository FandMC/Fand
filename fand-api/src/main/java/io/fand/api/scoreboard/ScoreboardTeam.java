package io.fand.api.scoreboard;

import io.fand.api.entity.Entity;
import io.fand.api.entity.Player;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jspecify.annotations.Nullable;

/**
 * A persistent vanilla scoreboard team.
 */
public interface ScoreboardTeam {

    String name();

    Component displayName();

    void setDisplayName(Component displayName);

    Component prefix();

    void setPrefix(Component prefix);

    Component suffix();

    void setSuffix(Component suffix);

    @Nullable NamedTextColor color();

    void setColor(@Nullable NamedTextColor color);

    boolean allowFriendlyFire();

    void setAllowFriendlyFire(boolean allow);

    boolean seeFriendlyInvisibles();

    void setSeeFriendlyInvisibles(boolean see);

    TeamVisibility nameTagVisibility();

    void setNameTagVisibility(TeamVisibility visibility);

    TeamVisibility deathMessageVisibility();

    void setDeathMessageVisibility(TeamVisibility visibility);

    TeamCollisionRule collisionRule();

    void setCollisionRule(TeamCollisionRule rule);

    /** Point-in-time snapshot of this team's member strings; immutable. */
    Set<String> members();

    boolean addMember(String member);

    default boolean addPlayer(Player player) {
        return addMember(player.name());
    }

    default boolean addEntity(Entity entity) {
        return addMember(entity.uniqueId().toString());
    }

    boolean removeMember(String member);

    boolean contains(String member);

    void clearMembers();

    void unregister();
}
