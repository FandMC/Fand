package io.fand.server.world;

import io.fand.api.item.ItemStack;
import io.fand.api.world.Particle;
import io.fand.api.world.ParticlePlayback;
import io.fand.server.item.FandItemStacks;
import com.mojang.brigadier.StringReader;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.commands.arguments.ParticleArgument;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ParticleEffects {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParticleEffects.class);
    private static final ConcurrentHashMap<String, Optional<ParticleOptions>> PARSE_CACHE = new ConcurrentHashMap<>();

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
        // Check if this is an ItemStack-based particle via reflection
        // We use reflection to avoid circular dependencies between API and server modules
        try {
            var stackField = particle.getClass().getDeclaredField("stack");
            stackField.setAccessible(true);
            var stack = (ItemStack) stackField.get(particle);
            if (stack != null && !stack.isEmpty()) {
                var vanillaStack = FandItemStacks.toVanilla(stack);
                var template = net.minecraft.world.item.ItemStackTemplate.fromNonEmptyStack(vanillaStack);
                return Optional.of(new ItemParticleOption(ParticleTypes.ITEM, template));
            }
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
            // Not an ItemParticleImpl, continue with normal parsing
        }

        // For non-ItemStack particles, use cached parsing with fail-fast
        String argument = particle.argument();
        return PARSE_CACHE.computeIfAbsent(argument, arg -> {
            try {
                return Optional.of(ParticleArgument.readParticle(new StringReader(arg), registries));
            } catch (Exception failure) {
                LOGGER.warn("Invalid particle argument '{}'; particle will be skipped at playback", arg, failure);
                return Optional.empty();
            }
        });
    }
}
