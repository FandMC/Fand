package io.fand.server.network.packet;

import io.fand.api.packet.PacketContext;
import io.fand.api.packet.PacketController;
import io.fand.api.packet.PacketType;
import io.fand.api.packet.PacketView;
import java.util.Objects;

final class PacketControllerImpl<T extends PacketView> implements PacketController<T> {

    private final PacketContext context;
    private final PacketType type;
    private T view;
    private boolean replaced;
    private boolean cancelled;

    PacketControllerImpl(PacketContext context, PacketType type, T view) {
        this.context = Objects.requireNonNull(context, "context");
        this.type = Objects.requireNonNull(type, "type");
        this.view = Objects.requireNonNull(view, "view");
        if (view.packetType() != type) {
            throw new IllegalArgumentException("Packet view type mismatch: " + view.packetType() + " != " + type);
        }
    }

    @Override
    public PacketContext context() {
        return context;
    }

    @Override
    public PacketType type() {
        return type;
    }

    @Override
    public T view() {
        return view;
    }

    @Override
    public void replace(T replacement) {
        Objects.requireNonNull(replacement, "replacement");
        if (replacement.packetType() != type) {
            throw new IllegalArgumentException("Replacement view type mismatch: " + replacement.packetType() + " != " + type);
        }
        this.view = replacement;
        this.replaced = true;
    }

    @Override
    public boolean replaced() {
        return replaced;
    }

    @Override
    public void cancel() {
        this.cancelled = true;
    }

    @Override
    public boolean cancelled() {
        return cancelled;
    }
}
