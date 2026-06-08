package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ServerboundRecipeBookChangeSettingsPacket}. */
public interface ServerboundRecipeBookChangeSettingsView extends PacketView {

    default Object bookType() {
        return require("bookType", Object.class);
    }
    default boolean isOpen() {
        return require("isOpen", boolean.class);
    }
    default boolean isFiltering() {
        return require("isFiltering", boolean.class);
    }

    /** Returns a copy with {@code bookType} replaced. */
    default ServerboundRecipeBookChangeSettingsView withBookType(Object bookType) {
        return (ServerboundRecipeBookChangeSettingsView) with("bookType", bookType);
    }
    /** Returns a copy with {@code isOpen} replaced. */
    default ServerboundRecipeBookChangeSettingsView withIsOpen(boolean isOpen) {
        return (ServerboundRecipeBookChangeSettingsView) with("isOpen", isOpen);
    }
    /** Returns a copy with {@code isFiltering} replaced. */
    default ServerboundRecipeBookChangeSettingsView withIsFiltering(boolean isFiltering) {
        return (ServerboundRecipeBookChangeSettingsView) with("isFiltering", isFiltering);
    }
}
