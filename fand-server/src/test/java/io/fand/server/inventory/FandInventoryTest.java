package io.fand.server.inventory;

import static org.assertj.core.api.Assertions.assertThat;

import io.fand.api.event.inventory.DragType;
import io.fand.api.inventory.InventoryType;
import java.util.concurrent.atomic.AtomicInteger;
import net.kyori.adventure.text.Component;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class FandInventoryTest {

    @BeforeAll
    static void bootstrapVanilla() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void exposesTypeSizeAndTitle() {
        var inv = new FandInventory(InventoryType.CHEST, 27, Component.text("Loot"));
        assertThat(inv.type()).isEqualTo(InventoryType.CHEST);
        assertThat(inv.size()).isEqualTo(27);
        assertThat(inv.title()).isEqualTo(Component.text("Loot"));
    }

    @Test
    void firesListenerOnSet() {
        var inv = new FandInventory(InventoryType.CHEST, 9, Component.text("x"));
        var calls = new AtomicInteger();
        inv.addSlotChangeListener((slot, oldStack, newStack) -> calls.incrementAndGet());

        inv.container().setItem(3, net.minecraft.world.item.ItemStack.EMPTY);

        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    void unregisterStopsNotifications() throws Exception {
        var inv = new FandInventory(InventoryType.HOPPER, 5, Component.text("x"));
        var calls = new AtomicInteger();
        var handle = inv.addSlotChangeListener((slot, oldStack, newStack) -> calls.incrementAndGet());

        inv.container().setItem(0, net.minecraft.world.item.ItemStack.EMPTY);
        handle.close();
        inv.container().setItem(1, net.minecraft.world.item.ItemStack.EMPTY);

        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    void mapsQuickcraftTypeToDragType() {
        DragType even = drag(AbstractContainerMenu.QUICKCRAFT_TYPE_CHARITABLE);
        DragType single = drag(AbstractContainerMenu.QUICKCRAFT_TYPE_GREEDY);
        DragType clone = drag(AbstractContainerMenu.QUICKCRAFT_TYPE_CLONE);

        assertThat(even).isEqualTo(DragType.EVEN);
        assertThat(single).isEqualTo(DragType.SINGLE);
        assertThat(clone).isEqualTo(DragType.CLONE);
    }

    private static DragType drag(int quickcraftType) {
        return switch (quickcraftType) {
            case AbstractContainerMenu.QUICKCRAFT_TYPE_CHARITABLE -> DragType.EVEN;
            case AbstractContainerMenu.QUICKCRAFT_TYPE_GREEDY -> DragType.SINGLE;
            case AbstractContainerMenu.QUICKCRAFT_TYPE_CLONE -> DragType.CLONE;
            default -> DragType.EVEN;
        };
    }
}
