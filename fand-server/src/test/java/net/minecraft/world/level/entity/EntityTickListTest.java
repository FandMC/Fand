package net.minecraft.world.level.entity;

import io.fand.server.tick.OwnershipCell;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.Entity;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EntityTickListTest {
    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void sectionChangesMoveTickingEntitiesWithoutChangingSerialOrder() {
        var left = new OwnershipCell(-1, 0);
        var right = new OwnershipCell(1, 0);
        var cells = new IdentityHashMap<Entity, OwnershipCell>();
        var list = new EntityTickList(cells::get);
        var first = entity(1);
        var second = entity(2);
        cells.put(first, right);
        cells.put(second, left);
        list.add(first);
        list.add(second);
        var visited = new ArrayList<Entity>();
        list.forEach(entity -> {
            visited.add(entity);
            cells.put(second, right);
            list.move(second);
        });

        assertThat(visited).containsExactly(first, second);
        assertThat(list.cellSizes()).containsExactlyEntriesOf(Map.of(right, 2));
        visited.clear();
        list.forEach(visited::add);
        assertThat(visited).containsExactly(first, second);

        list.remove(second);
        cells.put(second, left);
        list.move(second);
        assertThat(list.contains(second)).isFalse();
        assertThat(list.cellSizes()).containsExactlyEntriesOf(Map.of(right, 1));
    }

    @Test
    void replacementAndTickingEndDuringIterationKeepTheOldSnapshot() {
        var list = new EntityTickList();
        var old = entity(1);
        var replacement = entity(1);
        var second = entity(2);
        list.add(old);
        list.add(second);
        var visited = new ArrayList<Entity>();
        list.forEach(entity -> {
            visited.add(entity);
            if (entity == old) {
                list.add(replacement);
                list.remove(old);
                list.remove(second);
            }
        });

        assertThat(visited).containsExactly(old, second);
        assertThat(list.contains(replacement)).isTrue();
        assertThat(list.contains(old)).isFalse();
        visited.clear();
        list.forEach(visited::add);
        assertThat(visited).containsExactly(replacement);
    }

    private static Entity entity(int id) {
        Entity entity = mock(Entity.class);
        when(entity.getId()).thenReturn(id);
        return entity;
    }
}
