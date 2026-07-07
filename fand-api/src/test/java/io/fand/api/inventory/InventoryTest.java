package io.fand.api.inventory;

import static org.assertj.core.api.Assertions.assertThat;

import io.fand.api.item.ItemStack;
import io.fand.api.item.ItemType;
import java.util.Arrays;
import java.util.List;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

final class InventoryTest {

    private static final ItemType DIAMOND = new TestItemType(Key.key("minecraft:diamond"), 64);
    private static final ItemType DIRT = new TestItemType(Key.key("minecraft:dirt"), 64);

    @Test
    void exposesSlotContentsAndEmptyState() {
        var inventory = new TestInventory(3);
        inventory.set(1, DIAMOND.stack(3));

        assertThat(inventory.contents()).containsExactly(ItemStack.EMPTY, DIAMOND.stack(3), ItemStack.EMPTY);
        assertThat(inventory.firstEmpty()).isZero();
        assertThat(inventory.empty()).isFalse();

        inventory.clear();

        assertThat(inventory.empty()).isTrue();
        assertThat(inventory.firstEmpty()).isZero();
    }

    @Test
    void replacesContentsAndClearsRemainingSlots() {
        var inventory = new TestInventory(4);
        inventory.setContents(List.of(DIAMOND.stack(2), DIRT.stack(5)));

        assertThat(inventory.contents())
                .containsExactly(DIAMOND.stack(2), DIRT.stack(5), ItemStack.EMPTY, ItemStack.EMPTY);

        inventory.setContents(DIRT.stack(1), DIAMOND.stack(1), DIRT.stack(2), DIAMOND.stack(2), DIRT.stack(3));

        assertThat(inventory.contents())
                .containsExactly(DIRT.stack(1), DIAMOND.stack(1), DIRT.stack(2), DIAMOND.stack(2));
    }

    @Test
    void countsAndChecksItemsByTypeOrExactStack() {
        var namedDiamond = DIAMOND.stack(1).withCustomName(Component.text("named"));
        var inventory = new TestInventory(3);
        inventory.setContents(DIAMOND.stack(3), namedDiamond, DIRT.stack(4));

        assertThat(inventory.count(DIAMOND)).isEqualTo(4);
        assertThat(inventory.count(namedDiamond)).isOne();
        assertThat(inventory.contains(DIAMOND)).isTrue();
        assertThat(inventory.contains(namedDiamond)).isTrue();
        assertThat(inventory.contains(namedDiamond.withAmount(2))).isFalse();
        assertThat(inventory.contains(ItemStack.EMPTY)).isFalse();
    }

    @Test
    void removesItemsAcrossSlots() {
        var inventory = new TestInventory(4);
        inventory.setContents(DIAMOND.stack(64), DIRT.stack(2), DIAMOND.stack(10), DIAMOND.stack(1));

        assertThat(inventory.remove(DIAMOND, 70)).isEqualTo(70);
        assertThat(inventory.contents())
                .containsExactly(ItemStack.EMPTY, DIRT.stack(2), DIAMOND.stack(4), DIAMOND.stack(1));

        assertThat(inventory.remove(DIAMOND.stack(10))).isEqualTo(5);
        assertThat(inventory.empty()).isFalse();
        assertThat(inventory.count(DIAMOND)).isZero();
    }

    @Test
    void defaultViewersIsEmptySnapshot() {
        assertThat(new TestInventory(1).viewers()).isEmpty();
    }

    private record TestItemType(Key key, int maxStackSize) implements ItemType {
    }

    private static final class TestInventory implements Inventory {

        private final ItemStack[] slots;

        private TestInventory(int size) {
            this.slots = new ItemStack[size];
            Arrays.fill(slots, ItemStack.EMPTY);
        }

        @Override
        public InventoryType type() {
            return InventoryType.CHEST;
        }

        @Override
        public int size() {
            return slots.length;
        }

        @Override
        public ItemStack get(int slot) {
            return slots[slot];
        }

        @Override
        public void set(int slot, ItemStack stack) {
            slots[slot] = stack;
        }

        @Override
        public ItemStack add(ItemStack stack) {
            for (var slot = 0; slot < slots.length; slot++) {
                if (slots[slot].empty()) {
                    slots[slot] = stack;
                    return ItemStack.EMPTY;
                }
            }
            return stack;
        }

        @Override
        public void clear() {
            Arrays.fill(slots, ItemStack.EMPTY);
        }
    }
}
