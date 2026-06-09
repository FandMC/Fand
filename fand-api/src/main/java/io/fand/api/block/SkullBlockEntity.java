package io.fand.api.block;

import java.util.Optional;
import net.kyori.adventure.key.Key;

/** Skull block entity. */
public interface SkullBlockEntity extends BlockEntity {

    Optional<Key> noteBlockSound();

    float animation();
}
