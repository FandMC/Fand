package io.fand.api.customitem;

import io.fand.api.item.ItemStack;
import java.util.Collection;
import java.util.Optional;
import net.kyori.adventure.key.Key;

/** Global custom-item identity registry. */
public interface CustomItemRegistry {

    CustomItemRegistration register(CustomItemType type);

    Optional<CustomItemType> type(Key id);

    Collection<CustomItemType> types();

    Optional<CustomItemType> customItem(ItemStack stack);

    Optional<Key> customId(ItemStack stack);

    ItemStack create(Key id, int amount);

    ItemStack tag(ItemStack stack, Key id);

    ItemStack untag(ItemStack stack);
}
