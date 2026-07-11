package io.fand.api.visibility;

import io.fand.api.entity.Player;

/**
 * Provider contract for player vanish state and viewer-specific visibility.
 * Implementations can be published through the cross-plugin service registry.
 */
public interface VanishService {

    boolean vanished(Player player);

    boolean canSee(Player viewer, Player target);
}
