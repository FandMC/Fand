package io.fand.api.enchantment;

import io.fand.api.item.ItemStack;
import java.util.Optional;
import net.kyori.adventure.key.Key;

public interface EnchantmentRegistry {

    Optional<EnchantmentView> enchantment(Key key);

    /** Whether {@code enchantment} can be applied to the physical item represented by {@code item}. */
    default boolean supports(Key enchantment, ItemStack item) {
        return false;
    }

    /** Whether two enchantments may coexist on the same item. */
    default boolean compatible(Key first, Key second) {
        return first.equals(second);
    }

    default EnchantmentRegistration register(CustomEnchantment enchantment) {
        throw new UnsupportedOperationException("Custom enchantment registration is not supported");
    }

    static EnchantmentRegistry empty() {
        return key -> Optional.empty();
    }
}
