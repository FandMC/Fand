package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundPlayerAbilitiesPacket}. */
public interface ClientboundPlayerAbilitiesView extends PacketView {

    default boolean invulnerable() {
        return require("invulnerable", boolean.class);
    }
    default boolean isFlying() {
        return require("isFlying", boolean.class);
    }
    default boolean canFly() {
        return require("canFly", boolean.class);
    }
    default boolean instabuild() {
        return require("instabuild", boolean.class);
    }
    default float flyingSpeed() {
        return require("flyingSpeed", float.class);
    }
    default float walkingSpeed() {
        return require("walkingSpeed", float.class);
    }

    /** Returns a copy with {@code invulnerable} replaced. */
    default ClientboundPlayerAbilitiesView withInvulnerable(boolean invulnerable) {
        return (ClientboundPlayerAbilitiesView) with("invulnerable", invulnerable);
    }
    /** Returns a copy with {@code isFlying} replaced. */
    default ClientboundPlayerAbilitiesView withIsFlying(boolean isFlying) {
        return (ClientboundPlayerAbilitiesView) with("isFlying", isFlying);
    }
    /** Returns a copy with {@code canFly} replaced. */
    default ClientboundPlayerAbilitiesView withCanFly(boolean canFly) {
        return (ClientboundPlayerAbilitiesView) with("canFly", canFly);
    }
    /** Returns a copy with {@code instabuild} replaced. */
    default ClientboundPlayerAbilitiesView withInstabuild(boolean instabuild) {
        return (ClientboundPlayerAbilitiesView) with("instabuild", instabuild);
    }
    /** Returns a copy with {@code flyingSpeed} replaced. */
    default ClientboundPlayerAbilitiesView withFlyingSpeed(float flyingSpeed) {
        return (ClientboundPlayerAbilitiesView) with("flyingSpeed", flyingSpeed);
    }
    /** Returns a copy with {@code walkingSpeed} replaced. */
    default ClientboundPlayerAbilitiesView withWalkingSpeed(float walkingSpeed) {
        return (ClientboundPlayerAbilitiesView) with("walkingSpeed", walkingSpeed);
    }
}
