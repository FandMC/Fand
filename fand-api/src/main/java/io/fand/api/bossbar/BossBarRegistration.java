package io.fand.api.bossbar;

import net.kyori.adventure.key.Key;

/**
 * Registered keyed boss bar. Closing the registration hides the bar from all
 * viewers and removes it from the owning {@link BossBarService}.
 */
public interface BossBarRegistration extends BossBarHandle {

    Key key();
}
