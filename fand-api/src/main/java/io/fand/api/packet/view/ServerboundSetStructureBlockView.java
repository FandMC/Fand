package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ServerboundSetStructureBlockPacket}. */
public interface ServerboundSetStructureBlockView extends PacketView {

    default Object pos() {
        return require("pos", Object.class);
    }
    default Object updateType() {
        return require("updateType", Object.class);
    }
    default Object mode() {
        return require("mode", Object.class);
    }
    default String name() {
        return require("name", String.class);
    }
    default Object offset() {
        return require("offset", Object.class);
    }
    default Object size() {
        return require("size", Object.class);
    }
    default Object mirror() {
        return require("mirror", Object.class);
    }
    default Object rotation() {
        return require("rotation", Object.class);
    }
    default String data() {
        return require("data", String.class);
    }
    default boolean ignoreEntities() {
        return require("ignoreEntities", boolean.class);
    }
    default boolean strict() {
        return require("strict", boolean.class);
    }
    default boolean showAir() {
        return require("showAir", boolean.class);
    }
    default boolean showBoundingBox() {
        return require("showBoundingBox", boolean.class);
    }
    default float integrity() {
        return require("integrity", float.class);
    }
    default long seed() {
        return require("seed", long.class);
    }

    /** Returns a copy with {@code pos} replaced. */
    default ServerboundSetStructureBlockView withPos(Object pos) {
        return (ServerboundSetStructureBlockView) with("pos", pos);
    }
    /** Returns a copy with {@code updateType} replaced. */
    default ServerboundSetStructureBlockView withUpdateType(Object updateType) {
        return (ServerboundSetStructureBlockView) with("updateType", updateType);
    }
    /** Returns a copy with {@code mode} replaced. */
    default ServerboundSetStructureBlockView withMode(Object mode) {
        return (ServerboundSetStructureBlockView) with("mode", mode);
    }
    /** Returns a copy with {@code name} replaced. */
    default ServerboundSetStructureBlockView withName(String name) {
        return (ServerboundSetStructureBlockView) with("name", name);
    }
    /** Returns a copy with {@code offset} replaced. */
    default ServerboundSetStructureBlockView withOffset(Object offset) {
        return (ServerboundSetStructureBlockView) with("offset", offset);
    }
    /** Returns a copy with {@code size} replaced. */
    default ServerboundSetStructureBlockView withSize(Object size) {
        return (ServerboundSetStructureBlockView) with("size", size);
    }
    /** Returns a copy with {@code mirror} replaced. */
    default ServerboundSetStructureBlockView withMirror(Object mirror) {
        return (ServerboundSetStructureBlockView) with("mirror", mirror);
    }
    /** Returns a copy with {@code rotation} replaced. */
    default ServerboundSetStructureBlockView withRotation(Object rotation) {
        return (ServerboundSetStructureBlockView) with("rotation", rotation);
    }
    /** Returns a copy with {@code data} replaced. */
    default ServerboundSetStructureBlockView withData(String data) {
        return (ServerboundSetStructureBlockView) with("data", data);
    }
    /** Returns a copy with {@code ignoreEntities} replaced. */
    default ServerboundSetStructureBlockView withIgnoreEntities(boolean ignoreEntities) {
        return (ServerboundSetStructureBlockView) with("ignoreEntities", ignoreEntities);
    }
    /** Returns a copy with {@code strict} replaced. */
    default ServerboundSetStructureBlockView withStrict(boolean strict) {
        return (ServerboundSetStructureBlockView) with("strict", strict);
    }
    /** Returns a copy with {@code showAir} replaced. */
    default ServerboundSetStructureBlockView withShowAir(boolean showAir) {
        return (ServerboundSetStructureBlockView) with("showAir", showAir);
    }
    /** Returns a copy with {@code showBoundingBox} replaced. */
    default ServerboundSetStructureBlockView withShowBoundingBox(boolean showBoundingBox) {
        return (ServerboundSetStructureBlockView) with("showBoundingBox", showBoundingBox);
    }
    /** Returns a copy with {@code integrity} replaced. */
    default ServerboundSetStructureBlockView withIntegrity(float integrity) {
        return (ServerboundSetStructureBlockView) with("integrity", integrity);
    }
    /** Returns a copy with {@code seed} replaced. */
    default ServerboundSetStructureBlockView withSeed(long seed) {
        return (ServerboundSetStructureBlockView) with("seed", seed);
    }
}
