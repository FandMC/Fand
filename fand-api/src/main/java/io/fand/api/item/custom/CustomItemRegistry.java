package io.fand.api.item.custom;

import io.fand.api.item.ItemStack;
import io.fand.api.item.ItemType;
import io.fand.api.item.component.ItemComponents;
import java.util.Collection;
import java.util.Optional;
import net.kyori.adventure.key.Key;

/** Global custom-item identity registry. */
public interface CustomItemRegistry {

    CustomItemRegistration register(CustomItemType type);

    default CustomItemRegistration register(Key id, ItemType baseType, ItemComponents defaultComponents) {
        return register(new CustomItemType(id, baseType, defaultComponents));
    }

    Optional<CustomItemType> type(Key id);

    /** Point-in-time snapshot of all registered custom-item types; immutable. */
    Collection<CustomItemType> types();

    Optional<CustomItemType> customItem(ItemStack stack);

    Optional<Key> customId(ItemStack stack);

    ItemStack create(Key id, int amount);

    ItemStack tag(ItemStack stack, Key id);

    ItemStack untag(ItemStack stack);
}
