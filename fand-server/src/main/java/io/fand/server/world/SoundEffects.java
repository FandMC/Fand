package io.fand.server.world;

import io.fand.api.world.Location;
import io.fand.api.world.sound.SoundCategory;
import io.fand.api.world.sound.SoundEffect;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.key.Key;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

public final class SoundEffects {

    private static final long DEFAULT_SEED = 0L;
    private static final ConcurrentHashMap<Key, Holder<SoundEvent>> SOUND_HOLDERS = new ConcurrentHashMap<>();

    private SoundEffects() {
    }

    public static void play(ServerLevel level, Location location, SoundEffect sound) {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(sound, "sound");
        level.playSeededSound(
                null,
                location.x(),
                location.y(),
                location.z(),
                resolveSound(sound.key()),
                resolveSource(sound.category()),
                sound.volume(),
                sound.pitch(),
                seedOf(sound));
    }

    public static void playTo(ServerPlayer player, Location location, SoundEffect sound) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(sound, "sound");
        var connection = player.connection;
        if (connection != null) {
            connection.send(new ClientboundSoundPacket(
                    resolveSound(sound.key()),
                    resolveSource(sound.category()),
                    location.x(),
                    location.y(),
                    location.z(),
                    sound.volume(),
                    sound.pitch(),
                    seedOf(sound)));
        }
    }

    public static SoundSource resolveSource(SoundCategory category) {
        return switch (category) {
            case MASTER -> SoundSource.MASTER;
            case MUSIC -> SoundSource.MUSIC;
            case RECORD -> SoundSource.RECORDS;
            case WEATHER -> SoundSource.WEATHER;
            case BLOCK -> SoundSource.BLOCKS;
            case HOSTILE -> SoundSource.HOSTILE;
            case NEUTRAL -> SoundSource.NEUTRAL;
            case PLAYER -> SoundSource.PLAYERS;
            case AMBIENT -> SoundSource.AMBIENT;
            case VOICE -> SoundSource.VOICE;
        };
    }

    static long seedOf(SoundEffect sound) {
        var seed = sound.seed();
        return seed.isPresent() ? seed.getAsLong() : DEFAULT_SEED;
    }

    private static Holder<SoundEvent> resolveSound(Key key) {
        return SOUND_HOLDERS.computeIfAbsent(key, current ->
                Holder.direct(SoundEvent.createVariableRangeEvent(id(current))));
    }

    private static Identifier id(Key key) {
        return Identifier.fromNamespaceAndPath(key.namespace(), key.value());
    }
}
