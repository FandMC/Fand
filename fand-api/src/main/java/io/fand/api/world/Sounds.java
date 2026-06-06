package io.fand.api.world;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.key.Key;

/**
 * Registry of common vanilla sound effects.
 *
 * <p>Sound instances are lazily constructed and cached. Custom sounds can be
 * obtained via {@link #sound(Key)}.
 */
public final class Sounds {

    private static final Map<Key, Sound> CACHE = new ConcurrentHashMap<>();

    private Sounds() {}

    public static Sound sound(Key key) {
        return CACHE.computeIfAbsent(key, SoundImpl::of);
    }

    public static Sound sound(String key) {
        return sound(Key.key(key));
    }

    public static Sound ENTITY_PLAYER_LEVELUP = sound(Key.key("minecraft:entity.player.levelup"));
    public static Sound ENTITY_EXPERIENCE_ORB_PICKUP = sound(Key.key("minecraft:entity.experience_orb.pickup"));
    public static Sound BLOCK_NOTE_BLOCK_PLING = sound(Key.key("minecraft:block.note_block.pling"));
    public static Sound BLOCK_ANVIL_USE = sound(Key.key("minecraft:block.anvil.use"));
    public static Sound ENTITY_VILLAGER_YES = sound(Key.key("minecraft:entity.villager.yes"));
    public static Sound ENTITY_VILLAGER_NO = sound(Key.key("minecraft:entity.villager.no"));
    public static Sound ENTITY_ENDERMAN_TELEPORT = sound(Key.key("minecraft:entity.enderman.teleport"));
    public static Sound ENTITY_GENERIC_EXPLODE = sound(Key.key("minecraft:entity.generic.explode"));
    public static Sound BLOCK_CHEST_OPEN = sound(Key.key("minecraft:block.chest.open"));
    public static Sound BLOCK_CHEST_CLOSE = sound(Key.key("minecraft:block.chest.close"));
    public static Sound ITEM_TOTEM_USE = sound(Key.key("minecraft:item.totem.use"));
    public static Sound ITEM_TRIDENT_THROW = sound(Key.key("minecraft:item.trident.throw"));
    public static Sound ITEM_TRIDENT_RETURN = sound(Key.key("minecraft:item.trident.return"));
    public static Sound ENTITY_ARROW_HIT_PLAYER = sound(Key.key("minecraft:entity.arrow.hit_player"));
    public static Sound ENTITY_PLAYER_HURT = sound(Key.key("minecraft:entity.player.hurt"));
    public static Sound ENTITY_PLAYER_DEATH = sound(Key.key("minecraft:entity.player.death"));
    public static Sound ENTITY_PLAYER_ATTACK_STRONG = sound(Key.key("minecraft:entity.player.attack.strong"));
    public static Sound ENTITY_PLAYER_ATTACK_WEAK = sound(Key.key("minecraft:entity.player.attack.weak"));
    public static Sound UI_BUTTON_CLICK = sound(Key.key("minecraft:ui.button.click"));
    public static Sound UI_TOAST_CHALLENGE_COMPLETE = sound(Key.key("minecraft:ui.toast.challenge_complete"));
    public static Sound MUSIC_DRAGON = sound(Key.key("minecraft:music.dragon"));
    public static Sound MUSIC_END = sound(Key.key("minecraft:music.end"));
    public static Sound MUSIC_NETHER_BASALT_DELTAS = sound(Key.key("minecraft:music.nether.basalt_deltas"));
    public static Sound AMBIENT_CAVE = sound(Key.key("minecraft:ambient.cave"));

    private record SoundImpl(Key key) implements Sound {
        static Sound of(Key key) {
            return new SoundImpl(key);
        }
    }
}
