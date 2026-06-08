package io.fand.api.packet.view;

import io.fand.api.packet.PacketView;
import java.util.UUID;

/** Typed view of {@link ClientboundUpdateMobEffectPacket}. */
public interface ClientboundUpdateMobEffectView extends PacketView {

    default int entityId() {
        return require("entityId", int.class);
    }
    default Object effect() {
        return require("effect", Object.class);
    }
    default int effectAmplifier() {
        return require("effectAmplifier", int.class);
    }
    default int effectDurationTicks() {
        return require("effectDurationTicks", int.class);
    }
    default byte flags() {
        return require("flags", byte.class);
    }

    /** Returns a copy with {@code entityId} replaced. */
    default ClientboundUpdateMobEffectView withEntityId(int entityId) {
        return (ClientboundUpdateMobEffectView) with("entityId", entityId);
    }
    /** Returns a copy with {@code effect} replaced. */
    default ClientboundUpdateMobEffectView withEffect(Object effect) {
        return (ClientboundUpdateMobEffectView) with("effect", effect);
    }
    /** Returns a copy with {@code effectAmplifier} replaced. */
    default ClientboundUpdateMobEffectView withEffectAmplifier(int effectAmplifier) {
        return (ClientboundUpdateMobEffectView) with("effectAmplifier", effectAmplifier);
    }
    /** Returns a copy with {@code effectDurationTicks} replaced. */
    default ClientboundUpdateMobEffectView withEffectDurationTicks(int effectDurationTicks) {
        return (ClientboundUpdateMobEffectView) with("effectDurationTicks", effectDurationTicks);
    }
    /** Returns a copy with {@code flags} replaced. */
    default ClientboundUpdateMobEffectView withFlags(byte flags) {
        return (ClientboundUpdateMobEffectView) with("flags", flags);
    }
}
