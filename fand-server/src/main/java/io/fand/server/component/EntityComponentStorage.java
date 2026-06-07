package io.fand.server.component;

import io.fand.api.component.DataComponentContainer;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;

public final class EntityComponentStorage {

    private EntityComponentStorage() {
    }

    public static DataComponentContainer container(MinecraftServer server, UUID uniqueId) {
        return new SavedDataComponentContainer(
                server,
                () -> server.getDataStorage().get(PersistentComponentData.entityType()),
                () -> server.getDataStorage().computeIfAbsent(PersistentComponentData.entityType()),
                uniqueId.toString());
    }
}
