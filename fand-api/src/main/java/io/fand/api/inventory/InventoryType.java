package io.fand.api.inventory;

/**
 * Coarse classification of a vanilla container by its menu type.
 * {@link #UNKNOWN} covers menus added by mods or future versions that this
 * mapping doesn't recognise.
 */
public enum InventoryType {
    PLAYER,
    CHEST,
    DISPENSER,
    DROPPER,
    FURNACE,
    BLAST_FURNACE,
    SMOKER,
    CRAFTING,
    CRAFTER,
    ANVIL,
    SMITHING,
    ENCHANTING,
    GRINDSTONE,
    BREWING,
    HOPPER,
    BEACON,
    LECTERN,
    LOOM,
    CARTOGRAPHY,
    STONECUTTER,
    SHULKER_BOX,
    MERCHANT,
    HORSE,
    UNKNOWN
}
