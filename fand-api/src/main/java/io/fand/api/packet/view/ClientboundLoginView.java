package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundLoginPacket}. */
public interface ClientboundLoginView extends PacketView {

    default int playerId() {
        return require("playerId", int.class);
    }
    default boolean hardcore() {
        return require("hardcore", boolean.class);
    }
    default Object levels() {
        return require("levels", Object.class);
    }
    default int maxPlayers() {
        return require("maxPlayers", int.class);
    }
    default int chunkRadius() {
        return require("chunkRadius", int.class);
    }
    default int simulationDistance() {
        return require("simulationDistance", int.class);
    }
    default boolean reducedDebugInfo() {
        return require("reducedDebugInfo", boolean.class);
    }
    default boolean showDeathScreen() {
        return require("showDeathScreen", boolean.class);
    }
    default boolean doLimitedCrafting() {
        return require("doLimitedCrafting", boolean.class);
    }
    default Object commonPlayerSpawnInfo() {
        return require("commonPlayerSpawnInfo", Object.class);
    }
    default boolean enforcesSecureChat() {
        return require("enforcesSecureChat", boolean.class);
    }

    /** Returns a copy with {@code playerId} replaced. */
    default ClientboundLoginView withPlayerId(int playerId) {
        return (ClientboundLoginView) with("playerId", playerId);
    }
    /** Returns a copy with {@code hardcore} replaced. */
    default ClientboundLoginView withHardcore(boolean hardcore) {
        return (ClientboundLoginView) with("hardcore", hardcore);
    }
    /** Returns a copy with {@code levels} replaced. */
    default ClientboundLoginView withLevels(Object levels) {
        return (ClientboundLoginView) with("levels", levels);
    }
    /** Returns a copy with {@code maxPlayers} replaced. */
    default ClientboundLoginView withMaxPlayers(int maxPlayers) {
        return (ClientboundLoginView) with("maxPlayers", maxPlayers);
    }
    /** Returns a copy with {@code chunkRadius} replaced. */
    default ClientboundLoginView withChunkRadius(int chunkRadius) {
        return (ClientboundLoginView) with("chunkRadius", chunkRadius);
    }
    /** Returns a copy with {@code simulationDistance} replaced. */
    default ClientboundLoginView withSimulationDistance(int simulationDistance) {
        return (ClientboundLoginView) with("simulationDistance", simulationDistance);
    }
    /** Returns a copy with {@code reducedDebugInfo} replaced. */
    default ClientboundLoginView withReducedDebugInfo(boolean reducedDebugInfo) {
        return (ClientboundLoginView) with("reducedDebugInfo", reducedDebugInfo);
    }
    /** Returns a copy with {@code showDeathScreen} replaced. */
    default ClientboundLoginView withShowDeathScreen(boolean showDeathScreen) {
        return (ClientboundLoginView) with("showDeathScreen", showDeathScreen);
    }
    /** Returns a copy with {@code doLimitedCrafting} replaced. */
    default ClientboundLoginView withDoLimitedCrafting(boolean doLimitedCrafting) {
        return (ClientboundLoginView) with("doLimitedCrafting", doLimitedCrafting);
    }
    /** Returns a copy with {@code commonPlayerSpawnInfo} replaced. */
    default ClientboundLoginView withCommonPlayerSpawnInfo(Object commonPlayerSpawnInfo) {
        return (ClientboundLoginView) with("commonPlayerSpawnInfo", commonPlayerSpawnInfo);
    }
    /** Returns a copy with {@code enforcesSecureChat} replaced. */
    default ClientboundLoginView withEnforcesSecureChat(boolean enforcesSecureChat) {
        return (ClientboundLoginView) with("enforcesSecureChat", enforcesSecureChat);
    }
}
