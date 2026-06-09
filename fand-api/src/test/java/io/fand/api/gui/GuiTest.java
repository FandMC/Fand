package io.fand.api.gui;

import static org.assertj.core.api.Assertions.assertThat;

import io.fand.api.inventory.InventoryType;
import io.fand.api.item.ItemStack;
import io.fand.api.item.ItemType;
import java.util.List;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

class GuiTest {

    private static final ItemType DIAMOND = new TestItemType(Key.key("minecraft:diamond"), 64);

    @Test
    void buildsProtectedButtonsAndPages() {
        var one = new ItemStack(DIAMOND, 1);
        var two = new ItemStack(DIAMOND, 2);

        var gui = Gui.builder(InventoryType.CHEST, 9, Component.text("Menu"))
                .button(0, one, click -> { })
                .page(1, 2, 0, List.of(one, two))
                .build();

        assertThat(gui.size()).isEqualTo(9);
        assertThat(gui.protectedSlot(0)).isTrue();
        assertThat(gui.handles(0)).isTrue();
        assertThat(gui.item(1)).isEqualTo(one);
        assertThat(gui.item(2)).isEqualTo(two);
    }

    @Test
    void buildsTypedMenusWithInitialProperties() {
        var gui = Gui.anvil(Component.text("Repair"))
                .property(0, 5)
                .build();

        assertThat(gui.type()).isEqualTo(InventoryType.ANVIL);
        assertThat(gui.size()).isEqualTo(3);
        assertThat(gui.properties()).containsEntry(0, 5);
    }

    private record TestItemType(Key key, int maxStackSize) implements ItemType {
    }
}
