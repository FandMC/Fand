package io.fand.server.player;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.ReferenceCountUtil;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import org.jspecify.annotations.Nullable;

final class SimulatedPlayerConnection extends Connection {

    private static final SocketAddress ADDRESS = new InetSocketAddress("127.0.0.1", 0);

    private final EmbeddedChannel channel;
    private volatile boolean connected = true;
    private volatile boolean disconnectionHandled;
    private volatile @Nullable DisconnectionDetails details;

    SimulatedPlayerConnection() {
        super(PacketFlow.SERVERBOUND);
        this.channel = new EmbeddedChannel(new DiscardOutboundPackets(), this);
        fand$setRemoteAddress(ADDRESS);
    }

    @Override
    public void send(Packet<?> packet) {
    }

    @Override
    public void send(Packet<?> packet, @Nullable ChannelFutureListener listener) {
        if (listener != null) {
            try {
                listener.operationComplete(channel.newSucceededFuture());
            } catch (Exception ex) {
                throw new IllegalStateException("Simulated player packet listener failed", ex);
            }
        }
    }

    @Override
    public void send(Packet<?> packet, @Nullable ChannelFutureListener listener, boolean flush) {
        send(packet, listener);
    }

    @Override
    public void flushChannel() {
    }

    @Override
    public void disconnect(Component reason) {
        disconnect(new DisconnectionDetails(reason));
    }

    @Override
    public void disconnect(DisconnectionDetails details) {
        this.details = details;
        this.connected = false;
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    public boolean isConnecting() {
        return false;
    }

    @Override
    public @Nullable DisconnectionDetails getDisconnectionDetails() {
        return details;
    }

    @Override
    public void setReadOnly() {
    }

    @Override
    public void handleDisconnection() {
        if (connected || disconnectionHandled) {
            return;
        }
        disconnectionHandled = true;
        PacketListener listener = getPacketListener();
        if (listener != null) {
            listener.onDisconnect(details == null
                    ? new DisconnectionDetails(Component.literal("Simulated player disconnected"))
                    : details);
        }
    }

    private static final class DiscardOutboundPackets extends ChannelOutboundHandlerAdapter {

        @Override
        public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
            ReferenceCountUtil.release(msg);
            promise.setSuccess();
        }
    }
}
