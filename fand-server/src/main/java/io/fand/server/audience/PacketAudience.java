package io.fand.server.audience;

import io.fand.server.command.AdventureBridge;
import java.util.OptionalLong;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.sound.SoundStop;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.TitlePart;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundClearTitlesPacket;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import org.jspecify.annotations.Nullable;

/**
 * Encodes Adventure {@link net.kyori.adventure.audience.Audience} verbs as
 * vanilla packets and sends them to a single {@link ServerPlayer}.
 *
 * <p>Stateless. All methods early-return when the player is disconnected; the
 * caller never has to null-check {@code connection}.
 */
public final class PacketAudience {

    private static final long DEFAULT_SEED = 0L;
    private static final int DEFAULT_FADE_IN = 10;
    private static final int DEFAULT_STAY = 70;
    private static final int DEFAULT_FADE_OUT = 20;

    private PacketAudience() {
    }

    public static void sendActionBar(ServerPlayer player, Component message) {
        send(player, new ClientboundSetActionBarTextPacket(toVanilla(player, message)));
    }

    public static void showTitle(ServerPlayer player, Title title) {
        send(player, new ClientboundSetTitleTextPacket(toVanilla(player, title.title())));
        send(player, new ClientboundSetSubtitleTextPacket(toVanilla(player, title.subtitle())));
        var times = title.times();
        send(player, new ClientboundSetTitlesAnimationPacket(
                times == null ? DEFAULT_FADE_IN : ticks(times.fadeIn().toMillis()),
                times == null ? DEFAULT_STAY : ticks(times.stay().toMillis()),
                times == null ? DEFAULT_FADE_OUT : ticks(times.fadeOut().toMillis())
        ));
    }

    @SuppressWarnings("unchecked")
    public static <T> void sendTitlePart(ServerPlayer player, TitlePart<T> part, T value) {
        if (part == TitlePart.TITLE) {
            send(player, new ClientboundSetTitleTextPacket(toVanilla(player, (Component) value)));
        } else if (part == TitlePart.SUBTITLE) {
            send(player, new ClientboundSetSubtitleTextPacket(toVanilla(player, (Component) value)));
        } else if (part == TitlePart.TIMES) {
            var times = (Title.Times) value;
            send(player, new ClientboundSetTitlesAnimationPacket(
                    ticks(times.fadeIn().toMillis()),
                    ticks(times.stay().toMillis()),
                    ticks(times.fadeOut().toMillis())
            ));
        }
    }

    public static void clearTitle(ServerPlayer player) {
        send(player, new ClientboundClearTitlesPacket(false));
    }

    public static void resetTitle(ServerPlayer player) {
        send(player, new ClientboundClearTitlesPacket(true));
    }

    public static void playSound(ServerPlayer player, Sound sound) {
        playSoundAt(player, sound, player.getX(), player.getY(), player.getZ());
    }

    public static void playSoundAt(ServerPlayer player, Sound sound, double x, double y, double z) {
        send(player, new ClientboundSoundPacket(
                resolveSound(sound),
                resolveSource(sound.source()),
                x,
                y,
                z,
                sound.volume(),
                sound.pitch(),
                seedOf(sound)
        ));
    }

    public static void stopSound(ServerPlayer player, SoundStop stop) {
        var sourceKey = stop.source();
        var soundKey = stop.sound();
        @Nullable SoundSource source = sourceKey == null ? null : resolveSource(sourceKey);
        @Nullable Identifier name = soundKey == null ? null : Identifier.parse(soundKey.asString());
        send(player, new ClientboundStopSoundPacket(name, source));
    }

    private static net.minecraft.network.chat.Component toVanilla(ServerPlayer player, Component message) {
        return AdventureBridge.toVanilla(message, player.registryAccess());
    }

    private static Holder<SoundEvent> resolveSound(Sound sound) {
        var event = SoundEvent.createVariableRangeEvent(Identifier.parse(sound.name().asString()));
        return Holder.direct(event);
    }

    static SoundSource resolveSource(Sound.Source source) {
        return switch (source) {
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

    static long seedOf(Sound sound) {
        OptionalLong seed = sound.seed();
        return seed.isPresent() ? seed.getAsLong() : DEFAULT_SEED;
    }

    static int ticks(long millis) {
        return (int) Math.max(0, millis / 50L);
    }

    private static void send(ServerPlayer player, Packet<?> packet) {
        var connection = player.connection;
        if (connection != null) {
            connection.send(packet);
        }
    }
}
