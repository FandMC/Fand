package io.fand.api.packet;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import net.kyori.adventure.key.Key;

/**
 * Registry for vanilla packet interceptors and custom payload channels.
 */
public interface PacketRegistry {

    Collection<PacketType> types();

    Optional<PacketType> type(PacketProtocol protocol, PacketDirection direction, Key key);

    default PlayerInfoPacketFactory playerInfo() {
        return PlayerInfoPacketFactory.unsupported();
    }

    default PacketSender sender() {
        return PacketSender.unsupported();
    }

    default ViewerIllusionService illusions() {
        return ViewerIllusionService.unsupported();
    }

    default PacketHelpers helpers() {
        return new PacketHelpers(this);
    }

    default PacketBuilder builder(PacketType type) {
        return helpers().builder(type);
    }

    default PacketView packet(PacketType type, Map<String, ?> fields) {
        throw new UnsupportedOperationException("Packet view creation is not supported");
    }

    PacketRegistration intercept(PacketType type, PacketInterceptor<PacketView> interceptor);

    default PacketRegistration intercept(
            PacketType type,
            PacketPriority priority,
            PacketInterceptor<PacketView> interceptor
    ) {
        if (priority != PacketPriority.NORMAL) {
            throw new UnsupportedOperationException("Packet interceptor priorities are not supported");
        }
        return intercept(type, interceptor);
    }

    <T extends PacketView> PacketRegistration intercept(
            PacketType type,
            Class<T> viewType,
            PacketInterceptor<T> interceptor);

    default <T extends PacketView> PacketRegistration intercept(
            PacketType type,
            Class<T> viewType,
            PacketPriority priority,
            PacketInterceptor<T> interceptor
    ) {
        if (priority != PacketPriority.NORMAL) {
            throw new UnsupportedOperationException("Packet interceptor priorities are not supported");
        }
        return intercept(type, viewType, interceptor);
    }

    /**
     * Registers a custom channel that this server may send to clients.
     */
    PacketRegistration register(CustomPacketDefinition definition);

    /**
     * Registers a custom channel and handles serverbound payloads for it.
     */
    PacketRegistration register(CustomPacketDefinition definition, CustomPacketHandler handler);
}
