package io.fand.server.inventory;

import static org.assertj.core.api.Assertions.assertThat;

import io.fand.api.inventory.InventoryType;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class InventoryTypesTest {

    @BeforeAll
    static void bootstrapVanilla() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void mapsKnownChestRows() {
        assertThat(InventoryTypes.resolve(MenuType.GENERIC_9x1)).isEqualTo(InventoryType.CHEST);
        assertThat(InventoryTypes.resolve(MenuType.GENERIC_9x6)).isEqualTo(InventoryType.CHEST);
    }

    @Test
    void mapsCommonStations() {
        assertThat(InventoryTypes.resolve(MenuType.ANVIL)).isEqualTo(InventoryType.ANVIL);
        assertThat(InventoryTypes.resolve(MenuType.FURNACE)).isEqualTo(InventoryType.FURNACE);
        assertThat(InventoryTypes.resolve(MenuType.BLAST_FURNACE)).isEqualTo(InventoryType.BLAST_FURNACE);
        assertThat(InventoryTypes.resolve(MenuType.SMOKER)).isEqualTo(InventoryType.SMOKER);
        assertThat(InventoryTypes.resolve(MenuType.CRAFTING)).isEqualTo(InventoryType.CRAFTING);
        assertThat(InventoryTypes.resolve(MenuType.BREWING_STAND)).isEqualTo(InventoryType.BREWING);
        assertThat(InventoryTypes.resolve(MenuType.HOPPER)).isEqualTo(InventoryType.HOPPER);
        assertThat(InventoryTypes.resolve(MenuType.MERCHANT)).isEqualTo(InventoryType.MERCHANT);
    }

    @Test
    void handlesMenusWithoutConstructableMenuType() {
        var menu = new UnknownMenu();

        assertThat(InventoryTypes.resolve(menu)).isEqualTo(InventoryType.UNKNOWN);
    }

    @Test
    void hasMappingForEveryRegisteredMenuType() {
        for (var type : net.minecraft.core.registries.BuiltInRegistries.MENU) {
            assertThat(InventoryTypes.resolve(type))
                    .as("MenuType %s maps to a non-UNKNOWN InventoryType", type)
                    .isNotEqualTo(InventoryType.UNKNOWN);
        }
    }

    private static final class UnknownMenu extends AbstractContainerMenu {

        private UnknownMenu() {
            super(null, 1);
        }

        @Override
        public ItemStack quickMoveStack(Player player, int slotIndex) {
            return ItemStack.EMPTY;
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }
    }
}
