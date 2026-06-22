package io.fand.server.component;

import io.fand.api.persistence.PersistentDataContainer;
import io.fand.server.util.ServerThreading;
import java.util.Objects;
import java.util.UUID;
import net.kyori.adventure.key.Key;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;

public final class AdvancementDataStorage {

    private AdvancementDataStorage() {
    }

    public static PersistentDataContainer get(MinecraftServer server, UUID playerId, Key advancement) {
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(advancement, "advancement");
        return ServerThreading.callBlocking(server, () -> {
            var data = server.getDataStorage().get(PersistentComponentData.advancementType());
            if (data == null) {
                return PersistentDataContainer.EMPTY;
            }
            return new PersistentDataContainer(data.get(id(playerId, advancement)).toJson());
        });
    }

    public static void set(MinecraftServer server, UUID playerId, Key advancement, PersistentDataContainer container) {
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(advancement, "advancement");
        Objects.requireNonNull(container, "container");
        ServerThreading.callBlocking(server, () -> {
            var data = server.getDataStorage().computeIfAbsent(PersistentComponentData.advancementType());
            data.put(id(playerId, advancement), io.fand.api.component.DataComponentMap.fromJson(container.toJson()));
            return null;
        });
    }

    private static String id(UUID playerId, Key advancement) {
        return playerId + "/" + Identifier.fromNamespaceAndPath(advancement.namespace(), advancement.value());
    }
}
