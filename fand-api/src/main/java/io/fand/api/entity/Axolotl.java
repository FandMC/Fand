package io.fand.api.entity;

public interface Axolotl extends Animal {

    enum Variant {
        LUCY,
        WILD,
        GOLD,
        CYAN,
        BLUE
    }

    Variant variant();

    void setVariant(Variant variant);

    boolean playingDead();

    void setPlayingDead(boolean playingDead);

    boolean fromBucket();

    void setFromBucket(boolean fromBucket);
}
