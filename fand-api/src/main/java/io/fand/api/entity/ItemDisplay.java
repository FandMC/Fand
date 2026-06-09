package io.fand.api.entity;

import io.fand.api.item.ItemStack;
import java.util.Locale;

/**
 * Item display entity.
 */
public interface ItemDisplay extends Display {

    ItemStack displayedItem();

    void setDisplayedItem(ItemStack item);

    Transform itemTransform();

    void setItemTransform(Transform transform);

    enum Transform {
        NONE,
        THIRD_PERSON_LEFT_HAND,
        THIRD_PERSON_RIGHT_HAND,
        FIRST_PERSON_LEFT_HAND,
        FIRST_PERSON_RIGHT_HAND,
        HEAD,
        GUI,
        GROUND,
        FIXED,
        ON_SHELF;

        public String serializedName() {
            return switch (this) {
                case THIRD_PERSON_LEFT_HAND -> "thirdperson_lefthand";
                case THIRD_PERSON_RIGHT_HAND -> "thirdperson_righthand";
                case FIRST_PERSON_LEFT_HAND -> "firstperson_lefthand";
                case FIRST_PERSON_RIGHT_HAND -> "firstperson_righthand";
                default -> name().toLowerCase(Locale.ROOT);
            };
        }
    }
}
