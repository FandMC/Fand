package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ServerboundSetJigsawBlockPacket}. */
public interface ServerboundSetJigsawBlockView extends PacketView {

    default Object pos() {
        return require("pos", Object.class);
    }
    default Object name() {
        return require("name", Object.class);
    }
    default Object target() {
        return require("target", Object.class);
    }
    default Object pool() {
        return require("pool", Object.class);
    }
    default String finalState() {
        return require("finalState", String.class);
    }
    default Object joint() {
        return require("joint", Object.class);
    }
    default int selectionPriority() {
        return require("selectionPriority", int.class);
    }
    default int placementPriority() {
        return require("placementPriority", int.class);
    }

    /** Returns a copy with {@code pos} replaced. */
    default ServerboundSetJigsawBlockView withPos(Object pos) {
        return (ServerboundSetJigsawBlockView) with("pos", pos);
    }
    /** Returns a copy with {@code name} replaced. */
    default ServerboundSetJigsawBlockView withName(Object name) {
        return (ServerboundSetJigsawBlockView) with("name", name);
    }
    /** Returns a copy with {@code target} replaced. */
    default ServerboundSetJigsawBlockView withTarget(Object target) {
        return (ServerboundSetJigsawBlockView) with("target", target);
    }
    /** Returns a copy with {@code pool} replaced. */
    default ServerboundSetJigsawBlockView withPool(Object pool) {
        return (ServerboundSetJigsawBlockView) with("pool", pool);
    }
    /** Returns a copy with {@code finalState} replaced. */
    default ServerboundSetJigsawBlockView withFinalState(String finalState) {
        return (ServerboundSetJigsawBlockView) with("finalState", finalState);
    }
    /** Returns a copy with {@code joint} replaced. */
    default ServerboundSetJigsawBlockView withJoint(Object joint) {
        return (ServerboundSetJigsawBlockView) with("joint", joint);
    }
    /** Returns a copy with {@code selectionPriority} replaced. */
    default ServerboundSetJigsawBlockView withSelectionPriority(int selectionPriority) {
        return (ServerboundSetJigsawBlockView) with("selectionPriority", selectionPriority);
    }
    /** Returns a copy with {@code placementPriority} replaced. */
    default ServerboundSetJigsawBlockView withPlacementPriority(int placementPriority) {
        return (ServerboundSetJigsawBlockView) with("placementPriority", placementPriority);
    }
}
