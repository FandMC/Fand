package io.fand.api.packet;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

/**
 * A typed, NMS-free view over a single packet's fields.
 *
 * <p>Every view implements this base — both the {@linkplain PacketType#viewType()
 * typed} sub-interfaces (e.g. {@code ClientboundSetEntityMotionView}) and the
 * plain {@code PacketView} handed out for packets that have no dedicated type.
 * Typed views only add compile-time convenience accessors on top of these
 * dynamic operations; reading {@code view.entityId()} and reading
 * {@code view.get("id", Integer.class)} hit the same underlying packet.
 *
 * <p>Views are immutable. {@link #with} returns a new view carrying the change;
 * the interceptor hands that result to {@link PacketController#replace}.
 * Changes chain: {@code view.with("a", 1).with("b", 2)}.
 *
 * <p><strong>Field names</strong> are the vanilla field names (record component
 * names, or declared field names for non-record packets). {@link #fieldNames}
 * lists them. <strong>Values</strong> are marshalled to friendly types:
 * primitives and {@code String} pass through; text becomes Adventure
 * {@link net.kyori.adventure.text.Component}; item stacks become
 * {@link io.fand.api.item.ItemStack}; positions become {@link Vec3d} /
 * {@link BlockPosition}; vanilla enums become their {@code String} name. Any
 * other field is opaque: it reads back only as {@code Object} and is preserved
 * unchanged through {@link #with}.
 */
public interface PacketView {

    /** The packet type this view wraps. */
    PacketType type();

    /** The names of the fields this view exposes, in declaration order. */
    Set<String> fieldNames();

    /** Whether {@code field} exists on this packet. */
    boolean has(String field);

    /**
     * Reads {@code field} as {@code type}.
     *
     * @return the value, or {@link Optional#empty()} if the field is absent,
     *         {@code null}, or its marshalled value is not assignable to
     *         {@code type}
     */
    <T> Optional<T> get(String field, Class<T> type);

    /**
     * Reads {@code field} as {@code type}, throwing if absent or mistyped.
     *
     * @throws NoSuchElementException if the field is absent, {@code null}, or
     *         not assignable to {@code type}
     */
    default <T> T require(String field, Class<T> type) {
        return get(field, type).orElseThrow(() -> new NoSuchElementException(
                "No " + type.getSimpleName() + " field '" + field + "' on " + type()));
    }

    /**
     * Whether this packet can be rebuilt with modified fields — i.e. whether
     * {@link #with} and {@link PacketController#replace} are supported. True for
     * record-based packets; false for the handful of packets whose vanilla
     * representation cannot be reconstructed from its fields (read-only).
     */
    boolean canReplace();

    /**
     * Returns a copy of this view with {@code field} set to {@code value}.
     * Unspecified fields are preserved unchanged, including opaque ones.
     *
     * @throws UnsupportedOperationException if {@link #canReplace()} is {@code false}
     * @throws IllegalArgumentException      if {@code field} does not exist or
     *                                       {@code value} cannot be marshalled
     *                                       onto it
     */
    PacketView with(String field, Object value);
}
