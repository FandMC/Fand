package io.fand.api.packet;

@FunctionalInterface
public interface CustomPacketHandler {

    void handle(PacketContext context, CustomPacket packet);
}
