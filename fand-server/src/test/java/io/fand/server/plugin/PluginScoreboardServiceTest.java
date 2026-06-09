package io.fand.server.plugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.fand.api.scoreboard.ScoreDisplaySlot;
import io.fand.api.scoreboard.ScoreNumberFormat;
import io.fand.api.scoreboard.ScoreRenderType;
import io.fand.api.scoreboard.ScoreboardObjective;
import io.fand.api.scoreboard.ScoreboardRegistration;
import io.fand.api.scoreboard.ScoreboardScore;
import io.fand.api.scoreboard.ScoreboardService;
import io.fand.api.scoreboard.ScoreboardTeam;
import io.fand.api.scoreboard.TeamCollisionRule;
import io.fand.api.scoreboard.TeamVisibility;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

final class PluginScoreboardServiceTest {

    @Test
    void scopesAndCleansPluginRegistrations() {
        var delegate = new FakeScoreboardService();
        var tracker = new PluginResourceTracker();
        var service = new PluginScoreboardService(delegate, tracker, "plug");

        var objectiveRegistration = service.registerObjective("kills", Component.text("Kills"));
        var teamRegistration = service.registerTeam("red");

        assertThat(objectiveRegistration.name()).isEqualTo("plug:kills");
        assertThat(teamRegistration.name()).isEqualTo("plug:red");
        assertThat(delegate.objective("plug:kills")).isPresent();
        assertThat(delegate.team("plug:red")).isPresent();

        tracker.close();

        assertThat(objectiveRegistration.active()).isFalse();
        assertThat(teamRegistration.active()).isFalse();
        assertThat(delegate.objective("plug:kills")).isEmpty();
        assertThat(delegate.team("plug:red")).isEmpty();
    }

    @Test
    void filtersLookupToPluginNamespace() {
        var delegate = new FakeScoreboardService();
        delegate.registerObjective("plug:kills", Component.text("Kills"));
        delegate.registerObjective("other:kills", Component.text("Other"));
        delegate.registerTeam("plug:red");
        delegate.registerTeam("other:red");

        var service = new PluginScoreboardService(delegate, new PluginResourceTracker(), "plug");

        assertThat(service.objectives()).extracting(ScoreboardObjective::name).containsExactly("plug:kills");
        assertThat(service.teams()).extracting(ScoreboardTeam::name).containsExactly("plug:red");
        assertThat(service.objective("kills")).isPresent();
        assertThat(service.team("red")).isPresent();
    }

    @Test
    void refusesToDisplayForeignObjectiveThroughPluginService() {
        var delegate = new FakeScoreboardService();
        delegate.registerObjective("other:kills", Component.text("Other"));
        var foreign = delegate.objective("other:kills").orElseThrow();
        var service = new PluginScoreboardService(delegate, new PluginResourceTracker(), "plug");

        assertThatThrownBy(() -> service.setDisplayedObjective(ScoreDisplaySlot.SIDEBAR, foreign))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static final class FakeScoreboardService implements ScoreboardService {

        private final Map<String, FakeObjective> objectives = new LinkedHashMap<>();
        private final Map<String, FakeTeam> teams = new LinkedHashMap<>();
        private final Map<ScoreDisplaySlot, ScoreboardObjective> displayed = new LinkedHashMap<>();

        @Override
        public Collection<? extends ScoreboardObjective> objectives() {
            return objectives.values();
        }

        @Override
        public Optional<? extends ScoreboardObjective> objective(String name) {
            return Optional.ofNullable(objectives.get(name));
        }

        @Override
        public ScoreboardRegistration registerObjective(String name, Component displayName) {
            var objective = new FakeObjective(name, displayName);
            objectives.put(name, objective);
            return new FakeRegistration(name, () -> objectives.remove(name));
        }

        @Override
        public ScoreboardRegistration registerObjective(String name, Component displayName, String criteria, ScoreRenderType renderType) {
            return registerObjective(name, displayName);
        }

        @Override
        public boolean removeObjective(String name) {
            return objectives.remove(name) != null;
        }

        @Override
        public Optional<? extends ScoreboardObjective> displayedObjective(ScoreDisplaySlot slot) {
            return Optional.ofNullable(displayed.get(slot));
        }

        @Override
        public void setDisplayedObjective(ScoreDisplaySlot slot, ScoreboardObjective objective) {
            displayed.put(slot, objective);
        }

        @Override
        public void clearDisplayedObjective(ScoreDisplaySlot slot) {
            displayed.remove(slot);
        }

        @Override
        public Collection<? extends ScoreboardTeam> teams() {
            return teams.values();
        }

        @Override
        public Optional<? extends ScoreboardTeam> team(String name) {
            return Optional.ofNullable(teams.get(name));
        }

        @Override
        public ScoreboardRegistration registerTeam(String name) {
            teams.put(name, new FakeTeam(name));
            return new FakeRegistration(name, () -> teams.remove(name));
        }

        @Override
        public boolean removeTeam(String name) {
            return teams.remove(name) != null;
        }

        @Override
        public Optional<? extends ScoreboardTeam> teamOf(String member) {
            return teams.values().stream().filter(team -> team.contains(member)).findFirst();
        }

        @Override
        public boolean removeMemberFromTeam(String member) {
            return teams.values().stream().anyMatch(team -> team.removeMember(member));
        }
    }

    private static final class FakeRegistration implements ScoreboardRegistration {

        private final String name;
        private final Runnable unregister;
        private boolean active = true;

        private FakeRegistration(String name, Runnable unregister) {
            this.name = name;
            this.unregister = unregister;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public boolean active() {
            return active;
        }

        @Override
        public void unregister() {
            if (active) {
                active = false;
                unregister.run();
            }
        }
    }

    private record FakeObjective(String name, Component displayName) implements ScoreboardObjective {

        @Override
        public String criteria() {
            return "dummy";
        }

        @Override
        public boolean readOnly() {
            return false;
        }

        @Override
        public void setDisplayName(Component displayName) {
        }

        @Override
        public ScoreRenderType renderType() {
            return ScoreRenderType.INTEGER;
        }

        @Override
        public void setRenderType(ScoreRenderType renderType) {
        }

        @Override
        public boolean displayAutoUpdate() {
            return false;
        }

        @Override
        public void setDisplayAutoUpdate(boolean displayAutoUpdate) {
        }

        @Override
        public ScoreNumberFormat numberFormat() {
            return ScoreNumberFormat.DEFAULT;
        }

        @Override
        public void setNumberFormat(ScoreNumberFormat format) {
        }

        @Override
        public ScoreboardScore score(String owner) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<? extends ScoreboardScore> existingScore(String owner) {
            return Optional.empty();
        }

        @Override
        public Collection<? extends ScoreboardScore> scores() {
            return Set.of();
        }

        @Override
        public boolean resetScore(String owner) {
            return false;
        }

        @Override
        public void unregister() {
        }
    }

    private static final class FakeTeam implements ScoreboardTeam {

        private final String name;
        private final java.util.LinkedHashSet<String> members = new java.util.LinkedHashSet<>();

        private FakeTeam(String name) {
            this.name = name;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public Component displayName() {
            return Component.text(name);
        }

        @Override
        public void setDisplayName(Component displayName) {
        }

        @Override
        public Component prefix() {
            return Component.empty();
        }

        @Override
        public void setPrefix(Component prefix) {
        }

        @Override
        public Component suffix() {
            return Component.empty();
        }

        @Override
        public void setSuffix(Component suffix) {
        }

        @Override
        public @Nullable NamedTextColor color() {
            return null;
        }

        @Override
        public void setColor(@Nullable NamedTextColor color) {
        }

        @Override
        public boolean allowFriendlyFire() {
            return true;
        }

        @Override
        public void setAllowFriendlyFire(boolean allow) {
        }

        @Override
        public boolean seeFriendlyInvisibles() {
            return true;
        }

        @Override
        public void setSeeFriendlyInvisibles(boolean see) {
        }

        @Override
        public TeamVisibility nameTagVisibility() {
            return TeamVisibility.ALWAYS;
        }

        @Override
        public void setNameTagVisibility(TeamVisibility visibility) {
        }

        @Override
        public TeamVisibility deathMessageVisibility() {
            return TeamVisibility.ALWAYS;
        }

        @Override
        public void setDeathMessageVisibility(TeamVisibility visibility) {
        }

        @Override
        public TeamCollisionRule collisionRule() {
            return TeamCollisionRule.ALWAYS;
        }

        @Override
        public void setCollisionRule(TeamCollisionRule rule) {
        }

        @Override
        public Set<String> members() {
            return Set.copyOf(members);
        }

        @Override
        public boolean addMember(String member) {
            return members.add(member);
        }

        @Override
        public boolean removeMember(String member) {
            return members.remove(member);
        }

        @Override
        public boolean contains(String member) {
            return members.contains(member);
        }

        @Override
        public void clearMembers() {
            members.clear();
        }

        @Override
        public void unregister() {
        }
    }
}
