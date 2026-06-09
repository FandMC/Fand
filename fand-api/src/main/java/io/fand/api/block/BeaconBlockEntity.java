package io.fand.api.block;

import java.util.Optional;
import net.kyori.adventure.key.Key;

/** Beacon block entity. */
public interface BeaconBlockEntity extends BlockEntity {

    int levels();

    Optional<Key> primaryEffect();

    boolean setPrimaryEffect(Key effect);

    void clearPrimaryEffect();

    Optional<Key> secondaryEffect();

    boolean setSecondaryEffect(Key effect);

    void clearSecondaryEffect();
}
