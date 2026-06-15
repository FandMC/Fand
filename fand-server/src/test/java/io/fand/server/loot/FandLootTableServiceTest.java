package io.fand.server.loot;

import static org.assertj.core.api.Assertions.assertThat;

import io.fand.api.item.ItemStack;
import io.fand.api.item.ItemType;
import io.fand.api.loot.LootContext;
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.Test;

final class FandLootTableServiceTest {

    private static final Key KEY = Key.key("fand:test_loot");
    private static final ItemType DIAMOND = new TestItemType(Key.key("minecraft:diamond"), 64);
    private static final ItemType EMERALD = new TestItemType(Key.key("minecraft:emerald"), 64);

    @Test
    void replacementGeneratesLootWithoutAttachedServer() {
        var service = new FandLootTableService(() -> null);
        var registration = service.replace(KEY, context -> java.util.List.of(new ItemStack(DIAMOND, 2)));

        assertThat(registration.key()).isEqualTo(KEY);
        assertThat(registration.active()).isTrue();
        assertThat(service.table(KEY)).isPresent();
        assertThat(service.generate(KEY, LootContext.empty()))
                .containsExactly(new ItemStack(DIAMOND, 2));

        registration.unregister();

        assertThat(registration.active()).isFalse();
        assertThat(service.table(KEY)).isEmpty();
    }

    @Test
    void oldRegistrationCannotRemoveReplacement() {
        var service = new FandLootTableService(() -> null);
        var first = service.replace(KEY, context -> java.util.List.of(new ItemStack(DIAMOND, 1)));
        var second = service.replace(KEY, context -> java.util.List.of(new ItemStack(EMERALD, 3)));

        first.unregister();

        assertThat(first.active()).isFalse();
        assertThat(second.active()).isTrue();
        assertThat(service.generate(KEY, LootContext.empty()))
                .containsExactly(new ItemStack(EMERALD, 3));
    }

    private record TestItemType(Key key, int maxStackSize) implements ItemType {
    }
}
