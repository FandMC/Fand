package io.fand.api.entity;

import io.fand.api.item.component.ItemDyeColor;
import io.fand.api.item.component.WolfSoundVariantKey;
import io.fand.api.item.component.WolfVariantKey;
import net.kyori.adventure.key.Key;

public interface Wolf extends Animal, Tameable, Angerable {

    Key variant();

    void setVariant(Key variant);

    default void setVariant(WolfVariantKey variant) {
        setVariant(variant.key());
    }

    Key soundVariant();

    void setSoundVariant(Key variant);

    default void setSoundVariant(WolfSoundVariantKey variant) {
        setSoundVariant(variant.key());
    }

    ItemDyeColor collarColor();

    void setCollarColor(ItemDyeColor color);

    boolean interested();

    void setInterested(boolean interested);
}
