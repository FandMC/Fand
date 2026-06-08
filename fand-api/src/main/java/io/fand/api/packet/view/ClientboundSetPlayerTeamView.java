package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundSetPlayerTeamPacket}. */
public interface ClientboundSetPlayerTeamView extends PacketView {

    default int method() {
        return require("method", int.class);
    }
    default String name() {
        return require("name", String.class);
    }
    default Object players() {
        return require("players", Object.class);
    }
    default Object parameters() {
        return require("parameters", Object.class);
    }
    default Object displayName() {
        return require("displayName", Object.class);
    }
    default Object playerPrefix() {
        return require("playerPrefix", Object.class);
    }
    default Object playerSuffix() {
        return require("playerSuffix", Object.class);
    }
    default Object nametagVisibility() {
        return require("nametagVisibility", Object.class);
    }
    default Object collisionRule() {
        return require("collisionRule", Object.class);
    }
    default Object color() {
        return require("color", Object.class);
    }
    default int options() {
        return require("options", int.class);
    }

    /** Returns a copy with {@code method} replaced. */
    default ClientboundSetPlayerTeamView withMethod(int method) {
        return (ClientboundSetPlayerTeamView) with("method", method);
    }
    /** Returns a copy with {@code name} replaced. */
    default ClientboundSetPlayerTeamView withName(String name) {
        return (ClientboundSetPlayerTeamView) with("name", name);
    }
    /** Returns a copy with {@code players} replaced. */
    default ClientboundSetPlayerTeamView withPlayers(Object players) {
        return (ClientboundSetPlayerTeamView) with("players", players);
    }
    /** Returns a copy with {@code parameters} replaced. */
    default ClientboundSetPlayerTeamView withParameters(Object parameters) {
        return (ClientboundSetPlayerTeamView) with("parameters", parameters);
    }
    /** Returns a copy with {@code displayName} replaced. */
    default ClientboundSetPlayerTeamView withDisplayName(Object displayName) {
        return (ClientboundSetPlayerTeamView) with("displayName", displayName);
    }
    /** Returns a copy with {@code playerPrefix} replaced. */
    default ClientboundSetPlayerTeamView withPlayerPrefix(Object playerPrefix) {
        return (ClientboundSetPlayerTeamView) with("playerPrefix", playerPrefix);
    }
    /** Returns a copy with {@code playerSuffix} replaced. */
    default ClientboundSetPlayerTeamView withPlayerSuffix(Object playerSuffix) {
        return (ClientboundSetPlayerTeamView) with("playerSuffix", playerSuffix);
    }
    /** Returns a copy with {@code nametagVisibility} replaced. */
    default ClientboundSetPlayerTeamView withNametagVisibility(Object nametagVisibility) {
        return (ClientboundSetPlayerTeamView) with("nametagVisibility", nametagVisibility);
    }
    /** Returns a copy with {@code collisionRule} replaced. */
    default ClientboundSetPlayerTeamView withCollisionRule(Object collisionRule) {
        return (ClientboundSetPlayerTeamView) with("collisionRule", collisionRule);
    }
    /** Returns a copy with {@code color} replaced. */
    default ClientboundSetPlayerTeamView withColor(Object color) {
        return (ClientboundSetPlayerTeamView) with("color", color);
    }
    /** Returns a copy with {@code options} replaced. */
    default ClientboundSetPlayerTeamView withOptions(int options) {
        return (ClientboundSetPlayerTeamView) with("options", options);
    }
}
