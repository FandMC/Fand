package io.fand.server.world;

import io.fand.api.world.Sound;
import io.fand.api.world.SoundPlayback;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

public final class SoundEffects {

    private SoundEffects() {}

    public static void playSound(ServerPlayer player, SoundPlayback playback) {
        var server = player.level().getServer();
        if (server == null) {
            return;
        }
        Runnable run = () -> sendSound(player, playback);
        if (server.isSameThread()) {
            run.run();
        } else {
            server.executeIfPossible(run);
        }
    }

    public static void playSound(ServerLevel level, SoundPlayback playback) {
        var vanillaSound = resolveSound(playback.sound());
        var source = resolveCategory(playback.category());
        var server = level.getServer();
        if (server == null) {
            return;
        }
        Runnable run = () -> {
            if (playback.minVolume() <= 0.0F) {
                level.playSeededSound(
                        null,
                        playback.x(), playback.y(), playback.z(),
                        vanillaSound,
                        source,
                        playback.volume(),
                        playback.pitch(),
                        playback.seed()
                );
                return;
            }
            for (var player : level.players()) {
                playSound(player, playback);
            }
        };
        if (server.isSameThread()) {
            run.run();
        } else {
            server.executeIfPossible(run);
        }
    }

    private static Holder<SoundEvent> resolveSound(io.fand.api.world.Sound sound) {
        var key = sound.key();
        var identifier = Identifier.fromNamespaceAndPath(key.namespace(), key.value());
        var event = BuiltInRegistries.SOUND_EVENT.getValue(identifier);
        return event == null
                ? Holder.direct(SoundEvent.createVariableRangeEvent(identifier))
                : BuiltInRegistries.SOUND_EVENT.wrapAsHolder(event);
    }

    private static Vec3 audiblePosition(ServerPlayer player, Holder<SoundEvent> sound, SoundPlayback playback) {
        var position = new Vec3(playback.x(), playback.y(), playback.z());
        var maxDistanceSquared = sound.value().getRange(playback.volume());
        maxDistanceSquared *= maxDistanceSquared;
        var deltaX = position.x() - player.getX();
        var deltaY = position.y() - player.getY();
        var deltaZ = position.z() - player.getZ();
        var distanceSquared = deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ;
        if (distanceSquared <= maxDistanceSquared) {
            return position;
        }
        if (playback.minVolume() <= 0.0F) {
            return null;
        }
        var distance = Math.sqrt(distanceSquared);
        return new Vec3(
                player.getX() + deltaX / distance * 2.0,
                player.getY() + deltaY / distance * 2.0,
                player.getZ() + deltaZ / distance * 2.0
        );
    }

    private static float effectiveVolume(ServerPlayer player, Holder<SoundEvent> sound, SoundPlayback playback) {
        var maxDistanceSquared = sound.value().getRange(playback.volume());
        maxDistanceSquared *= maxDistanceSquared;
        var deltaX = playback.x() - player.getX();
        var deltaY = playback.y() - player.getY();
        var deltaZ = playback.z() - player.getZ();
        var distanceSquared = deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ;
        return distanceSquared > maxDistanceSquared ? playback.minVolume() : playback.volume();
    }

    private static void sendSound(ServerPlayer player, SoundPlayback playback) {
        if (player.connection == null) {
            return;
        }
        var vanillaSound = resolveSound(playback.sound());
        var source = resolveCategory(playback.category());
        var position = audiblePosition(player, vanillaSound, playback);
        if (position == null) {
            return;
        }
        player.connection.send(new ClientboundSoundPacket(
                vanillaSound,
                source,
                position.x(), position.y(), position.z(),
                effectiveVolume(player, vanillaSound, playback),
                playback.pitch(),
                playback.seed()
        ));
    }

    private static SoundSource resolveCategory(Sound.Category category) {
        return switch (category) {
            case MASTER -> SoundSource.MASTER;
            case MUSIC -> SoundSource.MUSIC;
            case RECORDS -> SoundSource.RECORDS;
            case WEATHER -> SoundSource.WEATHER;
            case BLOCKS -> SoundSource.BLOCKS;
            case HOSTILE -> SoundSource.HOSTILE;
            case NEUTRAL -> SoundSource.NEUTRAL;
            case PLAYERS -> SoundSource.PLAYERS;
            case AMBIENT -> SoundSource.AMBIENT;
            case VOICE -> SoundSource.VOICE;
        };
    }
}
