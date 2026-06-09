package io.fand.server.network.packet;

import io.fand.api.packet.PacketType;
import io.fand.api.packet.PacketView;
import java.util.Map;

public final class PacketViewFactory {

    public PacketView view(PacketType type, Map<String, ?> fields) {
        return dynamic(type, fields).as(type.viewType());
    }

    public DynamicPacketView dynamic(PacketType type, Map<String, ?> fields) {
        return new DynamicPacketView(type, fields);
    }
}
