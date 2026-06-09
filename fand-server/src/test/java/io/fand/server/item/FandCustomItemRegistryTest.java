package io.fand.server.item;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.fand.api.customitem.CustomItemType;
import io.fand.api.item.ItemStack;
import io.fand.api.item.ItemType;
import net.kyori.adventure.key.Key;
import org.junit.jupiter.api.Test;

class FandCustomItemRegistryTest {

    private static final ItemType DIAMOND = new TestItemType(Key.key("minecraft:diamond"), 64);

    @Test
    void registersCreatesAndIdentifiesCustomItems() {
        var registry = new FandCustomItemRegistry();
        var id = Key.key("test:hammer");
        registry.register(CustomItemType.of(id, new ItemStack(DIAMOND, 1)));

        var stack = registry.create(id, 3);

        assertThat(stack.amount()).isEqualTo(3);
        assertThat(registry.customId(stack)).contains(id);
        assertThat(registry.customItem(stack)).get().extracting(CustomItemType::id).isEqualTo(id);
    }

    @Test
    void unregisterRemovesTypeButLeavesStackTagReadable() {
        var registry = new FandCustomItemRegistry();
        var id = Key.key("test:hammer");
        var registration = registry.register(CustomItemType.of(id, new ItemStack(DIAMOND, 1)));
        var stack = registry.create(id, 1);

        registration.unregister();

        assertThat(registry.customId(stack)).contains(id);
        assertThat(registry.customItem(stack)).isEmpty();
        assertThatThrownBy(() -> registry.create(id, 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private record TestItemType(Key key, int maxStackSize) implements ItemType {
    }
}
