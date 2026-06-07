package io.fand.api.recipe;

/**
 * Vanilla recipe families exposed by Fand.
 */
public enum RecipeType {
    SHAPED,
    SHAPELESS,
    SMELTING,
    BLASTING,
    SMOKING,
    CAMPFIRE_COOKING,
    STONECUTTING,
    UNKNOWN;

    public boolean cooking() {
        return switch (this) {
            case SMELTING, BLASTING, SMOKING, CAMPFIRE_COOKING -> true;
            default -> false;
        };
    }
}
