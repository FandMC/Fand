package io.fand.server.world;

import io.fand.api.world.Particle;
import io.fand.api.world.ParticlePlayback;
import com.mojang.brigadier.StringReader;
import java.util.Optional;
import net.minecraft.commands.arguments.ParticleArgument;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ParticleEffects {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParticleEffects.class);

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
            if (vanillaParticle.isEmpty()) {
                return;
            }
            level.sendParticles(
                    vanillaParticle.get(),
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
        if (vanillaParticle.isEmpty()) {
            return;
        }
        player.connection.send(new ClientboundLevelParticlesPacket(
                vanillaParticle.get(),
                playback.force(),
                playback.force(),
                playback.x(), playback.y(), playback.z(),
                (float) playback.offsetX(), (float) playback.offsetY(), (float) playback.offsetZ(),
                (float) playback.speed(),
                playback.count()
        ));
    }

    private static Optional<ParticleOptions> resolveParticle(Particle particle, HolderLookup.Provider registries) {
        try {
            return Optional.of(ParticleArgument.readParticle(new StringReader(particle.argument()), registries));
        } catch (Exception failure) {
            LOGGER.warn("Invalid particle argument '{}'; particle playback skipped", particle.argument(), failure);
            return Optional.empty();
        }
    }
}
