package io.fand.api.visibility;

import io.fand.api.entity.Player;

/**
 * Provider contract for player disguise state. Implementations can be
 * published through the cross-plugin service registry.
 */
public interface DisguiseService {

    boolean disguised(Player player);
}
