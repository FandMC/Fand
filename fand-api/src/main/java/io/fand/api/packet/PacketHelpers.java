package io.fand.api.packet;

import io.fand.api.entity.EntityType;
import io.fand.api.world.Location;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Convenience factories for common clientbound packet views.
 */
public final class PacketHelpers {

    private final PacketRegistry registry;

    PacketHelpers(PacketRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    public PacketBuilder builder(PacketType type) {
        return new PacketBuilder(registry, type);
    }

    public PacketBuilder entityMetadata(int entityId) {
        return builder(PacketType.PLAY_CLIENTBOUND_SET_ENTITY_DATA)
                .field("id", entityId)
                .field("packedItems", List.of());
    }

    public PacketBuilder entityMetadata(int entityId, List<?> packedItems) {
        return entityMetadata(entityId).field("packedItems", List.copyOf(packedItems));
    }

    public PacketBuilder displayEntity(int entityId, UUID uniqueId, EntityType type, Location location) {
        Objects.requireNonNull(uniqueId, "uniqueId");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(location, "location");
        return builder(PacketType.PLAY_CLIENTBOUND_ADD_ENTITY)
                .field("id", entityId)
                .field("uuid", uniqueId)
                .field("type", type)
                .field("x", location.x())
                .field("y", location.y())
                .field("z", location.z())
                .field("movement", null)
                .field("xRot", location.pitch())
                .field("yRot", location.yaw())
                .field("yHeadRot", location.yaw())
                .field("data", 0);
    }

    public PacketBuilder hologramEntity(int entityId, UUID uniqueId, EntityType textDisplayType, Location location) {
        return displayEntity(entityId, uniqueId, textDisplayType, location);
    }

    public PacketBuilder scoreboardTeam(String teamName, Collection<String> entries, Optional<?> parameters, int method) {
        Objects.requireNonNull(entries, "entries");
        Objects.requireNonNull(parameters, "parameters");
        return builder(PacketType.PLAY_CLIENTBOUND_SET_PLAYER_TEAM)
                .field("name", Objects.requireNonNull(teamName, "teamName"))
                .field("players", List.copyOf(entries))
                .field("parameters", parameters)
                .field("method", method);
    }

    public PacketBuilder nameplateTeam(String teamName, Collection<String> entries, Object parameters) {
        return scoreboardTeam(teamName, entries, Optional.of(Objects.requireNonNull(parameters, "parameters")), 0);
    }

    public PacketBuilder openScreen(int containerId, Object menuType, Object title) {
        return builder(PacketType.PLAY_CLIENTBOUND_OPEN_SCREEN)
                .field("containerId", containerId)
                .field("type", Objects.requireNonNull(menuType, "menuType"))
                .field("title", Objects.requireNonNull(title, "title"));
    }
}
