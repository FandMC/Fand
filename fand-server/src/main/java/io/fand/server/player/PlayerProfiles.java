package io.fand.server.player;

import com.google.common.collect.ImmutableMultimap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import io.fand.api.player.PlayerProfile;
import io.fand.api.player.PlayerSkin;
import net.minecraft.server.players.NameAndId;
import org.jspecify.annotations.Nullable;

public final class PlayerProfiles {

    private static final String TEXTURES = "textures";

    private PlayerProfiles() {
    }

    public static PlayerProfile fromVanilla(NameAndId profile) {
        return new PlayerProfile(profile.id(), profile.name());
    }

    public static PlayerProfile fromVanilla(GameProfile profile) {
        return new PlayerProfile(profile.id(), profile.name(), skin(profile));
    }

    public static NameAndId toVanilla(PlayerProfile profile) {
        return new NameAndId(profile.uniqueId(), profile.name());
    }

    public static GameProfile toGameProfile(PlayerProfile profile) {
        var skin = profile.skinOrNull();
        if (skin == null) {
            return new GameProfile(profile.uniqueId(), profile.name());
        }
        return new GameProfile(profile.uniqueId(), profile.name(), textures(skin));
    }

    public static GameProfile applyTo(GameProfile original, PlayerProfile profile) {
        var properties = ImmutableMultimap.<String, Property>builder();
        original.properties().forEach((name, property) -> {
            if (!TEXTURES.equals(name)) {
                properties.put(name, property);
            }
        });
        var skin = profile.skinOrNull();
        if (skin != null) {
            properties.put(TEXTURES, new Property(TEXTURES, skin.value(), skin.signatureOrNull()));
        }
        return new GameProfile(profile.uniqueId(), profile.name(), new PropertyMap(properties.build()));
    }

    public static GameProfile withSkin(GameProfile profile, PlayerSkin skin) {
        return applyTo(profile, fromVanilla(profile).withSkin(skin));
    }

    public static GameProfile withoutSkin(GameProfile profile) {
        return applyTo(profile, fromVanilla(profile).withSkin(null));
    }

    private static @Nullable PlayerSkin skin(GameProfile profile) {
        var properties = profile.properties().get(TEXTURES);
        if (properties.isEmpty()) {
            return null;
        }
        var property = properties.iterator().next();
        return new PlayerSkin(property.value(), property.signature());
    }

    private static PropertyMap textures(PlayerSkin skin) {
        return new PropertyMap(ImmutableMultimap.of(
                TEXTURES,
                new Property(TEXTURES, skin.value(), skin.signatureOrNull())));
    }
}
