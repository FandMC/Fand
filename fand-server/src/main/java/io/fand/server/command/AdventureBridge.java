package io.fand.server.command;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import org.jspecify.annotations.Nullable;

public final class AdventureBridge {

    private AdventureBridge() {
    }

    public static Component toVanilla(net.kyori.adventure.text.Component message, @Nullable RegistryAccess registries) {
        var element = JsonParser.parseString(GsonComponentSerializer.gson().serialize(message));
        var ops = registries == null ? JsonOps.INSTANCE : registries.createSerializationContext(JsonOps.INSTANCE);
        return ComponentSerialization.CODEC.parse(ops, element)
                .resultOrPartial(error -> {})
                .orElseThrow(() -> new IllegalStateException("Failed to convert Adventure component to vanilla: " + message));
    }
}
