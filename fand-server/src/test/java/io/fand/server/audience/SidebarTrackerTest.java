package io.fand.server.audience;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.fand.api.scoreboard.Sidebar;
import org.junit.jupiter.api.Test;

class SidebarTrackerTest {

    @Test
    void ownersAreStableAndUniquePerLineIndex() {
        assertThat(SidebarTracker.ownersFor(3))
                .containsExactly("fand:sidebar:0", "fand:sidebar:1", "fand:sidebar:2");
    }

    @Test
    void scoresSortRowsTopToBottom() {
        assertThat(SidebarTracker.scoreForIndex(0)).isEqualTo(Sidebar.MAX_LINES);
        assertThat(SidebarTracker.scoreForIndex(1)).isEqualTo(Sidebar.MAX_LINES - 1);
        assertThat(SidebarTracker.scoreForIndex(Sidebar.MAX_LINES - 1)).isEqualTo(1);
    }

    @Test
    void rejectsOutOfRangeIndexes() {
        assertThatThrownBy(() -> SidebarTracker.ownerForIndex(-1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SidebarTracker.ownerForIndex(Sidebar.MAX_LINES))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SidebarTracker.scoreForIndex(Sidebar.MAX_LINES))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsInvalidLineCounts() {
        assertThatThrownBy(() -> SidebarTracker.ownersFor(-1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SidebarTracker.ownersFor(Sidebar.MAX_LINES + 1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
