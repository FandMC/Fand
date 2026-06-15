package io.fand.api.enchantment;

import net.kyori.adventure.key.Key;

public interface EnchantmentRegistration extends AutoCloseable {

    Key key();

    boolean active();

    @Override
    void close();
}
