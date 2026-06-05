package io.fand.server.command;

import com.mojang.serialization.JsonOps;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AdventureBridge {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdventureBridge.class);

    private AdventureBridge() {
    }

    public static Component toVanilla(net.kyori.adventure.text.Component message, @Nullable RegistryAccess registries) {
        var element = GsonComponentSerializer.gson().serializeToTree(message);
        var ops = registries == null ? JsonOps.INSTANCE : registries.createSerializationContext(JsonOps.INSTANCE);
        return ComponentSerialization.CODEC.parse(ops, element)
                .resultOrPartial(error -> {})
                .orElseThrow(() -> new IllegalStateException("Failed to convert Adventure component to vanilla: " + message));
    }

    /**
     * Like {@link #toVanilla} but returns {@code fallback} if the conversion fails for any reason
     * (including listener-supplied components that the vanilla codec rejects). Use on hot paths
     * where degrading gracefully is preferable to propagating the exception.
     */
    public static Component toVanillaOrFallback(
            net.kyori.adventure.text.Component message,
            Component fallback,
            @Nullable RegistryAccess registries
    ) {
        try {
            return toVanilla(message, registries);
        } catch (RuntimeException failure) {
            LOGGER.warn("Falling back to original vanilla component after conversion failure", failure);
            return fallback;
        }
    }

    public static net.kyori.adventure.text.Component fromVanilla(Component message, @Nullable RegistryAccess registries) {
        var ops = registries == null ? JsonOps.INSTANCE : registries.createSerializationContext(JsonOps.INSTANCE);
        var element = ComponentSerialization.CODEC.encodeStart(ops, message)
                .resultOrPartial(error -> {})
                .orElseThrow(() -> new IllegalStateException("Failed to convert vanilla component to Adventure: " + message));
        return GsonComponentSerializer.gson().deserializeFromTree(element);
    }
}
