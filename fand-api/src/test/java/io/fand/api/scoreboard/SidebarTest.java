package io.fand.api.scoreboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

class SidebarTest {

    @Test
    void copiesLinesOnConstruction() {
        var lines = new ArrayList<>(List.of(new Sidebar.Line(Component.text("one"))));
        var sidebar = new Sidebar(Component.text("title"), lines);

        lines.add(new Sidebar.Line(Component.text("two")));

        assertThat(sidebar.lines()).hasSize(1);
    }

    @Test
    void rejectsTooManyLines() {
        var lines = java.util.stream.IntStream.range(0, Sidebar.MAX_LINES + 1)
                .mapToObj(index -> new Sidebar.Line(Component.text("line " + index)))
                .toList();

        assertThatThrownBy(() -> new Sidebar(Component.text("title"), lines))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("more than " + Sidebar.MAX_LINES);
    }

    @Test
    void rejectsNullLineText() {
        assertThatThrownBy(() -> new Sidebar.Line(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("text");
    }

    @Test
    void buildsFromComponents() {
        var sidebar = Sidebar.of(Component.text("title"), Component.text("one"), Component.text("two"));

        assertThat(sidebar.lines())
                .extracting(Sidebar.Line::text)
                .containsExactly(Component.text("one"), Component.text("two"));
    }
}
