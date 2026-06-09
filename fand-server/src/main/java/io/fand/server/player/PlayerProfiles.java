package io.fand.server.player;

import io.fand.api.player.PlayerProfile;
import net.minecraft.server.players.NameAndId;

final class PlayerProfiles {

    private PlayerProfiles() {
    }

    static PlayerProfile fromVanilla(NameAndId profile) {
        return new PlayerProfile(profile.id(), profile.name());
    }

    static NameAndId toVanilla(PlayerProfile profile) {
        return new NameAndId(profile.uniqueId(), profile.name());
    }
}
