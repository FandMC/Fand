package io.fand.api.entity;

import io.fand.api.item.component.CatSoundVariantKey;
import io.fand.api.item.component.CatVariantKey;
import io.fand.api.item.component.ItemDyeColor;
import net.kyori.adventure.key.Key;

public interface Cat extends Animal, Tameable {

    Key variant();

    void setVariant(Key variant);

    default void setVariant(CatVariantKey variant) {
        setVariant(variant.key());
    }

    Key soundVariant();

    void setSoundVariant(Key variant);

    default void setSoundVariant(CatSoundVariantKey variant) {
        setSoundVariant(variant.key());
    }

    ItemDyeColor collarColor();

    void setCollarColor(ItemDyeColor color);

    boolean lying();

    void setLying(boolean lying);

    boolean relaxed();

    void setRelaxed(boolean relaxed);

    boolean lyingOnSleepingPlayer();
}
