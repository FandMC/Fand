package io.fand.server.inventory;

import io.fand.api.inventory.InventoryType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.DispenserMenu;
import net.minecraft.world.inventory.HopperMenu;
import net.minecraft.world.inventory.ShulkerBoxMenu;

/**
 * Builds vanilla {@link MenuProvider}s for the subset of {@link InventoryType}s
 * that can be opened standalone (i.e. not tied to a recipe or block-state in
 * the world). Returns {@code null} for types that are not standalone-openable
 * — anvils, furnaces, beacons, etc. require a backing block.
 *
 * <p>Defaults the backing container to a fresh {@link SimpleContainer}, but
 * accepts an existing one (used by {@link FandInventory} so that listeners
 * stay attached to the same container across reopens).
 */
public final class OpenableContainers {

    private OpenableContainers() {
    }

    public static Built build(InventoryType type, int size) {
        return build(type, size, null, null);
    }

    public static Built build(InventoryType type, int size, Container preExisting, Component title) {
        return switch (type) {
            case CHEST -> chest(size, preExisting, title);
            case DISPENSER, DROPPER -> generic(
                    preExisting, 9,
                    (id, inv, container) -> new DispenserMenu(id, inv, container),
                    title != null ? title : Component.translatable("container.dispenser"));
            case HOPPER -> generic(
                    preExisting, 5,
                    (id, inv, container) -> new HopperMenu(id, inv, container),
                    title != null ? title : Component.translatable("container.hopper"));
            case SHULKER_BOX -> generic(
                    preExisting, 27,
                    (id, inv, container) -> new ShulkerBoxMenu(id, inv, container),
                    title != null ? title : Component.translatable("container.shulkerBox"));
            default -> null;
        };
    }

    private static Built generic(
            Container preExisting,
            int defaultSize,
            ContainerMenuFactory factory,
            Component title) {
        var container = preExisting != null ? preExisting : new SimpleContainer(defaultSize);
        return new Built(container, new SimpleMenuProvider(
                (id, inv, player) -> factory.create(id, inv, container), title));
    }

    private static Built chest(int requestedSize, Container preExisting, Component title) {
        int size = requestedSize == 0
                ? (preExisting != null ? preExisting.getContainerSize() : 27)
                : requestedSize;
        if (size <= 0 || size > 54 || size % 9 != 0) {
            throw new IllegalArgumentException(
                    "CHEST size must be a multiple of 9 in [9, 54], got " + requestedSize);
        }
        if (preExisting != null && preExisting.getContainerSize() != size) {
            throw new IllegalArgumentException(
                    "CHEST size " + size + " does not match container size " + preExisting.getContainerSize());
        }
        int rows = size / 9;
        var container = preExisting != null ? preExisting : new SimpleContainer(size);
        var menuType = switch (rows) {
            case 1 -> net.minecraft.world.inventory.MenuType.GENERIC_9x1;
            case 2 -> net.minecraft.world.inventory.MenuType.GENERIC_9x2;
            case 3 -> net.minecraft.world.inventory.MenuType.GENERIC_9x3;
            case 4 -> net.minecraft.world.inventory.MenuType.GENERIC_9x4;
            case 5 -> net.minecraft.world.inventory.MenuType.GENERIC_9x5;
            case 6 -> net.minecraft.world.inventory.MenuType.GENERIC_9x6;
            default -> throw new IllegalStateException("Unreachable rows=" + rows);
        };
        var resolvedTitle = title != null ? title : Component.translatable("container.chest");
        var provider = new SimpleMenuProvider(
                (id, inv, player) -> new ChestMenu(menuType, id, inv, container, rows),
                resolvedTitle);
        return new Built(container, provider);
    }

    @FunctionalInterface
    private interface ContainerMenuFactory {
        net.minecraft.world.inventory.AbstractContainerMenu create(
                int containerId, net.minecraft.world.entity.player.Inventory inventory, Container container);
    }

    public record Built(Container container, MenuProvider provider) {
    }
}
