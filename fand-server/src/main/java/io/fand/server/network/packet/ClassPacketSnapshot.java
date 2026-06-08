package io.fand.server.network.packet;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.world.item.ItemStack;

/**
 * Immutable snapshots of Class-based packets for copy-on-write semantics.
 * <p>
 * Class packets (non-record) have final fields and no canonical constructor pattern.
 * To enable {@code replace()}, we snapshot their state into a record at interception
 * time, expose the record as the PacketView, and rebuild the Class packet from the
 * snapshot on replace.
 * <p>
 * For packets with complex constructors or special requirements, define a dedicated
 * snapshot record here. For simple packets, {@link GenericClassPacketCodec} provides
 * automatic snapshotting via reflection.
 */
final class ClassPacketSnapshot {

    private ClassPacketSnapshot() {
    }

    /** Snapshot of {@link ClientboundSetHealthPacket}. */
    record SetHealth(float health, int food, float saturation) {
        Packet<?> rebuild() {
            return new ClientboundSetHealthPacket(health, food, saturation);
        }

        static SetHealth capture(ClientboundSetHealthPacket packet) {
            return new SetHealth(packet.getHealth(), packet.getFood(), packet.getSaturation());
        }
    }

    /** Snapshot of {@link ClientboundContainerSetSlotPacket}. */
    record ContainerSetSlot(int containerId, int stateId, int slot, ItemStack itemStack) {
        Packet<?> rebuild() {
            return new ClientboundContainerSetSlotPacket(containerId, stateId, slot, itemStack);
        }

        static ContainerSetSlot capture(ClientboundContainerSetSlotPacket packet) {
            return new ContainerSetSlot(
                    packet.getContainerId(),
                    packet.getStateId(),
                    packet.getSlot(),
                    packet.getItem()
            );
        }
    }
}
