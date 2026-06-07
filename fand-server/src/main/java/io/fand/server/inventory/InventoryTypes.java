package io.fand.server.inventory;

import io.fand.api.inventory.InventoryType;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AbstractMountInventoryMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MenuType;

/**
 * Maps a vanilla {@link MenuType} to the public {@link InventoryType} enum
 * via its registry key. Unrecognised types resolve to
 * {@link InventoryType#UNKNOWN}.
 */
final class InventoryTypes {

    private static final Map<String, InventoryType> BY_PATH = Map.ofEntries(
            Map.entry("generic_9x1", InventoryType.CHEST),
            Map.entry("generic_9x2", InventoryType.CHEST),
            Map.entry("generic_9x3", InventoryType.CHEST),
            Map.entry("generic_9x4", InventoryType.CHEST),
            Map.entry("generic_9x5", InventoryType.CHEST),
            Map.entry("generic_9x6", InventoryType.CHEST),
            Map.entry("generic_3x3", InventoryType.DISPENSER),
            Map.entry("crafter_3x3", InventoryType.CRAFTER),
            Map.entry("anvil", InventoryType.ANVIL),
            Map.entry("beacon", InventoryType.BEACON),
            Map.entry("blast_furnace", InventoryType.BLAST_FURNACE),
            Map.entry("brewing_stand", InventoryType.BREWING),
            Map.entry("crafting", InventoryType.CRAFTING),
            Map.entry("enchantment", InventoryType.ENCHANTING),
            Map.entry("furnace", InventoryType.FURNACE),
            Map.entry("grindstone", InventoryType.GRINDSTONE),
            Map.entry("hopper", InventoryType.HOPPER),
            Map.entry("lectern", InventoryType.LECTERN),
            Map.entry("loom", InventoryType.LOOM),
            Map.entry("merchant", InventoryType.MERCHANT),
            Map.entry("shulker_box", InventoryType.SHULKER_BOX),
            Map.entry("smithing", InventoryType.SMITHING),
            Map.entry("smoker", InventoryType.SMOKER),
            Map.entry("cartography_table", InventoryType.CARTOGRAPHY),
            Map.entry("stonecutter", InventoryType.STONECUTTER));

    private InventoryTypes() {
    }

    static InventoryType resolve(AbstractContainerMenu menu) {
        if (menu instanceof InventoryMenu) {
            return InventoryType.PLAYER;
        }
        if (menu instanceof AbstractMountInventoryMenu) {
            return InventoryType.HORSE;
        }
        MenuType<?> type;
        try {
            type = menu.getType();
        } catch (UnsupportedOperationException ignored) {
            return InventoryType.UNKNOWN;
        }
        return resolve(type);
    }

    static InventoryType resolve(MenuType<?> type) {
        Identifier key = BuiltInRegistries.MENU.getKey(type);
        if (key == null) {
            return InventoryType.UNKNOWN;
        }
        return BY_PATH.getOrDefault(key.getPath(), InventoryType.UNKNOWN);
    }
}
