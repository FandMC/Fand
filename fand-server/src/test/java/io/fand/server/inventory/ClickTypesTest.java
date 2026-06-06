package io.fand.server.inventory;

import static org.assertj.core.api.Assertions.assertThat;

import io.fand.api.event.inventory.ClickType;
import net.minecraft.world.inventory.ContainerInput;
import org.junit.jupiter.api.Test;

class ClickTypesTest {

    @Test
    void mapsLeftAndRightPickup() {
        assertThat(ClickTypes.resolve(ContainerInput.PICKUP, 0, 0)).isEqualTo(ClickType.PICKUP);
        assertThat(ClickTypes.resolve(ContainerInput.PICKUP, 1, 0)).isEqualTo(ClickType.PICKUP_HALF);
    }

    @Test
    void mapsOutsideDropToDropAndDropAll() {
        assertThat(ClickTypes.resolve(ContainerInput.PICKUP, 0, ClickTypes.OUTSIDE_SLOT))
                .isEqualTo(ClickType.DROP_ALL);
        assertThat(ClickTypes.resolve(ContainerInput.PICKUP, 1, ClickTypes.OUTSIDE_SLOT))
                .isEqualTo(ClickType.DROP);
    }

    @Test
    void mapsQuickMoveBothButtons() {
        assertThat(ClickTypes.resolve(ContainerInput.QUICK_MOVE, 0, 5)).isEqualTo(ClickType.QUICK_MOVE);
        assertThat(ClickTypes.resolve(ContainerInput.QUICK_MOVE, 1, 5)).isEqualTo(ClickType.QUICK_MOVE_ALL);
    }

    @Test
    void mapsSwapHotbarVsOffhand() {
        assertThat(ClickTypes.resolve(ContainerInput.SWAP, 0, 5)).isEqualTo(ClickType.SWAP);
        assertThat(ClickTypes.resolve(ContainerInput.SWAP, 8, 5)).isEqualTo(ClickType.SWAP);
        assertThat(ClickTypes.resolve(ContainerInput.SWAP, 40, 5)).isEqualTo(ClickType.SWAP_OFFHAND);
    }

    @Test
    void mapsThrowToDropVariants() {
        assertThat(ClickTypes.resolve(ContainerInput.THROW, 0, 7)).isEqualTo(ClickType.DROP);
        assertThat(ClickTypes.resolve(ContainerInput.THROW, 1, 7)).isEqualTo(ClickType.DROP_ALL);
    }

    @Test
    void mapsRemainingInputs() {
        assertThat(ClickTypes.resolve(ContainerInput.CLONE, 0, 0)).isEqualTo(ClickType.CLONE);
        assertThat(ClickTypes.resolve(ContainerInput.QUICK_CRAFT, 0, 0)).isEqualTo(ClickType.QUICK_CRAFT);
        assertThat(ClickTypes.resolve(ContainerInput.PICKUP_ALL, 0, 0)).isEqualTo(ClickType.PICKUP_ALL);
    }

    @Test
    void hasMappingForEveryContainerInput() {
        for (ContainerInput input : ContainerInput.values()) {
            assertThat(ClickTypes.resolve(input, 0, 0))
                    .as("ClickTypes maps %s", input)
                    .isNotNull();
        }
    }
}
