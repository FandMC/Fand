package io.fand.server.scoreboard;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

final class FandScoreboardRegistrationTest {

    @Test
    void oldObjectiveRegistrationCannotRemoveReplacementWithSameName() {
        var current = new AtomicReference<Object>();
        var firstObjective = new Object();
        var secondObjective = new Object();

        current.set(firstObjective);
        var first = registration("demo", current, firstObjective);
        current.set(null);
        current.set(secondObjective);
        var second = registration("demo", current, secondObjective);

        first.unregister();

        assertThat(first.active()).isFalse();
        assertThat(second.active()).isTrue();
        assertThat(current).hasValue(secondObjective);
    }

    @Test
    void oldTeamRegistrationCannotRemoveReplacementWithSameName() {
        var current = new AtomicReference<Object>();
        var firstTeam = new Object();
        var secondTeam = new Object();

        current.set(firstTeam);
        var first = registration("demo", current, firstTeam);
        current.set(null);
        current.set(secondTeam);
        var second = registration("demo", current, secondTeam);

        first.unregister();

        assertThat(first.active()).isFalse();
        assertThat(second.active()).isTrue();
        assertThat(current).hasValue(secondTeam);
    }

    private static FandScoreboardRegistration registration(String name, AtomicReference<Object> current, Object installed) {
        return new FandScoreboardRegistration(
                name,
                () -> current.get() == installed,
                () -> current.compareAndSet(installed, null));
    }
}
