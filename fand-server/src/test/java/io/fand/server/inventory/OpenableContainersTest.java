package io.fand.server.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.fand.api.inventory.InventoryType;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class OpenableContainersTest {

    @BeforeAll
    static void bootstrapVanilla() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void buildsChestForEachValidRow() {
        for (int rows = 1; rows <= 6; rows++) {
            var built = OpenableContainers.build(InventoryType.CHEST, rows * 9);
            assertThat(built).as("rows=%d", rows).isNotNull();
            assertThat(built.container().getContainerSize()).isEqualTo(rows * 9);
        }
    }

    @Test
    void chestDefaultSizeIsThreeRows() {
        var built = OpenableContainers.build(InventoryType.CHEST, 0);
        assertThat(built.container().getContainerSize()).isEqualTo(27);
    }

    @Test
    void chestRejectsInvalidSize() {
        assertThatThrownBy(() -> OpenableContainers.build(InventoryType.CHEST, 10))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> OpenableContainers.build(InventoryType.CHEST, 63))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void buildsFixedSizeTypes() {
        assertThat(OpenableContainers.build(InventoryType.HOPPER, 0).container().getContainerSize()).isEqualTo(5);
        assertThat(OpenableContainers.build(InventoryType.DISPENSER, 0).container().getContainerSize()).isEqualTo(9);
        assertThat(OpenableContainers.build(InventoryType.DROPPER, 0).container().getContainerSize()).isEqualTo(9);
        assertThat(OpenableContainers.build(InventoryType.SHULKER_BOX, 0).container().getContainerSize()).isEqualTo(27);
    }

    @Test
    void buildsVirtualPropertyMenus() {
        assertThat(OpenableContainers.build(InventoryType.ANVIL, 0).container().getContainerSize()).isEqualTo(3);
        assertThat(OpenableContainers.build(InventoryType.FURNACE, 0).container().getContainerSize()).isEqualTo(3);
        assertThat(OpenableContainers.build(InventoryType.BLAST_FURNACE, 0).container().getContainerSize()).isEqualTo(3);
        assertThat(OpenableContainers.build(InventoryType.SMOKER, 0).container().getContainerSize()).isEqualTo(3);
        assertThat(OpenableContainers.build(InventoryType.ENCHANTING, 0).container().getContainerSize()).isEqualTo(2);
        assertThat(OpenableContainers.build(InventoryType.BREWING, 0).container().getContainerSize()).isEqualTo(5);
    }

    @Test
    void returnsNullForUnsupportedBlockBackedTypes() {
        assertThat(OpenableContainers.build(InventoryType.BEACON, 0)).isNull();
    }

    @Test
    void returnsNullForPlayerAndUnknown() {
        assertThat(OpenableContainers.build(InventoryType.PLAYER, 0)).isNull();
        assertThat(OpenableContainers.build(InventoryType.UNKNOWN, 0)).isNull();
    }
}
