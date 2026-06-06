package io.fand.server.world;

import io.fand.api.world.Particle;
import io.fand.api.world.ParticlePlayback;
import com.mojang.brigadier.StringReader;
import net.minecraft.commands.arguments.ParticleArgument;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class ParticleEffects {

    private ParticleEffects() {}

    public static void spawnParticle(ServerPlayer player, ParticlePlayback playback) {
        var server = player.level().getServer();
        if (server == null) {
            return;
        }
        Runnable run = () -> sendParticle(player, playback);
        if (server.isSameThread()) {
            run.run();
        } else {
            server.executeIfPossible(run);
        }
    }

    public static void spawnParticle(ServerLevel level, ParticlePlayback playback) {
        var server = level.getServer();
        if (server == null) {
            return;
        }
        Runnable run = () -> {
            var vanillaParticle = resolveParticle(playback.particle(), level.registryAccess());
            level.sendParticles(
                    vanillaParticle,
                    playback.force(),
                    playback.force(),
                    playback.x(), playback.y(), playback.z(),
                    playback.count(),
                    playback.offsetX(), playback.offsetY(), playback.offsetZ(),
                    playback.speed()
            );
        };
        if (server.isSameThread()) {
            run.run();
        } else {
            server.executeIfPossible(run);
        }
    }

    private static void sendParticle(ServerPlayer player, ParticlePlayback playback) {
        if (player.connection == null) {
            return;
        }
        var vanillaParticle = resolveParticle(playback.particle(), player.registryAccess());
        player.connection.send(new ClientboundLevelParticlesPacket(
                vanillaParticle,
                playback.force(),
                playback.force(),
                playback.x(), playback.y(), playback.z(),
                (float) playback.offsetX(), (float) playback.offsetY(), (float) playback.offsetZ(),
                (float) playback.speed(),
                playback.count()
        ));
    }

    private static ParticleOptions resolveParticle(Particle particle, HolderLookup.Provider registries) {
        try {
            return ParticleArgument.readParticle(new StringReader(particle.argument()), registries);
        } catch (Exception ignored) {
        }
        return ParticleTypes.POOF;
    }
}
