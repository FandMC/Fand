package net.minecraft.world.level;

import io.fand.server.tick.OwnershipCell;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CellTickListTest {
    private static final OwnershipCell LEFT = new OwnershipCell(-1, 0);
    private static final OwnershipCell RIGHT = new OwnershipCell(5, 0);

    @Test
    void interleavedCellsKeepRegistrationOrderIncludingDuplicateAdds() {
        var list = newList();
        var first = new Member(1, RIGHT);
        var second = new Member(2, LEFT);
        var third = new Member(3, RIGHT);
        list.add(first);
        list.add(second);
        list.add(third);
        list.add(first);

        assertThat(list).containsExactly(first, second, third);
        assertThat(list.cellSizes()).containsExactlyInAnyOrderEntriesOf(Map.of(LEFT, 1, RIGHT, 2));
    }

    @Test
    void movementDuringTraversalKeepsEachMemberOnceAndRetiresEmptyCells() {
        var list = newList();
        var first = new Member(1, RIGHT);
        var second = new Member(2, LEFT);
        list.add(first);
        list.add(second);
        var visited = new ArrayList<Member>();
        list.forEach(member -> {
            visited.add(member);
            second.cell = RIGHT;
            list.move(second);
        });

        assertThat(visited).containsExactly(first, second);
        assertThat(list).containsExactly(first, second);
        assertThat(list.cellSizes()).containsExactlyEntriesOf(Map.of(RIGHT, 2));
        first.cell = LEFT;
        list.move(first);
        assertThat(list).containsExactly(first, second);
        assertThat(list.cellSizes()).containsExactlyInAnyOrderEntriesOf(Map.of(LEFT, 1, RIGHT, 1));
    }

    @Test
    void removalAndAdditionOnlyChangeTheNextTraversal() {
        var list = newList();
        var first = new Member(1, LEFT);
        var second = new Member(2, RIGHT);
        var added = new Member(3, LEFT);
        list.add(first);
        list.add(second);
        var visited = new ArrayList<Member>();
        list.forEach(member -> {
            visited.add(member);
            if (member == first) {
                list.remove(second);
                list.add(added);
                assertThat(list).containsExactly(first, added);
            }
        });

        assertThat(visited).containsExactly(first, second);
        assertThat(list).containsExactly(first, added);
        list.remove(first);
        list.add(first);
        assertThat(list).containsExactly(added, first);
    }

    @Test
    void replacementKeepsOrderAndStaleCallbacksCannotRemoveOrMoveIt() {
        var list = newList();
        var old = new Member(1, LEFT);
        var second = new Member(2, RIGHT);
        var replacement = new Member(1, RIGHT);
        list.add(old);
        list.add(second);
        var visited = new ArrayList<Member>();
        list.forEach(member -> {
            visited.add(member);
            if (member == old) {
                list.add(replacement);
                list.remove(old);
                old.cell = new OwnershipCell(99, 99);
                list.move(old);
            }
        });

        assertThat(visited).containsExactly(old, second);
        assertThat(list).containsExactly(replacement, second);
        assertThat(list.contains(old)).isFalse();
        assertThat(list.contains(replacement)).isTrue();
        assertThat(list.cellSizes()).containsExactlyEntriesOf(Map.of(RIGHT, 2));
    }

    @Test
    void clearingDuringTraversalDoesNotInvalidateTheCapturedMembers() {
        var list = newList();
        var first = new Member(1, LEFT);
        var second = new Member(2, RIGHT);
        list.add(first);
        list.add(second);
        var visited = new ArrayList<Member>();
        list.forEach(member -> {
            visited.add(member);
            list.clear();
        });

        assertThat(visited).containsExactly(first, second);
        assertThat(list).isEmpty();
        assertThat(list.size()).isZero();
        assertThat(list.cellSizes()).isEmpty();
        list.move(first);
        assertThat(list).isEmpty();
    }

    @Test
    void callbackFailureLeavesLaterTraversalUsable() {
        var list = newList();
        var first = new Member(1, LEFT);
        var next = new Member(2, RIGHT);
        list.add(first);
        var failure = new IllegalStateException("tick failed");

        assertThatThrownBy(() -> list.forEach(member -> {
            list.add(next);
            throw failure;
        })).isSameAs(failure);
        assertThat(list).containsExactly(first, next);
    }

    @Test
    void diagnosticSnapshotsAreDetachedAndCannotModifyMembership() {
        var list = newList();
        var member = new Member(1, LEFT);
        list.add(member);
        var counts = list.cellSizes();
        var iterator = list.iterator();
        list.remove(member);

        assertThat(counts).containsExactlyEntriesOf(Map.of(LEFT, 1));
        assertThatThrownBy(() -> counts.clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThat(iterator.next()).isSameAs(member);
        assertThatThrownBy(iterator::remove).isInstanceOf(UnsupportedOperationException.class);
        assertThat(list).isEmpty();
    }

    @Test
    void identityStorageKeepsEqualObjectsIndependent() {
        var list = CellTickList.<List<Integer>>byIdentity(ignored -> LEFT);
        List<Integer> first = new ArrayList<>(List.of(1));
        List<Integer> second = new ArrayList<>(List.of(1));
        list.add(first);
        list.add(second);
        assertThat(list.size()).isEqualTo(2);

        list.remove(first);

        assertThat(list.size()).isEqualTo(1);
        assertThat(list.iterator().next()).isSameAs(second);
        assertThat(list.contains(first)).isFalse();
    }

    private static CellTickList<Integer, Member> newList() {
        return new CellTickList<>(member -> member.id, member -> member.cell);
    }

    private static final class Member {
        private final int id;
        private OwnershipCell cell;

        private Member(int id, OwnershipCell cell) {
            this.id = id;
            this.cell = cell;
        }
    }
}
