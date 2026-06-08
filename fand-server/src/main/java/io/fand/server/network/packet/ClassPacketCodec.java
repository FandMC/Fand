package io.fand.server.network.packet;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;

/**
 * Codec for Class-based packets that snapshots fields into a record, then rebuilds
 * from the snapshot on {@code replace()}.
 *
 * @param <P> the vanilla packet type (a non-record class)
 * @param <S> the snapshot type (record or Object[])
 */
final class ClassPacketCodec<P extends Packet<?>, S> {

    private final Class<P> packetClass;
    private final SnapshotOps<P, S> ops;

    ClassPacketCodec(Class<P> packetClass, SnapshotOps<P, S> ops) {
        this.packetClass = packetClass;
        this.ops = ops;
    }

    Object read(Packet<?> packet) {
        if (!packetClass.isInstance(packet)) {
            throw new IllegalArgumentException("Expected " + packetClass.getName() + ", got " + packet.getClass());
        }
        return ops.capture(packetClass.cast(packet));
    }

    Packet<?> write(Packet<?> original, Object view) {
        if (!ops.snapshotClass().isInstance(view) && !(view instanceof Object[])) {
            throw new IllegalArgumentException("Expected snapshot of type " + ops.snapshotClass().getName());
        }
        @SuppressWarnings("unchecked")
        S snapshot = (S) view;
        return ops.rebuild(snapshot);
    }

    /** Operations for capturing and rebuilding a packet via its snapshot. */
    interface SnapshotOps<P extends Packet<?>, S> {
        S capture(P packet);

        Packet<?> rebuild(S snapshot);

        Class<?> snapshotClass();
    }

    // Factory methods for each supported packet
    static ClassPacketCodec<ClientboundSetHealthPacket, ClassPacketSnapshot.SetHealth> setHealth() {
        return new ClassPacketCodec<>(
                ClientboundSetHealthPacket.class,
                new SnapshotOps<>() {
                    @Override
                    public ClassPacketSnapshot.SetHealth capture(ClientboundSetHealthPacket packet) {
                        return ClassPacketSnapshot.SetHealth.capture(packet);
                    }

                    @Override
                    public Packet<?> rebuild(ClassPacketSnapshot.SetHealth snapshot) {
                        return snapshot.rebuild();
                    }

                    @Override
                    public Class<ClassPacketSnapshot.SetHealth> snapshotClass() {
                        return ClassPacketSnapshot.SetHealth.class;
                    }
                }
        );
    }

    static ClassPacketCodec<ClientboundContainerSetSlotPacket, ClassPacketSnapshot.ContainerSetSlot> containerSetSlot() {
        return new ClassPacketCodec<>(
                ClientboundContainerSetSlotPacket.class,
                new SnapshotOps<>() {
                    @Override
                    public ClassPacketSnapshot.ContainerSetSlot capture(ClientboundContainerSetSlotPacket packet) {
                        return ClassPacketSnapshot.ContainerSetSlot.capture(packet);
                    }

                    @Override
                    public Packet<?> rebuild(ClassPacketSnapshot.ContainerSetSlot snapshot) {
                        return snapshot.rebuild();
                    }

                    @Override
                    public Class<ClassPacketSnapshot.ContainerSetSlot> snapshotClass() {
                        return ClassPacketSnapshot.ContainerSetSlot.class;
                    }
                }
        );
    }
}
