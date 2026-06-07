package io.fand.api.packet;

import io.fand.api.entity.Player;
import org.jspecify.annotations.Nullable;

/**
 * Controls the fate of a single intercepted packet.
 *
 * <p>Instances are valid only for the duration of the
 * {@link PacketInterceptor#intercept} call that received them and must not be
 * retained. A controller is bound to one packet on one connection; it is not
 * thread-safe and is only ever touched on the I/O thread that delivered the
 * packet.
 */
public interface PacketController {

    /**
     * The player this packet belongs to — the sender for an inbound packet, the
     * recipient for an outbound one. Returns {@code null} when the connection
     * has no player yet (status pings and the early login/configuration phases).
     */
    @Nullable Player player();

    /**
     * Drops the packet: it is neither processed (inbound) nor sent (outbound).
     * Once cancelled, {@link #replace} has no effect and later interceptors for
     * the same packet are skipped.
     */
    void cancel();

    /**
     * Substitutes a modified view for the original packet. Build {@code view}
     * with {@link PacketView#with}; the implementation rebuilds the vanilla
     * packet from it before it continues through the pipeline.
     *
     * <p>The last replacement wins if called more than once. Has no effect
     * after {@link #cancel}.
     *
     * @param view the modified view, typically {@code original.with(...)}
     * @throws UnsupportedOperationException if the packet
     *         {@linkplain PacketView#canReplace() cannot be replaced}
     */
    void replace(PacketView view);
}
